package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.components.CineEase
import com.example.ui.components.cinePressScale
import com.example.ui.theme.CineTheme

/**
 * Clean top bar for the Media3 direct player:
 * Back button, 1-line title, compact episode/year/quality subtitle, and a single "Direct"/"Offline" chip.
 * No poster thumbnail.
 */
@Composable
fun DirectPlayerTopBar(
    media: MediaItem,
    season: Int,
    episode: Int,
    episodeTitle: String,
    qualityLabel: String,
    isLocalFile: Boolean,
    showPortraitPausedDetails: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val year = remember(media.releaseDate) { media.releaseDate.take(4).takeIf { it.length == 4 }.orEmpty() }
    val subtitleText = remember(media.mediaType, season, episode, episodeTitle, year, qualityLabel) {
        if (media.mediaType.equals("tv", ignoreCase = true)) {
            buildList {
                val baseEp = "S$season · E$episode"
                val epLabel = if (episodeTitle.isNotBlank()) "$baseEp — $episodeTitle" else baseEp
                add(epLabel)
                if (qualityLabel.isNotBlank()) add(qualityLabel)
            }.joinToString(" • ")
        } else {
            buildList {
                if (year.isNotBlank()) add(year)
                if (qualityLabel.isNotBlank()) add(qualityLabel)
            }.joinToString(" • ").ifBlank { "Feature Film" }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("direct_player_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleText,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = if (isLocalFile) {
                    Color(0xFF103227).copy(alpha = 0.75f)
                } else {
                    colors.primary.copy(alpha = 0.28f)
                },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = if (isLocalFile) colors.success.copy(alpha = 0.45f) else colors.primary.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(999.dp)
                )
            ) {
                Text(
                    text = if (isLocalFile) "Offline" else "Direct",
                    color = if (isLocalFile) colors.success else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showPortraitPausedDetails && media.overview.isNotBlank(),
            enter = fadeIn(tween(240, easing = CineEase)),
            exit = fadeOut(tween(180, easing = CineEase))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp, end = 12.dp, top = 6.dp)
                    .widthIn(max = 420.dp)
            ) {
                Text(
                    text = media.overview,
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Unobtrusive dark-glass center playback cluster:
 * 40.dp Play/Pause pill, 34.dp -10s / +10s pills, and optional Prev/Next episode pills for TV series.
 */
@Composable
fun DirectPlayerCenterControls(
    isPlaying: Boolean,
    isSeries: Boolean,
    hasPrevEpisode: Boolean,
    hasNextEpisode: Boolean,
    onRewind10: () -> Unit,
    onPlayPause: () -> Unit,
    onForward10: () -> Unit,
    onPrevEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSeries) {
            GlassCenterButton(
                icon = Icons.Default.SkipPrevious,
                contentDescription = "Previous episode",
                visualSizeDp = 34,
                iconSizeDp = 19,
                enabled = hasPrevEpisode,
                testTag = "direct_player_prev_episode",
                onClick = onPrevEpisode
            )
        }

        GlassCenterButton(
            icon = Icons.Default.Replay10,
            contentDescription = "Rewind 10 seconds",
            visualSizeDp = 34,
            iconSizeDp = 19,
            enabled = true,
            testTag = "direct_player_rewind",
            onClick = onRewind10
        )

        GlassCenterButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            visualSizeDp = 40,
            iconSizeDp = 22,
            enabled = true,
            accentBorder = colors.primary.copy(alpha = 0.55f),
            testTag = "direct_player_play_pause",
            onClick = onPlayPause
        )

        GlassCenterButton(
            icon = Icons.Default.Forward10,
            contentDescription = "Forward 10 seconds",
            visualSizeDp = 34,
            iconSizeDp = 19,
            enabled = true,
            testTag = "direct_player_forward",
            onClick = onForward10
        )

        if (isSeries) {
            GlassCenterButton(
                icon = Icons.Default.SkipNext,
                contentDescription = "Next episode",
                visualSizeDp = 34,
                iconSizeDp = 19,
                enabled = hasNextEpisode,
                testTag = "direct_player_next_episode",
                onClick = onNextEpisode
            )
        }
    }
}

