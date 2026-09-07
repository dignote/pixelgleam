package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artwork_progress")
data class ArtworkProgress(
    @PrimaryKey val artworkId: String,
    val completedRegionIdsJson: String,
    val isCompleted: Boolean = false,
    val lastSelectedColorId: Int? = null,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
