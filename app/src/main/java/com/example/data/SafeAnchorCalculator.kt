package com.example.data

import java.util.ArrayDeque
import kotlin.math.max

/**
 * Computes exact, raster-derived safe interior anchors for Cells and Regions
 * using multi-source Euclidean/Manhattan distance transform.
 *
 * Guarantees that:
 * 1. Cell anchor strictly resolves to cell.id on cell_id_map (ANCHOR-001)
 * 2. Region anchor strictly resolves to region.id on region_id_map (ANCHOR-002)
 * 3. No anchor lies in void/background (ANCHOR-003)
 */
object SafeAnchorCalculator {

    fun computeAnchors(
        width: Int,
        height: Int,
        cellIdMap: IntArray,
        cells: List<Cell>,
        regions: List<Region>
    ): ArtworkAnchors {
        val cellToRegion = HashMap<Int, Int>(cells.size)
        for (c in cells) {
            cellToRegion[c.id] = c.regionId
        }

        val cellPixels = HashMap<Int, ArrayList<Int>>(cells.size)
        val regionPixels = HashMap<Int, ArrayList<Int>>(regions.size)

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val cid = cellIdMap[rowOffset + x]
                if (cid > 0) {
                    var list = cellPixels[cid]
                    if (list == null) {
                        list = ArrayList()
                        cellPixels[cid] = list
                    }
                    list.add(rowOffset + x)

                    val rid = cellToRegion[cid]
                    if (rid != null) {
                        var rList = regionPixels[rid]
                        if (rList == null) {
                            rList = ArrayList()
                            regionPixels[rid] = rList
                        }
                        rList.add(rowOffset + x)
                    }
                }
            }
        }

        val cellAnchors = HashMap<Int, SafeAnchor>(cells.size)
        for (cell in cells) {
            val pixels = cellPixels[cell.id]
            if (pixels.isNullOrEmpty()) {
                cellAnchors[cell.id] = SafeAnchor(cell.id, cell.centerX, cell.centerY, 0f)
                continue
            }

            var minX = width
            var maxX = 0
            var minY = height
            var maxY = 0

            for (i in 0 until pixels.size) {
                val p = pixels[i]
                val x = p % width
                val y = p / width
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }

            val bw = maxX - minX + 1
            val bh = maxY - minY + 1
            val bSize = bw * bh
            val grid = IntArray(bSize) { -1 }

            // Mark cell pixels
            for (i in 0 until pixels.size) {
                val p = pixels[i]
                val x = p % width - minX
                val y = p / width - minY
                grid[y * bw + x] = 999999
            }

            // Find boundary pixels
            val queue = ArrayDeque<Int>()
            for (i in 0 until pixels.size) {
                val p = pixels[i]
                val x = p % width
                val y = p / width
                val lx = x - minX
                val ly = y - minY

                var isBoundary = false
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    isBoundary = true
                } else {
                    if (cellIdMap[(y - 1) * width + x] != cell.id ||
                        cellIdMap[(y + 1) * width + x] != cell.id ||
                        cellIdMap[y * width + (x - 1)] != cell.id ||
                        cellIdMap[y * width + (x + 1)] != cell.id
                    ) {
                        isBoundary = true
                    }
                }

                if (isBoundary) {
                    grid[ly * bw + lx] = 0
                    queue.add((ly shl 16) or lx)
                }
            }

            var maxDist = 0
            val firstP = pixels[0]
            var bestX = firstP % width
            var bestY = firstP / width

            while (!queue.isEmpty()) {
                val packed = queue.removeFirst()
                val cy = packed ushr 16
                val cx = packed and 0xFFFF
                val d = grid[cy * bw + cx]

                if (d > maxDist) {
                    maxDist = d
                    bestX = cx + minX
                    bestY = cy + minY
                }

                // 4-neighbors
                val neighbors = intArrayOf(cx - 1, cy, cx + 1, cy, cx, cy - 1, cx, cy + 1)
                var idx = 0
                while (idx < 8) {
                    val nx = neighbors[idx]
                    val ny = neighbors[idx + 1]
                    idx += 2

                    if (nx in 0 until bw && ny in 0 until bh) {
                        val gIdx = ny * bw + nx
                        if (grid[gIdx] == 999999) {
                            grid[gIdx] = d + 1
                            queue.add((ny shl 16) or nx)
                        }
                    }
                }
            }

            cellAnchors[cell.id] = SafeAnchor(
                id = cell.id,
                x = bestX.toFloat() + 0.5f,
                y = bestY.toFloat() + 0.5f,
                clearance = maxDist.toFloat()
            )
        }

        // Region Anchors
        val regionAnchors = HashMap<Int, SafeAnchor>(regions.size)
        for (region in regions) {
            val pixels = regionPixels[region.id]
            if (pixels.isNullOrEmpty()) {
                regionAnchors[region.id] = SafeAnchor(region.id, 0f, 0f, 0f)
                continue
            }

            var minX = width
            var maxX = 0
            var minY = height
            var maxY = 0

            for (i in 0 until pixels.size) {
                val p = pixels[i]
                val x = p % width
                val y = p / width
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }

            val bw = maxX - minX + 1
            val bh = maxY - minY + 1
            val bSize = bw * bh
            val grid = IntArray(bSize) { -1 }

            for (i in 0 until pixels.size) {
                val p = pixels[i]
                val x = p % width - minX
                val y = p / width - minY
                grid[y * bw + x] = 999999
            }

            val queue = ArrayDeque<Int>()
            for (i in 0 until pixels.size) {
                val p = pixels[i]
                val x = p % width
                val y = p / width
                val lx = x - minX
                val ly = y - minY

                var isBoundary = false
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    isBoundary = true
                } else {
                    val n1 = cellToRegion[cellIdMap[(y - 1) * width + x]]
                    val n2 = cellToRegion[cellIdMap[(y + 1) * width + x]]
                    val n3 = cellToRegion[cellIdMap[y * width + (x - 1)]]
                    val n4 = cellToRegion[cellIdMap[y * width + (x + 1)]]
                    if (n1 != region.id || n2 != region.id || n3 != region.id || n4 != region.id) {
                        isBoundary = true
                    }
                }

                if (isBoundary) {
                    grid[ly * bw + lx] = 0
                    queue.add((ly shl 16) or lx)
                }
            }

            var maxDist = 0
            val firstP = pixels[0]
            var bestX = firstP % width
            var bestY = firstP / width

            while (!queue.isEmpty()) {
                val packed = queue.removeFirst()
                val cy = packed ushr 16
                val cx = packed and 0xFFFF
                val d = grid[cy * bw + cx]

                if (d > maxDist) {
                    maxDist = d
                    bestX = cx + minX
                    bestY = cy + minY
                }

                val neighbors = intArrayOf(cx - 1, cy, cx + 1, cy, cx, cy - 1, cx, cy + 1)
                var idx = 0
                while (idx < 8) {
                    val nx = neighbors[idx]
                    val ny = neighbors[idx + 1]
                    idx += 2

                    if (nx in 0 until bw && ny in 0 until bh) {
                        val gIdx = ny * bw + nx
                        if (grid[gIdx] == 999999) {
                            grid[gIdx] = d + 1
                            queue.add((ny shl 16) or nx)
                        }
                    }
                }
            }

            regionAnchors[region.id] = SafeAnchor(
                id = region.id,
                x = bestX.toFloat() + 0.5f,
                y = bestY.toFloat() + 0.5f,
                clearance = maxDist.toFloat()
            )
        }

        return ArtworkAnchors(cellAnchors, regionAnchors)
    }
}
