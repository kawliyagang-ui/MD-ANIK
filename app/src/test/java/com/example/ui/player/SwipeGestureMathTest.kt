package com.example.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeGestureMathTest {

    @Test
    fun decideSwipeAxis_waitsUntilSlopThenLocksToDominantDirection() {
        val slopPx = 72f // ~24dp at xxhdpi

        // Movement below slop stays pending
        assertNull(SwipeGestureMath.decideSwipeAxis(dx = 20f, dy = 10f, touchSlopPx = slopPx))
        assertNull(SwipeGestureMath.decideSwipeAxis(dx = 0f, dy = -71f, touchSlopPx = slopPx))

        // Dominant horizontal movement locks HORIZONTAL
        assertEquals(
            SwipeAxis.HORIZONTAL,
            SwipeGestureMath.decideSwipeAxis(dx = 80f, dy = 20f, touchSlopPx = slopPx)
        )
        assertEquals(
            SwipeAxis.HORIZONTAL,
            SwipeGestureMath.decideSwipeAxis(dx = -75f, dy = 30f, touchSlopPx = slopPx)
        )

        // Dominant vertical movement locks VERTICAL
        assertEquals(
            SwipeAxis.VERTICAL,
            SwipeGestureMath.decideSwipeAxis(dx = 15f, dy = -80f, touchSlopPx = slopPx)
        )
        assertEquals(
            SwipeAxis.VERTICAL,
            SwipeGestureMath.decideSwipeAxis(dx = -25f, dy = 90f, touchSlopPx = slopPx)
        )
    }

    @Test
    fun decideVerticalSide_usesTouchDownHalfWithoutMiddleDeadZone() {
        val width = 1080f
        assertEquals(VerticalSwipeSide.LEFT_BRIGHTNESS, SwipeGestureMath.decideVerticalSide(0f, width))
        assertEquals(VerticalSwipeSide.LEFT_BRIGHTNESS, SwipeGestureMath.decideVerticalSide(450f, width))
        assertEquals(VerticalSwipeSide.LEFT_BRIGHTNESS, SwipeGestureMath.decideVerticalSide(539f, width))
        assertEquals(VerticalSwipeSide.RIGHT_VOLUME, SwipeGestureMath.decideVerticalSide(540f, width))
        assertEquals(VerticalSwipeSide.RIGHT_VOLUME, SwipeGestureMath.decideVerticalSide(600f, width))
        assertEquals(VerticalSwipeSide.RIGHT_VOLUME, SwipeGestureMath.decideVerticalSide(1080f, width))
    }

    @Test
    fun brightnessMapping_upDragIncreasesAndClampsAtOne_downDragClampsAt002() {
        // Window unset (< 0) resolves cleanly to fallback without Settings.System
        assertEquals(0.5f, SwipeGestureMath.resolveStartBrightness(-1f, 0.5f), 0.0001f)
        assertEquals(0.75f, SwipeGestureMath.resolveStartBrightness(0.75f, 0.5f), 0.0001f)
        assertEquals(0.02f, SwipeGestureMath.resolveStartBrightness(0.0f, 0.5f), 0.0001f)

        val height = 1000f
        // Upward drag (dragUpPx > 0) increases brightness
        val raised = SwipeGestureMath.mapBrightness(
            startBrightness = 0.4f,
            dragUpPx = 300f,
            screenHeightPx = height
        )
        assertTrue(raised > 0.4f)
        assertEquals(0.7f, raised, 0.0001f)

        // Upward drag past full range clamps cleanly at 1.0
        val clampedHigh = SwipeGestureMath.mapBrightness(
            startBrightness = 0.5f,
            dragUpPx = 900f,
            screenHeightPx = height
        )
        assertEquals(1.0f, clampedHigh, 0.0001f)

        // Downward drag (dragUpPx < 0) decreases brightness and clamps at 0.02
        val lowered = SwipeGestureMath.mapBrightness(
            startBrightness = 0.5f,
            dragUpPx = -250f,
            screenHeightPx = height
        )
        assertEquals(0.25f, lowered, 0.0001f)

        val clampedLow = SwipeGestureMath.mapBrightness(
            startBrightness = 0.3f,
            dragUpPx = -900f,
            screenHeightPx = height
        )
        assertEquals(0.02f, clampedLow, 0.0001f)
    }

    @Test
    fun volumeMapping_hitsZeroAndMaxAtExtremesAndRoundsSteps() {
        val height = 1000f
        val maxVol = 15

        // Zero drag preserves start volume
        val unchanged = SwipeGestureMath.mapVolume(
            startVolume = 7,
            maxVolume = maxVol,
            dragUpPx = 0f,
            screenHeightPx = height
        )
        assertEquals(7, unchanged.step)
        assertEquals(7f / 15f, unchanged.fraction, 0.0001f)

        // Full-height upward drag from 0 hits maxVolume and fraction 1.0
        val atMax = SwipeGestureMath.mapVolume(
            startVolume = 0,
            maxVolume = maxVol,
            dragUpPx = height,
            screenHeightPx = height
        )
        assertEquals(15, atMax.step)
        assertEquals(1.0f, atMax.fraction, 0.0001f)

        // Full-height downward drag from max hits 0 and fraction 0.0
        val atZero = SwipeGestureMath.mapVolume(
            startVolume = 15,
            maxVolume = maxVol,
            dragUpPx = -height,
            screenHeightPx = height
        )
        assertEquals(0, atZero.step)
        assertEquals(0.0f, atZero.fraction, 0.0001f)

        // Intermediate fractional drag rounds cleanly to nearest whole step
        val rounded = SwipeGestureMath.mapVolume(
            startVolume = 5,
            maxVolume = 10,
            dragUpPx = 260f, // +2.6 steps -> 7.6 -> rounds to 8
            screenHeightPx = height
        )
        assertEquals(8, rounded.step)
        assertEquals(0.8f, rounded.fraction, 0.0001f)
    }

    @Test
    fun seekMapping_mapsOverFullDurationAndNeverExceedsBounds() {
        val width = 1000f
        val duration = 120_000L // 2 minutes

        // Quarter screen right from 30s -> +30s = 60s
        val forward = SwipeGestureMath.mapSeekTarget(
            startPositionMs = 30_000L,
            durationMs = duration,
            dragDxPx = 250f,
            screenWidthPx = width
        )
        assertEquals(60_000L, forward.targetPositionMs)
        assertEquals(30_000L, forward.deltaMs)

        // Dragging well past right edge never exceeds durationMs
        val clampedEnd = SwipeGestureMath.mapSeekTarget(
            startPositionMs = 90_000L,
            durationMs = duration,
            dragDxPx = 800f,
            screenWidthPx = width
        )
        assertEquals(duration, clampedEnd.targetPositionMs)
        assertEquals(30_000L, clampedEnd.deltaMs)

        // Dragging past left edge clamps at 0
        val clampedStart = SwipeGestureMath.mapSeekTarget(
            startPositionMs = 20_000L,
            durationMs = duration,
            dragDxPx = -700f,
            screenWidthPx = width
        )
        assertEquals(0L, clampedStart.targetPositionMs)
        assertEquals(-20_000L, clampedStart.deltaMs)

        // Unknown duration (<= 0) returns safe non-negative position with 0 delta
        val unknownDuration = SwipeGestureMath.mapSeekTarget(
            startPositionMs = 10_000L,
            durationMs = 0L,
            dragDxPx = 500f,
            screenWidthPx = width
        )
        assertEquals(10_000L, unknownDuration.targetPositionMs)
        assertEquals(0L, unknownDuration.deltaMs)
    }
}
