package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.example.core.engine.GameplayCoreEngine
import com.example.core.engine.HitResolver
import com.example.core.engine.TapResolutionResult
import com.example.data.repository.AssetArtworkRepository
import com.example.data.validation.ArtworkContentValidator
import com.example.ui.canvas.ArtworkTransform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArtworkTopologyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = AssetArtworkRepository(context)

    @Test
    fun `validate all discovered artwork packages for strict topology invariants`() = runBlocking {
        val catalog = repository.scanDynamicCatalog()
        assertTrue("Dynamic catalog should discover artwork packages", catalog.isNotEmpty())

        for (item in catalog) {
            val artwork = repository.getArtwork(item.id)
            val decodedMap = repository.loadCellIdMap(item.id)
            val mask = repository.loadSubjectMask(item.id)

            val result = ArtworkContentValidator.validate(
                artwork = artwork,
                cellIdData = decodedMap.data,
                mapWidth = decodedMap.width,
                mapHeight = decodedMap.height,
                subjectMask = mask
            )

            if (result is ArtworkContentValidator.ValidationResult.Fail) {
                val errorSummary = result.violations.joinToString("\n")
                throw AssertionError("Artwork '${item.id}' failed validation with ${result.violations.size} violations:\n$errorSummary")
            }

            assertTrue("Artwork ${item.id} should pass validation", result is ArtworkContentValidator.ValidationResult.Pass)
        }
    }

    @Test
    fun `verify cell and region counts match artwork json exactly`() = runBlocking {
        val catalog = repository.scanDynamicCatalog()
        for (item in catalog) {
            val artwork = repository.getArtwork(item.id)
            assertEquals("Total cells for ${item.id} must match cells.size", artwork.cells.size, item.totalCells)
            assertEquals("Total regions for ${item.id} must match regions.size", artwork.regions.size, item.totalRegions)
        }
    }

    @Test
    fun `verify A1 to A5 packages have correct updated 100 percent topology counts`() = runBlocking {
        val catalog = repository.scanDynamicCatalog()
        val a1 = catalog.firstOrNull { it.id == "A1_blossom_bicycle" }
        assertNotNull(a1)
        assertEquals(400, a1?.totalCells)
        assertEquals(100, a1?.totalRegions)

        val a2 = catalog.firstOrNull { it.id == "A2_royal_koi_pair" }
        assertNotNull(a2)
        assertEquals(416, a2?.totalCells)
        assertEquals(130, a2?.totalRegions)

        val a3 = catalog.firstOrNull { it.id == "A3_cozy_tea_corner" }
        assertNotNull(a3)
        assertEquals(393, a3?.totalCells)
        assertEquals(128, a3?.totalRegions)

        val a4 = catalog.firstOrNull { it.id == "A4_moon_fairy_portrait" }
        assertNotNull(a4)
        assertEquals(424, a4?.totalCells)
        assertEquals(107, a4?.totalRegions)

        val a5 = catalog.firstOrNull { it.id == "A5_lucky_shiba_garden" }
        assertNotNull(a5)
        assertEquals(383, a5?.totalCells)
        assertEquals(135, a5?.totalRegions)
    }

    @Test
    fun `test gameplay engine invariants - wrong color produces no mutation`() = runBlocking {
        val artwork = repository.getArtwork("A1_blossom_bicycle")
        val decodedMap = repository.loadCellIdMap("A1_blossom_bicycle")

        val targetRegion = artwork.regions.first()
        val correctColor = targetRegion.colorId
        val wrongColor = if (correctColor == 1) 2 else 1

        val engine = GameplayCoreEngine(
            artwork = artwork,
            initialCompletedRegionIds = emptySet(),
            initialSelectedColorId = wrongColor
        )

        val targetCellId = targetRegion.cellIds.first()

        // Tap cell with wrong selected color
        val result = engine.processCellTap(targetCellId)
        assertTrue(result is TapResolutionResult.IgnoredWrongColor)
        assertTrue(!engine.completedRegionIds.contains(targetRegion.id))
    }

    @Test
    fun `test gameplay engine invariants - completed region tap is idempotent`() = runBlocking {
        val artwork = repository.getArtwork("A2_royal_koi_pair")
        val decodedMap = repository.loadCellIdMap("A2_royal_koi_pair")

        val targetRegion = artwork.regions.first()
        val correctColor = targetRegion.colorId

        val engine = GameplayCoreEngine(
            artwork = artwork,
            initialCompletedRegionIds = emptySet(),
            initialSelectedColorId = correctColor
        )

        val targetCellId = targetRegion.cellIds.first()

        // Fill region
        val firstResult = engine.processCellTap(targetCellId)
        assertTrue(firstResult is TapResolutionResult.Success)
        assertTrue(engine.completedRegionIds.contains(targetRegion.id))

        // Tap again on completed region
        val secondResult = engine.processCellTap(targetCellId)
        assertTrue(secondResult is TapResolutionResult.IgnoredAlreadyCompleted)
        assertTrue(engine.completedRegionIds.contains(targetRegion.id))
    }

    @Test
    fun `test coordinate transform roundtrip`() {
        val transform = ArtworkTransform(
            scale = 1.5f,
            translationX = 100f,
            translationY = 200f,
            artworkWidth = 1254,
            artworkHeight = 1254,
            viewportWidth = 1080f,
            viewportHeight = 1920f
        )

        val artworkPt = Offset(500f, 600f)
        val screenPt = transform.artworkToScreen(artworkPt)
        val roundtrip = transform.screenToArtwork(screenPt)

        assertNotNull(roundtrip)
        assertEquals(artworkPt.x, roundtrip!!.x, 0.01f)
        assertEquals(artworkPt.y, roundtrip.y, 0.01f)
    }

    @Test
    fun `verify catalog rejection invariants for invalid or incomplete packages`() = runBlocking {
        val catalog = repository.scanDynamicCatalog()

        // CATALOG-001: No duplicate IDs in catalog
        val ids = catalog.map { it.id }
        assertEquals("Catalog must not contain duplicate IDs", ids.size, ids.distinct().size)

        // CATALOG-005: gentle_bear lacks safe_anchors.json and subject_mask.png; must be excluded
        assertTrue("Incomplete package gentle_bear must be rejected", catalog.none { it.id == "gentle_bear" })

        // Valid gentle_bear_v2 must be present
        assertTrue("Complete package gentle_bear_v2 must be admitted", catalog.any { it.id == "gentle_bear_v2" })

        // CATALOG-002: Every admitted package must have folderName == id
        for (item in catalog) {
            val expectedFolder = "artworks/${item.id}"
            assertEquals("Asset folder must match item ID", expectedFolder, item.assetFolder)
        }
    }

    @Test
    fun `verify deterministic catalog sorting behavior`() = runBlocking {
        val catalog = repository.scanDynamicCatalog()
        val sortedCopy = catalog.sortedWith(
            compareBy<com.example.data.repository.ArtworkCatalogItem> { it.sortOrder }
                .thenBy { it.releaseOrder }
                .thenBy { it.id }
        )
        assertEquals("Catalog items must be deterministically sorted", sortedCopy.map { it.id }, catalog.map { it.id })
    }

    @Test
    fun `verify HRW rendezvous hashing daily stability across date seeds`() {
        val testIds = listOf("happy_corgi_v3", "gentle_bear_v2", "A1_blossom_bicycle", "A2_royal_koi_pair", "A3_cozy_tea_corner")
        
        fun hrwPick(dateSeed: String, candidateIds: List<String>): String {
            return candidateIds.maxByOrNull { id ->
                val str = "$dateSeed:$id"
                val bytes = str.toByteArray(Charsets.UTF_8)
                var hash = -3750763025362895003L
                for (b in bytes) {
                    hash = hash xor (b.toLong() and 0xFF)
                    hash *= 1099511628211L
                }
                hash
            } ?: candidateIds.first()
        }

        // Deterministic check: same dateSeed produces same winner
        val pick1 = hrwPick("daily_2026_247", testIds)
        val pick2 = hrwPick("daily_2026_247", testIds)
        assertEquals("Same dateSeed must always pick the same artwork", pick1, pick2)

        // HRW stability property: adding an artwork only reassigns if new artwork wins
        val addedIds = testIds + "B2_flower"
        val pickWithAdded = hrwPick("daily_2026_247", addedIds)
        assertTrue("HRW winner must either stay pick1 or be the newly added candidate", pickWithAdded == pick1 || pickWithAdded == "B2_flower")
    }

    @Test
    fun `canary auto-discovery discovers new package without Kotlin edits`() = runBlocking {
        val artworksDir = if (java.io.File("src/main/assets/artworks").exists()) {
            java.io.File("src/main/assets/artworks")
        } else {
            java.io.File("app/src/main/assets/artworks")
        }

        val canaryDir = java.io.File(artworksDir, "AUTO_DISCOVERY_TEST")
        val b4Dir = java.io.File(artworksDir, "B4_food")

        if (!b4Dir.exists()) {
            return@runBlocking
        }

        try {
            canaryDir.mkdirs()
            java.io.File(b4Dir, "cell_id_map.png").copyTo(java.io.File(canaryDir, "cell_id_map.png"), overwrite = true)
            java.io.File(b4Dir, "safe_anchors.json").copyTo(java.io.File(canaryDir, "safe_anchors.json"), overwrite = true)
            java.io.File(b4Dir, "settled_texture.png").copyTo(java.io.File(canaryDir, "settled_texture.png"), overwrite = true)
            java.io.File(b4Dir, "subject_mask.png").copyTo(java.io.File(canaryDir, "subject_mask.png"), overwrite = true)

            val originalJson = java.io.File(b4Dir, "artwork.json").readText()
            val canaryJson = originalJson
                .replace("\"id\": \"B4_food\"", "\"id\": \"AUTO_DISCOVERY_TEST\"")
                .replace("\"title\": \"B4_food\"", "\"title\": \"Canary Test\"")
                .replace("\"category\": \"benchmark\"", "\"categoryId\": \"canary_category\",\n  \"categoryLabel\": \"Canary Category\",\n  \"category\": \"benchmark\"")

            java.io.File(canaryDir, "artwork.json").writeText(canaryJson)

            // Also copy to merged assets directory if it exists so Robolectric's asset manager sees it
            val mergedAssetsDir = if (java.io.File("build/intermediates/assets/debug/mergeDebugAssets/artworks").exists()) {
                java.io.File("build/intermediates/assets/debug/mergeDebugAssets/artworks")
            } else if (java.io.File("app/build/intermediates/assets/debug/mergeDebugAssets/artworks").exists()) {
                java.io.File("app/build/intermediates/assets/debug/mergeDebugAssets/artworks")
            } else null

            val mergedCanaryDir = mergedAssetsDir?.let { java.io.File(it, "AUTO_DISCOVERY_TEST") }
            if (mergedCanaryDir != null) {
                canaryDir.copyRecursively(mergedCanaryDir, overwrite = true)
            }

            try {
                // Instantiate a new repository without cached catalog
                val testRepo = AssetArtworkRepository(context)
                val catalog = testRepo.scanDynamicCatalog()

                val canaryItem = catalog.firstOrNull { it.id == "AUTO_DISCOVERY_TEST" }
                assertNotNull("Canary package must be auto-discovered dynamically without Kotlin edits", canaryItem)
                assertEquals("AUTO_DISCOVERY_TEST", canaryItem?.id)
                assertEquals("Canary Test", canaryItem?.title)
                assertEquals("Canary Category", canaryItem?.category)
                assertEquals(359, canaryItem?.totalCells)
                assertEquals(32, canaryItem?.totalRegions)
            } finally {
                mergedCanaryDir?.deleteRecursively()
            }
        } finally {
            canaryDir.deleteRecursively()
        }
    }
}
