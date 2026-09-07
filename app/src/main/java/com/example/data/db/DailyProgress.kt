package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_progress")
data class DailyProgress(
    @PrimaryKey val dateString: String,
    val artworkId: String,
    val isCompleted: Boolean,
    val completedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface DailyProgressDao {
    @Query("SELECT * FROM daily_progress WHERE dateString = :dateString LIMIT 1")
    suspend fun getDailyProgress(dateString: String): DailyProgress?

    @Query("SELECT * FROM daily_progress")
    fun getAllDailyProgress(): Flow<List<DailyProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyProgress(dailyProgress: DailyProgress)
}