@Composable
private fun GlassCenterButton(
    icon: ImageVector,
    contentDescription: String,
    visualSizeDp: Int,
    iconSizeDp: Int,
    enabled: Boolean,
    accentBorder: Color = Color.White.copy(alpha = 0.22f),
    testTag: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .cinePressScale(interaction, enabled = enabled)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false, radius = 24.dp),
                enabled = enabled,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(visualSizeDp.dp)
                .background(
                    color = if (enabled) Color(0x9E0E0E13) else Color(0x520E0E13),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (enabled) accentBorder else Color.White.copy(alpha = 0.08f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.34f),
                modifier = Modifier.size(iconSizeDp.dp)
            )
        }
    }
}

/**
 * Quiet bottom control block:
 * - Current time on left & remaining time ("-MM:SS") on right (11.sp)
 * - Custom DirectPlayerSeekBar
 * - Compact 40.dp icon row (18.dp icons + 9.sp labels) for Speed, CC, Audio, Quality (if multi-HLS),
 *   Aspect Ratio, PiP (if supported), Lock, and Rotate/Fullscreen.
 */
@Composable
fun DirectPlayerBottomBar(
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    currentBaseSpeed: Float,
    aspectRatio: DirectAspectRatio,
    hasMultipleVideoQualities: Boolean,
    hasActiveSubtitles: Boolean,
    isPipSupported: Boolean,
    isFullScreen: Boolean,
    onScrubStart: (Long) -> Unit,
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenAspectRatio: () -> Unit,
    onEnterPip: () -> Unit,
    onLock: () -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingText = remember(currentPositionMs, durationMs) {
        if (durationMs > 0L) {
            val remaining = (durationMs - currentPositionMs).coerceAtLeast(0L)
            "-${formatDirectPlayerTime(remaining)}"
        } else {
            "--:--"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDirectPlayerTime(currentPositionMs),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = remainingText,
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }

        DirectPlayerSeekBar(
            currentPositionMs = currentPositionMs,
            bufferedPositionMs = bufferedPositionMs,
            durationMs = durationMs,
            onScrubStart = onScrubStart,
            onScrubChange = onScrubChange,
            onScrubFinished = onScrubFinished
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomControlAction(
                icon = Icons.Default.Speed,
                label = LongPressSpeedController.formatSpeedLabel(currentBaseSpeed),
                testTag = "direct_player_speed_button",
                onClick = onOpenSpeed
            )
            BottomControlAction(
                icon = Icons.Default.ClosedCaption,
                label = if (hasActiveSubtitles) "CC On" else "CC",
                highlighted = hasActiveSubtitles,
                testTag = "direct_player_cc_button",
                onClick = onOpenSubtitles
            )
            BottomControlAction(
                icon = Icons.Default.GraphicEq,
                label = "Audio",
                testTag = "direct_player_audio_button",
                onClick = onOpenAudio
            )
            if (hasMultipleVideoQualities) {
                BottomControlAction(
                    icon = Icons.Default.HighQuality,
                    label = "Quality",
                    testTag = "direct_player_quality_button",
                    onClick = onOpenQuality
                )
            }
            BottomControlAction(
                icon = Icons.Default.AspectRatio,
                label = aspectRatio.shortLabel,
                testTag = "direct_player_aspect_button",
                onClick = onOpenAspectRatio
            )
            if (isPipSupported) {
                BottomControlAction(
                    icon = Icons.Default.PictureInPictureAlt,
                    label = "PiP",
                    testTag = "direct_player_pip_button",
                    onClick = onEnterPip
                )
            }
            BottomControlAction(
                icon = Icons.Default.Lock,
                label = "Lock",
                testTag = "direct_player_lock_button",
                onClick = onLock
            )
            BottomControlAction(
                icon = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                label = if (isFullScreen) "Portrait" else "Full",
                testTag = "direct_player_fullscreen_button",
                onClick = onToggleFullScreen
            )
        }
    }
}

@Composable
private fun BottomControlAction(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = CineTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .cinePressScale(interaction)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (highlighted) colors.softRose else Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (highlighted) colors.softRose else Color.White.copy(alpha = 0.72f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
