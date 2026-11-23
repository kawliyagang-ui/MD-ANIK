package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CineTheme

/**
 * Compact overlay displayed inside the 16:9 video frame when WebTorrent P2P is selected
 * without an active magnet or .torrent link yet.
 */
@Composable
fun WebTorrentIdleStage(
    onLaunchDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0E17),
                        Color(0xFF121826),
                        Color(0xFF080A10)
                    )
                )
            )
            .padding(16.dp)
            .testTag("webtorrent_idle_stage"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.2f))
                    .border(1.dp, colors.softRose.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = colors.softRose,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "WebTorrent P2P Ready",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Paste a magnet:? URI, .torrent URL, or infoHash below to stream P2P.",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            CineGradientButton(
                onClick = onLaunchDemo,
                shape = RoundedCornerShape(8.dp),
                minHeight = 38.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("webtorrent_stage_demo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Stream Open-Source Demo (Sintel)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Interactive WebTorrent P2P deck below the video surface showing live swarm stats,
 * multi-file selector chips, and magnet / .torrent URI launcher.
 */
@Composable
fun WebTorrentControlDeck(
    activeTorrentSource: String,
    telemetry: WebTorrentTelemetry,
    torrentFiles: List<TorrentFileEntry>,
    onStartTorrent: (String) -> Unit,
    onSelectTorrentFile: (Int) -> Unit,
    onClearManualTorrent: () -> Unit,
    canClearManualTorrent: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    var inputUri by remember(activeTorrentSource) { mutableStateOf(activeTorrentSource) }
    var selectedFileIndex by remember(activeTorrentSource, torrentFiles.size) {
        mutableIntStateOf(
            torrentFiles.firstOrNull { it.name == telemetry.activeFileName }?.index
                ?: torrentFiles.firstOrNull()?.index
                ?: 0
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    val hasActiveStream = WebTorrentPolicy.isWebTorrentSource(activeTorrentSource)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("webtorrent_control_deck"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = colors.softRose,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "WebTorrent P2P Engine",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Browser WebRTC streaming • webtorrent.io",
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }

            if (hasActiveStream) {
                val badgeColor = when {
                    telemetry.errorMessage != null -> Color(0xFFEF4444)
                    telemetry.isReady -> colors.success
                    else -> colors.gold
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .border(0.75.dp, badgeColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${telemetry.peers} peers",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
        }

        // Live Swarm Telemetry when streaming
        if (hasActiveStream) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = telemetry.statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (telemetry.errorMessage != null) Color(0xFFEF4444) else colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                LinearProgressIndicator(
                    progress = { (telemetry.progressPercent.coerceIn(0, 100) / 100f) },
                    color = if (telemetry.isReady) colors.success else colors.softRose,
                    trackColor = colors.border,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Download speed",
                            tint = colors.success,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = WebTorrentPolicy.formatSpeed(telemetry.downloadSpeedBytes),
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Upload speed",
                            tint = colors.gold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = WebTorrentPolicy.formatSpeed(telemetry.uploadSpeedBytes),
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "${telemetry.progressPercent}% cached",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.gold
                    )
                }

                if (telemetry.activeFileName.isNotBlank()) {
                    Text(
                        text = "Playing: ${telemetry.activeFileName}",
                        fontSize = 11.sp,
                        color = colors.softRose,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Multi-file selector if torrent contains multiple video files
        if (torrentFiles.size > 1) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Files in Torrent (${torrentFiles.size})",
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
                    torrentFiles.forEach { file ->
                        val isSel = file.index == selectedFileIndex || file.name == telemetry.activeFileName
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedFileIndex = file.index
                                onSelectTorrentFile(file.index)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "${file.name} (${WebTorrentPolicy.formatBytes(file.lengthBytes)})",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentMid,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = colors.surface,
                                labelColor = colors.textPrimary
                            ),
                            modifier = Modifier.testTag("webtorrent_file_chip_${file.index}")
                        )
                    }
                }
            }
        }

        // Magnet / .torrent / InfoHash Input
        OutlinedTextField(
            value = inputUri,
            onValueChange = {
                inputUri = it
                validationError = null
            },
            label = { Text("Magnet URI, .torrent URL, or InfoHash") },
            placeholder = {
                Text(
                    text = "magnet:?xt=urn:btih:... or https://.../video.torrent",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (inputUri.isNotEmpty()) {
                    IconButton(onClick = {
                        inputUri = ""
                        validationError = null
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear torrent input",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    val trimmed = inputUri.trim()
                    if (WebTorrentPolicy.isWebTorrentSource(trimmed)) {
                        validationError = null
                        onStartTorrent(trimmed)
                    } else {
                        validationError = "Enter a valid magnet:? URI, .torrent URL, or 40-char infoHash."
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedLabelColor = colors.primary,
                unfocusedLabelColor = colors.textSecondary,
                cursorColor = colors.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("webtorrent_magnet_input")
        )

        if (validationError != null) {
            Text(
                text = validationError.orEmpty(),
                color = Color(0xFFEF4444),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val trimmed = inputUri.trim()
                    if (WebTorrentPolicy.isWebTorrentSource(trimmed)) {
                        validationError = null
                        onStartTorrent(trimmed)
                    } else {
                        validationError = "Enter a valid magnet:? URI, .torrent URL, or 40-char infoHash."
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("webtorrent_load_stream_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stream P2P", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    inputUri = WebTorrentPolicy.SAMPLE_SINTEL_MAGNET
                    validationError = null
                    onStartTorrent(WebTorrentPolicy.SAMPLE_SINTEL_MAGNET)
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("webtorrent_demo_button")
            ) {
                Text("Sintel Demo", fontSize = 12.sp, color = colors.gold, fontWeight = FontWeight.SemiBold)
            }

            if (canClearManualTorrent) {
                OutlinedButton(
                    onClick = {
                        inputUri = ""
                        validationError = null
                        onClearManualTorrent()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("webtorrent_reset_button")
                ) {
                    Text("Reset", fontSize = 12.sp, color = colors.textSecondary)
                }
            }
        }
    }
}
