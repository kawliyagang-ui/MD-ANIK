package com.example.ui.player

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class SwipeAxis {
    HORIZONTAL,
    VERTICAL
}

enum class VerticalSwipeSide {
    LEFT_BRIGHTNESS,
    RIGHT_VOLUME
}

data class VolumeDragResult(
    val step: Int,
    val fraction: Float
)

data class SeekDragResult(
    val targetPositionMs: Long,
    val deltaMs: Long
)

/**
 * Pure gesture decision and value-mapping helpers for the Media3 direct player surface.
 * Kept framework-free so axis locking, brightness, volume, and seek math can be verified
 * deterministically in JVM unit tests.
 */
object SwipeGestureMath {
    const val DRAG_SLOP_DP = 24f
    const val INDICATOR_HIDE_DELAY_MS = 800L
    const val BRIGHTNESS_MIN = 0.02f
    const val BRIGHTNESS_MAX = 1.0f
    const val DEFAULT_FALLBACK_BRIGHTNESS = 0.5f

    /**
     * Returns null until movement reaches [touchSlopPx], then decides ONCE whether the drag
     * is [SwipeAxis.HORIZONTAL] or [SwipeAxis.VERTICAL].
     */
    fun decideSwipeAxis(
        dx: Float,
        dy: Float,
        touchSlopPx: Float
    ): SwipeAxis? {
        val threshold = touchSlopPx.coerceAtLeast(1f)
        if (hypot(dx, dy) < threshold) return null
        return if (abs(dx) > abs(dy)) SwipeAxis.HORIZONTAL else SwipeAxis.VERTICAL
    }

    /**
     * Left vs. right half decision based strictly on the initial touch-down X coordinate.
     */
    fun decideVerticalSide(
        downX: Float,
        screenWidthPx: Float
    ): VerticalSwipeSide {
        val halfWidth = screenWidthPx.coerceAtLeast(1f) * 0.5f
        return if (downX < halfWidth) {
            VerticalSwipeSide.LEFT_BRIGHTNESS
        } else {
            VerticalSwipeSide.RIGHT_VOLUME
        }
    }

    /**
     * Resolves the starting brightness from [android.view.WindowManager.LayoutParams.screenBrightness].
     * When the window uses system default (< 0), falls back to [fallbackBrightness] without reading
     * Settings.System.
     */
    fun resolveStartBrightness(
        windowBrightness: Float,
        fallbackBrightness: Float = DEFAULT_FALLBACK_BRIGHTNESS
    ): Float {
        return if (windowBrightness in 0f..BRIGHTNESS_MAX) {
            windowBrightness.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
        } else {
            fallbackBrightness.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
        }
    }

    /**
     * Maps vertical upward drag distance ([dragUpPx] > 0 when dragging up, < 0 when dragging down)
     * across the full [screenHeightPx] over the 0..1 brightness range, clamped to 0.02..1.0.
     */
    fun mapBrightness(
        startBrightness: Float,
        dragUpPx: Float,
        screenHeightPx: Float
    ): Float {
        val safeHeight = screenHeightPx.coerceAtLeast(1f)
        val safeStart = startBrightness.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
        val updated = safeStart + (dragUpPx / safeHeight)
        return updated.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
    }

    /**
     * Maps vertical upward drag distance ([dragUpPx] > 0 when dragging up, < 0 when dragging down)
     * across [screenHeightPx] onto discrete AudioManager STREAM_MUSIC steps [0..maxVolume].
     */
    fun mapVolume(
        startVolume: Int,
        maxVolume: Int,
        dragUpPx: Float,
        screenHeightPx: Float
    ): VolumeDragResult {
        val safeMax = maxVolume.coerceAtLeast(1)
        val safeStart = startVolume.coerceIn(0, safeMax)
        val safeHeight = screenHeightPx.coerceAtLeast(1f)
        val dragFraction = dragUpPx / safeHeight
        val rawTarget = safeStart.toFloat() + (dragFraction * safeMax.toFloat())
        val step = rawTarget.roundToInt().coerceIn(0, safeMax)
        val fraction = (step.toFloat() / safeMax.toFloat()).coerceIn(0f, 1f)
        return VolumeDragResult(step = step, fraction = fraction)
    }

    /**
     * Maps horizontal drag fraction ([dragDxPx] / [screenWidthPx]) over the full [durationMs]
     * and clamps the target position cleanly within [0, durationMs].
     */
    fun mapSeekTarget(
        startPositionMs: Long,
        durationMs: Long,
        dragDxPx: Float,
        screenWidthPx: Float
    ): SeekDragResult {
        if (durationMs <= 0L) {
            return SeekDragResult(
                targetPositionMs = startPositionMs.coerceAtLeast(0L),
                deltaMs = 0L
            )
        }
        val safeStart = startPositionMs.coerceIn(0L, durationMs)
        val safeWidth = screenWidthPx.coerceAtLeast(1f)
        val dragFraction = (dragDxPx / safeWidth).toDouble()
        val rawDelta = (dragFraction * durationMs.toDouble()).roundToLongSafe()
        val target = (safeStart + rawDelta).coerceIn(0L, durationMs)
        return SeekDragResult(
            targetPositionMs = target,
            deltaMs = target - safeStart
        )
    }

    private fun Double.roundToLongSafe(): Long {
        return when {
            isNaN() -> 0L
            this >= Long.MAX_VALUE.toDouble() -> Long.MAX_VALUE
            this <= Long.MIN_VALUE.toDouble() -> Long.MIN_VALUE
            else -> kotlin.math.round(this).toLong()
        }
    }
}
