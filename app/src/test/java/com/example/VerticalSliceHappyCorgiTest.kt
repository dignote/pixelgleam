package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.engine.GameplayCoreEngine
import com.example.core.engine.TapResolutionResult
import com.example.data.Artwork
import com.example.data.db.AppDatabase
import com.example.data.repository.ArtworkProgressRepository
import com.example.ui.canvas.CanvasTransformHelper
import com.example.ui.canvas.CanvasViewport
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VerticalSliceHappyCorgiTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: ArtworkProgressRepository
    private lateinit var happyCorgiArtwork: Artwork
    private lateinit var allRegionIds: Set<Int>

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ArtworkProgressRepository(database)

        val jsonStr = context.assets.open("artworks/happy_corgi_v3/artwork.json").bufferedReader().use { it.readText() }
        happyCorgiArtwork = Artwork.parseFromJson(jsonStr)
        allRegionIds = happyCorgiArtwork.regions.map { it.id }.toSet()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testP0_VerticalSlice_ContentLoadingAndTopology() {
        // Section 1: ART-001 to ART-004
        assertNotNull(happyCorgiArtwork)
        assertEquals("happy_corgi_v3", happyCorgiArtwork.id)
        assertEquals("Happy Corgi", happyCorgiArtwork.title)
        assertEquals(43, happyCorgiArtwork.regions.size)
        assertTrue(happyCorgiArtwork.palette.isNotEmpty())
        assertTrue(happyCorgiArtwork.cells.isNotEmpty())

        // Validate that each cell in happy corgi belongs to a valid region
        happyCorgiArtwork.cells.forEach { cell ->
            assertTrue("Cell ${cell.id} must map to a valid region", allRegionIds.contains(cell.regionId))
            val region = happyCorgiArtwork.regions.first { it.id == cell.regionId }
            assertEquals(
                "Cell color must match region color",
                region.colorId,
                cell.colorId
            )
        }
    }

    @Test
    fun testP0_VerticalSlice_CompleteCUJ_FromEntryToCompletionAndRestore() = runBlocking {
        // Step 1: Open artwork with empty persistence
        val initialSaved = repository.getValidCompletedRegionIds(happyCorgiArtwork.id, allRegionIds)
        assertTrue(initialSaved.isEmpty())

        val initialColor = happyCorgiArtwork.palette.first().id
        val engine = GameplayCoreEngine(
            artwork = happyCorgiArtwork,
            initialCompletedRegionIds = emptySet(),
            initialSelectedColorId = initialColor
        )

        assertEquals(0, engine.completedRegionIds.size)
        assertFalse(engine.isAllRegionsCompleted())
        assertEquals(initialColor, engine.selectedColorId)

        // Step 2: Select Color and complete a connected region
        val targetColor = happyCorgiArtwork.palette.first().id
        engine.selectColor(targetColor)

        // Find regions of targetColor
        val colorRegions = happyCorgiArtwork.regions.filter { it.colorId == targetColor }
        assertTrue("Must have at least one region of target color", colorRegions.isNotEmpty())

        val firstRegion = colorRegions.first()
        val firstCellId = firstRegion.cellIds.first()

        // Tap cell with correct color selected
        val result1 = engine.processCellTap(firstCellId)
        assertTrue(result1 is TapResolutionResult.Success)
        val completedRegion1 = (result1 as TapResolutionResult.Success).completedRegion
        assertEquals(firstRegion.id, completedRegion1.id)

        // If there are other disconnected regions of the same color, verify they remain uncompleted
        if (colorRegions.size > 1) {
            val secondRegion = colorRegions[1]
            assertFalse(
                "Disconnected same-color region must remain uncompleted",
                engine.isRegionCompleted(secondRegion.id)
            )
        }

        // Step 3: Wrong-color tap must not mutate progress (REG-004)
        val otherColor = happyCorgiArtwork.palette.firstOrNull { it.id != targetColor }?.id
        if (otherColor != null) {
            val otherColorRegion = happyCorgiArtwork.regions.first { it.colorId == otherColor }
            val otherCellId = otherColorRegion.cellIds.first()
            val wrongTapResult = engine.processCellTap(otherCellId)
            assertTrue(wrongTapResult is TapResolutionResult.IgnoredWrongColor)
            assertEquals(1, engine.completedRegionIds.size)
        }

        // Step 4: Geometry & Viewport Coordinate Transformation (HIT-001 to HIT-006)
        val viewport = CanvasViewport(
            screenWidth = 1080f,
            screenHeight = 1920f,
            artworkWidth = happyCorgiArtwork.canvas.width,
            artworkHeight = happyCorgiArtwork.canvas.height,
            scale = 2.0f,
            panOffset = Offset(50f, -30f)
        )

        // Test coordinate inverse lookup
        val testScreenX = 540f
        val testScreenY = 960f
        val artOffset = CanvasTransformHelper.screenToArtworkSpace(testScreenX, testScreenY, viewport)
        assertNotNull(artOffset)

        // Step 5: Hint targeting without state mutation (HINT-001, HINT-002, INV-090)
        val beforeHintCompleted = engine.completedRegionIds
        val hintRegion = engine.findHintTargetRegion()
        assertNotNull("Hint must find an unfinished region", hintRegion)
        assertFalse(beforeHintCompleted.contains(hintRegion!!.id))
        assertEquals(
            "Hint must not mutate completedRegionIds",
            beforeHintCompleted,
            engine.completedRegionIds
        )

        // Step 6: Save progress to Room repository (SAVE-001, SAVE-002)
        repository.saveProgress(
            artworkId = happyCorgiArtwork.id,
            completedRegionIds = engine.completedRegionIds,
            isCompleted = engine.isAllRegionsCompleted(),
            selectedColorId = engine.selectedColorId
        )

        // Step 7: Simulate Kill / Restart and exact restoration (SAVE-003, LIFE-001)
        val restoredCompletedIds = repository.getValidCompletedRegionIds(happyCorgiArtwork.id, allRegionIds)
        val restoredColorId = repository.getSavedSelectedColorId(happyCorgiArtwork.id)
        assertEquals(engine.completedRegionIds, restoredCompletedIds)
        assertEquals(engine.selectedColorId, restoredColorId)

        // Fresh engine instance initialized from restored database state
        val restoredEngine = GameplayCoreEngine(
            artwork = happyCorgiArtwork,
            initialCompletedRegionIds = restoredCompletedIds,
            initialSelectedColorId = restoredColorId
        )
        assertEquals(restoredCompletedIds, restoredEngine.completedRegionIds)
        assertTrue(restoredEngine.isRegionCompleted(firstRegion.id))

        // Step 8: Complete all remaining regions and verify completion fires once (PROG-002, PROG-003, FSM-004)
        var completionFiredCount = 0
        happyCorgiArtwork.regions.forEach { reg ->
            if (!restoredEngine.isRegionCompleted(reg.id)) {
                restoredEngine.selectColor(reg.colorId)
                val tapRes = restoredEngine.processCellTap(reg.cellIds.first())
                if (tapRes is TapResolutionResult.Success && tapRes.isArtworkCompleted) {
                    completionFiredCount++
                }
            }
        }

        assertEquals(happyCorgiArtwork.regions.size, restoredEngine.completedRegionIds.size)
        assertTrue(restoredEngine.isAllRegionsCompleted())
        assertEquals("Completion event must fire exactly once", 1, completionFiredCount)

        // Step 9: Reopening completed artwork (SAVE-004)
        repository.saveProgress(
            artworkId = happyCorgiArtwork.id,
            completedRegionIds = restoredEngine.completedRegionIds,
            isCompleted = true,
            selectedColorId = restoredEngine.selectedColorId
        )
        assertTrue(repository.isArtworkCompleted(happyCorgiArtwork.id))

        val reopenedEngine = GameplayCoreEngine(
            artwork = happyCorgiArtwork,
            initialCompletedRegionIds = repository.getValidCompletedRegionIds(happyCorgiArtwork.id, allRegionIds),
            initialSelectedColorId = 1
        )
        assertTrue("Reopened artwork must report completed", reopenedEngine.isAllRegionsCompleted())
        assertEquals(happyCorgiArtwork.regions.size, reopenedEngine.completedRegionIds.size)

        // Step 10: Reset progress (SAVE-005)
        repository.deleteProgress(happyCorgiArtwork.id)
        val resetCompleted = repository.getValidCompletedRegionIds(happyCorgiArtwork.id, allRegionIds)
        assertTrue(resetCompleted.isEmpty())
        assertFalse(repository.isArtworkCompleted(happyCorgiArtwork.id))
    }

    @Test
    fun testP0_VerticalSlice_IdempotentTapsAndEdgeCases() {
        val engine = GameplayCoreEngine(
            artwork = happyCorgiArtwork,
            initialCompletedRegionIds = emptySet(),
            initialSelectedColorId = 1
        )

        // Void tap (cellId = 0 or negative)
        val voidResult = engine.processCellTap(0)
        assertTrue(voidResult is TapResolutionResult.IgnoredVoidOrOutOfBounds)

        val negativeResult = engine.processCellTap(-1)
        assertTrue(negativeResult is TapResolutionResult.IgnoredVoidOrOutOfBounds)

        // Rapid duplicate taps on same region
        val region = happyCorgiArtwork.regions.first()
        engine.selectColor(region.colorId)

        val tap1 = engine.processCellTap(region.cellIds.first())
        assertTrue(tap1 is TapResolutionResult.Success)

        val tap2 = engine.processCellTap(region.cellIds.first())
        assertTrue(tap2 is TapResolutionResult.IgnoredAlreadyCompleted)

        // Progress stays at 1
        assertEquals(1, engine.completedRegionIds.size)
    }
}
