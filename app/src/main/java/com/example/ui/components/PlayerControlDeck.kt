package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CineTheme

/**
 * Custom playback control bar rendered beneath the 16:9 video player in normal mode.
 * Provides -10s / +10s seek, play/pause, device-synced volume slider & mute, and full-screen toggle.
 */
@Composable
fun PlayerControlDeck(
    isPlaying: Boolean,
    isFullScreen: Boolean,
    volumeFraction: Float,
    isMuted: Boolean,
    onSeekBackward: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(colors.surfaceElevated, colors.surface)
                ),
                shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
            )
            .border(
                width = 0.75.dp,
                color = colors.border.copy(alpha = 0.65f),
                shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
            )
            .testTag("player_control_deck")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Transport & Full-Screen Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Seek Backward 10s
                    SeekControlButton(
                        isForward = false,
                        onClick = onSeekBackward,
                        modifier = Modifier.testTag("player_seek_backward_button")
                    )

                    // Play / Pause CTA
                    CineGradientButton(
                        onClick = onTogglePlayPause,
                        shape = RoundedCornerShape(10.dp),
                        minHeight = 48.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(
                                id = if (isPlaying) R.string.player_pause_desc else R.string.player_play_desc
                            ),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "Pause" else "Play",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Seek Forward 10s
                    SeekControlButton(
                        isForward = true,
                        onClick = onSeekForward,
                        modifier = Modifier.testTag("player_seek_forward_button")
                    )
                }

                // Full-Screen Toggle Button
                FullScreenToggleButton(
                    isFullScreen = isFullScreen,
                    onClick = onToggleFullScreen,
                    modifier = Modifier.testTag("player_fullscreen_toggle_button")
                )
            }

            // Volume Slider Row
            VolumeControlRow(
                volumeFraction = volumeFraction,
                isMuted = isMuted,
                onVolumeChange = onVolumeChange,
                onToggleMute = onToggleMute,
                showSliderTestTags = true
            )
        }
    }
}

/**
 * Floating translucent HUD overlay rendered over the video canvas during Full-Screen mode.
 */
@Composable
fun FullScreenPlayerHud(
    title: String,
    subtitle: String,
    visible: Boolean,
    isPlaying: Boolean,
    volumeFraction: Float,
    isMuted: Boolean,
    onShowHud: () -> Unit,
    onBackOrExitFullScreen: () -> Unit,
    onReload: () -> Unit,
    onSeekBackward: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onExitFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        // Auto-hidden Full-Screen HUD (Top & Bottom floating gradient bars)
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent)
                            )
                        )
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackOrExitFullScreen,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fullscreen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.player_fullscreen_exit_desc),
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.softRose,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onReload,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("fullscreen_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Player",
                            tint = Color.White
                        )
                    }

                    FullScreenToggleButton(
                        isFullScreen = true,
                        onClick = onExitFullScreen,
                        modifier = Modifier.testTag("fullscreen_top_exit_button")
                    )
                }

                // Bottom Control Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                            )
                        )
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SeekControlButton(
                                isForward = false,
                                onClick = onSeekBackward,
                                modifier = Modifier.testTag("player_seek_backward_button")
                            )

                            CineGradientButton(
                                onClick = onTogglePlayPause,
                                shape = RoundedCornerShape(10.dp),
                                minHeight = 48.dp,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("player_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(
                                        id = if (isPlaying) R.string.player_pause_desc else R.string.player_play_desc
                                    ),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPlaying) "Pause" else "Play",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            SeekControlButton(
                                isForward = true,
                                onClick = onSeekForward,
                                modifier = Modifier.testTag("player_seek_forward_button")
                            )
                        }

                        FullScreenToggleButton(
                            isFullScreen = true,
                            onClick = onExitFullScreen,
                            modifier = Modifier.testTag("player_fullscreen_toggle_button")
                        )
                    }

                    VolumeControlRow(
                        volumeFraction = volumeFraction,
                        isMuted = isMuted,
                        onVolumeChange = onVolumeChange,
                        onToggleMute = onToggleMute,
                        showSliderTestTags = true
                    )
                }
            }
        }

        // Compact floating pill when HUD is auto-hidden so user can recall controls without blocking iframe taps
        AnimatedVisibility(
            visible = !visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(14.dp)
        ) {
            Surface(
                onClick = onShowHud,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.68f),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .border(1.dp, colors.softRose.copy(alpha = 0.55f), CircleShape)
                    .testTag("player_fullscreen_show_hud_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(id = R.string.player_show_controls_desc),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Controls",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SeekFeedbackBadge(
    feedbackText: String?,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients

    AnimatedVisibility(
        visible = feedbackText != null,
        enter = fadeIn() + scaleIn(initialScale = 0.86f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(gradients.softBadge)
                .border(1.dp, colors.softRose.copy(alpha = 0.7f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("player_seek_feedback_pill"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = feedbackText.orEmpty(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun SeekControlButton(
    isForward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = if (isForward) "+10s" else "-10s"
    val icon = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10
    val contentDesc = stringResource(
        id = if (isForward) R.string.player_seek_forward_desc else R.string.player_seek_backward_desc
    )

    val colors = CineTheme.colors

    Surface(
        onClick = onClick,
        color = colors.surfaceElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FullScreenToggleButton(
    isFullScreen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors

    Surface(
        onClick = onClick,
        color = colors.surfaceElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .border(
                width = 1.dp,
                color = if (isFullScreen) colors.softRose.copy(alpha = 0.65f) else colors.border,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = stringResource(
                    id = if (isFullScreen) R.string.player_fullscreen_exit_desc else R.string.player_fullscreen_enter_desc
                ),
                tint = if (isFullScreen) colors.gold else colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun VolumeControlRow(
    volumeFraction: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    showSliderTestTags: Boolean
) {
    val colors = CineTheme.colors
    val effectiveVolume = if (isMuted) 0f else volumeFraction.coerceIn(0f, 1f)
    val volumeIcon = when {
        isMuted || effectiveVolume <= 0.005f -> Icons.AutoMirrored.Filled.VolumeOff
        effectiveVolume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }
    val volumeText = PlayerControlsPolicy.volumePercentLabel(effectiveVolume, isMuted)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated.copy(alpha = 0.85f))
            .border(0.75.dp, colors.border.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .then(if (showSliderTestTags) Modifier.testTag("player_mute_button") else Modifier)
        ) {
            Icon(
                imageVector = volumeIcon,
                contentDescription = stringResource(
                    id = if (isMuted || effectiveVolume <= 0.005f) {
                        R.string.player_unmute_desc
                    } else {
                        R.string.player_mute_desc
                    }
                ),
                tint = if (isMuted || effectiveVolume <= 0.005f) colors.softRose else colors.gold,
                modifier = Modifier.size(20.dp)
            )
        }

        Slider(
            value = effectiveVolume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = colors.softRose,
                inactiveTrackColor = colors.border
            ),
            modifier = Modifier
                .weight(1f)
                .then(if (showSliderTestTags) Modifier.testTag("player_volume_slider") else Modifier)
        )

        Text(
            text = volumeText,
            color = if (isMuted || effectiveVolume <= 0.005f) colors.softRose else colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(46.dp)
                .padding(end = 6.dp)
                .then(if (showSliderTestTags) Modifier.testTag("player_volume_label") else Modifier)
        )
    }
}
