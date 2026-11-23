package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MovieBoxSubject
import com.example.data.model.formatDownloadBytes
import com.example.ui.components.CineGradientButton
import com.example.ui.theme.CineTheme

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
    onRetryDownload: (DownloadEntity) -> Unit,
    onCancelOrDelete: (String) -> Unit,
    onRefreshStatus: () -> Unit,
    onDismissFeedback: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients

    var selectedFilter by remember { mutableStateOf("All") }
    var searchDraft by remember(movieBoxSearchQuery) { mutableStateOf(movieBoxSearchQuery) }
    var tvEpisodePickerSubject by remember { mutableStateOf<MovieBoxSubject?>(null) }

    val filteredDownloads = remember(downloads, selectedFilter) {
        when (selectedFilter) {
            "Active" -> downloads.filter { it.parsedStatus.isActive }
            "Completed" -> downloads.filter { it.parsedStatus == DownloadStatus.COMPLETED }
            "Failed" -> downloads.filter { it.parsedStatus == DownloadStatus.FAILED }
            else -> downloads
        }
    }

    val totalOfflineBytes = remember(downloads) {
        downloads.filter { it.parsedStatus == DownloadStatus.COMPLETED }
            .sumOf { maxOf(it.bytesDownloaded, it.totalBytes) }
    }
    val completedCount = remember(downloads) {
        downloads.count { it.parsedStatus == DownloadStatus.COMPLETED }
    }
    val activeCount = remember(downloads) {
        downloads.count { it.parsedStatus.isActive }
    }

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .testTag("downloads_screen")
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("downloads_back_button")
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
                    text = "Downloads & MovieBox",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                val summary = buildString {
                    append("$completedCount offline")
                    if (activeCount > 0) append(" • $activeCount active")
                    if (totalOfflineBytes > 0L) append(" • ${formatDownloadBytes(totalOfflineBytes)}")
                }
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    color = colors.gold,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(
                onClick = onRefreshStatus,
                modifier = Modifier.testTag("downloads_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh download progress",
                    tint = colors.textPrimary
                )
            }
        }

        AnimatedVisibility(visible = feedbackMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.success.copy(alpha = 0.14f))
                    .border(1.dp, colors.success.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feedbackMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismissFeedback, modifier = Modifier.size(28.dp)) {
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Direct MovieBox API Search Card
            item(key = "moviebox_search_card") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = colors.softRose,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MovieBox API Direct Search",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Host: ${movieBoxHostUrl.removePrefix("https://")} • 1080p/720p MP4 + Subtitles",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
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
                                Text("Search MovieBox (e.g. Inception, Breaking Bad)...", fontSize = 12.sp, color = colors.textMuted)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onSearchMovieBox(searchDraft) },
                                    modifier = Modifier.testTag("moviebox_hub_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search MovieBox API",
                                        tint = colors.softRose
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchMovieBox(searchDraft) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("moviebox_hub_search_input")
                        )

                        if (isSearchingMovieBox) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = colors.softRose,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Querying MovieBox mirrors...",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        } else if (!movieBoxSearchError.isNullOrBlank()) {
                            Text(
                                text = movieBoxSearchError,
                                fontSize = 12.sp,
                                color = colors.softRose
                            )
                        } else if (movieBoxSearchResults.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(
                                    items = movieBoxSearchResults,
                                    key = { index, item -> "mb_${index}_${item.subjectId}" }
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

            // Filter Chips
            item(key = "downloads_filter_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Active", "Completed", "Failed").forEach { tab ->
                        val selected = selectedFilter == tab
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = tab },
                            label = {
                                Text(
                                    text = when (tab) {
                                        "Active" -> "Active ($activeCount)"
                                        "Completed" -> "Offline ($completedCount)"
                                        else -> tab
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentMid,
                                selectedLabelColor = Color.White,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textPrimary
                            ),
                            modifier = Modifier.testTag("downloads_filter_$tab")
                        )
                    }
                }
            }

            if (filteredDownloads.isEmpty()) {
                item(key = "downloads_empty_state") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = colors.gold,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = if (selectedFilter == "All") {
                                "No Downloads Yet"
                            } else {
                                "No $selectedFilter Downloads"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Tap Download on any movie or TV episode page, or search MovieBox above to save HD videos for offline playback.",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                items(filteredDownloads, key = { it.id }) { download ->
                    DownloadItemCard(
                        download = download,
                        onPlayOffline = { onPlayOffline(download) },
                        onExportToDevice = { onExportToDevice(download) },
                        onRetry = { onRetryDownload(download) },
                        onCancelOrDelete = { onCancelOrDelete(download.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose Season & Episode to fetch from MovieBox API:",
                        fontSize = 12.sp,
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
                    Text("Fetch Links", color = colors.softRose, fontWeight = FontWeight.Bold)
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
private fun MovieBoxResultCard(
    subject: MovieBoxSubject,
    onDownloadClick: () -> Unit
) {
    val colors = CineTheme.colors
    Column(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onDownloadClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.background)
        ) {
            if (subject.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = subject.coverUrl,
                    contentDescription = subject.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = subject.mediaTypeLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        Text(
            text = subject.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = listOfNotNull(
                subject.releaseYear.takeIf { it.isNotBlank() },
                subject.imdbRating.takeIf { it.isNotBlank() }?.let { "★ $it" }
            ).joinToString(" • ").ifBlank { "MovieBox Ready" },
            fontSize = 10.sp,
            color = colors.textSecondary,
            maxLines = 1
        )
        CineGradientButton(
            onClick = onDownloadClick,
            shape = RoundedCornerShape(8.dp),
            fillWidth = true,
            minHeight = 32.dp,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download ${subject.title}",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Get Links", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun DownloadItemCard(
    download: DownloadEntity,
    onPlayOffline: () -> Unit,
    onExportToDevice: () -> Unit,
    onRetry: () -> Unit,
    onCancelOrDelete: () -> Unit
) {
    val colors = CineTheme.colors
    val status = download.parsedStatus
    val statusColor = when (status) {
        DownloadStatus.COMPLETED -> colors.success
        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> colors.softRose
        DownloadStatus.PAUSED -> colors.gold
        DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> colors.info
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
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
                        .background(colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (download.fullPosterUrl.isNotBlank()) {
                        AsyncImage(
                            model = download.fullPosterUrl,
                            contentDescription = download.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = colors.softRose,
                            modifier = Modifier.size(26.dp)
                        )
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
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = status.displayLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            text = download.qualityLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.gold
                        )
                        if (download.totalBytes > 0L) {
                            Text(
                                text = "• ${formatDownloadBytes(download.totalBytes)}",
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                    Text(
                        text = download.providerName + if (download.subtitleLabel.isNotBlank()) " • Sub: ${download.subtitleLabel}" else "",
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (status.isActive) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (download.progressPercent > 0) {
                        LinearProgressIndicator(
                            progress = { download.progressPercent / 100f },
                            color = colors.softRose,
                            trackColor = colors.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    } else {
                        LinearProgressIndicator(
                            color = colors.softRose,
                            trackColor = colors.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val sizeProg = if (download.totalBytes > 0L) {
                            "${formatDownloadBytes(download.bytesDownloaded)} / ${formatDownloadBytes(download.totalBytes)}"
                        } else if (download.bytesDownloaded > 0L) {
                            formatDownloadBytes(download.bytesDownloaded)
                        } else {
                            download.errorReason.ifBlank { "Starting download..." }
                        }
                        Text(text = sizeProg, fontSize = 11.sp, color = colors.textSecondary)
                        Text(
                            text = "${download.progressPercent}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.softRose
                        )
                    }
                }
            } else if (status == DownloadStatus.FAILED && download.errorReason.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = colors.softRose,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = download.errorReason,
                        fontSize = 11.sp,
                        color = colors.softRose
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (status) {
                    DownloadStatus.COMPLETED -> {
                        CineGradientButton(
                            onClick = onPlayOffline,
                            shape = RoundedCornerShape(8.dp),
                            minHeight = 40.dp,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Offline",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Offline", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onExportToDevice,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderCopy,
                                contentDescription = "Save to Downloads folder",
                                tint = colors.gold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp, color = colors.textPrimary)
                        }
                        IconButton(
                            onClick = onCancelOrDelete,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete download",
                                tint = colors.softRose
                            )
                        }
                    }

                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = colors.gold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Download", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(
                            onClick = onCancelOrDelete,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove failed download",
                                tint = colors.softRose
                            )
                        }
                    }

                    else -> {
                        OutlinedButton(
                            onClick = onCancelOrDelete,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel download",
                                tint = colors.softRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel Download", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.softRose)
                        }
                    }
                }
            }
        }
    }
}
