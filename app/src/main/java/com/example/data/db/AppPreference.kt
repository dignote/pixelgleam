package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_preferences")
data class AppPreference(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface AppPreferenceDao {
    @Query("SELECT value FROM app_preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreference(key: String): String?

    @Query("SELECT value FROM app_preferences WHERE `key` = :key LIMIT 1")
    fun observePreference(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreference(preference: AppPreference)

    @Query("SELECT * FROM app_preferences")
    fun getAllPreferences(): Flow<List<AppPreference>>
}
