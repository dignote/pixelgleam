package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.engine.GameplayCoreEngine
import com.example.core.engine.HitResolver
import com.example.core.engine.HitResult
import com.example.core.engine.PrimaryGameplayState
import com.example.core.engine.TapResolutionResult
import com.example.data.Artwork
import com.example.data.Png16Decoder
import com.example.data.RegionIdRaster
import com.example.data.SafeAnchorCalculator
import com.example.data.validation.ArtworkContentValidator
import com.example.ui.animation.GemFlightVisual
import com.example.ui.animation.GemPresentationManager
import com.example.ui.animation.LandingEffectVisual
import androidx.compose.ui.geometry.Offset
import com.example.ui.canvas.ArtworkTransform
import com.example.ui.canvas.DebugMode
import com.example.ui.canvas.TapProbeDiagnostics
import com.example.data.db.AppDatabase
import com.example.data.db.ArtworkProgress
import com.example.data.db.DailyProgress
import com.example.data.repository.AppSettingsRepository
import com.example.data.repository.ArtworkCatalogItem
import com.example.data.repository.ArtworkProgressRepository
import com.example.data.repository.AssetArtworkRepository
import com.example.audio.AndroidAudioController
import com.example.audio.AndroidHapticController
import com.example.audio.AudioController
import com.example.audio.HapticController
import com.example.audio.HapticPattern
import com.example.audio.SoundEffect
import com.example.ui.shell.ArtworkStatus
import com.example.ui.shell.ArtworkSummaryItem
import com.example.ui.shell.DailyChallengeInfo
import com.example.ui.shell.DayChallengeItem
import com.example.ui.shell.MyArtFilter
import com.example.ui.shell.ShellTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStream
import java.util.Calendar
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class SparkleParticle(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val color: Int,
    val size: Float,
    val alpha: Float,
    val vx: Float,
    val vy: Float,
    val life: Float, // duration in frames or ms
    val maxLife: Float
)

sealed interface GameLoadState {
    object Idle : GameLoadState
    object Loading : GameLoadState
    data class Success(val artwork: Artwork) : GameLoadState
    data class Error(val message: String) : GameLoadState
}

