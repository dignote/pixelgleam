package com.example.ui.canvas

import androidx.compose.ui.geometry.Offset

data class CanvasViewport(
    val screenWidth: Float,
    val screenHeight: Float,
    val artworkWidth: Int,
    val artworkHeight: Int,
    val scale: Float = 1.0f,
    val panOffset: Offset = Offset.Zero
) {
    val baseScale: Float
        get() {
            if (artworkWidth <= 0 || artworkHeight <= 0) return 1.0f
            return (screenWidth / artworkWidth).coerceAtMost(screenHeight / artworkHeight)
        }

    val displayWidth: Float get() = artworkWidth * baseScale
    val displayHeight: Float get() = artworkHeight * baseScale

    val centerOffsetX: Float get() = (screenWidth - displayWidth) / 2.0f
    val centerOffsetY: Float get() = (screenHeight - displayHeight) / 2.0f

    val totalScale: Float get() = baseScale * scale
}

object CanvasTransformHelper {

    /**
     * Maps a screen coordinate (e.g. from pointer tap) to the exact pixel coordinate in artwork space.
     * Returns null if transformed coordinate is outside the artwork boundaries.
     */
    fun screenToArtworkSpace(
        screenX: Float,
        screenY: Float,
        viewport: CanvasViewport
    ): Offset? {
        val totalScale = viewport.totalScale
        if (totalScale <= 0f) return null

        val adjustedX = screenX - viewport.panOffset.x - viewport.centerOffsetX
        val adjustedY = screenY - viewport.panOffset.y - viewport.centerOffsetY

        val artX = adjustedX / totalScale
        val artY = adjustedY / totalScale

        if (artX < 0f || artX >= viewport.artworkWidth || artY < 0f || artY >= viewport.artworkHeight) {
            return null
        }

        return Offset(artX, artY)
    }

    /**
     * Resolves Cell ID from transformed artwork coordinates with deterministic boundary tap-forgiveness.
     * If direct pixel lookup hits Cell-ID 0 (void or boundary line), searches immediate neighborhood (radius up to 3px)
     * for the nearest non-void cell.
     */
    fun lookupCellId(
        artworkX: Float,
        artworkY: Float,
        width: Int,
        height: Int,
        cellIdMap: IntArray,
        forgivenessRadius: Int = 3
    ): Int {
        val px = artworkX.toInt()
        val py = artworkY.toInt()

        if (px < 0 || px >= width || py < 0 || py >= height) return 0

        val directCellId = cellIdMap[py * width + px]
        if (directCellId > 0) {
            return directCellId
        }

        // Tap forgiveness: search nearby neighborhood for closest valid cell
        if (forgivenessRadius <= 0) return 0

        var closestCellId = 0
        var minDistanceSq = Float.MAX_VALUE

        for (dy in -forgivenessRadius..forgivenessRadius) {
            for (dx in -forgivenessRadius..forgivenessRadius) {
                if (dx == 0 && dy == 0) continue
                val nx = px + dx
                val ny = py + dy
                if (nx in 0 until width && ny in 0 until height) {
                    val neighborCellId = cellIdMap[ny * width + nx]
                    if (neighborCellId > 0) {
                        val distSq = (dx * dx + dy * dy).toFloat()
                        if (distSq < minDistanceSq) {
                            minDistanceSq = distSq
                            closestCellId = neighborCellId
                        }
                    }
                }
            }
        }

        return closestCellId
    }
}
