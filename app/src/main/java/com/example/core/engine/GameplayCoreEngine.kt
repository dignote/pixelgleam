package com.example.core.engine

import com.example.data.Artwork
import com.example.data.PaletteColor
import com.example.data.Region

enum class PrimaryGameplayState {
    LOADING,
    PLAYING,
    PAUSED,
    COMPLETING,
    COMPLETED
}

sealed interface TapResolutionResult {
    data class Success(
        val completedRegion: Region,
        val colorId: Int,
        val isColorCompleted: Boolean,
        val isArtworkCompleted: Boolean
    ) : TapResolutionResult

    object IgnoredAlreadyCompleted : TapResolutionResult
    data class IgnoredWrongColor(val expectedColorId: Int, val tappedColorId: Int) : TapResolutionResult
    object IgnoredVoidOrOutOfBounds : TapResolutionResult
    object IgnoredInvalidState : TapResolutionResult
}

data class GameplayProgress(
    val totalRegions: Int,
    val completedRegionCount: Int,
    val totalCells: Int,
    val completedCellCount: Int,
    val progressFraction: Float,
    val isCompleted: Boolean
)

data class ColorProgress(
    val color: PaletteColor,
    val totalRegions: Int,
    val completedRegions: Int,
    val totalCells: Int,
    val completedCells: Int,
    val isCompleted: Boolean
)

