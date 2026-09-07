package com.example.ui.shell

enum class ShellTab(val label: String) {
    GALLERY("Gallery"),
    DAILY("Daily"),
    MY_ART("My Art"),
    SETTINGS("Settings")
}

enum class ArtworkStatus {
    NEW,
    IN_PROGRESS,
    COMPLETED
}

enum class MyArtFilter(val label: String) {
    ALL("All"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    NEW("New")
}

data class ArtworkSummaryItem(
    val id: String,
    val title: String,
    val category: String,
    val totalRegions: Int,
    val totalCells: Int,
    val completedRegionsCount: Int,
    val progressPercent: Float,
    val status: ArtworkStatus,
    val categoryId: String = "",
    val lifecycle: String = "active",
    val galleryVisible: Boolean = true
)

data class DailyChallengeInfo(
    val dateFormatted: String,
    val dayOfWeek: String,
    val dayNumber: Int,
    val monthName: String,
    val featuredArtwork: ArtworkSummaryItem,
    val weeklyDays: List<DayChallengeItem>
)

data class DayChallengeItem(
    val dayLabel: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val artwork: ArtworkSummaryItem
)
