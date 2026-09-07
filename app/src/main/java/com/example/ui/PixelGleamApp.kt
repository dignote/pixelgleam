package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Artwork
import com.example.data.PaletteColor
import com.example.ui.canvas.InteractiveMosaicCanvas
import com.example.ui.shell.ArtworkCompletionDialog
import com.example.ui.shell.ArtworkDetailSheet
import com.example.ui.shell.ArtworkSummaryItem
import com.example.ui.shell.DailyChallengeInfo
import com.example.ui.shell.DailyScreen
import com.example.ui.shell.HomeScreen
import com.example.ui.shell.MyArtFilter
import com.example.ui.shell.MyArtScreen
import com.example.ui.shell.SettingsScreen
import com.example.ui.shell.ShellTab
import com.example.ui.shell.TutorialDialog
import com.example.viewmodel.GameLoadState
import com.example.viewmodel.PixelGleamViewModel
import kotlinx.coroutines.launch

@Composable
fun PixelGleamApp(viewModel: PixelGleamViewModel) {
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val artworkSummaries by viewModel.artworkSummaries.collectAsStateWithLifecycle()
    val galleryArtworkSummaries by viewModel.galleryArtworkSummaries.collectAsStateWithLifecycle()
    val dailyInfo by viewModel.dailyInfo.collectAsStateWithLifecycle()
    val myArtFilter by viewModel.myArtFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedDetailArtworkId by viewModel.selectedDetailArtworkId.collectAsStateWithLifecycle()
    val showTutorialDialog by viewModel.showTutorialDialog.collectAsStateWithLifecycle()

    val musicEnabled by viewModel.musicEnabled.collectAsStateWithLifecycle()
    val sfxEnabled by viewModel.sfxEnabled.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = loadState) {
            is GameLoadState.Idle -> {
                ProductShell(
                    selectedTab = selectedTab,
                    artworkSummaries = artworkSummaries,
                    galleryArtworkSummaries = galleryArtworkSummaries,
                    dailyChallengeInfo = dailyInfo,
                    myArtFilter = myArtFilter,
                    selectedCategory = selectedCategory,
                    detailArtworkId = selectedDetailArtworkId,
                    showTutorialDialog = showTutorialDialog,
                    musicEnabled = musicEnabled,
                    sfxEnabled = sfxEnabled,
                    hapticsEnabled = hapticsEnabled,
                    onSelectTab = { tab -> viewModel.selectTab(tab) },
                    onSelectCategory = { cat -> viewModel.selectCategory(cat) },
                    onSelectFilter = { filter -> viewModel.selectMyArtFilter(filter) },
                    onOpenArtworkDetail = { id -> viewModel.showArtworkDetail(id) },
                    onPlayArtwork = { id -> viewModel.selectArtwork(id) },
                    onReplayArtwork = { id -> viewModel.replayArtwork(id) },
                    onCloseArtworkDetail = { viewModel.dismissArtworkDetail() },
                    onOpenTutorial = { viewModel.openTutorial() },
                    onCloseTutorial = { viewModel.dismissTutorial() },
                    onToggleMusic = { viewModel.toggleMusic(it) },
                    onToggleSfx = { viewModel.toggleSfx(it) },
                    onToggleHaptics = { viewModel.toggleHaptics(it) },
                    onResetAllProgress = { viewModel.resetAllUserData() }
                )
            }
            is GameLoadState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading gem mosaic...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            is GameLoadState.Success -> {
                GameplayScreen(
                    artwork = state.artwork,
                    viewModel = viewModel,
                    onBack = {
                        viewModel.exitToDashboard()
                    }
                )
            }
            is GameLoadState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { viewModel.exitToDashboard() }) {
                                Text("Gallery")
                            }
                            Button(onClick = { viewModel.resetProgress() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductShell(
    selectedTab: ShellTab,
    artworkSummaries: List<ArtworkSummaryItem>,
    galleryArtworkSummaries: List<ArtworkSummaryItem> = emptyList(),
    dailyChallengeInfo: DailyChallengeInfo?,
    myArtFilter: MyArtFilter,
    selectedCategory: String,
    detailArtworkId: String?,
    showTutorialDialog: Boolean,
    musicEnabled: Boolean,
    sfxEnabled: Boolean,
    hapticsEnabled: Boolean,
    onSelectTab: (ShellTab) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectFilter: (MyArtFilter) -> Unit,
    onOpenArtworkDetail: (String) -> Unit,
    onPlayArtwork: (String) -> Unit,
    onReplayArtwork: (String) -> Unit,
    onCloseArtworkDetail: () -> Unit,
    onOpenTutorial: () -> Unit,
    onCloseTutorial: () -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleSfx: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onResetAllProgress: () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("shell_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == ShellTab.GALLERY,
                    onClick = { onSelectTab(ShellTab.GALLERY) },
                    icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Gallery") },
                    label = { Text("Gallery") },
                    modifier = Modifier.testTag("nav_tab_gallery")
                )
                NavigationBarItem(
                    selected = selectedTab == ShellTab.DAILY,
                    onClick = { onSelectTab(ShellTab.DAILY) },
                    icon = { Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Daily") },
                    label = { Text("Daily") },
                    modifier = Modifier.testTag("nav_tab_daily")
                )
                NavigationBarItem(
                    selected = selectedTab == ShellTab.MY_ART,
                    onClick = { onSelectTab(ShellTab.MY_ART) },
                    icon = { Icon(imageVector = Icons.Default.Palette, contentDescription = "My Art") },
                    label = { Text("My Art") },
                    modifier = Modifier.testTag("nav_tab_my_art")
                )
                NavigationBarItem(
                    selected = selectedTab == ShellTab.SETTINGS,
                    onClick = { onSelectTab(ShellTab.SETTINGS) },
                    icon = { Icon(imageVector = Icons.Default.Tune, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                ShellTab.GALLERY -> HomeScreen(
                    artworks = galleryArtworkSummaries,
                    dailyInfo = dailyChallengeInfo,
                    selectedCategory = selectedCategory,
                    onSelectCategory = onSelectCategory,
                    onSelectArtwork = onOpenArtworkDetail,
                    onOpenTutorial = onOpenTutorial
                )
                ShellTab.DAILY -> DailyScreen(
                    dailyInfo = dailyChallengeInfo,
                    onSelectArtwork = onOpenArtworkDetail
                )
                ShellTab.MY_ART -> MyArtScreen(
                    artworks = artworkSummaries,
                    currentFilter = myArtFilter,
                    onFilterChanged = onSelectFilter,
                    onSelectArtwork = onOpenArtworkDetail
                )
                ShellTab.SETTINGS -> SettingsScreen(
                    musicEnabled = musicEnabled,
                    sfxEnabled = sfxEnabled,
                    hapticsEnabled = hapticsEnabled,
                    onToggleMusic = onToggleMusic,
                    onToggleSfx = onToggleSfx,
                    onToggleHaptics = onToggleHaptics,
                    onOpenTutorial = onOpenTutorial,
                    onResetAllProgress = onResetAllProgress
                )
            }

            // Detail Sheet Overlay
            if (detailArtworkId != null) {
                val detailItem = artworkSummaries.firstOrNull { it.id == detailArtworkId }
                if (detailItem != null) {
                    ArtworkDetailSheet(
                        artwork = detailItem,
                        onPlay = onPlayArtwork,
                        onReplay = onReplayArtwork,
                        onDismiss = onCloseArtworkDetail
                    )
                }
            }

            // Tutorial Dialog Overlay
            if (showTutorialDialog) {
                TutorialDialog(onDismiss = onCloseTutorial)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameplayScreen(
    artwork: Artwork,
    viewModel: PixelGleamViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        viewModel.exitToDashboard()
    }

    val revealedBitmap by viewModel.revealedBitmap.collectAsStateWithLifecycle()
    val selectedColorId by viewModel.selectedColorId.collectAsStateWithLifecycle()
    val completedRegionIds by viewModel.completedRegionIds.collectAsStateWithLifecycle()
    val activeParticles by viewModel.activeParticles.collectAsStateWithLifecycle()
    val activeGemFlights by viewModel.activeGemFlights.collectAsStateWithLifecycle()
    val activeLandingEffects by viewModel.activeLandingEffects.collectAsStateWithLifecycle()
    val isArtworkCompleted by viewModel.isArtworkCompleted.collectAsStateWithLifecycle()
    val showCompletionModal by viewModel.showCompletionModal.collectAsStateWithLifecycle()
    val hintTargetRegionId by viewModel.hintTargetRegionId.collectAsStateWithLifecycle()
    val debugMode by viewModel.debugMode.collectAsStateWithLifecycle()
    val tapProbeDiagnostics by viewModel.tapProbeDiagnostics.collectAsStateWithLifecycle()

    if (showCompletionModal) {
        ArtworkCompletionDialog(
            artworkTitle = artwork.title,
            artworkId = artwork.id,
            onNextArtwork = { viewModel.playNextArtwork() },
            onBackToGallery = { viewModel.exitToDashboard() },
            onReplay = { viewModel.replayArtwork(artwork.id) },
            onDismiss = { viewModel.dismissCompletionModal() }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        artwork.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.exitToDashboard()
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Debug Mode Cycle Button (Section 15)
                    IconButton(
                        onClick = {
                            viewModel.cycleDebugMode()
                        },
                        modifier = Modifier.testTag("debug_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Debug Mode: ${debugMode.name}",
                            tint = if (debugMode != com.example.ui.canvas.DebugMode.OFF) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Hint Button
                    IconButton(
                        onClick = {
                            viewModel.useHint()
                        },
                        modifier = Modifier.testTag("hint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Reset Artwork Progress Button
                    IconButton(
                        onClick = {
                            viewModel.replayArtwork(artwork.id)
                        },
                        modifier = Modifier.testTag("reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Interactive Mosaic Canvas Area with Unified ArtworkTransform & Safe Anchors
            InteractiveMosaicCanvas(
                artwork = artwork,
                revealedBitmap = revealedBitmap,
                cellIdMapData = viewModel.cellIdMapData,
                selectedColorId = selectedColorId,
                completedRegionIds = completedRegionIds,
                activeParticles = activeParticles,
                activeGemFlights = activeGemFlights,
                activeLandingEffects = activeLandingEffects,
                gemPresentationManager = viewModel.gemPresentationManager,
                hintTargetRegionId = hintTargetRegionId,
                debugMode = debugMode,
                tapProbeDiagnostics = tapProbeDiagnostics,
                onScreenTapped = { tapOffset, transform ->
                    viewModel.handleScreenTap(tapOffset, transform)
                },
                onTransformChanged = { transform ->
                    viewModel.updateTransform(transform)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // Bottom Palette / Color Selection Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Palette Colors",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (com.example.BuildConfig.DEBUG) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = { viewModel.autoCompleteSelectedColor() },
                                    label = { Text("Fill Color", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Fill Selected Color",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("test_fill_color_button")
                                )
                                FilledTonalButton(
                                    onClick = { viewModel.autoCompleteAllColors() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("test_fill_all_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fill All (TEST)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(artwork.palette) { col ->
                            // Count remaining uncompleted cells for this color
                            val colorRegions = artwork.regions.filter { it.colorId == col.id }
                            val totalCells = colorRegions.sumOf { it.cellIds.size }
                            val completedCells = colorRegions.filter { completedRegionIds.contains(it.id) }.sumOf { it.cellIds.size }
                            val remaining = totalCells - completedCells
                            val isCompleted = remaining == 0

                            val isSelected = selectedColorId == col.id

                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("color_item_${col.id}")
                            ) {
                                val borderModifier = if (isSelected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier.border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(col.hex)))
                                        .then(borderModifier)
                                        .onGloballyPositioned { coordinates ->
                                            val pos = coordinates.positionInRoot()
                                            val sz = coordinates.size
                                            val center = Offset(pos.x + sz.width / 2f, pos.y + sz.height / 2f)
                                            viewModel.updatePaletteColorPosition(col.id, center)
                                        }
                                        .clickable { viewModel.selectColor(col.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = if (isLightColor(col.hex)) Color.Black else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Text(
                                            text = col.id.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isLightColor(col.hex)) Color.Black else Color.White
                                        )
                                    }
                                }

                                // Remaining cells Badge
                                if (!isCompleted) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = remaining.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onError,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun isLightColor(hex: String): Boolean {
    return try {
        val rgb = android.graphics.Color.parseColor(hex)
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        luminance > 0.5
    } catch (e: Exception) {
        true
    }
}
