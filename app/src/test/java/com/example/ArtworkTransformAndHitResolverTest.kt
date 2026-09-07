package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.core.engine.HitResolver
import com.example.core.engine.HitResult
import com.example.data.*
import com.example.data.validation.ArtworkContentValidator
import com.example.ui.canvas.ArtworkTransform
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArtworkTransformAndHitResolverTest {

    @Test
    fun testArtworkTransform_ForwardAndInverseMapping() {
        val transform = ArtworkTransform(
            scale = 2.0f,
            translationX = 50f,
            translationY = -30f,
            artworkWidth = 1000,
            artworkHeight = 1000,
            viewportWidth = 800f,
            viewportHeight = 800f
        )

        // Point at (250, 250) in artwork space
        val artPoint = Offset(250f, 250f)
        val screenPoint = transform.artworkToScreen(artPoint)
        val roundTrip = transform.screenToArtwork(screenPoint)

        assertNotNull(roundTrip)
        assertEquals(artPoint.x, roundTrip!!.x, 0.001f)
        assertEquals(artPoint.y, roundTrip.y, 0.001f)
    }

    @Test
    fun testArtworkTransform_OutOfBoundsReturnsNull() {
        val transform = ArtworkTransform(
            scale = 1.0f,
            translationX = 0f,
            translationY = 0f,
            artworkWidth = 500,
            artworkHeight = 500,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        )

        // Tapping far outside the artwork viewport
        val outOfBounds = transform.screenToArtwork(Offset(-200f, -200f))
        assertNull(outOfBounds)
        assertFalse(transform.isInsideArtwork(Offset(-200f, -200f)))
    }

    @Test
    fun testSafeAnchorCalculator_DerivesInteriorPoint() {
        // Create an L-shaped cell where naive bounding box center is outside the cell
        // 5x5 grid
        // Cell 1 covers: (0,0), (0,1), (0,2), (1,2), (2,2)
        // Naive center is at x=1.0, y=1.4 (might be at (1,1) which is void!)
        val width = 5
        val height = 5
        val cellIdMap = IntArray(width * height)

        val cell1Pixels = listOf(
            0 to 0, 0 to 1, 0 to 2,
            1 to 2, 2 to 2
        )
        cell1Pixels.forEach { (x, y) ->
            cellIdMap[y * width + x] = 1
        }

        val cells = listOf(
            Cell(id = 1, centerX = 1.0f, centerY = 1.0f, colorId = 1, regionId = 1, neighbors = emptyList())
        )
        val regions = listOf(
            Region(id = 1, colorId = 1, cellIds = listOf(1))
        )

        val anchors = SafeAnchorCalculator.computeAnchors(width, height, cellIdMap, cells, regions)
        val cell1Anchor = anchors.cellAnchors[1]

        assertNotNull(cell1Anchor)
        val ax = cell1Anchor!!.x.toInt()
        val ay = cell1Anchor.y.toInt()

        // The safe anchor MUST be on one of the valid cell pixels!
        val pixelValue = cellIdMap[ay * width + ax]
        assertEquals("Safe anchor ($ax, $ay) must fall on a pixel belonging to cell 1", 1, pixelValue)
    }

    @Test
    fun testRegionIdRaster_LookupAndDerivation() {
        val width = 4
        val height = 4
        val cellIdMap = IntArray(width * height)
        cellIdMap[0] = 1 // Cell 1 -> Region 10
        cellIdMap[1] = 2 // Cell 2 -> Region 20

        val cellMap = mapOf(
            1 to Cell(id = 1, centerX = 0f, centerY = 0f, colorId = 1, regionId = 10, neighbors = emptyList()),
            2 to Cell(id = 2, centerX = 1f, centerY = 0f, colorId = 2, regionId = 20, neighbors = emptyList())
        )

        val regionRaster = RegionIdRaster.fromCellIdRaster(width, height, cellIdMap, cellMap)
        assertEquals(10, regionRaster.getRegionId(0, 0))
        assertEquals(20, regionRaster.getRegionId(1, 0))
        assertEquals(0, regionRaster.getRegionId(2, 0))
    }

    @Test
    fun testHitResolver_ExactAndForgivenHits() {
        val width = 100
        val height = 100
        val cellIdMap = IntArray(width * height)

        // Cell 5 at (50, 50)
        cellIdMap[50 * width + 50] = 5
        cellIdMap[50 * width + 51] = 5
        cellIdMap[51 * width + 50] = 5
        cellIdMap[51 * width + 51] = 5

        val cell5 = Cell(id = 5, centerX = 50.5f, centerY = 50.5f, colorId = 1, regionId = 1, neighbors = emptyList())
        val region1 = Region(id = 1, colorId = 1, cellIds = listOf(5))

        val artwork = Artwork(
            id = "test_art",
            title = "Test",
            category = "test",
            canvas = CanvasSize(width, height),
            palette = listOf(PaletteColor(id = 1, hex = "#FF0000", name = "Red")),
            regions = listOf(region1),
            cells = listOf(cell5),
            cellMap = mapOf(5 to cell5),
            regionMap = mapOf(1 to region1)
        )

        val transform = ArtworkTransform(
            scale = 1.0f,
            translationX = 0f,
            translationY = 0f,
            artworkWidth = width,
            artworkHeight = height,
            viewportWidth = 100f,
            viewportHeight = 100f
        )

        // 1. Exact Hit on (50.5, 50.5)
        val exactHit = HitResolver.resolveTap(
            screenTap = Offset(50.5f, 50.5f),
            transform = transform,
            cellIdMapData = cellIdMap,
            artwork = artwork,
            selectedColorId = 1
        )
        assertTrue("Expected ExactCell hit", exactHit is HitResult.ExactCell)
        assertEquals(5, (exactHit as HitResult.ExactCell).cellId)

        // 2. Boundary / Forgiven Hit 1.2px away at (49.0, 50.0)
        val forgivenHit = HitResolver.resolveTap(
            screenTap = Offset(49.0f, 50.0f),
            transform = transform,
            cellIdMapData = cellIdMap,
            artwork = artwork,
            selectedColorId = 1,
            forgivenessRadiusPx = 2.0f
        )
        assertTrue("Expected ForgivenCell hit", forgivenHit is HitResult.ForgivenCell)
        assertEquals(5, (forgivenHit as HitResult.ForgivenCell).cellId)

        // 3. Void Hit far away at (10.0, 10.0)
        val voidHit = HitResolver.resolveTap(
            screenTap = Offset(10.0f, 10.0f),
            transform = transform,
            cellIdMapData = cellIdMap,
            artwork = artwork,
            selectedColorId = 1,
            forgivenessRadiusPx = 2.0f
        )
        assertTrue("Expected Void hit", voidHit is HitResult.Void)
    }

    @Test
    fun testArtworkContentValidator_DetectsDimensionMismatch() {
        val cell1 = Cell(id = 1, centerX = 5f, centerY = 5f, colorId = 1, regionId = 1, neighbors = emptyList())
        val region1 = Region(id = 1, colorId = 1, cellIds = listOf(1))

        val artwork = Artwork(
            id = "bad_art",
            title = "Bad Art",
            category = "test",
            canvas = CanvasSize(100, 100),
            palette = listOf(PaletteColor(id = 1, hex = "#FF0000", name = "Red")),
            regions = listOf(region1),
            cells = listOf(cell1),
            cellMap = mapOf(1 to cell1),
            regionMap = mapOf(1 to region1)
        )
        // Raster with mismatched dimensions (50x50)
        val badRaster = IntArray(50 * 50)

        val result = ArtworkContentValidator.validate(artwork, badRaster, 50, 50)
        assertTrue("Validator should fail on dimension mismatch", result is ArtworkContentValidator.ValidationResult.Fail)
        val violations = (result as ArtworkContentValidator.ValidationResult.Fail).violations
        assertTrue(violations.any { it.contains("MAP-001") || it.contains("dimensions") })
    }
}
