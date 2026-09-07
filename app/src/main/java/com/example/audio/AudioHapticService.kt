package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class SoundEffect {
    TAP,
    NAVIGATION,
    PALETTE_SELECT,
    GEM_PLACE,
    REGION_COMPLETE,
    COLOR_COMPLETE,
    HINT,
    WRONG_TAP,
    ARTWORK_COMPLETE
}

enum class HapticPattern {
    LIGHT_TAP,
    NAVIGATION,
    PALETTE_SELECT,
    GEM_PLACE,
    REGION_COMPLETE,
    COLOR_COMPLETE,
    HINT,
    WRONG_TAP,
    ARTWORK_COMPLETE
}

interface AudioController {
    fun playBgm()
    fun pauseBgm()
    fun stopBgm()
    fun playSound(effect: SoundEffect, pitch: Float = 1.0f)
    fun setMusicEnabled(enabled: Boolean)
    fun setSfxEnabled(enabled: Boolean)
    fun isMusicEnabled(): Boolean
    fun isSfxEnabled(): Boolean
    fun release()
}

interface HapticController {
    fun performHaptic(pattern: HapticPattern)
    fun setHapticsEnabled(enabled: Boolean)
    fun isHapticsEnabled(): Boolean
}

/**
 * Procedural synthesis engine for "Soft Gem Workshop" audio identity.
 * Generates pristine, 16-bit 44.1kHz PCM WAV audio for all sound effects and ambient BGM.
 */
object GemAudioSynthesizer {
    private const val SAMPLE_RATE = 44100

    fun generateWav(samples: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val numSamples = samples.size
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalSize)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // fmt subchunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat (1 = PCM)
        buffer.putShort(1) // NumChannels (1 = Mono)
        buffer.putInt(sampleRate) // SampleRate
        buffer.putInt(sampleRate * 2) // ByteRate (SampleRate * NumChannels * BitsPerSample/8)
        buffer.putShort(2) // BlockAlign (NumChannels * BitsPerSample/8)
        buffer.putShort(16) // BitsPerSample (16 bits)

        // data subchunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(dataSize)

        for (sample in samples) {
            buffer.putShort(sample)
        }