class PixelGleamViewModel(
    application: Application,
    private val database: AppDatabase
) : AndroidViewModel(application) {

    val repository = ArtworkProgressRepository(database)
    val settingsRepository = AppSettingsRepository(application)
    val catalogRepository = AssetArtworkRepository(application)
    val hapticController: HapticController = AndroidHapticController(application)
    val audioController: AudioController = AndroidAudioController(application)
    private val context: Context get() = getApplication()

    // Product Shell Navigation & Filter states (Phase A7)
    private val _selectedTab = MutableStateFlow(ShellTab.GALLERY)
    val selectedTab: StateFlow<ShellTab> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _myArtFilter = MutableStateFlow(MyArtFilter.ALL)
    val myArtFilter: StateFlow<MyArtFilter> = _myArtFilter.asStateFlow()

    private val _selectedDetailArtworkId = MutableStateFlow<String?>(null)
    val selectedDetailArtworkId: StateFlow<String?> = _selectedDetailArtworkId.asStateFlow()

    private val _showTutorialDialog = MutableStateFlow(false)
    val showTutorialDialog: StateFlow<Boolean> = _showTutorialDialog.asStateFlow()

    val musicEnabled: StateFlow<Boolean> = settingsRepository.musicEnabled
    val sfxEnabled: StateFlow<Boolean> = settingsRepository.sfxEnabled
    val hapticsEnabled: StateFlow<Boolean> = settingsRepository.hapticsEnabled
    val tutorialCompleted: StateFlow<Boolean> = settingsRepository.tutorialCompleted

    private val _artworkSummaries = MutableStateFlow<List<ArtworkSummaryItem>>(emptyList())
    val artworkSummaries: StateFlow<List<ArtworkSummaryItem>> = _artworkSummaries.asStateFlow()

    val galleryArtworkSummaries: StateFlow<List<ArtworkSummaryItem>> = _artworkSummaries
        .map { summaries ->
            summaries.filter { it.lifecycle == "active" && it.galleryVisible }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _dailyInfo = MutableStateFlow<DailyChallengeInfo?>(null)
    val dailyInfo: StateFlow<DailyChallengeInfo?> = _dailyInfo.asStateFlow()

    private val _artworkList = MutableStateFlow<List<ArtworkInfo>>(emptyList())
    val artworkList: StateFlow<List<ArtworkInfo>> = _artworkList.asStateFlow()

    private val _loadState = MutableStateFlow<GameLoadState>(GameLoadState.Idle)
    val loadState: StateFlow<GameLoadState> = _loadState.asStateFlow()

    // Loaded Artwork details
    var currentArtwork: Artwork? = null
        private set
    var cellIdMapData: IntArray? = null
        private set
    private var settledPixels: IntArray? = null
    private var width = 0
    private var height = 0

    var gameplayEngine: GameplayCoreEngine? = null
        private set

    // Region to pixel 1D index mapping
    private val regionPixelIndices = HashMap<Int, ArrayList<Int>>()

    val gemPresentationManager = GemPresentationManager(application)

    private val _activeGemFlights = MutableStateFlow<List<GemFlightVisual>>(emptyList())
    val activeGemFlights: StateFlow<List<GemFlightVisual>> = _activeGemFlights.asStateFlow()

    private val _activeLandingEffects = MutableStateFlow<List<LandingEffectVisual>>(emptyList())
    val activeLandingEffects: StateFlow<List<LandingEffectVisual>> = _activeLandingEffects.asStateFlow()

    // Game state
    private val _selectedColorId = MutableStateFlow<Int?>(null)
    val selectedColorId: StateFlow<Int?> = _selectedColorId.asStateFlow()

    private val _completedRegionIds = MutableStateFlow<Set<Int>>(emptySet())
    val completedRegionIds: StateFlow<Set<Int>> = _completedRegionIds.asStateFlow()

    private val _revealedBitmap = MutableStateFlow<Bitmap?>(null)
    val revealedBitmap: StateFlow<Bitmap?> = _revealedBitmap.asStateFlow()

    private val _activeParticles = MutableStateFlow<List<SparkleParticle>>(emptyList())
    val activeParticles: StateFlow<List<SparkleParticle>> = _activeParticles.asStateFlow()

    // Completion & Celebration state (Phase A6 Vertical Slice)
    private val _isArtworkCompleted = MutableStateFlow<Boolean>(false)
    val isArtworkCompleted: StateFlow<Boolean> = _isArtworkCompleted.asStateFlow()

    private val _showCompletionModal = MutableStateFlow<Boolean>(false)
    val showCompletionModal: StateFlow<Boolean> = _showCompletionModal.asStateFlow()

    // Hint targeting state (Phase A6)
    private val _hintTargetRegionId = MutableStateFlow<Int?>(null)
    val hintTargetRegionId: StateFlow<Int?> = _hintTargetRegionId.asStateFlow()

    private val _hintTargetCell = MutableStateFlow<Offset?>(null)
    val hintTargetCell: StateFlow<Offset?> = _hintTargetCell.asStateFlow()

    // Map artwork ID to completed percentage
    private val _completionProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val completionProgress: StateFlow<Map<String, Float>> = _completionProgress.asStateFlow()

    // Dynamic Palette origin tracking & transform model
    private val paletteColorPositionsScreen = java.util.concurrent.ConcurrentHashMap<Int, Offset>()
    private var currentTransform: ArtworkTransform? = null
    private val playedLaunchSoundIds = HashSet<Long>()
    private val playedImpactSoundIds = HashSet<Long>()
    private var isCompletionSequenceRunning = false

    fun updatePaletteColorPosition(colorId: Int, screenCenter: Offset) {
        paletteColorPositionsScreen[colorId] = screenCenter
    }

    fun updateTransform(transform: ArtworkTransform) {
        currentTransform = transform
    }

    private fun getOriginForColor(
        colorId: Int,
        colorIndex: Int,
        paletteCount: Int,
        artworkWidth: Int,
        artworkHeight: Int
    ): Offset {
        val screenPos = paletteColorPositionsScreen[colorId]
        val transform = currentTransform
        if (screenPos != null && transform != null) {
            return transform.screenToArtworkUnclamped(screenPos)
        }
        val originX = ((colorIndex + 0.5f) / paletteCount.coerceAtLeast(1)) * artworkWidth
        val originY = artworkHeight + 180f
        return Offset(originX, originY)
    }

    private fun scheduleCompletionSequence() {
        if (isCompletionSequenceRunning) return
        isCompletionSequenceRunning = true
        _isArtworkCompleted.value = true

        viewModelScope.launch {
            // Spawn celebratory sparkles burst
            val artwork = currentArtwork
            if (artwork != null) {
                val celebrationParticles = ArrayList<SparkleParticle>()
                val w = artwork.canvas.width.toFloat()
                val h = artwork.canvas.height.toFloat()
                for (i in 0 until 40) {
                    val rx = Random.nextFloat() * w
                    val ry = Random.nextFloat() * h
                    val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                    val speed = Random.nextFloat() * 5f + 2f
                    val cHex = artwork.palette.randomOrNull()?.hex ?: "#FFD700"
                    val pColor = try { Color.parseColor(cHex) } catch (_: Exception) { Color.YELLOW }
                    celebrationParticles.add(
                        SparkleParticle(
                            x = rx,
                            y = ry,
                            color = pColor,
                            size = Random.nextFloat() * 12f + 6f,
                            alpha = 1f,
                            vx = cos(angle) * speed,
                            vy = sin(angle) * speed,
                            life = 0f,
                            maxLife = Random.nextFloat() * 25f + 25f
                        )
                    )
                }
                _activeParticles.value = _activeParticles.value + celebrationParticles
            }

            // 1. Wait for active gem flights to finish landing (max 1.2s timeout)
            val startWait = System.currentTimeMillis()
            while (_activeGemFlights.value.isNotEmpty() && System.currentTimeMillis() - startWait < 1200L) {
                delay(50L)
            }

            // 2. Rest delay for 1.8s so user can admire the completed artwork
            delay(1800L)

            // 3. Play victory audio & haptics and show modal
            audioController.playSound(SoundEffect.ARTWORK_COMPLETE)
            hapticController.performHaptic(HapticPattern.ARTWORK_COMPLETE)
            _showCompletionModal.value = true
            isCompletionSequenceRunning = false
        }
    }

    // Debug Mode & Tap Probe state
    private val _debugMode = MutableStateFlow(DebugMode.OFF)
    val debugMode: StateFlow<DebugMode> = _debugMode.asStateFlow()

    private val _tapProbeDiagnostics = MutableStateFlow<TapProbeDiagnostics?>(null)
    val tapProbeDiagnostics: StateFlow<TapProbeDiagnostics?> = _tapProbeDiagnostics.asStateFlow()

    var regionIdRaster: RegionIdRaster? = null
        private set

    private val _catalogItems = MutableStateFlow<List<ArtworkCatalogItem>>(emptyList())
    val catalogItems: List<ArtworkCatalogItem> get() = _catalogItems.value

    fun setDebugMode(mode: DebugMode) {
        _debugMode.value = mode
    }

    fun cycleDebugMode() {
        val modes = DebugMode.values()
        val nextOrdinal = (_debugMode.value.ordinal + 1) % modes.size
        _debugMode.value = modes[nextOrdinal]
    }

    init {
        observeProgress()
        startParticleAnimationLoop()
        hapticController.setHapticsEnabled(settingsRepository.hapticsEnabled.value)
        audioController.setMusicEnabled(settingsRepository.musicEnabled.value)
        audioController.setSfxEnabled(settingsRepository.sfxEnabled.value)
        viewModelScope.launch {
            gemPresentationManager.preloadAssets()
        }
        viewModelScope.launch {
            settingsRepository.hapticsEnabled.collectLatest { enabled ->
                hapticController.setHapticsEnabled(enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.musicEnabled.collectLatest { enabled ->
                audioController.setMusicEnabled(enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.sfxEnabled.collectLatest { enabled ->
                audioController.setSfxEnabled(enabled)
            }
        }
    }

    private fun loadArtworkList() {
        _artworkList.value = catalogItems.map {
            ArtworkInfo(it.id, it.title, it.category)
        }
    }

    private fun buildSummaries(items: List<ArtworkCatalogItem>, list: List<com.example.data.db.ArtworkProgress>): List<ArtworkSummaryItem> {
        val progressMap = HashMap<String, Float>()
        val progressEntityMap = list.associateBy { it.artworkId }

        val summaries = items.map { catalogItem ->
            val entity = progressEntityMap[catalogItem.id]
            val validIds = if (entity != null) {
                ArtworkProgressRepository.parseJsonToRegionIds(entity.completedRegionIdsJson)
            } else {
                emptySet()
            }
            val completedCount = validIds.size
            val isDone = (entity?.isCompleted == true) || (completedCount >= catalogItem.totalRegions)
            val percent = if (isDone) 100f else ((completedCount.toFloat() / catalogItem.totalRegions.toFloat()).coerceIn(0f, 1f) * 100f)
            val status = when {
                isDone -> ArtworkStatus.COMPLETED
                completedCount > 0 -> ArtworkStatus.IN_PROGRESS
                else -> ArtworkStatus.NEW
            }

            progressMap[catalogItem.id] = percent

            ArtworkSummaryItem(
                id = catalogItem.id,
                title = catalogItem.title,
                category = catalogItem.category,
                totalRegions = catalogItem.totalRegions,
                totalCells = catalogItem.totalCells,
                completedRegionsCount = if (isDone) catalogItem.totalRegions else completedCount,
                progressPercent = percent,
                status = status,
                categoryId = catalogItem.categoryId,
                lifecycle = catalogItem.lifecycle,
                galleryVisible = catalogItem.galleryVisible
            )
        }

        _completionProgress.value = progressMap
        return summaries
    }

    private fun observeProgress() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                catalogRepository.getCatalog(),
                repository.allProgress,
                repository.allDailyProgress
            ) { items, list, dailyList ->
                _catalogItems.value = items
                loadArtworkList()
                val summaries = buildSummaries(items, list)
                Pair(summaries, dailyList)
            }.collectLatest { (summaries, dailyList) ->
                _artworkSummaries.value = summaries
                _dailyInfo.value = computeDailyChallenge(summaries, dailyList)
            }
        }
    }

    private fun computeDailyChallenge(
        summaries: List<ArtworkSummaryItem>,
        persistedDailyList: List<DailyProgress> = emptyList()
    ): DailyChallengeInfo? {
        if (summaries.isEmpty()) return null
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK)

        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val dayOfWeek = dayNames[dayOfWeekInt - 1]
        val monthName = monthNames[calendar.get(Calendar.MONTH)]
        val dateFormatted = "$dayOfWeek, $monthName $dayOfMonth"

        val year = calendar.get(Calendar.YEAR)
        val todayDateKey = String.format("%04d-%02d-%02d", year, calendar.get(Calendar.MONTH) + 1, dayOfMonth)
        val todaySeed = "daily_${year}_${dayOfYear}"
        
        // Check if there is an explicit persisted assignment for today; otherwise deterministic HRW
        val persistedToday = persistedDailyList.firstOrNull { it.dateString == todayDateKey }
        val featuredArtwork = if (persistedToday != null) {
            summaries.firstOrNull { it.id == persistedToday.artworkId } ?: selectDailyArtworkByHrw(todaySeed, summaries)
        } else {
            selectDailyArtworkByHrw(todaySeed, summaries)
        }

        val weeklyDays = ArrayList<DayChallengeItem>()
        val tempCal = calendar.clone() as Calendar
        tempCal.firstDayOfWeek = Calendar.MONDAY
        tempCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val shortDayLabels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        for (i in 0 until 7) {
            val dOfYear = tempCal.get(Calendar.DAY_OF_YEAR)
            val dOfMonth = tempCal.get(Calendar.DAY_OF_MONTH)
            val dYear = tempCal.get(Calendar.YEAR)
            val isToday = (dOfYear == dayOfYear) && (dYear == year)
            val dDateKey = String.format("%04d-%02d-%02d", dYear, tempCal.get(Calendar.MONTH) + 1, dOfMonth)
            val dSeed = "daily_${dYear}_${dOfYear}"
            val persistedDay = persistedDailyList.firstOrNull { it.dateString == dDateKey }
            val artItem = if (persistedDay != null) {
                summaries.firstOrNull { it.id == persistedDay.artworkId } ?: selectDailyArtworkByHrw(dSeed, summaries)
            } else {
                selectDailyArtworkByHrw(dSeed, summaries)
            }
            weeklyDays.add(
                DayChallengeItem(
                    dayLabel = shortDayLabels[i],
                    dayOfMonth = dOfMonth,
                    isToday = isToday,
                    artwork = artItem
                )
            )
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return DailyChallengeInfo(
            dateFormatted = dateFormatted,
            dayOfWeek = dayOfWeek,
            dayNumber = dayOfMonth,
            monthName = monthName,
            featuredArtwork = featuredArtwork,
            weeklyDays = weeklyDays
        )
    }

    private fun selectDailyArtworkByHrw(dateSeed: String, summaries: List<ArtworkSummaryItem>): ArtworkSummaryItem {
        return summaries.maxByOrNull { item ->
            val str = "$dateSeed:${item.id}"
            val bytes = str.toByteArray(Charsets.UTF_8)
            var hash = -3750763025362895003L
            for (b in bytes) {
                hash = hash xor (b.toLong() and 0xFF)
                hash *= 1099511628211L
            }
            hash
        } ?: summaries.first()
    }

    fun selectTab(tab: ShellTab) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
            audioController.playSound(SoundEffect.NAVIGATION)
            hapticController.performHaptic(HapticPattern.NAVIGATION)
        }
    }

    fun selectCategory(category: String) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            audioController.playSound(SoundEffect.TAP)
            hapticController.performHaptic(HapticPattern.LIGHT_TAP)
        }
    }

    fun selectMyArtFilter(filter: MyArtFilter) {
        if (_myArtFilter.value != filter) {
            _myArtFilter.value = filter
            audioController.playSound(SoundEffect.TAP)
            hapticController.performHaptic(HapticPattern.LIGHT_TAP)
        }
    }

    fun showArtworkDetail(artworkId: String) {
        _selectedDetailArtworkId.value = artworkId
        audioController.playSound(SoundEffect.NAVIGATION)
        hapticController.performHaptic(HapticPattern.LIGHT_TAP)
    }

    fun dismissArtworkDetail() {
        _selectedDetailArtworkId.value = null
        audioController.playSound(SoundEffect.TAP)
    }

    fun openTutorial() {
        _showTutorialDialog.value = true
        audioController.playSound(SoundEffect.TAP)
    }

    fun dismissTutorial(markAsCompleted: Boolean = true) {
        _showTutorialDialog.value = false
        audioController.playSound(SoundEffect.TAP)
        if (markAsCompleted) {
            settingsRepository.setTutorialCompleted(true)
        }
    }

    fun toggleMusic(enabled: Boolean) {
        settingsRepository.setMusicEnabled(enabled)
        audioController.playSound(SoundEffect.TAP)
    }

    fun toggleSfx(enabled: Boolean) {
        settingsRepository.setSfxEnabled(enabled)
        audioController.playSound(SoundEffect.TAP)
    }

    fun toggleHaptics(enabled: Boolean) {
        settingsRepository.setHapticsEnabled(enabled)
        hapticController.setHapticsEnabled(enabled)
        audioController.playSound(SoundEffect.TAP)
    }

    fun resetAllUserData() {
        viewModelScope.launch {
            repository.deleteAllProgress()
            exitToDashboard()
            audioController.playSound(SoundEffect.TAP)
        }
    }

    fun playNextArtwork() {
        val currentId = currentArtwork?.id
        val currentIndex = catalogItems.indexOfFirst { it.id == currentId }
        val nextIndex = if (currentIndex != -1) (currentIndex + 1) % catalogItems.size else 0
        selectArtwork(catalogItems[nextIndex].id)
    }

    fun replayArtwork(artworkId: String) {
        viewModelScope.launch {
            repository.deleteProgress(artworkId)
            selectArtwork(artworkId)
        }
    }

    fun selectArtwork(artworkId: String) {
        viewModelScope.launch {
            _loadState.value = GameLoadState.Loading
            try {
                withContext(Dispatchers.IO) {
                    val assets = context.assets
                    
                    // 1. Read and parse artwork.json
                    val jsonStr = assets.open("artworks/$artworkId/artwork.json").bufferedReader().use { it.readText() }
                    val safeAnchorsJson = try {
                        assets.open("artworks/$artworkId/safe_anchors.json").bufferedReader().use { it.readText() }
                    } catch (_: Exception) {
                        null
                    }
                    var artwork = Artwork.parseFromJson(jsonStr, safeAnchorsJson)
                    
                    width = artwork.canvas.width
                    height = artwork.canvas.height

                    // 2. Load settled_texture.png
                    val loadedPixels = IntArray(width * height)
                    assets.open("artworks/$artworkId/settled_texture.png").use { settledStream ->
                        val rawBitmap = BitmapFactory.decodeStream(settledStream)
                        val settledBitmap = if (rawBitmap.width != width || rawBitmap.height != height) {
                            Bitmap.createScaledBitmap(rawBitmap, width, height, true).also {
                                if (it != rawBitmap) rawBitmap.recycle()
                            }
                        } else {
                            rawBitmap
                        }
                        settledBitmap.getPixels(loadedPixels, 0, width, 0, 0, width, height)
                        settledBitmap.recycle()
                    }
                    settledPixels = loadedPixels

                    // 3. Decode 16-bit cell_id_map.png
                    val decodedCmap = assets.open("artworks/$artworkId/cell_id_map.png").use { cellMapStream ->
                        Png16Decoder.decodeGrayscale16(cellMapStream)
                    }
                    cellIdMapData = decodedCmap.data

                    // 3.5 Derive Safe Anchors if not present in package
                    if (artwork.cellAnchors.isEmpty() || artwork.regionAnchors.isEmpty()) {
                        val derivedAnchors = SafeAnchorCalculator.computeAnchors(
                            width = width,
                            height = height,
                            cellIdMap = decodedCmap.data,
                            cells = artwork.cells,
                            regions = artwork.regions
                        )
                        artwork = artwork.withAnchors(derivedAnchors)
                    }
                    currentArtwork = artwork

                    // Derive authoritative RegionIdRaster
                    regionIdRaster = RegionIdRaster.fromCellIdRaster(width, height, decodedCmap.data, artwork.cellMap)

                    // 4. Precompute region pixel indices
                    regionPixelIndices.clear()
                    val cellMap = artwork.cellMap
                    for (i in 0 until width * height) {
                        val cellId = decodedCmap.data[i]
                        if (cellId > 0) {
                            val cell = cellMap[cellId]
                            if (cell != null) {
                                val list = regionPixelIndices.getOrPut(cell.regionId) { ArrayList() }
                                list.add(i)
                            }
                        }
                    }

                    // Load validated completed progress from Persistence Repository (Phase A5)
                    val validArtworkRegionIds = artwork.regions.map { it.id }.toSet()
                    val savedCompletedIds = repository.getValidCompletedRegionIds(artworkId, validArtworkRegionIds)

                    // Create gameplay revealed bitmap (Hidden Topology organic presentation)
                    val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val revealPixels = IntArray(width * height)
                    
                    for (i in 0 until width * height) {
                        val cellId = decodedCmap.data[i]
                        if (cellId > 0) {
                            val cell = cellMap[cellId]
                            if (cell != null && savedCompletedIds.contains(cell.regionId)) {
                                revealPixels[i] = loadedPixels[i]
                            } else {
                                // Grayscale soft mosaic tone for unrevealed regions
                                val pix = loadedPixels[i]
                                val r = (pix ushr 16) and 0xFF
                                val g = (pix ushr 8) and 0xFF
                                val b = pix and 0xFF
                                val lum = (r * 299 + g * 587 + b * 114) / 1000
                                val baseVal = (lum * 0.45f + 130f).toInt().coerceIn(0, 255)
                                revealPixels[i] = (0xFF shl 24) or (baseVal shl 16) or (baseVal shl 8) or baseVal
                            }
                        } else {
                            revealPixels[i] = 0x00000000 // Background transparent
                        }
                    }
                    outputBitmap.setPixels(revealPixels, 0, width, 0, 0, width, height)

                    _completedRegionIds.value = savedCompletedIds
                    _revealedBitmap.value = outputBitmap
                    
                    val isAlreadyDone = savedCompletedIds.size == artwork.regions.size
                    _isArtworkCompleted.value = isAlreadyDone
                    _showCompletionModal.value = false
                    _hintTargetRegionId.value = null
                    _hintTargetCell.value = null

                    // Restore saved selected color if it still has uncompleted regions, else find first uncompleted color
                    val savedColorId = repository.getSavedSelectedColorId(artworkId)
                    val isSavedColorUsable = savedColorId != null && artwork.palette.any { it.id == savedColorId } &&
                            artwork.regions.filter { it.colorId == savedColorId }.any { !savedCompletedIds.contains(it.id) }

                    val initialColor = if (isSavedColorUsable) {
                        savedColorId
                    } else {
                        artwork.palette.firstOrNull { col ->
                            val matchingRegions = artwork.regions.filter { it.colorId == col.id }
                            matchingRegions.any { !savedCompletedIds.contains(it.id) }
                        }?.id ?: artwork.palette.firstOrNull()?.id
                    }
                    
                    gameplayEngine = GameplayCoreEngine(
                        artwork = artwork,
                        initialCompletedRegionIds = savedCompletedIds,
                        initialSelectedColorId = initialColor
                    )

                    _selectedColorId.value = initialColor
                    _loadState.value = GameLoadState.Success(artwork)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                _loadState.value = GameLoadState.Error("Failed to load artwork: ${t.localizedMessage ?: t.javaClass.simpleName}")
            }
        }
    }

    fun exitToDashboard() {
        _loadState.value = GameLoadState.Idle
        _showCompletionModal.value = false
        _hintTargetRegionId.value = null
        _hintTargetCell.value = null
        audioController.playSound(SoundEffect.NAVIGATION)
    }

    fun dismissCompletionModal() {
        _showCompletionModal.value = false
    }

    fun useHint() {
        val engine = gameplayEngine ?: return
        val artwork = currentArtwork ?: return
        val targetRegion = engine.findHintTargetRegion() ?: return

        audioController.playSound(SoundEffect.HINT)
        hapticController.performHaptic(HapticPattern.HINT)

        // Auto select the color of target region if not already selected
        if (_selectedColorId.value != targetRegion.colorId) {
            selectColor(targetRegion.colorId)
        }

        _hintTargetRegionId.value = targetRegion.id

        val firstCellId = targetRegion.cellIds.firstOrNull() ?: return
        val cell = artwork.cellMap[firstCellId]
        if (cell != null) {
            val anchorX = cell.anchorX
            val anchorY = cell.anchorY
            _hintTargetCell.value = Offset(anchorX, anchorY)

            // Sparkle guidance indicator
            val colorHex = artwork.palette.firstOrNull { it.id == targetRegion.colorId }?.hex ?: "#FFD700"
            val colorInt = try { Color.parseColor(colorHex) } catch (_: Exception) { Color.YELLOW }
            val hintParticles = (0 until 8).map {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = Random.nextFloat() * 2.5f + 1f
                SparkleParticle(
                    x = anchorX,
                    y = anchorY,
                    color = colorInt,
                    size = Random.nextFloat() * 8f + 4f,
                    alpha = 1f,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    life = 0f,
                    maxLife = 28f
                )
            }
            _activeParticles.value = _activeParticles.value + hintParticles
        }

        viewModelScope.launch {
            delay(3500)
            if (_hintTargetRegionId.value == targetRegion.id) {
                _hintTargetRegionId.value = null
                _hintTargetCell.value = null
            }
        }
    }

    fun handleScreenTap(screenTap: Offset, transform: ArtworkTransform) {
        val artwork = currentArtwork ?: return
        val cellIdMap = cellIdMapData ?: return
        val selectedCol = _selectedColorId.value

        val hitResult = HitResolver.resolveTap(
            screenTap = screenTap,
            transform = transform,
            cellIdMapData = cellIdMap,
            artwork = artwork,
            selectedColorId = selectedCol,
            forgivenessRadiusPx = 2.0f
        )

        val exactCid = when (hitResult) {
            is HitResult.ExactCell -> hitResult.cellId
            else -> null
        }
        val forgivenCid = when (hitResult) {
            is HitResult.ForgivenCell -> hitResult.cellId
            else -> null
        }
        val resolvedCid = hitResult.resolvedCellId
        val resolvedCell = if (resolvedCid != null) artwork.cellMap[resolvedCid] else null

        _tapProbeDiagnostics.value = TapProbeDiagnostics(
            screenCoord = screenTap,
            artworkCoord = when (hitResult) {
                is HitResult.ExactCell -> hitResult.artworkCoord
                is HitResult.ForgivenCell -> hitResult.artworkCoord
                is HitResult.Void -> hitResult.artworkCoord
                is HitResult.AmbiguousBoundary -> hitResult.artworkCoord
                else -> null
            },
            hitResult = hitResult,
            selectedColorId = selectedCol ?: -1,
            exactCellId = exactCid,
            forgivenCellId = forgivenCid,
            regionId = resolvedCell?.regionId,
            colorId = resolvedCell?.colorId
        )

        if (resolvedCid != null) {
            handleCellIdTap(resolvedCid)
        } else {
            hapticController.performHaptic(HapticPattern.LIGHT_TAP)
        }
    }

    fun selectColor(colorId: Int) {
        gameplayEngine?.selectColor(colorId)
        _selectedColorId.value = colorId
        audioController.playSound(SoundEffect.PALETTE_SELECT)
        hapticController.performHaptic(HapticPattern.PALETTE_SELECT)
        val artwork = currentArtwork
        if (artwork != null) {
            viewModelScope.launch {
                val isCompleted = _completedRegionIds.value.size == artwork.regions.size
                repository.saveProgress(artwork.id, _completedRegionIds.value, isCompleted, colorId)
            }
        }
    }

    private fun triggerGemFlightBurstForRegions(
        regions: List<com.example.data.Region>,
        artwork: Artwork,
        maxTotalGems: Int = 50
    ) {
        if (regions.isEmpty()) return
        val now = System.currentTimeMillis()
        val palette = artwork.palette
        val paletteCount = palette.size.coerceAtLeast(1)

        val newFlights = ArrayList<GemFlightVisual>()
        val newLandingFx = ArrayList<LandingEffectVisual>()
        val newParticles = ArrayList<SparkleParticle>()

        val targetCellInfos = ArrayList<Triple<Int, Offset, com.example.data.PaletteColor?>>()
        for (region in regions) {
            val paletteColor = palette.firstOrNull { it.id == region.colorId }
            val cellCount = region.cellIds.size
            val cellSample = if (cellCount > 6) {
                val step = (cellCount / 6).coerceAtLeast(1)
                region.cellIds.filterIndexed { idx, _ -> idx % step == 0 }.take(6)
            } else {
                region.cellIds
            }
            for (cid in cellSample) {
                val cell = artwork.cellMap[cid] ?: continue
                targetCellInfos.add(Triple(region.colorId, Offset(cell.anchorX, cell.anchorY), paletteColor))
            }
        }

        if (targetCellInfos.isEmpty()) return

        val sampledInfos = if (targetCellInfos.size > maxTotalGems) {
            val step = (targetCellInfos.size / maxTotalGems).coerceAtLeast(1)
            targetCellInfos.filterIndexed { idx, _ -> idx % step == 0 }.take(maxTotalGems)
        } else {
            targetCellInfos
        }

        sampledInfos.forEachIndexed { idx, info ->
            val colorId = info.first
            val targetPos = info.second
            val paletteColor = info.third

            val colorHex = paletteColor?.hex ?: "#FFFFFF"
            val colorInt = try { Color.parseColor(colorHex) } catch (_: Exception) { Color.WHITE }
            val gemType = GemPresentationManager.mapHexToGemName(colorHex)

            val colorIndex = palette.indexOfFirst { it.id == colorId }.coerceAtLeast(0)
            val baseOrigin = getOriginForColor(colorId, colorIndex, paletteCount, artwork.canvas.width, artwork.canvas.height)

            val startOffset = Offset(
                baseOrigin.x + (Random.nextFloat() - 0.5f) * 30f,
                baseOrigin.y + (Random.nextFloat() - 0.5f) * 30f
            )

            val flightTime = now + (idx * 12L) // 12ms rapid cascade stream
            val duration = 260L + Random.nextLong(60L)

            newFlights.add(
                GemFlightVisual(
                    id = now + idx,
                    startOffset = startOffset,
                    targetOffset = targetPos,
                    colorHex = colorHex,
                    gemType = gemType,
                    startTime = flightTime,
                    durationMs = duration,
                    arcHeight = -100f - Random.nextFloat() * 80f
                )
            )

            if (idx % 2 == 0) {
                newLandingFx.add(
                    LandingEffectVisual(
                        id = now + 1000 + idx,
                        center = targetPos,
                        colorHex = colorHex,
                        startTime = flightTime + (duration * 0.7f).toLong(),
                        durationMs = 320L,
                        maxRadius = 38f
                    )
                )
            }

            for (p in 0 until 4) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = Random.nextFloat() * 4f + 2f
                newParticles.add(
                    SparkleParticle(
                        x = targetPos.x,
                        y = targetPos.y,
                        color = colorInt,
                        size = Random.nextFloat() * 8f + 5f,
                        alpha = 1f,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        life = 0f,
                        maxLife = Random.nextFloat() * 16f + 12f
                    )
                )
            }
        }

        _activeGemFlights.value = _activeGemFlights.value + newFlights
        _activeLandingEffects.value = _activeLandingEffects.value + newLandingFx
        _activeParticles.value = _activeParticles.value + newParticles
    }

    fun autoCompleteSelectedColor() {
        val engine = gameplayEngine ?: return
        val artwork = currentArtwork ?: return
        val settled = settledPixels ?: return
        val revealed = _revealedBitmap.value ?: return
        val colId = _selectedColorId.value ?: return

        val newlyCompletedRegions = engine.completeSelectedColor(colId)
        if (newlyCompletedRegions.isEmpty()) return

        val updatedCompleted = engine.completedRegionIds
        _completedRegionIds.value = updatedCompleted
        val isAllCompleted = updatedCompleted.size == artwork.regions.size

        triggerGemFlightBurstForRegions(newlyCompletedRegions, artwork, maxTotalGems = 45)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                for (region in newlyCompletedRegions) {
                    val pixelsToReveal = regionPixelIndices[region.id] ?: continue
                    synchronized(revealed) {
                        for (idx in pixelsToReveal.indices) {
                            val pixelIdx = pixelsToReveal[idx]
                            if (pixelIdx in settled.indices) {
                                revealed.setPixel(pixelIdx % width, pixelIdx / width, settled[pixelIdx])
                            }
                        }
                    }
                }
                _revealedBitmap.value = revealed

                repository.saveProgress(
                    artworkId = artwork.id,
                    completedRegionIds = updatedCompleted,
                    isCompleted = isAllCompleted,
                    selectedColorId = _selectedColorId.value
                )
            }

            if (isAllCompleted) {
                scheduleCompletionSequence()
            } else {
                audioController.playSound(SoundEffect.COLOR_COMPLETE)
                hapticController.performHaptic(HapticPattern.COLOR_COMPLETE)

                // Select next available incomplete color if current color finished
                val nextColor = artwork.palette.firstOrNull { col ->
                    val colorRegions = artwork.regions.filter { it.colorId == col.id }
                    colorRegions.any { !updatedCompleted.contains(it.id) }
                }?.id
                if (nextColor != null) {
                    selectColor(nextColor)
                }
            }
        }
    }

    fun autoCompleteAllColors() {
        val engine = gameplayEngine ?: return
        val artwork = currentArtwork ?: return
        val settled = settledPixels ?: return
        val revealed = _revealedBitmap.value ?: return

        val newlyCompletedRegions = engine.completeAllRegions()
        val updatedCompleted = engine.completedRegionIds
        _completedRegionIds.value = updatedCompleted

        triggerGemFlightBurstForRegions(newlyCompletedRegions, artwork, maxTotalGems = 75)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                synchronized(revealed) {
                    revealed.setPixels(settled, 0, width, 0, 0, width, height)
                }
                _revealedBitmap.value = revealed

                repository.saveProgress(
                    artworkId = artwork.id,
                    completedRegionIds = updatedCompleted,
                    isCompleted = true,
                    selectedColorId = _selectedColorId.value
                )
            }

            scheduleCompletionSequence()
        }
    }

    fun resetArtworkProgress(artworkId: String) {
        viewModelScope.launch {
            repository.deleteProgress(artworkId)
            if (currentArtwork?.id == artworkId) {
                selectArtwork(artworkId)
            }
        }
    }

    fun handleCellIdTap(cellId: Int) {
        val engine = gameplayEngine ?: return
        val artwork = currentArtwork ?: return
        val settled = settledPixels ?: return
        val revealed = _revealedBitmap.value ?: return

        when (val result = engine.processCellTap(cellId)) {
            is TapResolutionResult.Success -> {
                val region = result.completedRegion
                val updatedCompleted = engine.completedRegionIds
                _completedRegionIds.value = updatedCompleted
                val isAllCompleted = updatedCompleted.size == artwork.regions.size

                viewModelScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            // Reveal pixels on canvas
                            val pixelsToReveal = regionPixelIndices[region.id]
                            if (pixelsToReveal != null) {
                                synchronized(revealed) {
                                    for (idx in pixelsToReveal.indices) {
                                        val pixelIdx = pixelsToReveal[idx]
                                        if (pixelIdx in settled.indices) {
                                            revealed.setPixel(pixelIdx % width, pixelIdx / width, settled[pixelIdx])
                                        }
                                    }
                                }
                            }

                            _revealedBitmap.value = revealed

                            // Persist progress to Room durably (Phase A5)
                            repository.saveProgress(
                                artworkId = artwork.id,
                                completedRegionIds = updatedCompleted,
                                isCompleted = isAllCompleted,
                                selectedColorId = _selectedColorId.value
                            )
                        }

                        // 1. Trigger transient G02 Gem Flight Visuals and Landing FX
                        val now = System.currentTimeMillis()
                        val artworkPaletteColor = artwork.palette.firstOrNull { it.id == region.colorId }
                        val colorHex = artworkPaletteColor?.hex ?: "#FFFFFF"
                        val colorInt = try { Color.parseColor(colorHex) } catch (_: Exception) { Color.WHITE }
                        val gemType = GemPresentationManager.mapHexToGemName(colorHex)

                        val paletteIndex = artwork.palette.indexOfFirst { it.id == region.colorId }.coerceAtLeast(0)
                        val paletteCount = artwork.palette.size.coerceAtLeast(1)
                        val baseOrigin = getOriginForColor(region.colorId, paletteIndex, paletteCount, artwork.canvas.width, artwork.canvas.height)

                        val regionCells = region.cellIds.mapNotNull { artwork.cellMap[it] }
                        val gemTargets = ArrayList<Offset>()
                        if (regionCells.size <= 12) {
                            // Spawn 1-2 gems per cell for dense, satisfying feedback
                            regionCells.forEach { cell ->
                                gemTargets.add(Offset(cell.anchorX, cell.anchorY))
                                gemTargets.add(Offset(cell.anchorX + (Random.nextFloat() - 0.5f) * 10f, cell.anchorY + (Random.nextFloat() - 0.5f) * 10f))
                            }
                        } else {
                            // Up to 25 targets across larger region
                            val step = (regionCells.size / 20).coerceAtLeast(1)
                            val sample = regionCells.filterIndexed { index, _ -> index % step == 0 }.take(25)
                            sample.forEach { cell ->
                                gemTargets.add(Offset(cell.anchorX, cell.anchorY))
                            }
                        }

                        val newFlights = ArrayList<GemFlightVisual>()
                        val newLandingFx = ArrayList<LandingEffectVisual>()
                        val newParticles = ArrayList<SparkleParticle>()

                        gemTargets.forEachIndexed { idx, targetPos ->
                            val startOffset = Offset(
                                baseOrigin.x + (Random.nextFloat() - 0.5f) * 30f,
                                baseOrigin.y + (Random.nextFloat() - 0.5f) * 30f
                            )
                            val flightTime = now + (idx * 16L) // 16ms rapid cascade rhythm
                            val duration = 270L + Random.nextLong(50L)

                            newFlights.add(
                                GemFlightVisual(
                                    id = now + idx,
                                    startOffset = startOffset,
                                    targetOffset = targetPos,
                                    colorHex = colorHex,
                                    gemType = gemType,
                                    startTime = flightTime,
                                    durationMs = duration,
                                    arcHeight = -110f - Random.nextFloat() * 70f
                                )
                            )

                            if (idx < 8 || idx % 2 == 0) {
                                newLandingFx.add(
                                    LandingEffectVisual(
                                        id = now + 1000 + idx,
                                        center = targetPos,
                                        colorHex = colorHex,
                                        startTime = flightTime + (duration * 0.72f).toLong(),
                                        durationMs = 320L
                                    )
                                )
                            }

                            for (p in 0 until 4) {
                                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                                val speed = Random.nextFloat() * 4f + 2f
                                newParticles.add(
                                    SparkleParticle(
                                        x = targetPos.x,
                                        y = targetPos.y,
                                        color = colorInt,
                                        size = Random.nextFloat() * 8f + 5f,
                                        alpha = 1f,
                                        vx = cos(angle) * speed,
                                        vy = sin(angle) * speed,
                                        life = 0f,
                                        maxLife = Random.nextFloat() * 18f + 12f
                                    )
                                )
                            }
                        }

                        if (newFlights.isNotEmpty()) {
                            _activeGemFlights.value = _activeGemFlights.value + newFlights
                        }
                        if (newLandingFx.isNotEmpty()) {
                            _activeLandingEffects.value = _activeLandingEffects.value + newLandingFx
                        }
                        if (newParticles.isNotEmpty()) {
                            _activeParticles.value = _activeParticles.value + newParticles
                        }

                        // Auto advance color if color is finished
                        if (result.isColorCompleted) {
                            val nextColor = artwork.palette.firstOrNull { col ->
                                !engine.isColorFullyCompleted(col.id)
                            }?.id
                            if (nextColor != null) {
                                engine.selectColor(nextColor)
                                _selectedColorId.value = nextColor
                            }
                        }

                        // Audio & Completion triggers
                        if (result.isArtworkCompleted) {
                            scheduleCompletionSequence()
                        } else if (result.isColorCompleted) {
                            audioController.playSound(SoundEffect.COLOR_COMPLETE)
                            hapticController.performHaptic(HapticPattern.COLOR_COMPLETE)
                        } else {
                            audioController.playSound(SoundEffect.GEM_PLACE)
                            hapticController.performHaptic(HapticPattern.GEM_PLACE)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            is TapResolutionResult.IgnoredWrongColor -> {
                audioController.playSound(SoundEffect.WRONG_TAP)
                hapticController.performHaptic(HapticPattern.WRONG_TAP)
            }
            is TapResolutionResult.IgnoredAlreadyCompleted,
            is TapResolutionResult.IgnoredVoidOrOutOfBounds,
            is TapResolutionResult.IgnoredInvalidState -> {
                // Non-mutating no-op
            }
        }
    }

    fun handleCanvasTap(localX: Float, localY: Float) {
        val cellIdMap = cellIdMapData ?: return
        val lx = localX.toInt()
        val ly = localY.toInt()
        if (lx < 0 || lx >= width || ly < 0 || ly >= height) return
        val cellId = cellIdMap[ly * width + lx]
        if (cellId > 0) {
            handleCellIdTap(cellId)
        }
    }

    fun resetProgress() {
        val artwork = currentArtwork ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Delete from Room via Repository
                repository.deleteProgress(artwork.id)
                
                // Re-initialize gameplay bitmap
                val cellMap = artwork.cellMap
                val isBoundary = BooleanArray(width * height)
                val cmap = cellIdMapData ?: return@withContext
                val settled = settledPixels ?: return@withContext

                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val i = y * width + x
                        val cellId = cmap[i]
                        if (cellId > 0) {
                            if (x < width - 1 && cmap[i + 1] != cellId) {
                                isBoundary[i] = true
                                isBoundary[i + 1] = true
                            }
                            if (y < height - 1 && cmap[i + width] != cellId) {
                                isBoundary[i] = true
                                isBoundary[i + width] = true
                            }
                        }
                    }
                }

                val revealed = _revealedBitmap.value ?: return@withContext
                val revealPixels = IntArray(width * height)
                for (i in 0 until width * height) {
                    val cellId = cmap[i]
                    if (cellId > 0) {
                        if (isBoundary[i]) {
                            revealPixels[i] = 0xFFD2D2D2.toInt()
                        } else {
                            revealPixels[i] = 0xFFF4F4F6.toInt()
                        }
                    } else {
                        revealPixels[i] = 0x00000000
                    }
                }
                revealed.setPixels(revealPixels, 0, width, 0, 0, width, height)

                val firstColor = artwork.palette.firstOrNull()?.id
                gameplayEngine = GameplayCoreEngine(
                    artwork = artwork,
                    initialCompletedRegionIds = emptySet(),
                    initialSelectedColorId = firstColor
                )

                _completedRegionIds.value = emptySet()
                _revealedBitmap.value = revealed
                _selectedColorId.value = firstColor
                _isArtworkCompleted.value = false
                _showCompletionModal.value = false
                _hintTargetRegionId.value = null
                _hintTargetCell.value = null
            }
        }
    }

    fun requestHint() {
        useHint()
    }

    fun selectNextArtwork() {
        val current = currentArtwork
        val list = catalogItems
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == current?.id }
        val nextArtwork = if (currentIndex != -1 && currentIndex + 1 < list.size) {
            list[currentIndex + 1]
        } else {
            list.first()
        }
        _showCompletionModal.value = false
        selectArtwork(nextArtwork.id)
    }

    private fun startParticleAnimationLoop() {
        viewModelScope.launch {
            while (true) {
                delay(16)
                val now = System.currentTimeMillis()

                // Update particles
                val current = _activeParticles.value
                if (current.isNotEmpty()) {
                    val updated = current.mapNotNull { p ->
                        if (p.life >= p.maxLife) {
                            null
                        } else {
                            p.copy(
                                x = p.x + p.vx,
                                y = p.y + p.vy,
                                vy = p.vy + 0.1f, // simple gravity
                                alpha = 1f - (p.life / p.maxLife),
                                life = p.life + 1f
                            )
                        }
                    }
                    _activeParticles.value = updated
                }

                // Update & prune Gem Flights
                val flights = _activeGemFlights.value
                if (flights.isNotEmpty()) {
                    var soundIndex = 0
                    flights.forEach { flight ->
                        val progress = flight.currentProgress(now)
                        val flightId = flight.id

                        // Launch sound: trigger when launch start time reached
                        if (now >= flight.startTime && !playedLaunchSoundIds.contains(flightId)) {
                            playedLaunchSoundIds.add(flightId)
                            val launchPitch = 1.05f + (soundIndex % 5) * 0.05f
                            audioController.playSound(SoundEffect.TAP, launchPitch)
                            hapticController.performHaptic(HapticPattern.LIGHT_TAP)
                            soundIndex++
                        }

                        // Impact sound: trigger when gem arrives at target cell
                        if (progress >= 0.85f && !playedImpactSoundIds.contains(flightId)) {
                            playedImpactSoundIds.add(flightId)
                            val impactPitch = 1.0f + (soundIndex % 8) * 0.04f
                            audioController.playSound(SoundEffect.GEM_PLACE, impactPitch)
                            hapticController.performHaptic(HapticPattern.GEM_PLACE)
                        }
                    }

                    val active = flights.filter { !it.isFinished(now) }
                    if (active.size != flights.size) {
                        val finished = flights.filter { it.isFinished(now) }
                        finished.forEach {
                            playedLaunchSoundIds.remove(it.id)
                            playedImpactSoundIds.remove(it.id)
                        }
                        _activeGemFlights.value = active
                    }
                }

                // Update & prune Landing Effects
                val landingFx = _activeLandingEffects.value
                if (landingFx.isNotEmpty()) {
                    val active = landingFx.filter { !it.isFinished(now) }
                    if (active.size != landingFx.size) {
                        _activeLandingEffects.value = active
                    }
                }
            }
        }
    }

    fun resumeAudio() {
        (audioController as? AndroidAudioController)?.onAppForeground()
    }

    fun pauseAudio() {
        (audioController as? AndroidAudioController)?.onAppBackground()
    }

    override fun onCleared() {
        super.onCleared()
        audioController.release()
    }

    class Factory(
        private val application: Application,
        private val database: AppDatabase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PixelGleamViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PixelGleamViewModel(application, database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class ArtworkInfo(
    val id: String,
    val title: String,
    val category: String
)
