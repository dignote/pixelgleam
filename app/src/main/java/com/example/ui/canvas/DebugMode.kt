package com.example.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.example.core.engine.HitResult

enum class DebugMode(val displayName: String) {
    OFF("Normal Play"),
    CELL_IDS("Cell Boundaries & IDs"),
    REGION_IDS("Region Boundaries & IDs"),
    ANCHORS("Safe Anchors & Clearance"),
    TAP_PROBE("Tap Hit Diagnostics")
}

data class TapProbeDiagnostics(
    val screenCoord: Offset,
    val artworkCoord: Offset?,
    val hitResult: HitResult,
    val selectedColorId: Int,
    val exactCellId: Int?,
    val forgivenCellId: Int?,
    val regionId: Int?,
    val colorId: Int?,
    val timestampMs: Long = System.currentTimeMillis()
)
