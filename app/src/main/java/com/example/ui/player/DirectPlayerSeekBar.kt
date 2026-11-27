package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CineEase
import com.example.ui.theme.CineTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Custom Compose seek bar tailored for the Cinetra direct player:
 * - Thin 3.dp track at rest expanding smoothly to 6.dp while scrubbing or focused on TV
 * - Deep red played progress, translucent buffered bar, faint background track
 * - Thumb hidden (0.dp) at rest, smoothly growing to 14.dp with white core while dragging or TV-focused
 * - Floating preview pill above the thumb displaying target timestamp and +/- delta
 */
@Composable
fun DirectPlayerSeekBar(
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    onScrubStart: (Long) -> Unit,
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isTvDevice: Boolean = false
) {
    val colors = CineTheme.colors
    val density = LocalDensity.current

    var isScrubbing by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var scrubOriginMs by remember { mutableLongStateOf(currentPositionMs) }
    var scrubTargetMs by remember { mutableLongStateOf(currentPositionMs) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var previewPillWidthPx by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val effectivePos = if (isScrubbing) scrubTargetMs else currentPositionMs.coerceIn(0L, safeDuration)
    val playedFraction = (effectivePos.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedPositionMs.coerceIn(0L, safeDuration).toFloat() / safeDuration.toFloat())
        .coerceIn(playedFraction, 1f)

    val trackHeight by animateDpAsState(
        targetValue = if (isScrubbing || isFocused) 6.dp else 3.dp,
        animationSpec = tween(durationMillis = 140, easing = CineEase),
        label = "seekTrackHeight"
    )
    val thumbDiameter by animateDpAsState(
        targetValue = if (isScrubbing || isFocused) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 140, easing = CineEase),
        label = "seekThumbSize"
    )

    val deltaMs = scrubTargetMs - scrubOriginMs

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("direct_player_seek_bar")
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Floating preview pill aligned to thumb x-position while scrubbing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isScrubbing && durationMs > 0L,
                    enter = fadeIn(tween(120)),
                    exit = fadeOut(tween(140))
                ) {
                    val thumbX = playedFraction * totalWidthPx
                    val halfPill = previewPillWidthPx / 2f
                    val clampedX = (thumbX - halfPill)
                        .coerceIn(0f, (totalWidthPx - previewPillWidthPx).coerceAtLeast(0f))

                    Row(
                        modifier = Modifier
                            .offset { IntOffset(clampedX.roundToInt(), 0) }
                            .onSizeChanged { previewPillWidthPx = it.width.toFloat() }
                            .background(Color(0xD9121217), RoundedCornerShape(999.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDirectPlayerTime(scrubTargetMs),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (abs(deltaMs) >= 1000L) {
                            Text(
                                text = formatSeekDelta(deltaMs),
                                color = colors.softRose,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Interactive 48.dp touch-target / TV D-pad seek track canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .then(
                        if (isTvDevice) {
                            Modifier
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type != KeyEventType.KeyDown || durationMs <= 0L) {
                                        return@onPreviewKeyEvent false
                                    }
                                    when (keyEvent.key) {
                                        Key.DirectionLeft -> {
                                            val target = (effectivePos - 10_000L).coerceIn(0L, durationMs)
                                            onScrubFinished(target)
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            val target = (effectivePos + 10_000L).coerceIn(0L, durationMs)
                                            onScrubFinished(target)
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                .then(
                                    if (isFocused) {
                                        Modifier
                                            .border(2.dp, Color.White, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 6.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                        } else {
                            Modifier
                        }
                    )
                    .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(durationMs) {
                        detectTapGestures { offset ->
                            if (durationMs <= 0L) return@detectTapGestures
                            val frac = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            val target = (frac * durationMs).toLong().coerceIn(0L, durationMs)
                            onScrubFinished(target)
                        }
                    }
                    .pointerInput(durationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                if (durationMs <= 0L) return@detectHorizontalDragGestures
                                isScrubbing = true
                                scrubOriginMs = currentPositionMs.coerceIn(0L, durationMs)
                                val frac = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                                val target = (frac * durationMs).toLong().coerceIn(0L, durationMs)
                                scrubTargetMs = target
                                onScrubStart(target)
                            },
                            onHorizontalDrag = { change, _ ->
                                if (!isScrubbing || durationMs <= 0L) return@detectHorizontalDragGestures
                                change.consume()
                                val frac = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                                val target = (frac * durationMs).toLong().coerceIn(0L, durationMs)
                                scrubTargetMs = target
                                onScrubChange(target)
                            },
                            onDragEnd = {
                                if (isScrubbing) {
                                    isScrubbing = false
                                    onScrubFinished(scrubTargetMs)
                                }
                            },
                            onDragCancel = {
                                if (isScrubbing) {
                                    isScrubbing = false
                                    onScrubFinished(scrubTargetMs)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    val trackH = trackHeight.toPx()
                    val centerY = size.height / 2f
                    val topY = centerY - (trackH / 2f)
                    val corner = CornerRadius(trackH / 2f, trackH / 2f)

                    // Faint background track
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.18f),
                        topLeft = Offset(0f, topY),
                        size = Size(size.width, trackH),
                        cornerRadius = corner
                    )

                    // Buffered bar
                    if (bufferedFraction > 0f && durationMs > 0L) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.36f),
                            topLeft = Offset(0f, topY),
                            size = Size(size.width * bufferedFraction, trackH),
                            cornerRadius = corner
                        )
                    }

                    // Played bar
                    if (playedFraction > 0f) {
                        drawRoundRect(
                            color = colors.primary,
                            topLeft = Offset(0f, topY),
                            size = Size(size.width * playedFraction, trackH),
                            cornerRadius = corner
                        )
                    }

                    // Circular thumb visible while scrubbing
                    val thumbRadius = (thumbDiameter.toPx() / 2f)
                    if (thumbRadius > 0.5f) {
                        val cx = (size.width * playedFraction).coerceIn(thumbRadius, size.width - thumbRadius)
                        drawCircle(
                            color = colors.primary.copy(alpha = 0.35f),
                            radius = thumbRadius * 1.45f,
                            center = Offset(cx, centerY)
                        )
                        drawCircle(
                            color = colors.primary,
                            radius = thumbRadius,
                            center = Offset(cx, centerY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = thumbRadius * 0.42f,
                            center = Offset(cx, centerY)
                        )
                    }
                }
            }
        }
    }
}

fun formatDirectPlayerTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

fun formatSeekDelta(deltaMs: Long): String {
    val sign = if (deltaMs >= 0L) "+" else "-"
    return "$sign${formatDirectPlayerTime(abs(deltaMs))}"
}
