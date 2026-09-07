package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.audio.AudioController
import com.example.audio.GemAudioSynthesizer
import com.example.audio.HapticController
import com.example.audio.HapticPattern
import com.example.audio.SoundEffect
import com.example.data.Artwork
import com.example.data.Png16Decoder
import com.example.data.db.AppPreference
import com.example.data.db.DailyProgress
import com.example.ui.navigation.PixelGleamScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FoundationArchitectureTest {

    @Test
    fun testRealAssetCellIdMapDecoding() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inputStream = context.assets.open("artworks/happy_corgi_v3/cell_id_map.png")
        val decoded = Png16Decoder.decodeGrayscale16(inputStream)
        assertEquals(365, decoded.width)
        assertEquals(455, decoded.height)
        assertEquals(365 * 455, decoded.data.size)
        // Check that at least some pixels contain valid cell IDs > 0
        val maxCellId = decoded.data.maxOrNull() ?: 0
        assertTrue("Cell ID map should contain positive cell IDs", maxCellId > 0)

        // Also test B2_flower 1254x1254 benchmark asset
        val b2Stream = context.assets.open("artworks/B2_flower/cell_id_map.png")
        val b2Decoded = Png16Decoder.decodeGrayscale16(b2Stream)
        assertEquals(1254, b2Decoded.width)
        assertEquals(1254, b2Decoded.height)
        val b2MaxCellId = b2Decoded.data.maxOrNull() ?: 0
        assertTrue("B2_flower should contain positive cell IDs", b2MaxCellId > 0)

        // Also test production assets A1 to A5
        val expectedCounts = mapOf(
            "A1_blossom_bicycle" to 400,
            "A2_royal_koi_pair" to 416,
            "A3_cozy_tea_corner" to 393,
            "A4_moon_fairy_portrait" to 424,
            "A5_lucky_shiba_garden" to 383
        )
        for ((artId, expectedMaxId) in expectedCounts) {
            val stream = context.assets.open("artworks/$artId/cell_id_map.png")
            val decodedMap = Png16Decoder.decodeGrayscale16(stream)
            assertEquals(1254, decodedMap.width)
            assertEquals(1254, decodedMap.height)
            val maxId = decodedMap.data.maxOrNull() ?: 0
            assertEquals(expectedMaxId, maxId)
        }
    }

    @Test
    fun testArtworkJsonParsingAndInvariants() {
        val sampleJson = """
        {
            "id": "test_sample",
            "title": "Test Artwork",
            "category": "Testing",
            "canvas": { "width": 32, "height": 32 },
            "palette": [
                { "id": 1, "hex": "#FF5500", "name": "Orange" },
                { "id": 2, "hex": "#00AAFF", "name": "Blue" }
            ],
            "cells": [
                { "id": 1, "center": [10.0, 10.0], "colorId": 1, "regionId": 101, "neighbors": [2] },
                { "id": 2, "center": [11.0, 10.0], "colorId": 1, "regionId": 101, "neighbors": [1] },
                { "id": 3, "center": [20.0, 20.0], "colorId": 2, "regionId": 102, "neighbors": [] }
            ],
            "regions": [
                { "id": 101, "colorId": 1, "cellIds": [1, 2] },
                { "id": 102, "colorId": 2, "cellIds": [3] }
            ]
        }
        """.trimIndent()

        val artwork = Artwork.parseFromJson(sampleJson)

        assertEquals("test_sample", artwork.id)
        assertEquals(32, artwork.canvas.width)
        assertEquals(32, artwork.canvas.height)
        assertEquals(2, artwork.palette.size)
        assertEquals(3, artwork.cells.size)
        assertEquals(2, artwork.regions.size)

        // Maps integrity
        assertNotNull(artwork.cellMap[1])
        assertNotNull(artwork.cellMap[2])
        assertNotNull(artwork.cellMap[3])
        assertEquals(101, artwork.cellMap[1]?.regionId)
        assertEquals(101, artwork.cellMap[2]?.regionId)
        assertEquals(102, artwork.cellMap[3]?.regionId)

        assertNotNull(artwork.regionMap[101])
        assertEquals(2, artwork.regionMap[101]?.cellIds?.size)
    }

    @Test
    fun testNavigationDestinations() {
        val gallery: PixelGleamScreen = PixelGleamScreen.Gallery
        val gameplay: PixelGleamScreen = PixelGleamScreen.Gameplay("happy_corgi_v3")
        val daily: PixelGleamScreen = PixelGleamScreen.Daily
        val myArt: PixelGleamScreen = PixelGleamScreen.MyArt
        val settings: PixelGleamScreen = PixelGleamScreen.Settings
        val tutorial: PixelGleamScreen = PixelGleamScreen.Tutorial

        assertTrue(gallery is PixelGleamScreen.Gallery)
        assertEquals("happy_corgi_v3", (gameplay as PixelGleamScreen.Gameplay).artworkId)
        assertTrue(daily is PixelGleamScreen.Daily)
        assertTrue(myArt is PixelGleamScreen.MyArt)
        assertTrue(settings is PixelGleamScreen.Settings)
        assertTrue(tutorial is PixelGleamScreen.Tutorial)
    }

    @Test
    fun testDatabaseEntities() {
        val pref = AppPreference(key = "music_enabled", value = "true")
        assertEquals("music_enabled", pref.key)
        assertEquals("true", pref.value)

        val daily = DailyProgress(dateString = "2026-08-29", artworkId = "happy_corgi_v3", isCompleted = true)
        assertEquals("2026-08-29", daily.dateString)
        assertEquals("happy_corgi_v3", daily.artworkId)
        assertTrue(daily.isCompleted)
    }

    @Test
    fun testHapticControllerContract() {
        var hapticTriggered: HapticPattern? = null
        val mockHaptic = object : HapticController {
            private var enabled = true
            override fun performHaptic(pattern: HapticPattern) {
                if (enabled) hapticTriggered = pattern
            }
            override fun setHapticsEnabled(enabled: Boolean) {
                this.enabled = enabled
            }
            override fun isHapticsEnabled(): Boolean = enabled
        }

        mockHaptic.performHaptic(HapticPattern.REGION_COMPLETE)
        assertEquals(HapticPattern.REGION_COMPLETE, hapticTriggered)

        mockHaptic.setHapticsEnabled(false)
        mockHaptic.performHaptic(HapticPattern.ARTWORK_COMPLETE)
        // Should remain unchanged because haptics was disabled
        assertEquals(HapticPattern.REGION_COMPLETE, hapticTriggered)
    }

    @Test
    fun testAudioControllerContract() {
        var soundPlayed: SoundEffect? = null
        val mockAudio = object : AudioController {
            private var music = true
            private var sfx = true
            override fun playSound(effect: SoundEffect, pitch: Float) {
                if (sfx) soundPlayed = effect
            }
            override fun playBgm() {}
            override fun pauseBgm() {}
            override fun stopBgm() {}
            override fun setMusicEnabled(enabled: Boolean) { music = enabled }
            override fun setSfxEnabled(enabled: Boolean) { sfx = enabled }
            override fun isMusicEnabled(): Boolean = music
            override fun isSfxEnabled(): Boolean = sfx
            override fun release() {}
        }

        mockAudio.playSound(SoundEffect.GEM_PLACE)
        assertEquals(SoundEffect.GEM_PLACE, soundPlayed)

        mockAudio.setSfxEnabled(false)
        mockAudio.playSound(SoundEffect.ARTWORK_COMPLETE)
        assertEquals(SoundEffect.GEM_PLACE, soundPlayed)
    }

    @Test
    fun testGemAudioSynthesizerWavGeneration() {
        val tapWav = GemAudioSynthesizer.createTapSound()
        assertTrue("Tap WAV bytes should not be empty", tapWav.isNotEmpty())
        assertEquals('R'.code.toByte(), tapWav[0])
        assertEquals('I'.code.toByte(), tapWav[1])
        assertEquals('F'.code.toByte(), tapWav[2])
        assertEquals('F'.code.toByte(), tapWav[3])
        assertEquals('W'.code.toByte(), tapWav[8])
        assertEquals('A'.code.toByte(), tapWav[9])
        assertEquals('V'.code.toByte(), tapWav[10])
        assertEquals('E'.code.toByte(), tapWav[11])

        val gemPlaceWav = GemAudioSynthesizer.createGemPlaceSound()
        assertTrue("Gem Place WAV bytes should not be empty", gemPlaceWav.isNotEmpty())

        val bgmWav = GemAudioSynthesizer.createBgmSound()
        assertTrue("BGM WAV bytes should not be empty", bgmWav.isNotEmpty())
        assertEquals('R'.code.toByte(), bgmWav[0])
        assertEquals('I'.code.toByte(), bgmWav[1])
        assertEquals('F'.code.toByte(), bgmWav[2])
        assertEquals('F'.code.toByte(), bgmWav[3])
    }
}
