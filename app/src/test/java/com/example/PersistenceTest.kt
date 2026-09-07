package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.engine.GameplayCoreEngine
import com.example.core.engine.TapResolutionResult
import com.example.data.Artwork
import com.example.data.db.AppDatabase
import com.example.data.db.ArtworkProgress
import com.example.data.repository.ArtworkProgressRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersistenceTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ArtworkProgressRepository

    private val sampleArtworkJson = """
    {
      "id": "test_art",
      "title": "Test Art",
      "category": "Testing",
      "canvas": { "width": 100, "height": 100 },
      "palette": [
        { "id": 1, "hex": "#FF0000", "name": "Ruby Red" },
        { "id": 2, "hex": "#0000FF", "name": "Sky Blue" }
      ],
      "cells": [
        { "id": 101, "center": [10.0, 10.0], "colorId": 1, "regionId": 10, "neighbors": [102] },
        { "id": 102, "center": [15.0, 15.0], "colorId": 1, "regionId": 10, "neighbors": [101] },
        { "id": 201, "center": [40.0, 40.0], "colorId": 2, "regionId": 20, "neighbors": [202] },
        { "id": 202, "center": [45.0, 45.0], "colorId": 2, "regionId": 20, "neighbors": [201] },
        { "id": 301, "center": [70.0, 70.0], "colorId": 1, "regionId": 30, "neighbors": [] }
      ],
      "regions": [
        { "id": 10, "colorId": 1, "cellIds": [101, 102] },
        { "id": 20, "colorId": 2, "cellIds": [201, 202] },
        { "id": 30, "colorId": 1, "cellIds": [301] }
      ]
    }
    """.trimIndent()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ArtworkProgressRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSaveAndRestoreExactCompletedRegions() = runBlocking {
        val validRegionIds = setOf(10, 20, 30)
        val initialSaved = setOf(10, 30)

        repository.saveProgress(
            artworkId = "test_art",
            completedRegionIds = initialSaved,
            isCompleted = false,
            selectedColorId = 2
        )

        val restoredIds = repository.getValidCompletedRegionIds("test_art", validRegionIds)
        assertEquals(setOf(10, 30), restoredIds)

        val savedColor = repository.getSavedSelectedColorId("test_art")
        assertEquals(2, savedColor)

        val isCompleted = repository.isArtworkCompleted("test_art")
        assertFalse(isCompleted)
    }

    @Test
    fun testCorruptSaveJsonHandling() = runBlocking {
        val validRegionIds = setOf(10, 20, 30)

        // Directly insert corrupt JSON data into DAO
        database.artworkProgressDao().saveProgress(
            ArtworkProgress(
                artworkId = "test_art",
                completedRegionIdsJson = "MALFORMED_JSON_STRING_NOT_AN_ARRAY{123,456",
                isCompleted = false
            )
        )

        // Must handle corruption safely without crashing, returning emptySet
        val restored = repository.getValidCompletedRegionIds("test_art", validRegionIds)
        assertTrue(restored.isEmpty())
    }

    @Test
    fun testStaleOrInvalidRegionIdsFiltered() = runBlocking {
        val validRegionIds = setOf(10, 20, 30)

        // Save contains region IDs from obsolete versions or corrupted entries
        database.artworkProgressDao().saveProgress(
            ArtworkProgress(
                artworkId = "test_art",
                completedRegionIdsJson = "[10, 99999, -42, 8888, 30]",
                isCompleted = false
            )
        )

        val restored = repository.getValidCompletedRegionIds("test_art", validRegionIds)
        // Only valid IDs (10 and 30) must be accepted
        assertEquals(setOf(10, 30), restored)
    }

    @Test
    fun testIdempotentCompletionAfterRestore() = runBlocking {
        val artwork = Artwork.parseFromJson(sampleArtworkJson)
        val restoredCompletedIds = setOf(10)

        val engine = GameplayCoreEngine(
            artwork = artwork,
            initialCompletedRegionIds = restoredCompletedIds,
            initialSelectedColorId = 1
        )

        // Tapping already completed cell in region 10
        val tapResultAlreadyCompleted = engine.processCellTap(101)
        assertTrue(tapResultAlreadyCompleted is TapResolutionResult.IgnoredAlreadyCompleted)
        assertEquals(setOf(10), engine.completedRegionIds)

        // Tapping new cell with matching color (Color 1, Region 30, Cell 301)
        val tapResultSuccess = engine.processCellTap(301)
        assertTrue(tapResultSuccess is TapResolutionResult.Success)
        assertEquals(setOf(10, 30), engine.completedRegionIds)

        // Save updated progress
        repository.saveProgress(
            artworkId = artwork.id,
            completedRegionIds = engine.completedRegionIds,
            isCompleted = false,
            selectedColorId = 1
        )

        // Verify restoration after simulated restart
        val restoredAgain = repository.getValidCompletedRegionIds(artwork.id, setOf(10, 20, 30))
        assertEquals(setOf(10, 30), restoredAgain)
    }

    @Test
    fun testResetProgress() = runBlocking {
        repository.saveProgress(
            artworkId = "test_art",
            completedRegionIds = setOf(10, 20),
            isCompleted = true,
            selectedColorId = 1
        )

        repository.deleteProgress("test_art")

        val restored = repository.getValidCompletedRegionIds("test_art", setOf(10, 20, 30))
        assertTrue(restored.isEmpty())

        val savedColor = repository.getSavedSelectedColorId("test_art")
        assertNull(savedColor)
    }

    @Test
    fun testAppPreferencesPersistence() = runBlocking {
        repository.savePreference("sound_enabled", "true")
        repository.savePreference("haptics_enabled", "false")
        repository.savePreference("last_played_artwork", "happy_corgi_v3")

        assertEquals("true", repository.getPreference("sound_enabled", "false"))
        assertEquals("false", repository.getPreference("haptics_enabled", "true"))
        assertEquals("happy_corgi_v3", repository.getPreference("last_played_artwork", "default"))
        assertEquals("fallback", repository.getPreference("non_existent_key", "fallback"))
    }

    @Test
    fun testDailyProgressPersistence() = runBlocking {
        repository.saveDailyProgress(
            dateString = "2026-09-04",
            artworkId = "gentle_bear_v2",
            isCompleted = true
        )

        val progress = repository.getDailyProgress("2026-09-04")
        org.junit.Assert.assertNotNull(progress)
        assertEquals("2026-09-04", progress?.dateString)
        assertEquals("gentle_bear_v2", progress?.artworkId)
        assertTrue(progress?.isCompleted == true)

        val nonExistent = repository.getDailyProgress("2099-01-01")
        assertNull(nonExistent)
    }

    @Test
    fun testRoomMigration1To2PreservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_db"
        context.deleteDatabase(dbName)

        // 1. Create a V1 database schema using SQLite directly
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `artwork_progress` (
                                `artworkId` TEXT NOT NULL PRIMARY KEY,
                                `completedRegionIdsJson` TEXT NOT NULL,
                                `isCompleted` INTEGER NOT NULL,
                                `lastSelectedColorId` INTEGER,
                                `lastUpdatedTimestamp` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `app_preferences` (
                                `key` TEXT NOT NULL PRIMARY KEY,
                                `value` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val v1Db = helper.writableDatabase
        v1Db.execSQL("INSERT INTO artwork_progress (artworkId, completedRegionIdsJson, isCompleted, lastUpdatedTimestamp) VALUES ('test_art_1', '[1,2,3]', 0, 1000)")
        v1Db.execSQL("INSERT INTO app_preferences (`key`, `value`) VALUES ('theme', 'dark')")
        v1Db.close()

        // 2. Open with Room V2 and MIGRATION_1_2
        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val progressRepo = ArtworkProgressRepository(migratedDb)
        runBlocking {
            val restoredRegionIds = progressRepo.getValidCompletedRegionIds("test_art_1", setOf(1, 2, 3, 4))
            assertEquals(setOf(1, 2, 3), restoredRegionIds)

            val restoredPref = progressRepo.getPreference("theme")
            assertEquals("dark", restoredPref)

            // Verify new daily_progress table created by migration is usable
            progressRepo.saveDailyProgress("2026-09-04", "test_art_1", true)
            val daily = progressRepo.getDailyProgress("2026-09-04")
            org.junit.Assert.assertNotNull(daily)
            assertEquals("test_art_1", daily?.artworkId)
            assertTrue(daily?.isCompleted == true)
        }

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}