        return buffer.array()
    }

    /**
     * UI Tap / button click sound (35ms crisp crystal click).
     */
    fun createTapSound(): ByteArray {
        val duration = 0.035
        val n = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t / 0.007)
            val f = 1200.0 - 400.0 * (t / duration)
            val v = sin(2.0 * PI * f * t) * env
            samples[i] = (v * 16000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return generateWav(samples)
    }

    /**
     * Navigation sound (65ms soft airy transition tone).
     */
    fun createNavigationSound(): ByteArray {
        val duration = 0.065
        val n = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = sin(PI * (t / duration)) * exp(-t / 0.035)
            val f = 750.0 + 350.0 * (t / duration)
            val v = sin(2.0 * PI * f * t) * env
            samples[i] = (v * 17000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return generateWav(samples)
    }

    /**
     * Palette color selection (55ms delicate crystal bell tap).
     */
    fun createPaletteSelectSound(): ByteArray {
        val duration = 0.055
        val n = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t / 0.014)
            val f1 = 1760.0 // A6
            val f2 = 3520.0 // A7 overtone
            val v = (sin(2.0 * PI * f1 * t) * 0.75 + sin(2.0 * PI * f2 * t) * 0.25) * env
            samples[i] = (v * 20000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return generateWav(samples)
    }

    /**
     * Gem placement sound (90ms satisfying crystalline stone placement snap).
     */
    fun createGemPlaceSound(): ByteArray {
        val duration = 0.090
        val n = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            // Attack transient (click) + crystal resonance
            val clickEnv = exp(-t / 0.005)
            val click = sin(2.0 * PI * 2800.0 * t) * clickEnv * 0.4
            val bodyEnv = exp(-t / 0.025)
            val f1 = 1320.0 // E6
            val f2 = 2640.0 // E7
            val f3 = 3960.0 // B7 shimmer
            val body = (sin(2.0 * PI * f1 * t) * 0.6 + sin(2.0 * PI * f2 * t) * 0.3 + sin(2.0 * PI * f3 * t) * 0.1) * bodyEnv
            val v = click + body
            samples[i] = (v * 22000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return generateWav(samples)
    }

    /**
     * Region complete sound (220ms two-tone crystal chord chime).
     */
    fun createRegionCompleteSound(): ByteArray {
        val duration = 0.220
        val n = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t / 0.055)
            val f1 = 659.25 // E5
            val f2 = 987.77 // B5
            val f3 = 1661.22 // G#6
            val v = (sin(2.0 * PI * f1 * t) * 0.45 + sin(2.0 * PI * f2 * t) * 0.35 + sin(2.0 * PI * f3 * t) * 0.20) * env
            samples[i] = (v * 21000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return generateWav(samples)
    }

    /**
     * Color complete sound (380ms ascending crystal arpeggio shimmer).
     */
    fun createColorCompleteSound(): ByteArray {
        val duration = 0.380
        val n = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(n)
        val freqs = doubleArrayOf(1046.5, 1318.5, 1568.0, 2093.0) // C6, E6, G6, C7
        val noteStarts = doubleArrayOf(0.0, 0.060, 0.120, 0.180)

        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            var sum = 0.0
            for (idx in freqs.indices) {
                val start = noteStarts[idx]
                if (t >= start) {
                    val dt = t - start
                    val env = exp(-dt / 0.065)
                    val f = freqs[idx]
                    val noteVal = (sin(2.0 * PI * f * dt) * 0.8 + sin(2.0 * PI * f * 2.0 * dt) * 0.2) * env
                    sum += noteVal * 0.35
                }
            }
            samples[i] = (sum * 24000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return generateWav(samples)
    }

    /**
     * Hint sound (380ms magical sparkling chime).
     */
    fun createHintSound(): ByteArray {
        val duration = 0.380
        val n = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t / 0.090)
            val lfo = sin(2.0 * PI * 14.0 * t) * 200.0
            val f = 1450.0 + lfo
            val fOvertone = 2900.0 + lfo * 2.0
            val v = (sin(2.0 * PI * f * t) * 0.7 + sin(2.0 * PI * fOvertone * t) * 0.3) * env
            samples[i] = (v * 20000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return generateWav(samples)
    }

    /**
     * Wrong tap sound (45ms subtle, gentle damped bump - non-punitive).
     */
    fun createWrongTapSound(): ByteArray {
        val duration = 0.045
        val n = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t / 0.011)
            val f = 180.0
            val v = sin(2.0 * PI * f * t) * env
            samples[i] = (v * 15000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return generateWav(samples)
    }

    /**
     * Artwork complete sound (850ms celebratory full crystalline fanfare).
     */
    fun createArtworkCompleteSound(): ByteArray {
        val duration = 0.850
        val n = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(n)
        // Grand arpeggio: C5, G5, C6, E6, G6, C7, E7
        val freqs = doubleArrayOf(523.25, 783.99, 1046.5, 1318.5, 1568.0, 2093.0, 2637.0)
        val noteStarts = doubleArrayOf(0.0, 0.055, 0.110, 0.165, 0.220, 0.275, 0.330)

        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            var sum = 0.0
            for (idx in freqs.indices) {
                val start = noteStarts[idx]
                if (t >= start) {
                    val dt = t - start
                    val env = exp(-dt / 0.14)
                    val f = freqs[idx]
                    val noteVal = (sin(2.0 * PI * f * dt) * 0.75 + sin(2.0 * PI * f * 2.0 * dt) * 0.25) * env
                    sum += noteVal * 0.25
                }
            }
            samples[i] = (sum * 25000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return generateWav(samples)
    }

    /**
     * Ambient BGM (12.0s seamless loop of relaxing crystal kalimba & bell pad music).
     * "Soft Gem Workshop" Identity.
     */
    fun createBgmSound(): ByteArray {
        val duration = 12.0
        val n = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(n)

        // Pentatonic peaceful notes: C4, E4, G4, A4, B4, C5, D5, E5, G5, A5
        val melodyNotes = listOf(
            Pair(0.0, 523.25),   // C5
            Pair(0.75, 659.25),  // E5
            Pair(1.5, 783.99),   // G5
            Pair(2.25, 987.77),  // B5
            Pair(3.0, 880.00),   // A5
            Pair(3.75, 783.99),  // G5
            Pair(4.5, 659.25),   // E5
            Pair(5.25, 587.33),  // D5
            Pair(6.0, 523.25),   // C5
            Pair(6.75, 659.25),  // E5
            Pair(7.5, 783.99),   // G5
            Pair(8.25, 1046.50), // C6
            Pair(9.0, 880.00),   // A5
            Pair(9.75, 783.99),  // G5
            Pair(10.5, 659.25),  // E5
            Pair(11.25, 587.33)  // D5
        )

        // Ambient chord roots: C, Am, F, G (3.0s each)
        val chords = listOf(
            Triple(0.0, 3.0, doubleArrayOf(261.63, 329.63, 392.00, 493.88)), // Cmaj7
            Triple(3.0, 6.0, doubleArrayOf(220.00, 261.63, 329.63, 392.00)), // Am7
            Triple(6.0, 9.0, doubleArrayOf(174.61, 261.63, 329.63, 392.00)), // Fmaj7
            Triple(9.0, 12.0, doubleArrayOf(196.00, 261.63, 293.66, 392.00)) // Gsus4
        )

        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            var sampleVal = 0.0

            // 1. Ambient pad chords
            for (chord in chords) {
                if (t >= chord.first && t < chord.second) {
                    val chordTime = t - chord.first
                    // Soft envelope for chord
                    val chordEnv = sin(PI * (chordTime / (chord.second - chord.first))).coerceAtLeast(0.0)
                    var chordSum = 0.0
                    for (f in chord.third) {
                        chordSum += sin(2.0 * PI * f * t)
                    }
                    sampleVal += (chordSum / chord.third.size) * chordEnv * 0.28
                }
            }

            // 2. Plucked Kalimba Melody
            for (note in melodyNotes) {
                val start = note.first
                if (t >= start && t < start + 1.2) {
                    val dt = t - start
                    val env = exp(-dt / 0.35)
                    val f = note.second
                    val noteTone = (sin(2.0 * PI * f * dt) * 0.8 + sin(2.0 * PI * f * 2.0 * dt) * 0.2) * env
                    sampleVal += noteTone * 0.42
                }
            }

            // Loop boundary crossfade (50ms) to ensure zero audible pop/click at loop restart
            val boundaryFade = min(t / 0.05, (duration - t) / 0.05).coerceIn(0.0, 1.0)
            sampleVal *= boundaryFade

            samples[i] = (sampleVal * 18000).toInt().coerceIn(-32767, 32767).toShort()
        }

        return generateWav(samples)
    }
}

/**
 * Production Android Audio Controller.
 * Manages SoundPool for ultra-low latency SFX and MediaPlayer for seamless looping ambient BGM.
 */
class AndroidAudioController(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : AudioController {

    private val soundPool: SoundPool
    private val soundMap = HashMap<SoundEffect, Int>()
    private var mediaPlayer: MediaPlayer? = null

    private var musicEnabled: Boolean = true
    private var sfxEnabled: Boolean = true
    private var isAppInForeground: Boolean = true
    private var isBgmPrepared: Boolean = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        // Asynchronously synthesize and load SFX and BGM into memory/cache
        scope.launch {
            try {
                val audioDir = File(context.cacheDir, "gem_audio").apply { if (!exists()) mkdirs() }

                val sfxFiles = mapOf(
                    SoundEffect.TAP to File(audioDir, "tap.wav") to { GemAudioSynthesizer.createTapSound() },
                    SoundEffect.NAVIGATION to File(audioDir, "navigation.wav") to { GemAudioSynthesizer.createNavigationSound() },
                    SoundEffect.PALETTE_SELECT to File(audioDir, "palette_select.wav") to { GemAudioSynthesizer.createPaletteSelectSound() },
                    SoundEffect.GEM_PLACE to File(audioDir, "gem_place.wav") to { GemAudioSynthesizer.createGemPlaceSound() },
                    SoundEffect.REGION_COMPLETE to File(audioDir, "region_complete.wav") to { GemAudioSynthesizer.createRegionCompleteSound() },
                    SoundEffect.COLOR_COMPLETE to File(audioDir, "color_complete.wav") to { GemAudioSynthesizer.createColorCompleteSound() },
                    SoundEffect.HINT to File(audioDir, "hint.wav") to { GemAudioSynthesizer.createHintSound() },
                    SoundEffect.WRONG_TAP to File(audioDir, "wrong_tap.wav") to { GemAudioSynthesizer.createWrongTapSound() },
                    SoundEffect.ARTWORK_COMPLETE to File(audioDir, "artwork_complete.wav") to { GemAudioSynthesizer.createArtworkCompleteSound() }
                )

                for ((entry, generator) in sfxFiles) {
                    val (effect, file) = entry
                    if (!file.exists() || file.length() == 0L) {
                        FileOutputStream(file).use { it.write(generator()) }
                    }
                    val soundId = soundPool.load(file.absolutePath, 1)
                    synchronized(soundMap) {
                        soundMap[effect] = soundId
                    }
                }

                // Initialize BGM
                val bgmFile = File(audioDir, "bgm_ambient.wav")
                if (!bgmFile.exists() || bgmFile.length() == 0L) {
                    FileOutputStream(bgmFile).use { it.write(GemAudioSynthesizer.createBgmSound()) }
                }

                synchronized(this@AndroidAudioController) {
                    try {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(bgmFile.absolutePath)
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_GAME)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            isLooping = true
                            setVolume(0.35f, 0.35f)
                            prepare()
                            isBgmPrepared = true
                            if (musicEnabled && isAppInForeground) {
                                start()
                            }
                        }
                    } catch (e: Throwable) {
                        Log.w("AudioController", "MediaPlayer init skipped: ${e.message}")
                    }
                }
            } catch (e: Throwable) {
                Log.w("AudioController", "Failed to initialize sound assets: ${e.message}")
            }
        }
    }

    @Synchronized
    override fun playBgm() {
        if (!musicEnabled || !isAppInForeground) return
        try {
            mediaPlayer?.let { player ->
                if (isBgmPrepared && !player.isPlaying) {
                    player.start()
                }
            }
        } catch (_: Throwable) {}
    }

    @Synchronized
    override fun pauseBgm() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                }
            }
        } catch (_: Throwable) {}
    }

    @Synchronized
    override fun stopBgm() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    player.seekTo(0)
                }
            }
        } catch (_: Throwable) {}
    }

    override fun playSound(effect: SoundEffect, pitch: Float) {
        if (!sfxEnabled) return
        try {
            val soundId = synchronized(soundMap) { soundMap[effect] } ?: return
            val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
            soundPool.play(soundId, 0.9f, 0.9f, 1, 0, clampedPitch)
        } catch (_: Throwable) {}
    }

    @Synchronized
    override fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (enabled) {
            playBgm()
        } else {
            pauseBgm()
        }
    }

    override fun setSfxEnabled(enabled: Boolean) {
        sfxEnabled = enabled
    }

    override fun isMusicEnabled(): Boolean = musicEnabled

    override fun isSfxEnabled(): Boolean = sfxEnabled

    @Synchronized
    fun onAppForeground() {
        isAppInForeground = true
        if (musicEnabled) {
            playBgm()
        }
    }

    @Synchronized
    fun onAppBackground() {
        isAppInForeground = false
        pauseBgm()
    }

    @Synchronized
    override fun release() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
            soundPool.release()
        } catch (_: Throwable) {}
    }
}

