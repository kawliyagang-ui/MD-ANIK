package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Tracks
import com.example.ui.components.CineEase
import com.example.ui.theme.CineTheme
import kotlin.math.abs

enum class DirectPlayerActiveSheet {
    NONE,
    SPEED,
    SUBTITLES,
    AUDIO,
    QUALITY,
    ASPECT_RATIO
}

data class DirectTrackChoice(
    val trackType: Int,
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

@Composable
fun DirectPlayerSheetsHost(
    activeSheet: DirectPlayerActiveSheet,
    currentBaseSpeed: Float,
    currentAspectRatio: DirectAspectRatio,
    videoTrackChoices: List<DirectTrackChoice>,
    isAutoVideoSelected: Boolean,
    audioTrackChoices: List<DirectTrackChoice>,
    subtitleTrackChoices: List<DirectTrackChoice>,
    areSubtitlesDisabled: Boolean,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectAspectRatio: (DirectAspectRatio) -> Unit,
    onSelectAutoVideo: () -> Unit,
    onSelectVideoTrack: (DirectTrackChoice) -> Unit,
    onSelectAudioTrack: (DirectTrackChoice) -> Unit,
    onDisableSubtitles: () -> Unit,
    onSelectSubtitleTrack: (DirectTrackChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = activeSheet != DirectPlayerActiveSheet.NONE
    val firstOptionFocusRequester = remember { FocusRequester() }
    LaunchedEffect(activeSheet) {
        if (activeSheet != DirectPlayerActiveSheet.NONE) {
            runCatching { firstOptionFocusRequester.requestFocus() }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180, easing = CineEase)),
            exit = fadeOut(tween(160, easing = CineEase))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = tween(220, easing = CineEase),
                initialOffsetY = { it / 2 }
            ) + fadeIn(tween(180, easing = CineEase)),
            exit = slideOutVertically(
                animationSpec = tween(180, easing = CineEase),
                targetOffsetY = { it / 2 }
            ) + fadeOut(tween(150, easing = CineEase)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            when (activeSheet) {
                DirectPlayerActiveSheet.SPEED -> {
                    DirectPlayerSheetContainer(title = "Playback Speed") {
                        LongPressSpeedController.SPEED_PRESETS.forEachIndexed { idx, speed ->
                            val selected = abs(currentBaseSpeed - speed) < 0.01f
                            val label = if (abs(speed - 1.0f) < 0.01f) {
                                "1.0x (Normal)"
                            } else {
                                "${speed}x"
                            }
                            DirectSheetOptionRow(
                                label = label,
                                selected = selected,
                                testTag = "sheet_speed_${speed}",
                                focusRequester = if (idx == 0) firstOptionFocusRequester else null,
                                onClick = {
                                    onSelectSpeed(speed)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                DirectPlayerActiveSheet.SUBTITLES -> {
                    DirectPlayerSheetContainer(title = "Subtitles (CC)") {
                        DirectSheetOptionRow(
                            label = "Off",
                            selected = areSubtitlesDisabled || subtitleTrackChoices.none { it.isSelected },
                            testTag = "sheet_subtitle_off",
                            focusRequester = firstOptionFocusRequester,
                            onClick = {
                                onDisableSubtitles()
                                onDismiss()
                            }
                        )
                        if (subtitleTrackChoices.isEmpty()) {
                            Text(
                                text = "No embedded subtitle tracks in this stream.",
                                color = Color.White.copy(alpha = 0.56f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        } else {
                            subtitleTrackChoices.forEachIndexed { idx, choice ->
                                DirectSheetOptionRow(
                                    label = choice.label,
                                    selected = !areSubtitlesDisabled && choice.isSelected,
                                    testTag = "sheet_subtitle_$idx",
                                    onClick = {
                                        onSelectSubtitleTrack(choice)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }

                DirectPlayerActiveSheet.AUDIO -> {
                    DirectPlayerSheetContainer(title = "Audio Track") {
                        if (audioTrackChoices.isEmpty()) {
                            DirectSheetOptionRow(
                                label = "Default Audio",
                                selected = true,
                                testTag = "sheet_audio_default",
                                focusRequester = firstOptionFocusRequester,
                                onClick = onDismiss
                            )
                        } else {
                            audioTrackChoices.forEachIndexed { idx, choice ->
                                DirectSheetOptionRow(
                                    label = choice.label,
                                    selected = choice.isSelected,
                                    testTag = "sheet_audio_$idx",
                                    focusRequester = if (idx == 0) firstOptionFocusRequester else null,
                                    onClick = {
                                        onSelectAudioTrack(choice)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }

                DirectPlayerActiveSheet.QUALITY -> {
                    DirectPlayerSheetContainer(title = "Video Quality") {
                        DirectSheetOptionRow(
                            label = "Auto (Adaptive)",
                            selected = isAutoVideoSelected,
                            testTag = "sheet_quality_auto",
                            focusRequester = firstOptionFocusRequester,
                            onClick = {
                                onSelectAutoVideo()
                                onDismiss()
                            }
                        )
                        videoTrackChoices.forEachIndexed { idx, choice ->
                            DirectSheetOptionRow(
                                label = choice.label,
                                selected = !isAutoVideoSelected && choice.isSelected,
                                testTag = "sheet_quality_$idx",
                                onClick = {
                                    onSelectVideoTrack(choice)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                DirectPlayerActiveSheet.ASPECT_RATIO -> {
                    DirectPlayerSheetContainer(title = "Aspect Ratio") {
                        DirectAspectRatio.entries.forEachIndexed { idx, ratio ->
                            DirectSheetOptionRow(
                                label = ratio.label,
                                selected = ratio == currentAspectRatio,
                                testTag = "sheet_aspect_${ratio.name}",
                                focusRequester = if (idx == 0) firstOptionFocusRequester else null,
                                onClick = {
                                    onSelectAspectRatio(ratio)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                DirectPlayerActiveSheet.NONE -> Unit
            }
        }
    }
}

@Composable
private fun DirectPlayerSheetContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = Color(0xFF141419),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier
            .widthIn(max = 540.dp)
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.24f))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(4.dp))
            content()
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DirectSheetOptionRow(
    label: String,
    selected: Boolean,
    testTag: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val colors = CineTheme.colors
    val interaction = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isFocused -> colors.primary.copy(alpha = 0.28f)
                    selected -> colors.primary.copy(alpha = 0.14f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isFocused) {
                    Modifier.border(2.dp, Color.White, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(
                        width = if (selected) 5.dp else 1.5.dp,
                        color = if (selected) colors.primary else Color.White.copy(alpha = 0.36f),
                        shape = CircleShape
                    )
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = if (selected || isFocused) Color.White else Color.White.copy(alpha = 0.84f),
                fontWeight = if (selected || isFocused) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = colors.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
