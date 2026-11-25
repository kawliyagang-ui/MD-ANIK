package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.CineTheme
import com.example.ui.viewmodel.DownloadSheetUiState

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
    val colors = CineTheme.colors
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = colors.softRose,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sheetState.targetMedia?.title ?: "Download Options",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                        fontSize = 12.sp,
                        color = colors.gold,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            val bodyStage = when {
                sheetState.isLoading -> 0
                sheetState.errorMessage != null && sheetState.bundle == null -> 1
                sheetState.bundle != null -> 2
                else -> 3
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
                        CircularProgressIndicator(color = colors.softRose)
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = colors.softRose,
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
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_sheet_retry_button")
                        ) {
                            Text("Retry", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                        }

                        2 -> {
                    val bundle = sheetState.bundle ?: return@Column
                    val languageGroups = bundle.languageGroups
                    val hasMultipleLanguages = languageGroups.size > 1

                    // Multi-language selector chips (only shown when multiple audio languages exist)
                    if (hasMultipleLanguages) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = colors.softRose,
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
                                FilterChip(
                                    selected = selectedLanguage == "All",
                                    onClick = { selectedLanguage = "All" },
                                    label = { Text("All (${languageGroups.size})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.accentMid,
                                        selectedLabelColor = Color.White,
                                        containerColor = colors.surfaceElevated,
                                        labelColor = colors.textPrimary
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("download_lang_chip_all")
                                )
                                languageGroups.forEach { group ->
                                    FilterChip(
                                        selected = selectedLanguage.equals(group.language, ignoreCase = true),
                                        onClick = { selectedLanguage = group.language },
                                        label = {
                                            Text(
                                                text = "${group.language} (${group.options.size})",
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.accentMid,
                                            selectedLabelColor = Color.White,
                                            containerColor = colors.surfaceElevated,
                                            labelColor = colors.textPrimary
                                        ),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("download_lang_chip_${group.language.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    // Subtitle Picker
                    if (bundle.subtitles.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = colors.gold,
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
                                FilterChip(
                                    selected = selectedSubtitle == null,
                                    onClick = { selectedSubtitle = null },
                                    label = { Text("No Subtitles", fontSize = 11.sp) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                                bundle.subtitles.forEach { sub ->
                                    FilterChip(
                                        selected = selectedSubtitle?.id == sub.id,
                                        onClick = { selectedSubtitle = sub },
                                        label = { Text(sub.languageName, fontSize = 11.sp) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
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

                        visibleGroups.forEach { group ->
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
                                        color = colors.gold
                                    )
                                    Text(
                                        text = "${group.options.size} ${if (group.options.size == 1) "file" else "files"}",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }

                                group.options.forEach { option ->
                                    DownloadQualityOptionRow(
                                        option = option,
                                        languageLabel = group.language,
                                        isRecommended = groupRecommended?.id == option.id,
                                        testTag = "download_quality_option_${group.language.lowercase()}_${option.resolution}",
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

                        bundle.options.forEach { option ->
                            DownloadQualityOptionRow(
                                option = option,
                                languageLabel = null,
                                isRecommended = recommendedOpt?.id == option.id,
                                testTag = "download_quality_option_${option.resolution}",
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
    onClick: () -> Unit
) {
    val colors = CineTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .cinePressScale(interactionSource = interactionSource)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
            .border(
                width = if (isRecommended) 1.5.dp else 1.dp,
                color = if (isRecommended) colors.primary else colors.border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
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
                tint = if (option.resolution >= 1080) colors.gold else colors.softRose,
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
                            color = colors.gold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.gold.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BEST PICK",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.accentMid)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${option.format} • ${option.formattedSize}",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        }

        CineGradientButton(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            minHeight = 36.dp,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download ${option.resolutionLabel}",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Get", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
