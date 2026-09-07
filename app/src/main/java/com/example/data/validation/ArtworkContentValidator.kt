package com.example.data.validation

import android.graphics.Bitmap
import com.example.data.Artwork
import com.example.data.RegionIdRaster
import java.util.ArrayDeque

/**
 * Production Content Validator enforcing strict P0 geometric, topological,
 * and anchor correctness rules.
 */
object ArtworkContentValidator {

    sealed class ValidationResult {
        object Pass : ValidationResult()
        data class Fail(val violations: List<String>) : ValidationResult()
    }

    fun validate(
        artwork: Artwork,
        cellIdData: IntArray,
        mapWidth: Int,
        mapHeight: Int,
        subjectMask: Bitmap? = null
    ): ValidationResult {
        val violations = ArrayList<String>()

        // MAP-001: cell_id_map dimensions == artwork canvas dimensions
        if (mapWidth != artwork.canvas.width || mapHeight != artwork.canvas.height) {
            violations.add("MAP-001: cell_id_map dimensions (${mapWidth}x${mapHeight}) do not match artwork canvas (${artwork.canvas.width}x${artwork.canvas.height})")
        }

        val rasterCellIds = HashSet<Int>()
        val cellPixelsMap = HashMap<Int, ArrayList<Int>>()
        val regionPixelsMap = HashMap<Int, ArrayList<Int>>()
        val totalPixels = mapWidth * mapHeight

        for (i in 0 until totalPixels) {
            val cid = cellIdData[i]
            if (cid > 0) {
                rasterCellIds.add(cid)
                var cList = cellPixelsMap[cid]
                if (cList == null) {
                    cList = ArrayList()
                    cellPixelsMap[cid] = cList
                }
                cList.add(i)

                val cell = artwork.cellMap[cid]
                if (cell != null) {
                    var rList = regionPixelsMap[cell.regionId]
                    if (rList == null) {
                        rList = ArrayList()
                        regionPixelsMap[cell.regionId] = rList
                    }
                    rList.add(i)
                }
            }
        }

        // MAP-002: Every non-zero raster Cell-ID exists in artwork.json
        for (cid in rasterCellIds) {
            if (!artwork.cellMap.containsKey(cid)) {
                violations.add("MAP-002: Raster Cell-ID $cid does not exist in artwork.json")
            }
        }

        // MAP-003: Every JSON Cell-ID occurs in the raster
        for (cell in artwork.cells) {
            if (!rasterCellIds.contains(cell.id)) {
                violations.add("MAP-003: JSON Cell-ID ${cell.id} does not occur in raster cell_id_map")
            }
        }

        // MAP-004: Every Cell references an existing Region
        for (cell in artwork.cells) {
            if (!artwork.regionMap.containsKey(cell.regionId)) {
                violations.add("MAP-004: Cell ${cell.id} references non-existent region ${cell.regionId}")
            }
        }

        // MAP-005: Every Region references valid Cells
        for (region in artwork.regions) {
            for (cid in region.cellIds) {
                if (!artwork.cellMap.containsKey(cid)) {
                    violations.add("MAP-005: Region ${region.id} references non-existent Cell $cid")
                }
            }
        }

        // MAP-006: Cell -> Region and Region -> Cell relationships are reciprocal
        for (cell in artwork.cells) {
            val region = artwork.regionMap[cell.regionId]
            if (region != null && !region.cellIds.contains(cell.id)) {
                violations.add("MAP-006: Cell ${cell.id} references region ${cell.regionId}, but region's cellIds does not contain ${cell.id}")
            }
        }
        for (region in artwork.regions) {
            for (cid in region.cellIds) {
                val cell = artwork.cellMap[cid]
                if (cell != null && cell.regionId != region.id) {
                    violations.add("MAP-006: Region ${region.id} lists cell $cid, but cell's regionId is ${cell.regionId}")
                }
            }
        }

        // MAP-007: Cell.colorId == Region.colorId
        for (cell in artwork.cells) {
            val region = artwork.regionMap[cell.regionId]
            if (region != null && cell.colorId != region.colorId) {
                violations.add("MAP-007: Cell ${cell.id} colorId (${cell.colorId}) does not match Region ${region.id} colorId (${region.colorId})")
            }
        }

        val regionRaster = RegionIdRaster.fromCellIdRaster(mapWidth, mapHeight, cellIdData, artwork.cellMap)

        // ANCHOR-001 & ANCHOR-003: Cell Anchor validation
        for (cell in artwork.cells) {
            val anchor = cell.safeAnchor
            if (anchor == null) {
                violations.add("ANCHOR-001: Cell ${cell.id} is missing safeAnchor")
            } else {
                val px = anchor.x.toInt()
                val py = anchor.y.toInt()
                if (px < 0 || px >= mapWidth || py < 0 || py >= mapHeight) {
                    violations.add("ANCHOR-003: Cell ${cell.id} safeAnchor (${anchor.x}, ${anchor.y}) is out of canvas bounds")
                } else {
                    val actualCid = cellIdData[py * mapWidth + px]
                    if (actualCid != cell.id) {
                        violations.add("ANCHOR-001: Cell ${cell.id} safeAnchor at ($px, $py) resolves to Cell $actualCid instead of ${cell.id}")
                    }
                }
            }
        }

        // ANCHOR-002 & ANCHOR-003: Region Anchor validation
        for (region in artwork.regions) {
            val anchor = region.safeAnchor
            if (anchor == null) {
                violations.add("ANCHOR-002: Region ${region.id} is missing safeAnchor")
            } else {
                val px = anchor.x.toInt()
                val py = anchor.y.toInt()
                if (px < 0 || px >= mapWidth || py < 0 || py >= mapHeight) {
                    violations.add("ANCHOR-003: Region ${region.id} safeAnchor (${anchor.x}, ${anchor.y}) is out of canvas bounds")
                } else {
                    val actualRid = regionRaster.getRegionId(px, py)
                    if (actualRid != region.id) {
                        violations.add("ANCHOR-002: Region ${region.id} safeAnchor at ($px, $py) resolves to Region $actualRid instead of ${region.id}")
                    }
                }
            }
        }

        // SUBJECT-001: Playable Cell pixels consistency with subject_mask
        if (subjectMask != null && subjectMask.width == mapWidth && subjectMask.height == mapHeight) {
            var maskViolations = 0
            for (y in 0 until mapHeight) {
                for (x in 0 until mapWidth) {
                    val cid = cellIdData[y * mapWidth + x]
                    if (cid > 0) {
                        val maskPixel = subjectMask.getPixel(x, y)
                        val alpha = (maskPixel ushr 24) and 0xFF
                        val r = (maskPixel ushr 16) and 0xFF
                        val g = (maskPixel ushr 8) and 0xFF
                        val b = maskPixel and 0xFF
                        // If completely transparent or pitch black when mask is binary
                        if (alpha == 0 && (r + g + b == 0)) {
                            maskViolations++
                        }
                    }
                }
            }
            if (maskViolations > 10) { // allow negligible antialiasing fringe
                violations.add("SUBJECT-001: $maskViolations playable cell pixels fall outside subject_mask")
            }
        }

        // REGION-001: Region connectivity (4-orthogonal connectivity check)
        for (region in artwork.regions) {
            val regPixels = regionPixelsMap[region.id] ?: emptyList<Int>()

            if (regPixels.isNotEmpty()) {
                val regSet = HashSet(regPixels)
                val visited = HashSet<Int>()
                val queue = ArrayDeque<Int>()
                val start = regPixels[0]
                queue.add(start)
                visited.add(start)

                while (!queue.isEmpty()) {
                    val p = queue.removeFirst()
                    val x = p % mapWidth
                    val y = p / mapWidth

                    val neighbors = intArrayOf(
                        if (x > 0) p - 1 else -1,
                        if (x < mapWidth - 1) p + 1 else -1,
                        if (y > 0) p - mapWidth else -1,
                        if (y < mapHeight - 1) p + mapWidth else -1
                    )
                    for (n in neighbors) {
                        if (n != -1 && regSet.contains(n) && visited.add(n)) {
                            queue.add(n)
                        }
                    }
                }

                if (visited.size != regPixels.size) {
                    violations.add("REGION-001: Region ${region.id} is disconnected (connected component ${visited.size} != total pixels ${regPixels.size})")
                }
            }
        }

        // CELL-001: Cell 4-connectivity check
        for (cell in artwork.cells) {
            val cellPixels = cellPixelsMap[cell.id] ?: emptyList<Int>()

            if (cellPixels.isNotEmpty()) {
                val cellSet = HashSet(cellPixels)
                val visited = HashSet<Int>()
                val queue = ArrayDeque<Int>()
                val start = cellPixels[0]
                queue.add(start)
                visited.add(start)

                while (!queue.isEmpty()) {
                    val p = queue.removeFirst()
                    val x = p % mapWidth
                    val y = p / mapWidth

                    val neighbors = intArrayOf(
                        if (x > 0) p - 1 else -1,
                        if (x < mapWidth - 1) p + 1 else -1,
                        if (y > 0) p - mapWidth else -1,
                        if (y < mapHeight - 1) p + mapWidth else -1
                    )
                    for (n in neighbors) {
                        if (n != -1 && cellSet.contains(n) && visited.add(n)) {
                            queue.add(n)
                        }
                    }
                }

                if (visited.size != cellPixels.size) {
                    violations.add("CELL-001: Cell ${cell.id} is disconnected (connected component ${visited.size} != total pixels ${cellPixels.size})")
                }
            }
        }

        // REGION-002: All cells within a region must match the region's colorId
        for (cell in artwork.cells) {
            val region = artwork.regionMap[cell.regionId]
            if (region != null && cell.colorId != region.colorId) {
                violations.add("REGION-002: Cell ${cell.id} colorId (${cell.colorId}) does not match Region ${region.id} colorId (${region.colorId})")
            }
        }

        return if (violations.isEmpty()) ValidationResult.Pass else ValidationResult.Fail(violations)
    }
}
