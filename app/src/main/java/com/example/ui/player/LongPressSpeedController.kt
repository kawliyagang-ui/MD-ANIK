package com.example.ui.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import kotlin.math.abs

/**
 * Supported aspect/resize modes for the Media3 direct player.
 */
enum class DirectAspectRatio(val label: String, val shortLabel: String) {
    FIT("Fit to Screen", "Fit"),
    FILL("Stretch to Fill", "Fill"),
    ZOOM("Crop / Zoom", "Zoom"),
    RATIO_16_9("16:9 Widescreen", "16:9"),
    RATIO_4_3("4:3 Classic", "4:3");

    @UnstableApi
    fun toResizeMode(): Int = when (this) {
        FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }
}

/**
 * Coordinates long-press 2.0x speed boost state cleanly and deterministically.
 *
 * Designed as pure Kotlin logic so hold/release/cancel/edge-case transitions can be
 * verified directly in JVM unit tests without mocking Android views or ExoPlayer.
 */
class LongPressSpeedController(
    initialBaseSpeed: Float = 1.0f,
    val boostSpeed: Float = 2.0f,
    private val onApplySpeed: (Float) -> Unit
) {
    var baseSpeed: Float = initialBaseSpeed
        private set

    var activeSpeed: Float = initialBaseSpeed
        private set

    var isBoosting: Boolean = false
        private set

    fun canBoost(
        isPlaying: Boolean,
        isBuffering: Boolean,
        isEnded: Boolean,
        hasError: Boolean,
        isLocked: Boolean,
        hasOpenMenu: Boolean,
        isScrubbing: Boolean,
        isInPipMode: Boolean
    ): Boolean {
        return isPlaying &&
            !isBuffering &&
            !isEnded &&
            !hasError &&
            !isLocked &&
            !hasOpenMenu &&
            !isScrubbing &&
            !isInPipMode
    }

    /**
     * Activates 2.0x boost if playback is eligible and not already boosting.
     * Returns true if boost transitioned from inactive -> active.
     */
    fun onHoldThresholdReached(
        isPlaying: Boolean,
        isBuffering: Boolean = false,
        isEnded: Boolean = false,
        hasError: Boolean = false,
        isLocked: Boolean = false,
        hasOpenMenu: Boolean = false,
        isScrubbing: Boolean = false,
        isInPipMode: Boolean = false
    ): Boolean {
        if (isBoosting) return false
        if (!canBoost(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                isEnded = isEnded,
                hasError = hasError,
                isLocked = isLocked,
                hasOpenMenu = hasOpenMenu,
                isScrubbing = isScrubbing,
                isInPipMode = isInPipMode
            )
        ) {
            return false
        }
        isBoosting = true
        activeSpeed = boostSpeed
        onApplySpeed(boostSpeed)
        return true
    }

    /**
     * Ends an active boost on finger lift or gesture cancel, restoring [baseSpeed].
     */
    fun onReleaseOrCancel(): Boolean {
        if (!isBoosting) return false
        isBoosting = false
        activeSpeed = baseSpeed
        onApplySpeed(baseSpeed)
        return true
    }

    /**
     * Updates the user's chosen speed from the speed selector sheet.
     */
    fun setUserSpeed(speed: Float) {
        baseSpeed = speed
        if (!isBoosting) {
            activeSpeed = speed
            onApplySpeed(speed)
        }
    }

    /**
     * Ensures boost cleanly disengages if playback pauses, ends, locks, or errors mid-hold.
     */
    fun syncEligibility(eligible: Boolean): Boolean {
        if (!eligible && isBoosting) {
            return onReleaseOrCancel()
        }
        return false
    }

    companion object {
        const val HOLD_THRESHOLD_MS = 350L
        const val RAMP_DURATION_MS = 200L
        const val RELEASE_RAMP_DURATION_MS = 140L

        val SPEED_PRESETS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

        fun interpolateSpeed(from: Float, to: Float, fraction: Float): Float {
            val clamped = fraction.coerceIn(0f, 1f)
            return from + (to - from) * clamped
        }

        fun formatSpeedLabel(speed: Float): String {
            return when {
                abs(speed - 1.0f) < 0.01f -> "1.0x"
                abs(speed - 2.0f) < 0.01f -> "2.0x"
                abs(speed - 0.5f) < 0.01f -> "0.5x"
                abs(speed - 1.5f) < 0.01f -> "1.5x"
                else -> "${speed}x"
            }
        }
    }
}
