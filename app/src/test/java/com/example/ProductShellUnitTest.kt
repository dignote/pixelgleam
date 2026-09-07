package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.AppSettingsRepository
import com.example.data.repository.ArtworkProgressRepository
import com.example.ui.shell.ArtworkStatus
import com.example.ui.shell.MyArtFilter
import com.example.ui.shell.ShellTab
import com.example.viewmodel.GameLoadState
import com.example.viewmodel.PixelGleamViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class ProductShellUnitTest {

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var database: AppDatabase
    private lateinit var progressRepository: ArtworkProgressRepository
    private lateinit var settingsRepository: AppSettingsRepository
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
        context = ApplicationProvider.getApplicationContext()
        application = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        progressRepository = ArtworkProgressRepository(database)
        settingsRepository = AppSettingsRepository(context)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun awaitNonEmptySummaries(
        viewModel: PixelGleamViewModel
    ): List<com.example.ui.shell.ArtworkSummaryItem> = withTimeout(10_000) {
        while (viewModel.artworkSummaries.value.isEmpty()) {
            mainDispatcher.scheduler.runCurrent()
            delay(10)
        }
        viewModel.artworkSummaries.value
    }

    private suspend fun awaitDailyInfo(
        viewModel: PixelGleamViewModel,
        artworkId: String? = null
    ) = withTimeout(10_000) {
        while (viewModel.dailyInfo.value == null ||
            (artworkId != null && viewModel.dailyInfo.value?.featuredArtwork?.id != artworkId)
        ) {
            mainDispatcher.scheduler.runCurrent()
            delay(10)
        }
        viewModel.dailyInfo.value
    }

    private suspend fun awaitPersistedDailyAssignment(dateKey: String) = withTimeout(10_000) {
        var persisted = progressRepository.getDailyProgress(dateKey)
        while (persisted == null) {
            mainDispatcher.scheduler.runCurrent()
            delay(10)
            persisted = progressRepository.getDailyProgress(dateKey)
        }
        persisted
    }

    @Test
    fun testAppSettingsRepository_DefaultValuesAndToggles() = runBlocking {
        // Defaults
        assertTrue("Music should be enabled by default", settingsRepository.musicEnabled.value)
        assertTrue("SFX should be enabled by default", settingsRepository.sfxEnabled.value)
        assertTrue("Haptics should be enabled by default", settingsRepository.hapticsEnabled.value)
        assertFalse("Tutorial should not be completed initially", settingsRepository.tutorialCompleted.value)

        // Mutate
        settingsRepository.setMusicEnabled(false)
        assertFalse(settingsRepository.musicEnabled.value)

        settingsRepository.setSfxEnabled(false)
        assertFalse(settingsRepository.sfxEnabled.value)

        settingsRepository.setHapticsEnabled(false)
        assertFalse(settingsRepository.hapticsEnabled.value)

        settingsRepository.setTutorialCompleted(true)
        assertTrue(settingsRepository.tutorialCompleted.value)

        // Restore
        settingsRepository.setMusicEnabled(true)
        settingsRepository.setSfxEnabled(true)
        settingsRepository.setHapticsEnabled(true)
    }

    @Test
    fun testArtworkProgressRepository_DeleteAllProgress() = runBlocking {
        // Save progress for multiple artworks
        progressRepository.saveProgress("art_1", setOf(1, 2, 3), false, 1)
        progressRepository.saveProgress("art_2", setOf(4, 5), true, 2)

        val beforeList = progressRepository.allProgress.first()
        assertEquals(2, beforeList.size)

        // Delete all
        progressRepository.deleteAllProgress()

        val afterList = progressRepository.allProgress.first()
        assertTrue("All progress should be cleared after deleteAllProgress", afterList.isEmpty())
    }

    @Test
    fun testPixelGleamViewModel_ShellTabNavigation() = runBlocking {
        val viewModel = PixelGleamViewModel(application, database)

        // Initial tab is Gallery
        assertEquals(ShellTab.GALLERY, viewModel.selectedTab.value)

        // Select Daily
        viewModel.selectTab(ShellTab.DAILY)
        assertEquals(ShellTab.DAILY, viewModel.selectedTab.value)

        // Select My Art
        viewModel.selectTab(ShellTab.MY_ART)
        assertEquals(ShellTab.MY_ART, viewModel.selectedTab.value)

        // Select Settings
        viewModel.selectTab(ShellTab.SETTINGS)
        assertEquals(ShellTab.SETTINGS, viewModel.selectedTab.value)
    }

    @Test
    fun testPixelGleamViewModel_FiltersAndDetailDialog() = runBlocking {
        val viewModel = PixelGleamViewModel(application, database)

        // Category filter
        assertEquals("All", viewModel.selectedCategory.value)
        viewModel.selectCategory("Botanical & Nature")
        assertEquals("Botanical & Nature", viewModel.selectedCategory.value)

        // My Art filter
        assertEquals(MyArtFilter.ALL, viewModel.myArtFilter.value)
        viewModel.selectMyArtFilter(MyArtFilter.COMPLETED)
        assertEquals(MyArtFilter.COMPLETED, viewModel.myArtFilter.value)

        // Artwork Detail Sheet
        assertNull(viewModel.selectedDetailArtworkId.value)
        viewModel.showArtworkDetail("happy_corgi_v3")
        assertEquals("happy_corgi_v3", viewModel.selectedDetailArtworkId.value)
        viewModel.dismissArtworkDetail()
        assertNull(viewModel.selectedDetailArtworkId.value)

        // Tutorial Dialog
        assertFalse(viewModel.showTutorialDialog.value)
        viewModel.openTutorial()
        assertTrue(viewModel.showTutorialDialog.value)
        viewModel.dismissTutorial(markAsCompleted = true)
        assertFalse(viewModel.showTutorialDialog.value)
        assertTrue(viewModel.tutorialCompleted.value)
    }

    @Test
    fun testPixelGleamViewModel_CatalogSummariesAndDailyComputation() = runBlocking {
        val viewModel = PixelGleamViewModel(application, database)

        val catalog = viewModel.catalogRepository.getCatalog().first()
        assertTrue("Catalog items should be loaded dynamically", catalog.isNotEmpty())

        // Verify gentle_bear_v2 is present in Animals & Friends category
        val bear = catalog.firstOrNull { it.id == "gentle_bear_v2" }
        assertNotNull(bear)
        assertEquals("Gentle Bear", bear?.title)
        assertEquals("Animals & Friends", bear?.category)
        assertEquals(26, bear?.totalRegions)
        assertEquals(372, bear?.totalCells)

        // Wait for initial dailyInfo emission
        val dailyInfo = awaitDailyInfo(viewModel)
        assertNotNull(dailyInfo)
        assertNotNull(dailyInfo?.featuredArtwork)
        assertEquals(7, dailyInfo?.weeklyDays?.size)
        assertTrue("Exactly one day should be marked today", dailyInfo?.weeklyDays?.count { it.isToday } == 1)

        // Test explicit persisted daily challenge assignment override
        val calendar = java.util.Calendar.getInstance()
        val todayKey = String.format("%04d-%02d-%02d", calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.DAY_OF_MONTH))
        progressRepository.saveDailyProgress(todayKey, "gentle_bear_v2", false)

        val updatedDailyInfo = awaitDailyInfo(viewModel, "gentle_bear_v2")
        assertNotNull(updatedDailyInfo)
        assertEquals("gentle_bear_v2", updatedDailyInfo?.featuredArtwork?.id)
    }

    @Test
    fun testGentleBear_LoadAndGameplay() = runBlocking {
        val assetRepo = com.example.data.repository.AssetArtworkRepository(context)
        val artwork = assetRepo.getArtwork("gentle_bear_v2")
        assertNotNull(artwork)
        assertEquals("gentle_bear_v2", artwork.id)
        assertEquals("Gentle Bear", artwork.title)
        assertEquals("Animals & Friends", artwork.category)
        assertEquals(1254, artwork.canvas.width)
        assertEquals(1254, artwork.canvas.height)
        assertEquals(9, artwork.palette.size)
        assertEquals(372, artwork.cells.size)
        assertEquals(26, artwork.regions.size)

        // Texture & Cell map verification
        val texture = assetRepo.loadSettledTexture("gentle_bear_v2")
        assertNotNull(texture)
        assertEquals(1254, texture.width)
        assertEquals(1254, texture.height)

        val cellMap = assetRepo.loadCellIdMap("gentle_bear_v2")
        assertNotNull(cellMap)
        assertEquals(1254, cellMap.width)
        assertEquals(1254, cellMap.height)
        assertEquals(1254 * 1254, cellMap.data.size)

        // Topology and color consistency validation
        val allRegionIds = artwork.regions.map { it.id }.toSet()
        artwork.cells.forEach { cell ->
            assertTrue("Cell ${cell.id} must map to a valid region", allRegionIds.contains(cell.regionId))
            val region = artwork.regions.first { it.id == cell.regionId }
            assertEquals("Cell color must match region color", region.colorId, cell.colorId)
        }

        // Gameplay core engine verification with gentle_bear
        val engine = com.example.core.engine.GameplayCoreEngine(artwork, emptySet())
        val firstCell = artwork.cells.first()
        engine.selectColor(firstCell.colorId)
        val result = engine.processCellTap(firstCell.id)
        assertTrue("Tapping matching cell should succeed", result is com.example.core.engine.TapResolutionResult.Success)
        assertTrue("Region should be marked completed", engine.completedRegionIds.contains(firstCell.regionId))
    }

    @Test
    fun testAllCatalogArtworks_LoadInViewModelSuccessfully() = runBlocking {
        val viewModel = PixelGleamViewModel(application, database)

        val catalogItems = withTimeout(10_000) {
            while (viewModel.catalogItems.isEmpty()) {
                mainDispatcher.scheduler.runCurrent()
                delay(10)
            }
            viewModel.catalogItems
        }

        for (item in catalogItems) {
            viewModel.selectArtwork(item.id)
            var attempts = 0
            while (viewModel.loadState.value !is GameLoadState.Success && attempts < 100) {
                mainDispatcher.scheduler.runCurrent()
                delay(50)
                attempts++
            }
            val state = viewModel.loadState.value
            assertTrue("Artwork ${item.id} should load to Success state, but was $state", state is GameLoadState.Success)
            assertNotNull("Revealed bitmap should not be null for ${item.id}", viewModel.revealedBitmap.value)
        }
    }

    @Test
    fun testArtworkMetadataNormalizationAndGalleryFiltering() = runBlocking {
        val viewModel = PixelGleamViewModel(application, database)

        val fullSummaries = awaitNonEmptySummaries(viewModel)
        mainDispatcher.scheduler.runCurrent()
        val gallerySummaries = withTimeout(10_000) {
            while (viewModel.galleryArtworkSummaries.value.isEmpty()) {
                mainDispatcher.scheduler.runCurrent()
                delay(10)
            }
            viewModel.galleryArtworkSummaries.value
        }

        // Full catalog summaries must include internal/retired artworks
        val internalItem = fullSummaries.firstOrNull { it.id == "B2_flower" }
        assertNotNull("Full catalog must preserve internal benchmarks", internalItem)
        assertEquals("internal", internalItem?.lifecycle)
        assertEquals(false, internalItem?.galleryVisible)

        // Gallery summaries must filter out non-gallery items
        val galleryItemIds = gallerySummaries.map { it.id }.toSet()
        assertFalse("Gallery must not contain internal benchmark B2_flower", galleryItemIds.contains("B2_flower"))
        assertTrue("Gallery must contain active item happy_corgi_v3", galleryItemIds.contains("happy_corgi_v3"))

        // All gallery items must have active lifecycle and true galleryVisible
        gallerySummaries.forEach { item ->
            assertEquals("Gallery item must be active", "active", item.lifecycle)
            assertTrue("Gallery item must be galleryVisible", item.galleryVisible)
        }

        // Test CategoryTaxonomyContract ordering
        val categoryOrder = com.example.data.repository.CategoryTaxonomyContract.CANONICAL_CATEGORY_ORDER
        val animalsIndex = com.example.data.repository.CategoryTaxonomyContract.getCategoryOrderIndex("animals_friends")
        val cozyIndex = com.example.data.repository.CategoryTaxonomyContract.getCategoryOrderIndex("cozy_scenes")
        assertTrue("animals_friends comes before cozy_scenes", animalsIndex < cozyIndex)
        assertEquals(Int.MAX_VALUE, com.example.data.repository.CategoryTaxonomyContract.getCategoryOrderIndex("unknown_category"))
    }

    @Test
    fun testSevenProductionArtworksHaveCanonicalCatalogAndGalleryMetadata() = runBlocking {
        val expectedMetadata = mapOf(
            "sakura_cat_window" to ("animals_friends" to "Animals & Friends"),
            "rainy_bookshop" to ("cozy_scenes" to "Cozy Scenes"),
            "matcha_tea_set" to ("food_treats" to "Food & Treats"),
            "forest_witch" to ("characters" to "Characters"),
            "hummingbird_hibiscus" to ("flowers_nature" to "Flowers & Nature"),
            "butterfly_bloom" to ("ornamental_patterns" to "Ornamental Patterns"),
            "aurora_mandala" to ("geometric_mandalas" to "Geometric Mandalas")
        )
        val viewModel = PixelGleamViewModel(application, database)
        val catalog = viewModel.catalogRepository.getCatalog().first()
        val dynamicCatalog = com.example.data.repository.AssetArtworkRepository(context).scanDynamicCatalog()
        val galleryIds = awaitNonEmptySummaries(viewModel)
            .filter { it.lifecycle == "active" && it.galleryVisible }
            .map { it.id }
            .toSet()

        expectedMetadata.forEach { (id, expected) ->
            val (categoryId, categoryLabel) = expected
            val catalogItems = catalog.filter { it.id == id }
            val dynamicItems = dynamicCatalog.filter { it.id == id }
            assertEquals("Catalog must contain $id exactly once", 1, catalogItems.size)
            assertEquals("Dynamic discovery must contain $id exactly once", 1, dynamicItems.size)

            val catalogItem = catalogItems.single()
            val dynamicItem = dynamicItems.single()
            assertEquals(categoryId, catalogItem.categoryId)
            assertEquals(categoryLabel, catalogItem.category)
            assertEquals("active", catalogItem.lifecycle)
            assertTrue(catalogItem.galleryVisible)
            assertEquals("Catalog and package category IDs must agree for $id", categoryId, dynamicItem.categoryId)
            assertEquals("Catalog and package labels must agree for $id", categoryLabel, dynamicItem.category)
            assertEquals("Catalog and package lifecycle must agree for $id", catalogItem.lifecycle, dynamicItem.lifecycle)
            assertEquals("Catalog and package gallery visibility must agree for $id", catalogItem.galleryVisible, dynamicItem.galleryVisible)
            assertTrue("Active gallery artwork $id must be displayed in Gallery", id in galleryIds)
        }
    }

    @Test
    fun testDailyAssignmentUsesEligibleArtworkAndPersistsInitialHrwWinner() = runBlocking {
        val viewModel = PixelGleamViewModel(application, database)
        val daily = awaitDailyInfo(viewModel)
        val featured = checkNotNull(daily?.featuredArtwork)

        assertEquals("Daily HRW candidate must be active", "active", featured.lifecycle)
        assertTrue("Daily HRW candidate must be gallery-visible", featured.galleryVisible)

        val calendar = java.util.Calendar.getInstance()
        val todayKey = String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
        val persisted = awaitPersistedDailyAssignment(todayKey)
        assertEquals("Initial HRW winner must be locked to today's date", featured.id, persisted.artworkId)
        assertFalse("A new Daily assignment must not start completed", persisted.isCompleted)
    }
}
