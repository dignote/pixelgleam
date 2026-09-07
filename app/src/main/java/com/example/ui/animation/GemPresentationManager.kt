package com.example.ui.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.PaletteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class GemFlightVisual(
    val id: Long,
    val startOffset: Offset,
    val targetOffset: Offset,
    val colorHex: String,
    val gemType: String,
    val startTime: Long,
    val durationMs: Long = 350L,
    val arcHeight: Float = -60f
) {
    fun currentProgress(now: Long): Float {
        val elapsed = now - startTime
        return (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
    }

    fun isFinished(now: Long): Boolean = (now - startTime) >= durationMs

    fun currentPosition(progress: Float): Offset {
        // Quadratic bezier curve with arc height
        val t = progress
        val easeT = 1f - (1f - t) * (1f - t) // easeOutQuad
        val currentX = (1 - easeT) * startOffset.x + easeT * targetOffset.x
        val linearY = (1 - easeT) * startOffset.y + easeT * targetOffset.y
        // Parabolic arc displacement
        val arc = 4f * t * (1f - t) * arcHeight
        return Offset(currentX, linearY + arc)
    }
}

data class LandingEffectVisual(
    val id: Long,
    val center: Offset,
    val colorHex: String,
    val startTime: Long,
    val durationMs: Long = 400L,
    val maxRadius: Float = 36f
) {
    fun currentProgress(now: Long): Float {
        val elapsed = now - startTime
        return (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
    }

    fun isFinished(now: Long): Boolean = (now - startTime) >= durationMs
}

class GemPresentationManager(private val context: Context) {

    private val gemBitmaps = ConcurrentHashMap<String, Bitmap>()
    private val effectBitmaps = ConcurrentHashMap<String, Bitmap>()

    suspend fun preloadAssets() = withContext(Dispatchers.IO) {
        val gemFiles = listOf(
            "gem_red.png", "gem_orange.png", "gem_yellow.png",
            "gem_green.png", "gem_aqua.png", "gem_blue.png",
            "gem_purple.png", "gem_pink.png"
        )
        gemFiles.forEach { file ->
            try {
                context.assets.open("gem_g02/$file").use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        gemBitmaps[file.removeSuffix(".png")] = bmp
                    }
                }
            } catch (e: Exception) {
                // Ignore missing file
            }
        }

        val fxFiles = listOf(
            "FX_LANDING_RING.png", "FX_LANDING_SPARKLE.png",
            "FX_GLINT_STAR.png", "FX_SETTLE_TWINKLE.png", "FX_FLIGHT_TRAIL.png"
        )
        fxFiles.forEach { file ->
            try {
                context.assets.open("effects_g02/$file").use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        effectBitmaps[file.removeSuffix(".png")] = bmp
                    }
                }
            } catch (e: Exception) {
                // Ignore missing file
            }
        }
    }

    fun getGemBitmapForColor(hex: String): Bitmap? {
        val colorName = mapHexToGemName(hex)
        return gemBitmaps[colorName] ?: gemBitmaps.values.firstOrNull()
    }

    fun getEffectBitmap(effectName: String): Bitmap? {
        return effectBitmaps[effectName]
    }

    companion object {
        fun mapHexToGemName(hex: String): String {
            return try {
                val color = android.graphics.Color.parseColor(hex)
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(color, hsv)
                val hue = hsv[0] // 0..360
                val sat = hsv[1]
                val value = hsv[2]

                when {
                    hue in 15f..45f -> "gem_orange"
                    hue in 45f..70f -> "gem_yellow"
                    hue in 70f..160f -> "gem_green"
                    hue in 160f..200f -> "gem_aqua"
                    hue in 200f..260f -> "gem_blue"
                    hue in 260f..320f -> "gem_purple"
                    hue in 320f..345f -> "gem_pink"
                    else -> "gem_red"
                }
            } catch (e: Exception) {
                "gem_master_red"
            }
        }
    }
}
