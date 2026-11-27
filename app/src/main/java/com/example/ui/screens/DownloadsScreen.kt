package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadSeasonGroup
import com.example.data.model.DownloadSeriesGroup
import com.example.data.model.DownloadStatus
import com.example.data.model.MovieBoxSubject
import com.example.data.model.PlaybackSourcePolicy
import com.example.data.model.formatDownloadBytes
import com.example.ui.components.CategoryPill
import com.example.ui.components.CineDialogEntryBox
import com.example.ui.components.CineEase
import com.example.ui.components.CinetraEmptyState
import com.example.ui.components.RedButton
import com.example.ui.components.StylishProgressBar
import com.example.ui.components.cinePressScale
import com.example.ui.components.rememberCineImageRequest
import com.example.ui.components.rememberDarkImagePainter
import com.example.ui.theme.CinetraTheme
import kotlinx.coroutines.delay

@Composable
fun DownloadsScreen(
    downloads: List<DownloadEntity>,
    movieBoxSearchQuery: String,
    movieBoxSearchResults: List<MovieBoxSubject>,
    isSearchingMovieBox: Boolean,
    movieBoxSearchError: String?,
    feedbackMessage: String?,
    movieBoxHostUrl: String,
    onSearchMovieBox: (String) -> Unit,
    onOpenMovieBoxSubject: (MovieBoxSubject, Int, Int) -> Unit,
    onPlayOffline: (DownloadEntity) -> Unit,
    onExportToDevice: (DownloadEntity) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onRetryDownload: (DownloadEntity) -> Unit,
    onCancelOrDelete: (String) -> Unit,
    onRefreshStatus: () -> Unit,
    onDismissFeedback: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CinetraTheme.colors
    val bottomClearance = CinetraTheme.bottomBarClearance

    var selectedFilter by remember { mutableStateOf("All") }
    var searchDraft by remember(movieBoxSearchQuery) { mutableStateOf(movieBoxSearchQuery) }
    var tvEpisodePickerSubject by remember { mutableStateOf<MovieBoxSubject?>(null) }
    var collapsedSeasons by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasFinishedInitialStagger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(260L)
        hasFinishedInitialStagger = true
    }

    val filteredDownloads = remember(downloads, selectedFilter) {
        when (selectedFilter) {
            "Active" -> downloads.filter { it.parsedStatus.isActive }
            "Completed" -> downloads.filter { it.parsedStatus == DownloadStatus.COMPLETED }
            "Failed" -> downloads.filter { it.parsedStatus == DownloadStatus.FAILED }
            else -> downloads
        }
    }

    val grouping = remember(filteredDownloads) {
        PlaybackSourcePolicy.groupDownloadsForDisplay(filteredDownloads)
    }

    val totalOfflineBytes = remember(downloads) {
        downloads.filter { it.parsedStatus == DownloadStatus.COMPLETED }
            .sumOf { maxOf(it.bytesDownloaded, it.totalBytes) }
    }
    val completedCount = remember(downloads) {
        downloads.count { it.parsedStatus == DownloadStatus.COMPLETED }
    }
    val runningCount = remember(downloads) {
        downloads.count { it.parsedStatus == DownloadStatus.RUNNING }
    }
    val queuedOrPausedCount = remember(downloads) {
        downloads.count { it.parsedStatus == DownloadStatus.QUEUED || it.parsedStatus == DownloadStatus.PAUSED }
    }

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("downloads_screen")
    ) {
        // Header Bar
        Surface(
            color = colors.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 22.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("downloads_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Downloads",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        val summary = remember(completedCount, runningCount, queuedOrPausedCount, totalOfflineBytes) {
                            buildString {
                                append("$completedCount completed")
                                if (runningCount > 0) append(" • $runningCount downloading")
                                if (queuedOrPausedCount > 0) append(" • $queuedOrPausedCount queued/paused")
                                if (totalOfflineBytes > 0L) append(" • ${formatDownloadBytes(totalOfflineBytes)}")
                            }
                        }
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onRefreshStatus,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("downloads_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync download queue",
                            tint = colors.textPrimary
                        )
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.6f))
            }
        }

        AnimatedVisibility(
            visible = feedbackMessage != null,
            enter = fadeIn(tween(180, easing = CineEase)) + expandVertically(tween(200, easing = CineEase)),
            exit = fadeOut(tween(140, easing = CineEase)) + shrinkVertically(tween(180, easing = CineEase))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(colors.success.copy(alpha = 0.14f))
                    .border(1.dp, colors.success.copy(alpha = 0.45f), MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feedbackMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismissFeedback, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter Pills
            item(key = "downloads_filter_row", contentType = "filter_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeTotal = runningCount + queuedOrPausedCount
                    listOf("All", "Active", "Completed", "Failed").forEach { tab ->
                        val label = when (tab) {
                            "Active" -> "Active ($activeTotal)"
                            "Completed" -> "Completed ($completedCount)"
                            else -> tab
                        }
                        CategoryPill(
                            label = label,
                            selected = selectedFilter == tab,
                            onClick = { selectedFilter = tab },
                            modifier = Modifier.testTag("downloads_filter_$tab")
                        )
                    }
                }
            }

            if (filteredDownloads.isEmpty()) {
                item(key = "downloads_empty_state", contentType = "empty_state") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                    ) {
                        CinetraEmptyState(
                            icon = Icons.Default.Download,
                            title = if (selectedFilter == "All") {
                                "No Downloads Yet"
                            } else {
                                "No $selectedFilter Downloads"
                            },
                            subtitle = "Tap Download on any movie, full TV season, or episode to watch offline inside Cinetra."
                        )
                    }
                }
            } else {
                // Grouped TV Series -> Seasons
                itemsIndexed(
                    items = grouping.seriesGroups,
                    key = { _, g -> "series_${g.mediaId}" },
                    contentType = { _, _ -> "series_group" }
                ) { idx, seriesGroup ->
                    StaggeredInitialItem(
                        index = idx,
                        skipStagger = hasFinishedInitialStagger
                    ) {
                        SeriesDownloadGroupCard(
                            seriesGroup = seriesGroup,
                            collapsedSeasons = collapsedSeasons,
                            onToggleSeasonCollapse = { key ->
                                collapsedSeasons = if (key in collapsedSeasons) {
                                    collapsedSeasons - key
                                } else {
                                    collapsedSeasons + key
                                }
                            },
                            onPlayOffline = onPlayOffline,
                            onExportToDevice = onExportToDevice,
                            onPauseDownload = onPauseDownload,
                            onResumeDownload = onResumeDownload,
                            onRetryDownload = onRetryDownload,
                            onCancelOrDelete = onCancelOrDelete
                        )
                    }
                }

                // Movie Downloads
                val seriesOffset = grouping.seriesGroups.size
                itemsIndexed(
                    items = grouping.movieDownloads,
                    key = { _, d -> d.id },
                    contentType = { _, _ -> "movie_download" }
                ) { idx, download ->
                    StaggeredInitialItem(
                        index = seriesOffset + idx,
                        skipStagger = hasFinishedInitialStagger
                    ) {
                        DownloadItemCard(
                            download = download,
                            onPlayOffline = { onPlayOffline(download) },
                            onExportToDevice = { onExportToDevice(download) },
                            onPause = { onPauseDownload(download.id) },
                            onResume = { onResumeDownload(download.id) },
                            onRetry = { onRetryDownload(download) },
                            onCancelOrDelete = { onCancelOrDelete(download.id) }
                        )
                    }
                }
            }

            // Catalog Download Discovery Card
            item(key = "catalog_download_search_card", contentType = "catalog_search") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Catalog Download Search",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Search movies & series for offline HD downloads",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedTextField(
                            value = searchDraft,
                            onValueChange = { searchDraft = it },
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = "Search title (e.g. Inception, Breaking Bad)...",
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onSearchMovieBox(searchDraft) },
                                    modifier = Modifier.testTag("catalog_download_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search catalog downloads",
                                        tint = colors.primary
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchMovieBox(searchDraft) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.border,
                                focusedContainerColor = colors.surfaceElevated,
                                unfocusedContainerColor = colors.surfaceElevated,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                cursorColor = colors.primary
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("catalog_download_search_input")
                        )

                        if (isSearchingMovieBox) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = colors.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Searching available downloads...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        } else if (!movieBoxSearchError.isNullOrBlank()) {
                            Text(
                                text = movieBoxSearchError,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary
                            )
                        } else if (movieBoxSearchResults.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(
                                    items = movieBoxSearchResults,
                                    key = { index, item -> "mb_${index}_${item.subjectId}" },
                                    contentType = { _, _ -> "mb_subject" }
                                ) { _, subject ->
                                    MovieBoxResultCard(
                                        subject = subject,
                                        onDownloadClick = {
                                            if (subject.subjectType == 2) {
                                                tvEpisodePickerSubject = subject
                                            } else {
                                                onOpenMovieBoxSubject(subject, 1, 1)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "downloads_bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(bottomClearance + 24.dp))
            }
        }
    }

    tvEpisodePickerSubject?.let { subject ->
        var seasonInput by remember(subject.subjectId) { mutableIntStateOf(1) }
        var episodeInput by remember(subject.subjectId) { mutableIntStateOf(1) }
        AlertDialog(
            onDismissRequest = { tvEpisodePickerSubject = null },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "${subject.title} • Episode",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                CineDialogEntryBox {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Choose Season & Episode to fetch download links:",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = seasonInput.toString(),
                                onValueChange = { seasonInput = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                                label = { Text("Season") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = episodeInput.toString(),
                                onValueChange = { episodeInput = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                                label = { Text("Episode") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = seasonInput
                        val e = episodeInput
                        tvEpisodePickerSubject = null
                        onOpenMovieBoxSubject(subject, s, e)
                    }
                ) {
                    Text("Fetch Links", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tvEpisodePickerSubject = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SeriesDownloadGroupCard(
    seriesGroup: DownloadSeriesGroup,
    collapsedSeasons: Set<String>,
    onToggleSeasonCollapse: (String) -> Unit,
    onPlayOffline: (DownloadEntity) -> Unit,
    onExportToDevice: (DownloadEntity) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onRetryDownload: (DownloadEntity) -> Unit,
    onCancelOrDelete: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        seriesGroup.seasons.forEach { seasonGroup ->
            SeasonDownloadGroupCard(
                seasonGroup = seasonGroup,
                isExpanded = seasonGroup.seriesKey !in collapsedSeasons,
                onToggleExpand = { onToggleSeasonCollapse(seasonGroup.seriesKey) },
                onPlayOffline = onPlayOffline,
                onExportToDevice = onExportToDevice,
                onPauseDownload = onPauseDownload,
                onResumeDownload = onResumeDownload,
                onRetryDownload = onRetryDownload,
                onCancelOrDelete = onCancelOrDelete
            )
        }
    }
}

@Composable
private fun SeasonDownloadGroupCard(
    seasonGroup: DownloadSeasonGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPlayOffline: (DownloadEntity) -> Unit,
    onExportToDevice: (DownloadEntity) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onRetryDownload: (DownloadEntity) -> Unit,
    onCancelOrDelete: (String) -> Unit
) {
    val colors = CinetraTheme.colors
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            .testTag("download_season_group_${seasonGroup.seriesKey}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Season Group Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (seasonGroup.posterUrl.isNotBlank()) {
                        val seasonPosterRequest = rememberCineImageRequest(
                            url = seasonGroup.posterUrl,
                            widthDp = 52.dp,
                            heightDp = 74.dp
                        )
                        val darkPlaceholder = rememberDarkImagePainter(colors.surfaceElevated)
                        AsyncImage(
                            model = seasonPosterRequest,
                            contentDescription = seasonGroup.seriesTitle,
                            placeholder = darkPlaceholder,
                            error = darkPlaceholder,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                val animatedSeasonProgress by animateFloatAsState(
                    targetValue = (seasonGroup.overallProgressPercent / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 220, easing = CineEase),
                    label = "seasonGroupProgress"
                )
                val animatedSeasonPercent by animateIntAsState(
                    targetValue = seasonGroup.overallProgressPercent.coerceIn(0, 100),
                    animationSpec = tween(durationMillis = 200, easing = CineEase),
                    label = "seasonGroupPercent"
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = seasonGroup.seriesTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Season ${seasonGroup.seasonNumber} • ${seasonGroup.completedCount}/${seasonGroup.episodes.size} Completed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    val isGroupComplete = seasonGroup.completedCount == seasonGroup.episodes.size && seasonGroup.episodes.isNotEmpty()
                    StylishProgressBar(
                        progress = animatedSeasonProgress,
                        label = "$animatedSeasonPercent%",
                        fillStartColor = if (isGroupComplete) colors.success else colors.accentEnd,
                        fillEndColor = if (isGroupComplete) colors.success else colors.accentStart,
                        dotColor = if (isGroupComplete) colors.success else com.example.ui.theme.CinetraTokens.ProgressGlowDot,
                        labelColor = if (isGroupComplete) colors.success else colors.accentStart,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (seasonGroup.totalExpectedBytes > 0L) {
                            "${formatDownloadBytes(seasonGroup.totalDownloadedBytes)} / ${formatDownloadBytes(seasonGroup.totalExpectedBytes)}"
                        } else {
                            "${seasonGroup.episodes.size} episode(s)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }

                IconButton(onClick = onToggleExpand, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse season" else "Expand season",
                        tint = colors.textSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(180, easing = CineEase)) + expandVertically(tween(220, easing = CineEase)),
                exit = fadeOut(tween(140, easing = CineEase)) + shrinkVertically(tween(180, easing = CineEase))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = colors.border)
                    seasonGroup.episodes.forEachIndexed { idx, epDownload ->
                        EpisodeDownloadRow(
                            download = epDownload,
                            onPlayOffline = { onPlayOffline(epDownload) },
                            onExportToDevice = { onExportToDevice(epDownload) },
                            onPause = { onPauseDownload(epDownload.id) },
                            onResume = { onResumeDownload(epDownload.id) },
                            onRetry = { onRetryDownload(epDownload) },
                            onCancelOrDelete = { onCancelOrDelete(epDownload.id) }
                        )
                        if (idx < seasonGroup.episodes.lastIndex) {
                            HorizontalDivider(
                                color = colors.border,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeDownloadRow(
    download: DownloadEntity,
    onPlayOffline: () -> Unit,
    onExportToDevice: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancelOrDelete: () -> Unit
) {
    val colors = CinetraTheme.colors
    val status = download.parsedStatus

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("download_card_${download.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E${download.episode}${download.episodeTitle.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sizeText = when {
                    download.totalBytes > 0L -> "${formatDownloadBytes(download.bytesDownloaded)} / ${formatDownloadBytes(download.totalBytes)}"
                    download.bytesDownloaded > 0L -> formatDownloadBytes(download.bytesDownloaded)
                    else -> download.qualityLabel
                }
                Text(
                    text = "${download.format} • $sizeText",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    fadeIn(tween(180, easing = CineEase)) togetherWith
                        fadeOut(tween(140, easing = CineEase))
                },
                label = "epDownloadStatusBadge"
            ) { animatedStatus ->
                val pillColor = when (animatedStatus) {
                    DownloadStatus.COMPLETED -> colors.success
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> colors.primary
                    DownloadStatus.PAUSED -> colors.warning
                    DownloadStatus.RUNNING -> colors.primary
                    DownloadStatus.QUEUED -> colors.textSecondary
                }
                Text(
                    text = animatedStatus.displayLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = pillColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pillColor.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        if (status.isActive) {
            val animatedProgress by animateFloatAsState(
                targetValue = (download.progressPercent / 100f).coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 220, easing = CineEase),
                label = "epDownloadProgress"
            )
            val animatedPercent by animateIntAsState(
                targetValue = download.progressPercent.coerceIn(0, 100),
                animationSpec = tween(durationMillis = 200, easing = CineEase),
                label = "epDownloadPercent"
            )
            if (download.progressPercent > 0) {
                val isPaused = status == DownloadStatus.PAUSED
                StylishProgressBar(
                    progress = animatedProgress,
                    label = "$animatedPercent%",
                    fillStartColor = if (isPaused) colors.warning else colors.accentEnd,
                    fillEndColor = if (isPaused) colors.warning else colors.accentStart,
                    dotColor = if (isPaused) colors.warning else com.example.ui.theme.CinetraTokens.ProgressGlowDot,
                    labelColor = if (isPaused) colors.warning else colors.accentStart,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        color = if (status == DownloadStatus.PAUSED) colors.warning else colors.primary,
                        trackColor = colors.surfaceHover,
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Text(
                        text = "$animatedPercent%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (status == DownloadStatus.PAUSED) colors.warning else colors.accentStart
                    )
                }
            }
        } else if (status == DownloadStatus.FAILED && download.errorReason.isNotBlank()) {
            Text(
                text = download.errorReason,
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        DownloadActionButtonsRow(
            status = status,
            onPlayOffline = onPlayOffline,
            onExportToDevice = onExportToDevice,
            onPause = onPause,
            onResume = onResume,
            onRetry = onRetry,
            onCancelOrDelete = onCancelOrDelete
        )
    }
}

@Composable
private fun MovieBoxResultCard(
    subject: MovieBoxSubject,
    onDownloadClick: () -> Unit
) {
    val colors = CinetraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val darkPlaceholder = rememberDarkImagePainter(colors.background)
    Column(
        modifier = Modifier
            .width(136.dp)
            .cinePressScale(interactionSource = interactionSource)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onDownloadClick
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.background)
        ) {
            if (subject.coverUrl.isNotBlank()) {
                val coverRequest = rememberCineImageRequest(
                    url = subject.coverUrl,
                    widthDp = 120.dp,
                    heightDp = 148.dp
                )
                AsyncImage(
                    model = coverRequest,
                    contentDescription = subject.title,
                    placeholder = darkPlaceholder,
                    error = darkPlaceholder,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = subject.title,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = listOfNotNull(
                subject.mediaTypeLabel.takeIf { it.isNotBlank() },
                subject.releaseYear.takeIf { it.isNotBlank() },
                subject.imdbRating.takeIf { it.isNotBlank() }?.let { "★ $it" }
            ).joinToString(" • ").ifBlank { "HD Ready" },
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        RedButton(
            onClick = onDownloadClick,
            shape = RoundedCornerShape(10.dp),
            fillWidth = true,
            minHeight = 36.dp,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download ${subject.title}",
                tint = colors.onPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Get Links", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
        }
    }
}

@Composable
private fun DownloadItemCard(
    download: DownloadEntity,
    onPlayOffline: () -> Unit,
    onExportToDevice: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancelOrDelete: () -> Unit
) {
    val colors = CinetraTheme.colors
    val status = download.parsedStatus

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            .testTag("download_card_${download.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (download.fullPosterUrl.isNotBlank()) {
                        val posterReq = rememberCineImageRequest(
                            url = download.fullPosterUrl,
                            widthDp = 64.dp,
                            heightDp = 90.dp
                        )
                        val darkPlaceholder = rememberDarkImagePainter(colors.surfaceElevated)
                        AsyncImage(
                            model = posterReq,
                            contentDescription = download.title,
                            placeholder = darkPlaceholder,
                            error = darkPlaceholder,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    if (status == DownloadStatus.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(colors.success.copy(alpha = 0.9f))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = "Downloaded",
                                tint = colors.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = download.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${download.episodeSubtitle} • ${download.format}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedContent(
                            targetState = status,
                            transitionSpec = {
                                fadeIn(tween(180, easing = CineEase)) togetherWith
                                    fadeOut(tween(140, easing = CineEase))
                            },
                            label = "movieDownloadStatusBadge"
                        ) { animatedStatus ->
                            val pillColor = when (animatedStatus) {
                                DownloadStatus.COMPLETED -> colors.success
                                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> colors.primary
                                DownloadStatus.PAUSED -> colors.warning
                                DownloadStatus.RUNNING -> colors.primary
                                DownloadStatus.QUEUED -> colors.textSecondary
                            }
                            Text(
                                text = animatedStatus.displayLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(pillColor.copy(alpha = 0.14f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = download.qualityLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textSecondary
                        )
                        if (download.totalBytes > 0L) {
                            val totalBytesLabel = remember(download.totalBytes) {
                                formatDownloadBytes(download.totalBytes)
                            }
                            Text(
                                text = "• $totalBytesLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }
                    val cleanProvider = remember(download.providerName) {
                        val raw = download.providerName.trim()
                        when {
                            raw.isBlank() ||
                                raw.contains("moviebox", ignoreCase = true) ||
                                raw.contains("aoneroom", ignoreCase = true) ||
                                raw.contains("inmoviebox", ignoreCase = true) -> "Cloud HD"
                            else -> raw
                        }
                    }
                    Text(
                        text = cleanProvider + if (download.subtitleLabel.isNotBlank()) " • Sub: ${download.subtitleLabel}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (status.isActive) {
                val animatedProgress by animateFloatAsState(
                    targetValue = (download.progressPercent / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 220, easing = CineEase),
                    label = "movieDownloadProgress"
                )
                val animatedPercent by animateIntAsState(
                    targetValue = download.progressPercent.coerceIn(0, 100),
                    animationSpec = tween(durationMillis = 200, easing = CineEase),
                    label = "movieDownloadPercent"
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (download.progressPercent > 0) {
                        val isPaused = status == DownloadStatus.PAUSED
                        StylishProgressBar(
                            progress = animatedProgress,
                            label = "$animatedPercent%",
                            fillStartColor = if (isPaused) colors.warning else colors.accentEnd,
                            fillEndColor = if (isPaused) colors.warning else colors.accentStart,
                            dotColor = if (isPaused) colors.warning else com.example.ui.theme.CinetraTokens.ProgressGlowDot,
                            labelColor = if (isPaused) colors.warning else colors.accentStart,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                color = if (status == DownloadStatus.PAUSED) colors.warning else colors.primary,
                                trackColor = colors.surfaceHover,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                            Text(
                                text = "$animatedPercent%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (status == DownloadStatus.PAUSED) colors.warning else colors.accentStart
                            )
                        }
                    }
                    val sizeProg = if (download.totalBytes > 0L) {
                        "${formatDownloadBytes(download.bytesDownloaded)} / ${formatDownloadBytes(download.totalBytes)}"
                    } else if (download.bytesDownloaded > 0L) {
                        formatDownloadBytes(download.bytesDownloaded)
                    } else {
                        download.errorReason.ifBlank {
                            if (status == DownloadStatus.QUEUED) "Waiting in queue..." else "Downloading..."
                        }
                    }
                    Text(text = sizeProg, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                }
            } else if (status == DownloadStatus.FAILED && download.errorReason.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = download.errorReason,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }
            }

            DownloadActionButtonsRow(
                status = status,
                onPlayOffline = onPlayOffline,
                onExportToDevice = onExportToDevice,
                onPause = onPause,
                onResume = onResume,
                onRetry = onRetry,
                onCancelOrDelete = onCancelOrDelete
            )
        }
    }
}

@Composable
private fun StaggeredInitialItem(
    index: Int,
    skipStagger: Boolean,
    content: @Composable () -> Unit
) {
    if (skipStagger || index >= 8) {
        content()
        return
    }
    val progress = remember { Animatable(0f) }
    val offsetPx = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(Unit) {
        delay(index * 25L)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = CineEase)
        )
    }
    Box(
        modifier = Modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            translationY = (1f - p) * offsetPx
        }
    ) {
        content()
    }
}

@Composable
private fun DownloadActionButtonsRow(
    status: DownloadStatus,
    onPlayOffline: () -> Unit,
    onExportToDevice: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancelOrDelete: () -> Unit
) {
    val colors = CinetraTheme.colors
    AnimatedContent(
        targetState = status,
        transitionSpec = {
            fadeIn(tween(180, easing = CineEase)) togetherWith
                fadeOut(tween(140, easing = CineEase))
        },
        label = "downloadActionRowTransition"
    ) { animatedStatus ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (animatedStatus) {
                DownloadStatus.COMPLETED -> {
                    RedButton(
                        onClick = onPlayOffline,
                        shape = MaterialTheme.shapes.small,
                        minHeight = 44.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                    }
                    OutlinedButton(
                        onClick = onExportToDevice,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.textPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderCopy,
                            contentDescription = "Save to Downloads folder",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save to Downloads folder",
                            fontSize = 11.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onCancelOrDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete download",
                            tint = colors.textSecondary
                        )
                    }
                }

                DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                    OutlinedButton(
                        onClick = onPause,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    }
                    OutlinedButton(
                        onClick = onCancelOrDelete,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                    }
                }

                DownloadStatus.PAUSED -> {
                    RedButton(
                        onClick = onResume,
                        shape = MaterialTheme.shapes.small,
                        minHeight = 44.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                    }
                    OutlinedButton(
                        onClick = onCancelOrDelete,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                    }
                }

                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                    OutlinedButton(
                        onClick = onRetry,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    }
                    IconButton(
                        onClick = onCancelOrDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
