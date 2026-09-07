package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtworkProgressDao {
    @Query("SELECT * FROM artwork_progress WHERE artworkId = :artworkId")
    suspend fun getProgress(artworkId: String): ArtworkProgress?

    @Query("SELECT * FROM artwork_progress")
    fun getAllProgress(): Flow<List<ArtworkProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ArtworkProgress)

    @Query("DELETE FROM artwork_progress WHERE artworkId = :artworkId")
    suspend fun deleteProgress(artworkId: String)

    @Query("DELETE FROM artwork_progress")
    suspend fun deleteAllProgress()
}
