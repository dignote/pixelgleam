package com.example

import androidx.compose.ui.geometry.Offset
import com.example.ui.animation.GemFlightVisual
import com.example.ui.animation.GemPresentationManager
import com.example.ui.animation.LandingEffectVisual
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GemPresentationTest {

    @Test
    fun testGemFlight_TrajectoryAndProgress() {
        val start = Offset(100f, 500f)
        val target = Offset(300f, 200f)
        val startTime = 1000L
        val duration = 300L

        val flight = GemFlightVisual(
            id = 1L,
            startOffset = start,
            targetOffset = target,
            colorHex = "#E53935",
            gemType = "gem_red",
            startTime = startTime,
            durationMs = duration,
            arcHeight = -80f
        )

        // At start (t = 0)
        assertEquals(0f, flight.currentProgress(1000L), 0.001f)
        assertFalse(flight.isFinished(1000L))
        val pos0 = flight.currentPosition(0f)
        assertEquals(start.x, pos0.x, 0.001f)
        assertEquals(start.y, pos0.y, 0.001f)

        // At midpoint (t = 0.5)
        assertEquals(0.5f, flight.currentProgress(1150L), 0.001f)
        val posMid = flight.currentPosition(0.5f)
        // With arcHeight = -80, at t=0.5, arc displacement is 4 * 0.5 * 0.5 * (-80) = -80
        assertTrue(posMid.x in start.x..target.x)

        // At finish (t = 1.0)
        assertEquals(1f, flight.currentProgress(1300L), 0.001f)
        assertTrue(flight.isFinished(1300L))
        val pos1 = flight.currentPosition(1f)
        assertEquals(target.x, pos1.x, 0.001f)
        assertEquals(target.y, pos1.y, 0.001f)
    }

    @Test
    fun testLandingEffect_ProgressAndLifecycle() {
        val center = Offset(250f, 250f)
        val startTime = 2000L
        val duration = 400L

        val landing = LandingEffectVisual(
            id = 2L,
            center = center,
            colorHex = "#43A047",
            startTime = startTime,
            durationMs = duration,
            maxRadius = 40f
        )

        assertEquals(0f, landing.currentProgress(2000L), 0.001f)
        assertFalse(landing.isFinished(2000L))

        assertEquals(0.5f, landing.currentProgress(2200L), 0.001f)

        assertEquals(1f, landing.currentProgress(2400L), 0.001f)
        assertTrue(landing.isFinished(2400L))
    }

    @Test
    fun testHexColorToGemMapping() {
        assertEquals("gem_red", GemPresentationManager.mapHexToGemName("#E53935"))
        assertEquals("gem_orange", GemPresentationManager.mapHexToGemName("#FB8C00"))
        assertEquals("gem_yellow", GemPresentationManager.mapHexToGemName("#FDD835"))
        assertEquals("gem_green", GemPresentationManager.mapHexToGemName("#43A047"))
        assertEquals("gem_blue", GemPresentationManager.mapHexToGemName("#1E88E5"))
        assertEquals("gem_purple", GemPresentationManager.mapHexToGemName("#8E24AA"))
    }
}
