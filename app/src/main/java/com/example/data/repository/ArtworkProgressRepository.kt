package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.AppPreference
import com.example.data.db.ArtworkProgress
import com.example.data.db.DailyProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray

class ArtworkProgressRepository(private val database: AppDatabase) {

    val allProgress: Flow<List<ArtworkProgress>> = database.artworkProgressDao().getAllProgress()
    val allDailyProgress: Flow<List<DailyProgress>> = database.dailyProgressDao().getAllDailyProgress()

    /**
     * Restores completed region IDs for an artwork, safely parsing JSON and filtering
     * out any stale, invalid, or out-of-bounds IDs against the current artwork valid region set.
     */
    suspend fun getValidCompletedRegionIds(
        artworkId: String,
        validArtworkRegionIds: Set<Int>
    ): Set<Int> = withContext(Dispatchers.IO) {
        try {
            val entity = database.artworkProgressDao().getProgress(artworkId) ?: return@withContext emptySet()
            val rawIds = parseJsonToRegionIds(entity.completedRegionIdsJson)
            // Filter strictly against valid artwork region IDs to handle stale or corrupt IDs safely
            rawIds.filter { validArtworkRegionIds.contains(it) }.toSet()
        } catch (t: Throwable) {
            t.printStackTrace()
            emptySet()
        }
    }

    /**
     * Retrieves the last saved selected color ID for an artwork, if any.
     */
    suspend fun getSavedSelectedColorId(artworkId: String): Int? = withContext(Dispatchers.IO) {
        try {
            val entity = database.artworkProgressDao().getProgress(artworkId)
            entity?.lastSelectedColorId
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Checks if the artwork was marked as completed in persistence.
     */
    suspend fun isArtworkCompleted(artworkId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = database.artworkProgressDao().getProgress(artworkId)
            entity?.isCompleted ?: false
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Persists accepted region completion promptly and durably.
     */
    suspend fun saveProgress(
        artworkId: String,
        completedRegionIds: Set<Int>,
        isCompleted: Boolean,
        selectedColorId: Int? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val jsonArr = JSONArray(completedRegionIds.toList())
            val progress = ArtworkProgress(
                artworkId = artworkId,
                completedRegionIdsJson = jsonArr.toString(),
                isCompleted = isCompleted,
                lastSelectedColorId = selectedColorId,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            database.artworkProgressDao().saveProgress(progress)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    /**
     * Resets progress for an artwork (e.g. user restart/replay).
     */
     suspend fun deleteProgress(artworkId: String) = withContext(Dispatchers.IO) {
         try {
             database.artworkProgressDao().deleteProgress(artworkId)
         } catch (t: Throwable) {
             t.printStackTrace()
         }
     }

    /**
     * Resets all artwork progress (wipe user data).
     */
    suspend fun deleteAllProgress() = withContext(Dispatchers.IO) {
        try {
            database.artworkProgressDao().deleteAllProgress()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    // App Preferences / Settings Persistence
    suspend fun getPreference(key: String, defaultValue: String = ""): String = withContext(Dispatchers.IO) {
        try {
            database.appPreferenceDao().getPreference(key) ?: defaultValue
        } catch (t: Throwable) {
            defaultValue
        }
    }

    suspend fun savePreference(key: String, value: String) = withContext(Dispatchers.IO) {
        try {
            database.appPreferenceDao().savePreference(AppPreference(key, value))
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun observePreference(key: String, defaultValue: String = ""): Flow<String> {
        return database.appPreferenceDao().observePreference(key).map { it ?: defaultValue }
    }

    // Daily Challenge Persistence
    suspend fun getDailyProgress(dateString: String): DailyProgress? = withContext(Dispatchers.IO) {
        try {
            database.dailyProgressDao().getDailyProgress(dateString)
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun saveDailyProgress(dateString: String, artworkId: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        try {
            database.dailyProgressDao().saveDailyProgress(
                DailyProgress(
                    dateString = dateString,
                    artworkId = artworkId,
                    isCompleted = isCompleted,
                    completedTimestamp = System.currentTimeMillis()
                )
            )
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    companion object {
        /**
         * Safe helper to parse JSON array of integers without crashing on malformed strings.
         */
        fun parseJsonToRegionIds(jsonStr: String?): Set<Int> {
            if (jsonStr.isNullOrBlank()) return emptySet()
            val result = HashSet<Int>()
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val id = arr.optInt(i, -1)
                    if (id >= 0) {
                        result.add(id)
                    }
                }
            } catch (t: Throwable) {
                // Return whatever was successfully parsed or emptySet on corruption
            }
            return result
        }
    }
}