class AndroidHapticController(context: Context) : HapticController {
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Throwable) {
        null
    }

    private var hapticsEnabled: Boolean = true

    override fun performHaptic(pattern: HapticPattern) {
        if (!hapticsEnabled || vibrator == null) return

        try {
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (pattern) {
                    HapticPattern.LIGHT_TAP -> VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticPattern.NAVIGATION -> VibrationEffect.createOneShot(14, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticPattern.PALETTE_SELECT -> VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticPattern.GEM_PLACE -> VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticPattern.REGION_COMPLETE -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticPattern.COLOR_COMPLETE -> VibrationEffect.createWaveform(longArrayOf(0, 20, 30, 25), -1)
                    HapticPattern.HINT -> VibrationEffect.createWaveform(longArrayOf(0, 15, 30, 20), -1)
                    HapticPattern.ARTWORK_COMPLETE -> VibrationEffect.createWaveform(longArrayOf(0, 35, 45, 45, 40, 70), -1)
                    HapticPattern.WRONG_TAP -> VibrationEffect.createOneShot(20, 45)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (pattern) {
                    HapticPattern.LIGHT_TAP -> vibrator.vibrate(12)
                    HapticPattern.NAVIGATION -> vibrator.vibrate(14)
                    HapticPattern.PALETTE_SELECT -> vibrator.vibrate(15)
                    HapticPattern.GEM_PLACE -> vibrator.vibrate(18)
                    HapticPattern.REGION_COMPLETE -> vibrator.vibrate(30)
                    HapticPattern.COLOR_COMPLETE -> vibrator.vibrate(longArrayOf(0, 20, 30, 25), -1)
                    HapticPattern.HINT -> vibrator.vibrate(longArrayOf(0, 15, 30, 20), -1)
                    HapticPattern.ARTWORK_COMPLETE -> vibrator.vibrate(longArrayOf(0, 35, 45, 45, 40, 70), -1)
                    HapticPattern.WRONG_TAP -> vibrator.vibrate(20)
                }
            }
        } catch (_: Throwable) {
            // Silently swallow any vibration security exception or hardware errors
        }
    }

    override fun setHapticsEnabled(enabled: Boolean) {
        hapticsEnabled = enabled
    }

    override fun isHapticsEnabled(): Boolean = hapticsEnabled
}

