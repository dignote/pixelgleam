package com.example.ui.canvas

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.HitResult
import com.example.data.Artwork
import com.example.ui.animation.GemFlightVisual
import com.example.ui.animation.GemPresentationManager
import com.example.ui.animation.LandingEffectVisual
import com.example.viewmodel.SparkleParticle
import kotlin.math.sin

@Composable
fun InteractiveMosaicCanvas(
    artwork: Artwork,
    revealedBitmap: Bitmap?,
    cellIdMapData: IntArray?,
    selectedColorId: Int?,
    completedRegionIds: Set<Int>,
    activeParticles: List<SparkleParticle>,
    activeGemFlights: List<GemFlightVisual> = emptyList(),
    activeLandingEffects: List<LandingEffectVisual> = emptyList(),
    gemPresentationManager: GemPresentationManager? = null,
    hintTargetRegionId: Int? = null,
    debugMode: DebugMode = DebugMode.OFF,
    tapProbeDiagnostics: TapProbeDiagnostics? = null,
    onScreenTapped: (tapOffset: Offset, transform: ArtworkTransform) -> Unit,
    onTransformChanged: ((ArtworkTransform) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Pulsing animation for selected color indicator badges
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0EFEA)) // Neutral warm ivory atelier canvas background
            .testTag("interactive_mosaic_canvas")
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val artWidth = artwork.canvas.width
        val artHeight = artwork.canvas.height

        val transform = remember(screenWidth, screenHeight, artWidth, artHeight, scale, panOffset) {
            ArtworkTransform(
                scale = scale,
                translationX = panOffset.x,
                translationY = panOffset.y,
                artworkWidth = artWidth,
                artworkHeight = artHeight,
                viewportWidth = screenWidth,
                viewportHeight = screenHeight
            )
        }

        LaunchedEffect(transform) {
            onTransformChanged?.invoke(transform)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(artwork.id, screenWidth, screenHeight, artWidth, artHeight) {
                    // Two-finger transformation gestures (pan + zoom around focal point)
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (oldScale * zoom).coerceIn(0.5f, 6.0f)
                        val effectiveZoom = if (oldScale > 0f) newScale / oldScale else 1.0f

                        val baseScale = if (artWidth > 0 && artHeight > 0 && screenWidth > 0f && screenHeight > 0f) {
                            (screenWidth / artWidth.toFloat()).coerceAtMost(screenHeight / artHeight.toFloat())
                        } else 1.0f

                        val centerOffsetX = (screenWidth - artWidth * baseScale) / 2.0f
                        val centerOffsetY = (screenHeight - artHeight * baseScale) / 2.0f

                        val centroidRelX = centroid.x - centerOffsetX
                        val centroidRelY = centroid.y - centerOffsetY

                        val newPanX = (panOffset.x - centroidRelX) * effectiveZoom + centroidRelX + pan.x
                        val newPanY = (panOffset.y - centroidRelY) * effectiveZoom + centroidRelY + pan.y

                        scale = newScale
                        val maxPanX = screenWidth * newScale
                        val maxPanY = screenHeight * newScale
                        panOffset = Offset(
                            newPanX.coerceIn(-maxPanX, maxPanX),
                            newPanY.coerceIn(-maxPanY, maxPanY)
                        )
                    }
                }
                .pointerInput(artwork.id, transform) {
                    // Tap gestures with single source of truth coordinate translation
                    detectTapGestures { tapOffset ->
                        onScreenTapped(tapOffset, transform)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val now = System.currentTimeMillis()

                // 1. Layer 1: Render Revealed Settled Mosaic Bitmap via unified transform
                if (revealedBitmap != null) {
                    val screenBounds = transform.artworkToScreen(
                        Rect(
                            left = 0f,
                            top = 0f,
                            right = artWidth.toFloat(),
                            bottom = artHeight.toFloat()
                        )
                    )

                    drawImage(
                        image = revealedBitmap.asImageBitmap(),
                        dstOffset = IntOffset(screenBounds.left.toInt(), screenBounds.top.toInt()),
                        dstSize = IntSize(
                            width = screenBounds.width.toInt().coerceAtLeast(1),
                            height = screenBounds.height.toInt().coerceAtLeast(1)
                        )
                    )
                }

                // 2. Layer 2: Adaptive Number Badges on uncompleted matching cells/regions
                val selectedCol = selectedColorId
                if (selectedCol != null) {
                    val matchingPalette = artwork.palette.firstOrNull { it.id == selectedCol }
                    val colorHex = matchingPalette?.hex ?: "#333333"
                    val isLight = isHexLight(colorHex)
                    val bubbleColor = safeParseColor(colorHex, Color(0xFF555555))
                    val textColor = if (isLight) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
                    val matchingRegions = artwork.regions.filter { it.colorId == selectedCol }

                    // Adaptive anchor selection: at low zoom show region anchors, at high zoom show cell anchors
                    val isZoomedIn = scale >= 1.4f

                    matchingRegions.forEach { region ->
                        if (!completedRegionIds.contains(region.id)) {
                            if (isZoomedIn) {
                                region.cellIds.forEach { cellId ->
                                    val cell = artwork.cellMap[cellId]
                                    if (cell != null) {
                                        val screenCenter = transform.artworkToScreen(Offset(cell.anchorX, cell.anchorY))
                                        drawNumberBadge(
                                            screenCenter = screenCenter,
                                            number = "$selectedCol",
                                            bubbleColor = bubbleColor,
                                            textColor = textColor,
                                            pulseScale = pulseScale,
                                            baseRadius = 11f
                                        )
                                    }
                                }
                            } else {
                                // Single region-level anchor badge
                                val screenCenter = transform.artworkToScreen(Offset(region.anchorX, region.anchorY))
                                drawNumberBadge(
                                    screenCenter = screenCenter,
                                    number = "$selectedCol",
                                    bubbleColor = bubbleColor,
                                    textColor = textColor,
                                    pulseScale = pulseScale,
                                    baseRadius = 13f
                                )
                            }
                        }
                    }
                }

                // 3. Layer 3: Hint Target Beacon
                if (hintTargetRegionId != null) {
                    val targetRegion = artwork.regions.firstOrNull { it.id == hintTargetRegionId }
                    if (targetRegion != null) {
                        val screenCenter = transform.artworkToScreen(Offset(targetRegion.anchorX, targetRegion.anchorY))
                        val beaconRadius = 22f * pulseScale
                        // Pulsing radiant golden ring
                        drawCircle(
                            color = Color(0xFFFFD700).copy(alpha = 0.9f),
                            radius = beaconRadius,
                            center = screenCenter,
                            style = Stroke(width = 4f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.7f),
                            radius = beaconRadius * 0.7f,
                            center = screenCenter,
                            style = Stroke(width = 2f)
                        )
                    }
                }

                // 4. Layer 4: Transient G02 Gem Flight Visuals
                activeGemFlights.forEach { flight ->
                    val progress = flight.currentProgress(now)
                    if (progress in 0f..1f) {
                        val artPos = flight.currentPosition(progress)
                        val screenPos = transform.artworkToScreen(artPos)
                        val gemBmp = gemPresentationManager?.getGemBitmapForColor(flight.colorHex)
                        val parsedColor = safeParseColor(flight.colorHex, Color.White)

                        // Render motion trail behind gem
                        for (t in 1..3) {
                            val trailProgress = (progress - t * 0.04f).coerceIn(0f, 1f)
                            val trailArtPos = flight.currentPosition(trailProgress)
                            val trailScreenPos = transform.artworkToScreen(trailArtPos)
                            val trailAlpha = 0.45f * (1f - t / 4f) * (1f - progress)
                            drawCircle(
                                color = parsedColor.copy(alpha = trailAlpha),
                                radius = (20f * (1f - t * 0.25f) * transform.totalScale.coerceIn(0.8f, 1.8f)).coerceAtLeast(3f),
                                center = trailScreenPos
                            )
                        }

                        if (gemBmp != null) {
                            val gemSize = (38f * (0.85f + 0.3f * sin(progress * Math.PI.toFloat()))) * transform.totalScale.coerceIn(0.8f, 2.0f)
                            val halfSize = gemSize / 2f

                            // Flight trail / motion glow
                            drawCircle(
                                color = parsedColor.copy(alpha = 0.45f * (1f - progress)),
                                radius = gemSize * 0.85f,
                                center = screenPos
                            )

                            // Render Gem Sprite
                            drawImage(
                                image = gemBmp.asImageBitmap(),
                                dstOffset = IntOffset((screenPos.x - halfSize).toInt(), (screenPos.y - halfSize).toInt()),
                                dstSize = IntSize(gemSize.toInt().coerceAtLeast(1), gemSize.toInt().coerceAtLeast(1)),
                                alpha = 1f
                            )
                        } else {
                            drawCircle(
                                color = parsedColor,
                                radius = 14f * transform.totalScale.coerceIn(0.8f, 2.0f),
                                center = screenPos
                            )
                        }
                    }
                }

                // 5. Layer 5: Transient Landing Rings & Bursts
                activeLandingEffects.forEach { landing ->
                    val progress = landing.currentProgress(now)
                    if (progress in 0f..1f) {
                        val screenCenter = transform.artworkToScreen(landing.center)
                        val ringRadius = landing.maxRadius * progress * transform.totalScale.coerceIn(0.8f, 2.0f)
                        val alpha = (1f - progress).coerceIn(0f, 1f)
                        val fxColor = safeParseColor(landing.colorHex, Color.White)

                        // Expanding shockwave ring
                        drawCircle(
                            color = fxColor.copy(alpha = alpha * 0.75f),
                            radius = ringRadius,
                            center = screenCenter,
                            style = Stroke(width = (3f * (1f - progress) + 1f) * transform.totalScale.coerceIn(0.8f, 1.5f))
                        )

                        // Center flash
                        drawCircle(
                            color = Color.White.copy(alpha = alpha * 0.85f),
                            radius = ringRadius * 0.35f,
                            center = screenCenter
                        )
                    }
                }

                // 6. Layer 6: Active Sparkle Particles
                activeParticles.forEach { particle ->
                    val pAlpha = (1f - (particle.life / particle.maxLife)).coerceIn(0f, 1f)
                    val screenPos = transform.artworkToScreen(Offset(particle.x, particle.y))
                    val pSize = particle.size * (1f - (particle.life / particle.maxLife) * 0.5f) * transform.totalScale.coerceIn(0.8f, 1.5f)

                    drawCircle(
                        color = Color(particle.color).copy(alpha = pAlpha),
                        radius = pSize,
                        center = screenPos
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = pAlpha * 0.85f),
                        radius = pSize * 0.4f,
                        center = screenPos
                    )
                }

                // 7. Layer 7: Debug Mode Visualizations (5 Modes)
                when (debugMode) {
                    DebugMode.OFF -> { /* No debug overlay */ }
                    DebugMode.CELL_IDS -> {
                        artwork.cells.forEach { cell ->
                            val isComp = completedRegionIds.contains(cell.regionId)
                            val screenAnchor = transform.artworkToScreen(Offset(cell.anchorX, cell.anchorY))
                            drawCircle(
                                color = if (isComp) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                radius = 6f,
                                center = screenAnchor
                            )
                            val textLayout = textMeasurer.measure(
                                text = "C${cell.id}",
                                style = TextStyle(fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(screenAnchor.x - textLayout.size.width / 2f, screenAnchor.y - textLayout.size.height / 2f)
                            )
                        }
                    }
                    DebugMode.REGION_IDS -> {
                        artwork.regions.forEach { region ->
                            val isComp = completedRegionIds.contains(region.id)
                            val screenAnchor = transform.artworkToScreen(Offset(region.anchorX, region.anchorY))
                            drawCircle(
                                color = if (isComp) Color(0xFF2196F3) else Color(0xFFE91E63),
                                radius = 10f,
                                center = screenAnchor
                            )
                            val textLayout = textMeasurer.measure(
                                text = "R${region.id}",
                                style = TextStyle(fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(screenAnchor.x - textLayout.size.width / 2f, screenAnchor.y - textLayout.size.height / 2f)
                            )
                        }
                    }
                    DebugMode.ANCHORS -> {
                        artwork.cells.forEach { cell ->
                            val isComp = completedRegionIds.contains(cell.regionId)
                            val safeScreen = transform.artworkToScreen(Offset(cell.anchorX, cell.anchorY))
                            val legacyScreen = transform.artworkToScreen(Offset(cell.centerX, cell.centerY))

                            // Draw safe anchor crosshair (Green)
                            drawCircle(
                                color = if (isComp) Color(0xFF4CAF50) else Color(0xFF00E676),
                                radius = 4f,
                                center = safeScreen
                            )
                            // Draw clearance circle
                            val clearance = cell.safeAnchor?.clearance ?: 0f
                            val clearanceScreen = clearance * transform.totalScale
                            if (clearanceScreen > 4f) {
                                drawCircle(
                                    color = Color(0xFF00E676).copy(alpha = 0.3f),
                                    radius = clearanceScreen,
                                    center = safeScreen,
                                    style = Stroke(width = 1f)
                                )
                            }

                            // Draw legacy center (Orange dot) to visualize shift
                            drawCircle(
                                color = Color(0xFFFF5722).copy(alpha = 0.6f),
                                radius = 2.5f,
                                center = legacyScreen
                            )
                        }
                    }
                    DebugMode.TAP_PROBE -> {
                        tapProbeDiagnostics?.let { probe ->
                            // Draw last screen tap location (Cyan crosshair)
                            drawCircle(
                                color = Color.Cyan,
                                radius = 12f,
                                center = probe.screenCoord,
                                style = Stroke(width = 2.5f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3f,
                                center = probe.screenCoord
                            )

                            // Draw forgiveness search radius
                            val forgivenessScreenRadius = 2.0f * transform.totalScale
                            drawCircle(
                                color = Color.Cyan.copy(alpha = 0.35f),
                                radius = forgivenessScreenRadius,
                                center = probe.screenCoord,
                                style = Stroke(width = 1f)
                            )

                            // Connect line to resolved cell anchor if present
                            val resId = probe.hitResult.resolvedCellId
                            if (resId != null) {
                                val cell = artwork.cellMap[resId]
                                if (cell != null) {
                                    val cellScreen = transform.artworkToScreen(Offset(cell.anchorX, cell.anchorY))
                                    drawLine(
                                        color = Color.Green,
                                        start = probe.screenCoord,
                                        end = cellScreen,
                                        strokeWidth = 2f
                                    )
                                    drawCircle(
                                        color = Color.Green,
                                        radius = 6f,
                                        center = cellScreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Diagnostic HUD Card in top-left when in TAP_PROBE mode
            if (debugMode == DebugMode.TAP_PROBE && tapProbeDiagnostics != null) {
                val probe = tapProbeDiagnostics
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xDD000000)
                ) {
                    val statusText = when (val hr = probe.hitResult) {
                        is HitResult.ExactCell -> "EXACT: Cell #${hr.cellId} (Reg #${probe.regionId})"
                        is HitResult.ForgivenCell -> "FORGIVEN: Cell #${hr.cellId} (dist=${String.format("%.1f", hr.distancePx)})"
                        is HitResult.Void -> "VOID: Out of bounds / transparent"
                        is HitResult.AmbiguousBoundary -> "BOUNDARY: Multiple candidates"
                        is HitResult.OutsideArtwork -> "OUTSIDE CANVAS"
                    }
                    val artCoordStr = probe.artworkCoord?.let { "(${it.x.toInt()}, ${it.y.toInt()})" } ?: "N/A"

                    Text(
                        text = "🔍 TAP PROBE\nScreen: (${probe.screenCoord.x.toInt()}, ${probe.screenCoord.y.toInt()})\nArtwork: $artCoordStr\nHit: $statusText\nColor: ${probe.colorId ?: "None"} (Sel: ${probe.selectedColorId})",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF00FF66),
                            lineHeight = 14.sp
                        ),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawNumberBadge(
    screenCenter: Offset,
    number: String,
    bubbleColor: Color,
    textColor: Color,
    pulseScale: Float,
    baseRadius: Float
) {
    val radius = baseRadius * pulseScale

    // Outer subtle glow / border
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = radius + 2.5f,
        center = screenCenter
    )

    // Primary colored badge
    drawCircle(
        color = bubbleColor,
        radius = radius,
        center = screenCenter
    )

    // Inner soft highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.45f),
        radius = radius * 0.4f,
        center = Offset(screenCenter.x - radius * 0.25f, screenCenter.y - radius * 0.25f)
    )
}

private fun isHexLight(hex: String): Boolean {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        val r = android.graphics.Color.red(colorInt)
        val g = android.graphics.Color.green(colorInt)
        val b = android.graphics.Color.blue(colorInt)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        luminance > 0.55
    } catch (_: Exception) {
        false
    }
}

private fun safeParseColor(hex: String, fallback: Color = Color.Gray): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}
