package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MovieBoxDownloadOption
import com.example.data.model.MovieBoxSubject
import com.example.data.model.MovieBoxSubtitleOption
import com.example.data.model.PreferredDownloadQuality
import com.example.data.remote.MovieBoxApiClient
import com.example.ui.theme.CinetraTheme
import com.example.ui.tv.TvDevicePolicy
import com.example.ui.viewmodel.DownloadSheetUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieBoxDownloadSheet(
    sheetState: DownloadSheetUiState,
    preferredQuality: PreferredDownloadQuality,
    onDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onSelectSubject: (MovieBoxSubject) -> Unit = {},
    onDownloadOptionClick: (MovieBoxDownloadOption, MovieBoxSubtitleOption?) -> Unit,
    onRetry: () -> Unit
) {
    if (!sheetState.isOpen) return
    val colors = CinetraTheme.colors
    val context = LocalContext.current
    val isTvDevice = remember(context) { TvDevicePolicy.isTvDevice(context) }
    val retryFocusRequester = remember { FocusRequester() }
    val firstOptionFocusRequester = remember { FocusRequester() }
    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSubtitle by remember(sheetState.bundle?.subject?.subjectId, sheetState.season, sheetState.episode) {
        mutableStateOf(sheetState.bundle?.subtitles?.firstOrNull { it.languageCode.startsWith("en", ignoreCase = true) })
    }
    var selectedLanguage by remember(sheetState.bundle?.subject?.subjectId, sheetState.season, sheetState.episode) {
        mutableStateOf("All")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("download_options_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(colors.primaryContainer)
                        .border(1.dp, colors.primary.copy(alpha = 0.35f), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sheetState.targetMedia?.title ?: "Download Options",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val epLabel = if (sheetState.targetMedia?.mediaType == "tv") {
                        val titlePart = if (sheetState.episodeTitle.isNotBlank()) " • ${sheetState.episodeTitle}" else ""
                        "Season ${sheetState.season}, Episode ${sheetState.episode}$titlePart"
                    } else {
                        "Movie • Offline Download"
                    }
                    Text(
                        text = epLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = colors.border)

            val bodyStage = when {
                sheetState.isLoading -> 0
                sheetState.errorMessage != null && sheetState.bundle == null -> 1
                sheetState.bundle != null -> 2
                else -> 3
            }

            LaunchedEffect(isTvDevice, bodyStage, sheetState.bundle?.subject?.subjectId) {
                if (isTvDevice) {
                    delay(80L)
                    when (bodyStage) {
                        1 -> runCatching { retryFocusRequester.requestFocus() }
                        2 -> runCatching { firstOptionFocusRequester.requestFocus() }
                        else -> Unit
                    }
                }
            }

            AnimatedContent(
                targetState = bodyStage,
                transitionSpec = {
                    fadeIn(tween(durationMillis = 200, easing = CineEase)) togetherWith
                        fadeOut(tween(durationMillis = 150, easing = CineEase))
                },
                label = "downloadSheetBodyTransition"
            ) { stage ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (stage) {
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = colors.primary)
                                Text(
                                    text = "Fetching HD download links...",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Download Unavailable",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.textPrimary
                                    )
                                }
                                Text(
                                    text = sheetState.errorMessage.orEmpty(),
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 18.sp
                                )
                                var retryFocused by remember { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = onRetry,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(
                                        width = if (retryFocused) 2.dp else 1.dp,
                                        color = if (retryFocused) colors.textPrimary else colors.border
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .focusRequester(retryFocusRequester)
                                        .onFocusChanged { retryFocused = it.isFocused }
                                        .testTag("download_sheet_retry_button")
                                ) {
                                    Text("Retry", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                }
                            }
                        }

                        2 -> {
                            val bundle = sheetState.bundle ?: return@Column
                            val languageGroups = bundle.languageGroups
                            val hasMultipleLanguages = languageGroups.size > 1

                            if (hasMultipleLanguages) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Available Languages (${languageGroups.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CategoryPill(
                                            label = "All (${languageGroups.size})",
                                            selected = selectedLanguage == "All",
                                            onClick = { selectedLanguage = "All" },
                                            modifier = Modifier.testTag("download_lang_chip_all")
                                        )
                                        languageGroups.forEach { group ->
                                            CategoryPill(
                                                label = "${group.language} (${group.options.size})",
                                                selected = selectedLanguage.equals(group.language, ignoreCase = true),
                                                onClick = { selectedLanguage = group.language },
                                                modifier = Modifier.testTag("download_lang_chip_${group.language.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }

                            if (bundle.subtitles.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Subtitles,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Include Subtitles",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CategoryPill(
                                            label = "No Subtitles",
                                            selected = selectedSubtitle == null,
                                            onClick = { selectedSubtitle = null }
                                        )
                                        bundle.subtitles.forEach { sub ->
                                            CategoryPill(
                                                label = sub.languageName,
                                                selected = selectedSubtitle?.id == sub.id,
                                                onClick = { selectedSubtitle = sub }
                                            )
                                        }
                                    }
                                }
                            }

                            if (hasMultipleLanguages) {
                                val visibleGroups = if (selectedLanguage == "All") {
                                    languageGroups
                                } else {
                                    languageGroups.filter { it.language.equals(selectedLanguage, ignoreCase = true) }
                                }

                                visibleGroups.forEachIndexed { groupIdx, group ->
                                    val groupRecommended = remember(group.options, preferredQuality) {
                                        MovieBoxApiClient.pickOptionForPreferredQuality(group.options, preferredQuality)
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = "${group.language} Audio",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = "${group.options.size} ${if (group.options.size == 1) "file" else "files"}",
                                                fontSize = 11.sp,
                                                color = colors.textMuted
                                            )
                                        }

                                        group.options.forEachIndexed { optIdx, option ->
                                            DownloadQualityOptionRow(
                                                option = option,
                                                languageLabel = group.language,
                                                isRecommended = groupRecommended?.id == option.id,
                                                testTag = "download_quality_option_${group.language.lowercase()}_${option.resolution}",
                                                focusRequester = if (groupIdx == 0 && optIdx == 0) firstOptionFocusRequester else null,
                                                onClick = { onDownloadOptionClick(option, selectedSubtitle) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                val recommendedOpt = remember(bundle.options, preferredQuality) {
                                    MovieBoxApiClient.pickOptionForPreferredQuality(bundle.options, preferredQuality)
                                }
                                Text(
                                    text = "Available Qualities (${bundle.options.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary
                                )

                                bundle.options.forEachIndexed { optIdx, option ->
                                    DownloadQualityOptionRow(
                                        option = option,
                                        languageLabel = null,
                                        isRecommended = recommendedOpt?.id == option.id,
                                        testTag = "download_quality_option_${option.resolution}",
                                        focusRequester = if (optIdx == 0) firstOptionFocusRequester else null,
                                        onClick = { onDownloadOptionClick(option, selectedSubtitle) }
                                    )
                                }
                            }

                            Text(
                                text = "Saved offline for direct in-app playback",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadQualityOptionRow(
    option: MovieBoxDownloadOption,
    languageLabel: String?,
    isRecommended: Boolean,
    testTag: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val colors = CinetraTheme.colors
    val cardShape = MaterialTheme.shapes.medium // 14.dp
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .cinePressScale(interactionSource = interactionSource)
            .clip(cardShape)
            .background(if (isFocused) colors.surfaceHover else colors.surfaceElevated)
            .border(
                width = when {
                    isFocused -> 2.dp
                    isRecommended -> 1.5.dp
                    else -> 1.dp
                },
                color = when {
                    isFocused -> colors.textPrimary
                    isRecommended -> colors.primary
                    else -> colors.border
                },
                shape = cardShape
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.HighQuality,
                contentDescription = null,
                tint = if (isRecommended) colors.primary else colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = option.resolutionLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    if (!languageLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = languageLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BEST",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${option.format} • ${option.formattedSize}",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }
        }

        RedButton(
            onClick = onClick,
            minHeight = 38.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download ${option.resolutionLabel}",
                tint = colors.onPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Get", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
        }
    }
}