class GameplayCoreEngine(
    val artwork: Artwork,
    initialCompletedRegionIds: Set<Int> = emptySet(),
    initialSelectedColorId: Int? = null
) {
    var primaryState: PrimaryGameplayState = PrimaryGameplayState.PLAYING
        private set

    private val _completedRegionIds = mutableSetOf<Int>()
    val completedRegionIds: Set<Int> get() = _completedRegionIds

    var selectedColorId: Int = initialSelectedColorId ?: artwork.palette.firstOrNull()?.id ?: 1
        private set

    init {
        // Safe restore: only admit region IDs actually present in the artwork (INV-082)
        val validRegionIds = artwork.regions.map { it.id }.toSet()
        val sanitizedInitial = initialCompletedRegionIds.filter { it in validRegionIds }
        _completedRegionIds.addAll(sanitizedInitial)

        if (isAllRegionsCompleted()) {
            primaryState = PrimaryGameplayState.COMPLETED
        }
    }

    fun selectColor(colorId: Int) {
        if (artwork.palette.any { it.id == colorId }) {
            selectedColorId = colorId
        }
    }

    fun pause() {
        if (primaryState == PrimaryGameplayState.PLAYING) {
            primaryState = PrimaryGameplayState.PAUSED
        }
    }

    fun resume() {
        if (primaryState == PrimaryGameplayState.PAUSED) {
            primaryState = PrimaryGameplayState.PLAYING
        }
    }

    fun processCellTap(cellId: Int): TapResolutionResult {
        // INV-031: Commands only accepted in PLAYING
        if (primaryState != PrimaryGameplayState.PLAYING) {
            return TapResolutionResult.IgnoredInvalidState
        }

        // Void or 0 Cell ID is strictly non-interactive (INV-041)
        if (cellId <= 0) {
            return TapResolutionResult.IgnoredVoidOrOutOfBounds
        }

        val cell = artwork.cellMap[cellId] ?: return TapResolutionResult.IgnoredVoidOrOutOfBounds
        val region = artwork.regionMap[cell.regionId] ?: return TapResolutionResult.IgnoredVoidOrOutOfBounds

        // INV-012: Resolving an already completed region is idempotent
        if (_completedRegionIds.contains(region.id)) {
            return TapResolutionResult.IgnoredAlreadyCompleted
        }

        // INV-011: Selected color must match region color
        if (region.colorId != selectedColorId) {
            return TapResolutionResult.IgnoredWrongColor(
                expectedColorId = selectedColorId,
                tappedColorId = region.colorId
            )
        }

        // INV-010 & INV-015: Complete the single connected region atomically
        _completedRegionIds.add(region.id)

        val isColorCompleted = isColorFullyCompleted(region.colorId)
        val isArtworkCompleted = isAllRegionsCompleted()

        if (isArtworkCompleted && primaryState == PrimaryGameplayState.PLAYING) {
            primaryState = PrimaryGameplayState.COMPLETED
        }

        return TapResolutionResult.Success(
            completedRegion = region,
            colorId = region.colorId,
            isColorCompleted = isColorCompleted,
            isArtworkCompleted = isArtworkCompleted
        )
    }

    fun completeSelectedColor(colorId: Int): List<Region> {
        val matchingRegions = artwork.regions.filter { it.colorId == colorId && !_completedRegionIds.contains(it.id) }
        matchingRegions.forEach { _completedRegionIds.add(it.id) }
        if (isAllRegionsCompleted() && primaryState == PrimaryGameplayState.PLAYING) {
            primaryState = PrimaryGameplayState.COMPLETED
        }
        return matchingRegions
    }

    fun completeAllRegions(): List<Region> {
        val newlyCompleted = artwork.regions.filter { !_completedRegionIds.contains(it.id) }
        artwork.regions.forEach { _completedRegionIds.add(it.id) }
        if (artwork.regions.isNotEmpty()) {
            primaryState = PrimaryGameplayState.COMPLETED
        }
        return newlyCompleted
    }

    fun isRegionCompleted(regionId: Int): Boolean {
        return _completedRegionIds.contains(regionId)
    }

    fun isCellCompleted(cellId: Int): Boolean {
        val cell = artwork.cellMap[cellId] ?: return false
        return _completedRegionIds.contains(cell.regionId)
    }

    fun isColorFullyCompleted(colorId: Int): Boolean {
        val colorRegions = artwork.regions.filter { it.colorId == colorId }
        if (colorRegions.isEmpty()) return true
        return colorRegions.all { _completedRegionIds.contains(it.id) }
    }

    fun isAllRegionsCompleted(): Boolean {
        if (artwork.regions.isEmpty()) return false
        return artwork.regions.all { _completedRegionIds.contains(it.id) }
    }

    fun getOverallProgress(): GameplayProgress {
        val totalRegions = artwork.regions.size
        val completedRegions = _completedRegionIds.size
        val totalCells = artwork.cells.size
        var completedCells = 0
        for (regionId in _completedRegionIds) {
            val region = artwork.regionMap[regionId]
            if (region != null) {
                completedCells += region.cellIds.size
            }
        }

        val fraction = if (totalRegions > 0) completedRegions.toFloat() / totalRegions.toFloat() else 0f
        return GameplayProgress(
            totalRegions = totalRegions,
            completedRegionCount = completedRegions,
            totalCells = totalCells,
            completedCellCount = completedCells,
            progressFraction = fraction,
            isCompleted = isAllRegionsCompleted()
        )
    }

    fun getColorProgress(colorId: Int): ColorProgress? {
        val color = artwork.palette.firstOrNull { it.id == colorId } ?: return null
        val colorRegions = artwork.regions.filter { it.colorId == colorId }
        val completedCount = colorRegions.count { _completedRegionIds.contains(it.id) }
        val totalCells = colorRegions.sumOf { it.cellIds.size }
        val completedCells = colorRegions.filter { _completedRegionIds.contains(it.id) }.sumOf { it.cellIds.size }

        return ColorProgress(
            color = color,
            totalRegions = colorRegions.size,
            completedRegions = completedCount,
            totalCells = totalCells,
            completedCells = completedCells,
            isCompleted = colorRegions.isNotEmpty() && completedCount == colorRegions.size
        )
    }

    /**
     * Find an uncompleted region for hint targeting.
     * Respects INV-090, INV-091, INV-092.
     */
    fun findHintTargetRegion(): Region? {
        // First try to find an incomplete region of the currently selected color
        val matchingIncomplete = artwork.regions.firstOrNull {
            it.colorId == selectedColorId && !_completedRegionIds.contains(it.id)
        }
        if (matchingIncomplete != null) {
            return matchingIncomplete
        }

        // Fallback to any incomplete region in the artwork
        return artwork.regions.firstOrNull { !_completedRegionIds.contains(it.id) }
    }
}
