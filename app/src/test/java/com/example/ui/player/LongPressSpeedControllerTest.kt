package com.example.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LongPressSpeedControllerTest {

    @Test
    fun boost_applies2xSpeedWhenPlayingAndEligible() {
        val applied = mutableListOf<Float>()
        val controller = LongPressSpeedController(
            initialBaseSpeed = 1.0f,
            boostSpeed = 2.0f,
            onApplySpeed = { applied += it }
        )

        val started = controller.onHoldThresholdReached(isPlaying = true)

        assertTrue(started)
        assertTrue(controller.isBoosting)
        assertEquals(2.0f, controller.activeSpeed, 0.001f)
        assertEquals(listOf(2.0f), applied)
    }

    @Test
    fun release_restoresPrevious1xAnd125xSpeed() {
        var currentSpeed = 1.0f
        val controller = LongPressSpeedController(
            initialBaseSpeed = 1.0f,
            onApplySpeed = { currentSpeed = it }
        )

        assertTrue(controller.onHoldThresholdReached(isPlaying = true))
        assertEquals(2.0f, currentSpeed, 0.001f)

        assertTrue(controller.onReleaseOrCancel())
        assertFalse(controller.isBoosting)
        assertEquals(1.0f, currentSpeed, 0.001f)

        controller.setUserSpeed(1.25f)
        assertEquals(1.25f, currentSpeed, 0.001f)

        assertTrue(controller.onHoldThresholdReached(isPlaying = true))
        assertEquals(2.0f, currentSpeed, 0.001f)

        assertTrue(controller.onReleaseOrCancel())
        assertFalse(controller.isBoosting)
        assertEquals(1.25f, currentSpeed, 0.001f)
    }

    @Test
    fun holdWhilePausedBufferingEndedErrorLockedMenuOrScrubbing_isIgnored() {
        val applied = mutableListOf<Float>()
        val controller = LongPressSpeedController(
            initialBaseSpeed = 1.0f,
            onApplySpeed = { applied += it }
        )

        assertFalse(controller.onHoldThresholdReached(isPlaying = false))
        assertFalse(controller.onHoldThresholdReached(isPlaying = true, isBuffering = true))
        assertFalse(controller.onHoldThresholdReached(isPlaying = true, isEnded = true))
        assertFalse(controller.onHoldThresholdReached(isPlaying = true, hasError = true))
        assertFalse(controller.onHoldThresholdReached(isPlaying = true, isLocked = true))
        assertFalse(controller.onHoldThresholdReached(isPlaying = true, hasOpenMenu = true))
        assertFalse(controller.onHoldThresholdReached(isPlaying = true, isScrubbing = true))
        assertFalse(controller.onHoldThresholdReached(isPlaying = true, isInPipMode = true))

        assertFalse(controller.isBoosting)
        assertEquals(1.0f, controller.activeSpeed, 0.001f)
        assertTrue(applied.isEmpty())
    }

    @Test
    fun cancelOrIneligibilityMidBoost_restoresPreviousSpeed() {
        var currentSpeed = 1.5f
        val controller = LongPressSpeedController(
            initialBaseSpeed = 1.5f,
            onApplySpeed = { currentSpeed = it }
        )

        assertTrue(controller.onHoldThresholdReached(isPlaying = true))
        assertEquals(2.0f, currentSpeed, 0.001f)

        // Simulates lock toggled, playback paused, or gesture cancelled mid-hold
        assertTrue(controller.syncEligibility(eligible = false))
        assertFalse(controller.isBoosting)
        assertEquals(1.5f, currentSpeed, 0.001f)

        // Subsequent release after cancel is a clean no-op
        assertFalse(controller.onReleaseOrCancel())
        assertEquals(1.5f, currentSpeed, 0.001f)
    }

    @Test
    fun interpolateSpeed_rampsSmoothlyFromBaseToTarget() {
        assertEquals(1.0f, LongPressSpeedController.interpolateSpeed(1.0f, 2.0f, 0f), 0.001f)
        assertEquals(1.5f, LongPressSpeedController.interpolateSpeed(1.0f, 2.0f, 0.5f), 0.001f)
        assertEquals(2.0f, LongPressSpeedController.interpolateSpeed(1.0f, 2.0f, 1.0f), 0.001f)
        assertEquals(1.25f, LongPressSpeedController.interpolateSpeed(1.25f, 2.0f, -0.5f), 0.001f)
    }
}
