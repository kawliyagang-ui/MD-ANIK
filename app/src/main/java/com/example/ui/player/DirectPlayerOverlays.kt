package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.EpisodeInfo
import com.example.ui.components.CineEase
import com.example.ui.components.rememberDarkImagePainter
import com.example.ui.theme.CineTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Top-center "2.0x" glass pill shown ~12.dp below the top edge while long-press boost is active.
 */
@Composable
fun LongPressSpeedPill(
    visible: Boolean,
    speedLabel: String = "2.0x",
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "boostPillPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = CineEase),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boostPulseScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(140, easing = CineEase)),
        exit = fadeOut(tween(140, easing = CineEase)),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 12.dp)
            .testTag("long_press_speed_pill")
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .background(Color(0xB80E0E13), RoundedCornerShape(999.dp))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = speedLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Expanding double-tap seek indicator (left "-10s" or right "+10s") with accumulated delta and timestamp.
 */
@Composable
fun DoubleTapSeekOverlay(
    isForward: Boolean,
    accumulatedSeconds: Int,
    targetPositionMs: Long,
    modifier: Modifier = Modifier
) {
    val alignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
    val gradientBrush = remember(isForward) {
        if (isForward) {
            Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.14f))
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent)
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .fillMaxHeight()
                .background(gradientBrush),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(Color(0x990D0D12), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = if (isForward) "+${accumulatedSeconds}s" else "-${accumulatedSeconds}s",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDirectPlayerTime(targetPositionMs),
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Slim left Brightness or right Volume vertical indicator pill.
 */
@Composable
fun VerticalAdjustmentPill(
    isBrightness: Boolean,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val clamped = fraction.coerceIn(0f, 1f)
    val percent = (clamped * 100f).roundToInt()

    Column(
        modifier = modifier
            .background(Color(0xB80E0E13), RoundedCornerShape(999.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isBrightness) Icons.Default.BrightnessHigh else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (isBrightness) "Brightness" else "Volume",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 96.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(clamped)
                    .clip(CircleShape)
                    .background(if (isBrightness) Color.White else colors.primary)
            )
        }
        Text(
            text = "$percent%",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Center pill shown during horizontal drag gesture across the video surface.
 */
@Composable
fun HorizontalDragSeekPill(
    targetPositionMs: Long,
    deltaMs: Long,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    Column(
        modifier = modifier
            .background(Color(0xCC101015), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = formatDirectPlayerTime(targetPositionMs),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formatSeekDelta(deltaMs),
            color = colors.softRose,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Lock overlay: tapping anywhere reveals a small floating Unlock pill; holding it for ~1s fills
 * the circular progress ring and unlocks playback controls.
 */
@Composable
fun PlayerLockOverlay(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val scope = rememberCoroutineScope()
    var showUnlockButton by remember { mutableStateOf(true) }
    var showHoldHint by remember { mutableStateOf(false) }
    var holdInteractionTick by remember { mutableStateOf(0) }
    val ringProgress = remember { Animatable(0f) }

    LaunchedEffect(showUnlockButton, holdInteractionTick) {
        if (showUnlockButton) {
            delay(3000L)
            if (ringProgress.value == 0f) {
                showUnlockButton = false
            }
        }
    }

    LaunchedEffect(showHoldHint) {
        if (showHoldHint) {
            delay(1800L)
            showHoldHint = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    showUnlockButton = !showUnlockButton
                    holdInteractionTick++
                }
            }
            .testTag("direct_player_lock_overlay")
    ) {
        AnimatedVisibility(
            visible = showUnlockButton,
            enter = fadeIn(tween(180, easing = CineEase)),
            exit = fadeOut(tween(180, easing = CineEase)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(visible = showHoldHint) {
                    Text(
                        text = "Hold to unlock",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(Color(0xBA0E0E13), RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xC2101015))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    holdInteractionTick++
                                    showHoldHint = true
                                },
                                onPress = {
                                    holdInteractionTick++
                                    val job = scope.launch {
                                        ringProgress.snapTo(0f)
                                        ringProgress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(durationMillis = 950, easing = CineEase)
                                        )
                                        onUnlock()
                                    }
                                    val released = tryAwaitRelease()
                                    if (!released || ringProgress.value < 0.99f) {
                                        job.cancel()
                                        scope.launch {
                                            ringProgress.animateTo(0f, tween(150))
                                        }
                                    }
                                }
                            )
                        }
                        .testTag("direct_player_unlock_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 2.5.dp.toPx()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.22f),
                            style = Stroke(width = strokePx)
                        )
                        if (ringProgress.value > 0f) {
                            drawArc(
                                color = colors.primary,
                                startAngle = -90f,
                                sweepAngle = 360f * ringProgress.value,
                                useCenter = false,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Hold to unlock controls",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Designed buffering overlay replacing the stock Material spinner.
 */
@Composable
fun DirectPlayerBufferingOverlay(
    bufferedPercent: Int,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    Row(
        modifier = modifier
            .background(Color(0xBA0E0E13), RoundedCornerShape(999.dp))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("direct_player_buffering"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
            CircularProgressIndicator(
                color = colors.primary,
                trackColor = Color.White.copy(alpha = 0.18f),
                strokeWidth = 2.5.dp,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = if (bufferedPercent in 1..99) "Buffering • $bufferedPercent%" else "Buffering…",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Clean dark rounded error card with Retry (re-prepares the same URL) and optional Embed fallback.
 */
@Composable
fun DirectPlayerErrorCard(
    message: String,
    canFallbackToEmbed: Boolean,
    onRetry: () -> Unit,
    onFallbackToWebEmbed: (() -> Unit)?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    Surface(
        color = Color(0xF015151B),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .widthIn(max = 360.dp)
            .padding(24.dp)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .testTag("direct_player_error_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.primary.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Playback Interrupted",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.74f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("direct_player_retry_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (canFallbackToEmbed && onFallbackToWebEmbed != null) {
                    OutlinedButton(
                        onClick = onFallbackToWebEmbed,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Web Embed", color = colors.gold, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Go Back", color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

/**
 * Compact "Up Next" series episode card displayed in the last 10 seconds or when playback ends.
 */
@Composable
fun SeriesUpNextCard(
    season: Int,
    nextEpisode: EpisodeInfo,
    fallbackThumbUrl: String,
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val thumbUrl = nextEpisode.fullStillUrl.ifBlank { fallbackThumbUrl }
    val darkPainter = rememberDarkImagePainter()

    Surface(
        color = Color(0xE614141A),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .widthIn(max = 290.dp)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .testTag("direct_player_up_next_card")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = nextEpisode.name,
                    placeholder = darkPainter,
                    error = darkPainter,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 76.dp, height = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF202028))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UP NEXT • S$season · E${nextEpisode.episodeNumber}",
                        color = colors.softRose,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = nextEpisode.name.ifBlank { "Episode ${nextEpisode.episodeNumber}" },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Up Next",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onPlayNow,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("direct_player_up_next_play_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
