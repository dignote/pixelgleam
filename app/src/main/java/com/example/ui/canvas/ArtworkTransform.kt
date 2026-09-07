package com.example.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Authoritative, unified forward and inverse transform between
 * artwork raster coordinate space (pixels) and Compose screen coordinate space (physical pixels).
 *
 * Guarantees that rendering, hit testing, number placement, hints, gem flight targets,
 * and debug overlays all use the exact same mathematical model.
 */
data class ArtworkTransform(
    val scale: Float = 1.0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val artworkWidth: Int,
    val artworkHeight: Int,
    val viewportWidth: Float,
    val viewportHeight: Float
) {
    val baseScale: Float
        get() {
            if (artworkWidth <= 0 || artworkHeight <= 0 || viewportWidth <= 0f || viewportHeight <= 0f) return 1.0f
            return (viewportWidth / artworkWidth.toFloat()).coerceAtMost(viewportHeight / artworkHeight.toFloat())
        }

    val totalScale: Float
        get() = baseScale * scale

    val displayWidth: Float
        get() = artworkWidth.toFloat() * baseScale

    val displayHeight: Float
        get() = artworkHeight.toFloat() * baseScale

    val centerOffsetX: Float
        get() = (viewportWidth - displayWidth) / 2.0f

    val centerOffsetY: Float
        get() = (viewportHeight - displayHeight) / 2.0f

    /**
     * Maps an artwork raster coordinate (pixels) to Compose screen coordinate (pixels).
     */
    fun artworkToScreen(point: Offset): Offset {
        val sx = translationX + centerOffsetX + (point.x * totalScale)
        val sy = translationY + centerOffsetY + (point.y * totalScale)
        return Offset(sx, sy)
    }

    /**
     * Maps an artwork raster rectangle to a screen-space rectangle.
     */
    fun artworkToScreen(rect: Rect): Rect {
        val topLeft = artworkToScreen(rect.topLeft)
        val bottomRight = artworkToScreen(rect.bottomRight)
        return Rect(topLeft, bottomRight)
    }

    /**
     * Inverse transform without boundary checking: Maps any screen coordinate (pixels) to artwork raster space.
     */
    fun screenToArtworkUnclamped(screenPoint: Offset): Offset {
        val tScale = totalScale
        if (tScale <= 0f) return Offset(0f, 0f)

        val adjustedX = screenPoint.x - translationX - centerOffsetX
        val adjustedY = screenPoint.y - translationY - centerOffsetY

        val artX = adjustedX / tScale
        val artY = adjustedY / tScale

        return Offset(artX, artY)
    }

    /**
     * Inverse transform: Maps a screen coordinate (pixels) to artwork raster space.
     * Returns null if transformed coordinate is outside [0, artworkWidth) x [0, artworkHeight).
     */
    fun screenToArtwork(screenPoint: Offset): Offset? {
        val tScale = totalScale
        if (tScale <= 0f) return null

        val adjustedX = screenPoint.x - translationX - centerOffsetX
        val adjustedY = screenPoint.y - translationY - centerOffsetY

        val artX = adjustedX / tScale
        val artY = adjustedY / tScale

        if (artX < 0f || artX >= artworkWidth.toFloat() || artY < 0f || artY >= artworkHeight.toFloat()) {
            return null
        }

        return Offset(artX, artY)
    }

    /**
     * Checks if a screen point lies within the rendered artwork boundaries.
     */
    fun isInsideArtwork(screenPoint: Offset): Boolean {
        return screenToArtwork(screenPoint) != null
    }
}
