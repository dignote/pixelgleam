package com.example

import com.example.core.engine.GameplayCoreEngine
import com.example.core.engine.PrimaryGameplayState
import com.example.core.engine.TapResolutionResult
import com.example.data.Artwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameplayCoreEngineTest {

    private lateinit var testArtwork: Artwork

    @Before
    fun setUp() {
        // Construct a clean multi-region test artwork with disconnected same-color areas
        val json = """
        {
            "id": "corgi_test",
            "title": "Corgi Test",
            "category": "Animals",
            "canvas": { "width": 32, "height": 32 },
            "palette": [
                { "id": 1, "hex": "#E58B24", "name": "Orange" },
                { "id": 2, "hex": "#FFFFFF", "name": "White" },
                { "id": 3, "hex": "#2C2C2C", "name": "Black" }
            ],
            "cells": [
                { "id": 1, "center": [5.0, 5.0], "colorId": 1, "regionId": 101, "neighbors": [2] },
                { "id": 2, "center": [6.0, 5.0], "colorId": 1, "regionId": 101, "neighbors": [1] },
                { "id": 3, "center": [20.0, 20.0], "colorId": 1, "regionId": 102, "neighbors": [] },
                { "id": 4, "center": [10.0, 10.0], "colorId": 2, "regionId": 201, "neighbors": [] },
                { "id": 5, "center": [15.0, 15.0], "colorId": 3, "regionId": 301, "neighbors": [] }
            ],
            "regions": [
                { "id": 101, "colorId": 1, "cellIds": [1, 2] },
                { "id": 102, "colorId": 1, "cellIds": [3] },
                { "id": 201, "colorId": 2, "cellIds": [4] },
                { "id": 301, "colorId": 3, "cellIds": [5] }
            ]
        }
        """.trimIndent()
        testArtwork = Artwork.parseFromJson(json)
    }

    @Test
    fun testP0_ValidTapCompletesExactConnectedRegionOnly() {
        val engine = GameplayCoreEngine(testArtwork)
        engine.selectColor(1)

        // Tap cell 1 (belongs to region 101)
        val result = engine.processCellTap(1)

        assertTrue(result is TapResolutionResult.Success)
        val success = result as TapResolutionResult.Success
        assertEquals(101, success.completedRegion.id)
        assertEquals(1, success.colorId)
        assertFalse(success.isColorCompleted) // Region 102 (color 1) is still incomplete
        assertFalse(success.isArtworkCompleted)

        // Both cells in region 101 should be completed
        assertTrue(engine.isCellCompleted(1))
        assertTrue(engine.isCellCompleted(2))
        assertTrue(engine.isRegionCompleted(101))

        // Disconnected region 102 (same color 1) must remain INCOMPLETE (INV-005, INV-010)
        assertFalse(engine.isCellCompleted(3))
        assertFalse(engine.isRegionCompleted(102))
    }

    @Test
    fun testP0_DisconnectedSameColorAreasRequireSeparateTaps() {
        val engine = GameplayCoreEngine(testArtwork)
        engine.selectColor(1)

        // 1st tap on cell 1 -> completes region 101
        engine.processCellTap(1)
        assertEquals(setOf(101), engine.completedRegionIds)
        assertFalse(engine.isColorFullyCompleted(1))

        // 2nd tap on cell 3 -> completes disconnected region 102
        val result2 = engine.processCellTap(3)
        assertTrue(result2 is TapResolutionResult.Success)
        val success2 = result2 as TapResolutionResult.Success
        assertEquals(102, success2.completedRegion.id)
        assertTrue(success2.isColorCompleted) // Now color 1 is fully complete
        assertEquals(setOf(101, 102), engine.completedRegionIds)
    }

    @Test
    fun testP0_WrongColorTapDoesNotMutateProgress() {
        val engine = GameplayCoreEngine(testArtwork)
        engine.selectColor(2) // Selected color 2 (White)

        // Tapping cell 1 (Color 1 - Orange)
        val result = engine.processCellTap(1)

        assertTrue(result is TapResolutionResult.IgnoredWrongColor)
        val wrong = result as TapResolutionResult.IgnoredWrongColor
        assertEquals(2, wrong.expectedColorId)
        assertEquals(1, wrong.tappedColorId)

        // Zero mutations (INV-013)
        assertTrue(engine.completedRegionIds.isEmpty())
        assertFalse(engine.isCellCompleted(1))
        assertFalse(engine.isRegionCompleted(101))
        assertEquals(0, engine.getOverallProgress().completedRegionCount)
    }

    @Test
    fun testP0_IdempotentAlreadyCompletedRegion() {
        val engine = GameplayCoreEngine(testArtwork)
        engine.selectColor(1)

        // First tap completes region 101
        engine.processCellTap(1)
        assertEquals(1, engine.completedRegionIds.size)

        // Second tap on same region (cell 2)
        val result = engine.processCellTap(2)
        assertEquals(TapResolutionResult.IgnoredAlreadyCompleted, result)

        // State remains strictly 1 region completed
        assertEquals(1, engine.completedRegionIds.size)
        assertEquals(setOf(101), engine.completedRegionIds)
    }

    @Test
    fun testP0_VoidAndOutOfBoundsAreIgnored() {
        val engine = GameplayCoreEngine(testArtwork)
        engine.selectColor(1)

        val resultZero = engine.processCellTap(0)
        assertEquals(TapResolutionResult.IgnoredVoidOrOutOfBounds, resultZero)

        val resultNegative = engine.processCellTap(-5)
        assertEquals(TapResolutionResult.IgnoredVoidOrOutOfBounds, resultNegative)

        val resultNonExistent = engine.processCellTap(9999)
        assertEquals(TapResolutionResult.IgnoredVoidOrOutOfBounds, resultNonExistent)

        assertTrue(engine.completedRegionIds.isEmpty())
    }

    @Test
    fun testP0_LogicalCompletionEvaluatesArtworkState() {
        val engine = GameplayCoreEngine(testArtwork)

        // Complete all regions sequentially
        engine.selectColor(1)
        engine.processCellTap(1) // completes 101
        engine.processCellTap(3) // completes 102

        engine.selectColor(2)
        engine.processCellTap(4) // completes 201

        engine.selectColor(3)
        val finalResult = engine.processCellTap(5) // completes 301

        assertTrue(finalResult is TapResolutionResult.Success)
        val success = finalResult as TapResolutionResult.Success
        assertTrue(success.isColorCompleted)
        assertTrue(success.isArtworkCompleted)

        assertEquals(PrimaryGameplayState.COMPLETED, engine.primaryState)
        assertTrue(engine.isAllRegionsCompleted())
        assertEquals(1.0f, engine.getOverallProgress().progressFraction, 0.001f)
    }

    @Test
    fun testP0_HintLocatesUnfinishedRegionWithoutMutatingState() {
        val engine = GameplayCoreEngine(testArtwork)
        engine.selectColor(1)

        val target = engine.findHintTargetRegion()
        assertNotNull(target)
        assertEquals(1, target?.colorId)
        assertTrue(target?.id == 101 || target?.id == 102)

        // Hint does NOT complete the region (INV-090)
        assertTrue(engine.completedRegionIds.isEmpty())
    }
}
