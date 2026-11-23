package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
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
    onSearchQueryChange: (String) -> Unit,
    onSelectSubject: (MovieBoxSubject) -> Unit,
    onDownloadOptionClick: (MovieBoxDownloadOption, MovieBoxSubtitleOption?) -> Unit,
    onRetry: () -> Unit
) {
    if (!sheetState.isOpen) return
    val colors = CineTheme.colors
    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var queryDraft by remember(sheetState.targetMedia?.id, sheetState.searchQuery) {
        mutableStateOf(sheetState.searchQuery)
    }
    var selectedSubtitle by remember(sheetState.bundle?.subject?.subjectId, sheetState.season, sheetState.episode) {
        mutableStateOf(sheetState.bundle?.subtitles?.firstOrNull { it.languageCode.startsWith("en", ignoreCase = true) })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        modifier = Modifier.testTag("moviebox_download_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
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
                        text = sheetState.targetMedia?.title ?: "Download via MovieBox",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val epLabel = if (sheetState.targetMedia?.mediaType == "tv") {
                        val titlePart = if (sheetState.episodeTitle.isNotBlank()) " • ${sheetState.episodeTitle}" else ""
                        "Season ${sheetState.season}, Episode ${sheetState.episode}$titlePart"
                    } else {
                        "Movie • Connected to MovieBox API"
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

            // MovieBox Title Match Search
            OutlinedTextField(
                value = queryDraft,
                onValueChange = { queryDraft = it },
                singleLine = true,
                placeholder = { Text("Match title on MovieBox...", fontSize = 13.sp, color = colors.textMuted) },
                trailingIcon = {
                    IconButton(
                        onClick = { onSearchQueryChange(queryDraft) },
                        modifier = Modifier.testTag("moviebox_sheet_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search MovieBox",
                            tint = colors.softRose
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchQueryChange(queryDraft) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Alternate MovieBox Candidates Chips
            if (sheetState.searchCandidates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "MovieBox Matches (${sheetState.searchCandidates.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sheetState.searchCandidates.take(8).forEach { cand ->
                            val isSelected = sheetState.bundle?.subject?.subjectId == cand.subjectId
                            val yearTag = if (cand.releaseYear.isNotBlank()) " (${cand.releaseYear})" else ""
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectSubject(cand) },
                                label = {
                                    Text(
                                        text = "${cand.title}$yearTag",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentMid,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textPrimary
                                )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            when {
                sheetState.isLoading || sheetState.isSearchingCandidates -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = colors.softRose)
                        Text(
                            text = if (sheetState.isSearchingCandidates) {
                                "Searching MovieBox catalog..."
                            } else {
                                "Fetching HD download links from MovieBox API..."
                            },
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                sheetState.errorMessage != null && sheetState.bundle == null -> {
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
                                text = "MovieBox Source Unavailable",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.textPrimary
                            )
                        }
                        Text(
                            text = sheetState.errorMessage,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onRetry,
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Retry MovieBox", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { onSearchQueryChange(queryDraft) },
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Search Title", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                sheetState.bundle != null -> {
                    val bundle = sheetState.bundle
                    val recommendedOpt = remember(bundle.options, preferredQuality) {
                        MovieBoxApiClient.pickOptionForPreferredQuality(bundle.options, preferredQuality)
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
                                    label = { Text("No Subtitles", fontSize = 11.sp) }
                                )
                                bundle.subtitles.forEach { sub ->
                                    FilterChip(
                                        selected = selectedSubtitle?.id == sub.id,
                                        onClick = { selectedSubtitle = sub },
                                        label = { Text(sub.languageName, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Available Resolutions (${bundle.options.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )

                    bundle.options.forEach { option ->
                        val isRecommended = recommendedOpt?.id == option.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceElevated)
                                .border(
                                    width = if (isRecommended) 1.5.dp else 1.dp,
                                    color = if (isRecommended) colors.primary else colors.border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onDownloadOptionClick(option, selectedSubtitle) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("moviebox_quality_option_${option.resolution}"),
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
                                        if (isRecommended) {
                                            Spacer(modifier = Modifier.width(8.dp))
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
                                onClick = { onDownloadOptionClick(option, selectedSubtitle) },
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

                    Text(
                        text = "Source: ${bundle.sourceHost.removePrefix("https://")} • Saved offline in app storage",
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}
