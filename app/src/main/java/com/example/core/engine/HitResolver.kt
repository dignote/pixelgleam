package com.example.core.engine

import androidx.compose.ui.geometry.Offset
import com.example.data.Artwork
import com.example.data.Cell
import com.example.ui.canvas.ArtworkTransform
import kotlin.math.sqrt

sealed interface HitResult {
    val resolvedCellId: Int?
        get() = when (this) {
            is ExactCell -> cellId
            is ForgivenCell -> cellId
            else -> null
        }

    data class ExactCell(
        val cellId: Int,
        val regionId: Int,
        val colorId: Int,
        val artworkCoord: Offset
    ) : HitResult

    data class ForgivenCell(
        val cellId: Int,
        val regionId: Int,
        val colorId: Int,
        val distancePx: Float,
        val artworkCoord: Offset
    ) : HitResult

    data class Void(
        val artworkCoord: Offset
    ) : HitResult

    data class OutsideArtwork(
        val screenCoord: Offset
    ) : HitResult

    data class AmbiguousBoundary(
        val candidateCellIds: List<Int>,
        val artworkCoord: Offset
    ) : HitResult
}

/**
 * Authoritative Hit Resolver V2.
 * Single source of truth for all screen tap conversions and cell resolution.
 */
object HitResolver {

    const val DEFAULT_FORGIVENESS_RADIUS_PX = 2.0f

    fun resolveTap(
        screenTap: Offset,
        transform: ArtworkTransform,
        cellIdMapData: IntArray,
        artwork: Artwork,
        selectedColorId: Int? = null,
        forgivenessRadiusPx: Float = DEFAULT_FORGIVENESS_RADIUS_PX
    ): HitResult {
        val artworkCoord = transform.screenToArtwork(screenTap)
            ?: return HitResult.OutsideArtwork(screenTap)

        val width = artwork.canvas.width
        val height = artwork.canvas.height
        val px = artworkCoord.x.toInt().coerceIn(0, width - 1)
        val py = artworkCoord.y.toInt().coerceIn(0, height - 1)

        val exactCellId = cellIdMapData[py * width + px]
        if (exactCellId > 0) {
            val cell = artwork.cellMap[exactCellId]
            if (cell != null) {
                return HitResult.ExactCell(
                    cellId = exactCellId,
                    regionId = cell.regionId,
                    colorId = cell.colorId,
                    artworkCoord = artworkCoord
                )
            }
        }

        // Bounded forgiveness search within radius
        if (forgivenessRadiusPx <= 0f) {
            return HitResult.Void(artworkCoord)
        }

        val iRadius = forgivenessRadiusPx.toInt().coerceAtLeast(1)
        val candidates = ArrayList<CandidateHit>()

        val minX = (px - iRadius).coerceAtLeast(0)
        val maxX = (px + iRadius).coerceAtMost(width - 1)
        val minY = (py - iRadius).coerceAtLeast(0)
        val maxY = (py + iRadius).coerceAtMost(height - 1)

        val radiusSq = forgivenessRadiusPx * forgivenessRadiusPx

        for (ny in minY..maxY) {
            val rowOffset = ny * width
            for (nx in minX..maxX) {
                val cid = cellIdMapData[rowOffset + nx]
                if (cid > 0) {
                    val cell = artwork.cellMap[cid]
                    if (cell != null) {
                        val dx = (nx.toFloat() + 0.5f) - artworkCoord.x
                        val dy = (ny.toFloat() + 0.5f) - artworkCoord.y
                        val distSq = dx * dx + dy * dy
                        if (distSq <= radiusSq) {
                            candidates.add(
                                CandidateHit(
                                    cell = cell,
                                    distance = sqrt(distSq),
                                    matchesSelectedColor = (selectedColorId != null && cell.colorId == selectedColorId)
                                )
                            )
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return HitResult.Void(artworkCoord)
        }

        // Distinct candidate cells
        val distinctCandidates = candidates.groupBy { it.cell.id }.map { (_, list) ->
            list.minByOrNull { it.distance }!!
        }

        // Deterministic sorting / ranking:
        // 1. Matches active selected color first
        // 2. Minimum distance to tap
        // 3. Lower cell ID (deterministic tie-break)
        val sorted = distinctCandidates.sortedWith(
            compareByDescending<CandidateHit> { it.matchesSelectedColor }
                .thenBy { it.distance }
                .thenBy { it.cell.id }
        )

        val best = sorted.first()
        return HitResult.ForgivenCell(
            cellId = best.cell.id,
            regionId = best.cell.regionId,
            colorId = best.cell.colorId,
            distancePx = best.distance,
            artworkCoord = artworkCoord
        )
    }

    private data class CandidateHit(
        val cell: Cell,
        val distance: Float,
        val matchesSelectedColor: Boolean
    )
}
