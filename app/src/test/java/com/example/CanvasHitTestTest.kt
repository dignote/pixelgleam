package com.example

import androidx.compose.ui.geometry.Offset
import com.example.ui.canvas.CanvasTransformHelper
import com.example.ui.canvas.CanvasViewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CanvasHitTestTest {

    @Test
    fun testScreenToArtworkSpace_IdentityAndCenter() {
        // Screen 800x800, Artwork 800x800, 1.0x zoom, 0 pan
        val viewport = CanvasViewport(
            screenWidth = 800f,
            screenHeight = 800f,
            artworkWidth = 800,
            artworkHeight = 800,
            scale = 1.0f,
            panOffset = Offset.Zero
        )

        // Center tap (400, 400) should map to artwork (400, 400)
        val artOffset = CanvasTransformHelper.screenToArtworkSpace(400f, 400f, viewport)
        assertNotNull(artOffset)
        assertEquals(400f, artOffset!!.x, 0.001f)
        assertEquals(400f, artOffset.y, 0.001f)

        // Top-left (0, 0)
        val topLeft = CanvasTransformHelper.screenToArtworkSpace(0f, 0f, viewport)
        assertNotNull(topLeft)
        assertEquals(0f, topLeft!!.x, 0.001f)
        assertEquals(0f, topLeft.y, 0.001f)
    }

    @Test
    fun testScreenToArtworkSpace_ZoomAndPan() {
        // Screen 1080x1920, Artwork 1254x1254
        // baseScale = 1080 / 1254 ≈ 0.861244
        // zoomed 2.0x, pan (100, 50)
        val viewport = CanvasViewport(
            screenWidth = 1080f,
            screenHeight = 1920f,
            artworkWidth = 1254,
            artworkHeight = 1254,
            scale = 2.0f,
            panOffset = Offset(100f, 50f)
        )

        // Test forward -> inverse roundtrip
        val testArtX = 627f
        val testArtY = 627f

        val screenX = testArtX * viewport.totalScale + viewport.centerOffsetX + viewport.panOffset.x
        val screenY = testArtY * viewport.totalScale + viewport.centerOffsetY + viewport.panOffset.y

        val inv = CanvasTransformHelper.screenToArtworkSpace(screenX, screenY, viewport)
        assertNotNull(inv)
        assertEquals(testArtX, inv!!.x, 0.01f)
        assertEquals(testArtY, inv.y, 0.01f)
    }

    @Test
    fun testScreenToArtworkSpace_OutOfBoundsReturnsNull() {
        val viewport = CanvasViewport(
            screenWidth = 1000f,
            screenHeight = 2000f,
            artworkWidth = 500,
            artworkHeight = 500,
            scale = 1.0f,
            panOffset = Offset.Zero
        )

        // Screen is 1000x2000, artwork is 500x500 (baseScale = 2.0, display = 1000x1000, cy = 500)
        // A tap at (500, 100) is in the top letterbox void area (adjustedY = 100 - 500 = -400 < 0)
        val outOfBoundsTop = CanvasTransformHelper.screenToArtworkSpace(500f, 100f, viewport)
        assertNull(outOfBoundsTop)

        // A tap at (500, 1800) is in the bottom letterbox void area (adjustedY = 1800 - 500 = 1300 > 1000)
        val outOfBoundsBottom = CanvasTransformHelper.screenToArtworkSpace(500f, 1800f, viewport)
        assertNull(outOfBoundsBottom)
    }

    @Test
    fun testCellIdLookup_ExactMatch() {
        val width = 10
        val height = 10
        val cellIdMap = IntArray(width * height)

        // Fill region at (3..5, 3..5) with cell ID 42
        for (y in 3..5) {
            for (x in 3..5) {
                cellIdMap[y * width + x] = 42
            }
        }

        val cellId = CanvasTransformHelper.lookupCellId(4f, 4f, width, height, cellIdMap)
        assertEquals(42, cellId)
    }

    @Test
    fun testCellIdLookup_BoundaryTapForgiveness() {
        val width = 20
        val height = 20
        val cellIdMap = IntArray(width * height)

        // Place a cell at (10, 10)
        cellIdMap[10 * width + 10] = 77

        // Exact boundary line tap at (11, 10) where pixel value is 0 (boundary line)
        // With forgiveness radius 2, it should successfully resolve nearby cell 77
        val resolved = CanvasTransformHelper.lookupCellId(11f, 10f, width, height, cellIdMap, forgivenessRadius = 2)
        assertEquals(77, resolved)

        // Far tap at (15, 10) outside radius 2
        val farResolved = CanvasTransformHelper.lookupCellId(15f, 10f, width, height, cellIdMap, forgivenessRadius = 2)
        assertEquals(0, farResolved)
    }

    @Test
    fun testCellIdLookup_VoidReturnsZero() {
        val width = 10
        val height = 10
        val emptyMap = IntArray(width * height) // All 0

        val cellId = CanvasTransformHelper.lookupCellId(5f, 5f, width, height, emptyMap)
        assertEquals(0, cellId)
    }
}
