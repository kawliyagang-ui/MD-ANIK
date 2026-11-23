package com.example.ui.screens

import android.content.Intent
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.data.model.DownloadEntity
import com.example.ui.theme.CineTheme
import java.io.File

@Composable
fun OfflineVideoPlayerScreen(
    download: DownloadEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CineTheme.colors
    var isBuffering by remember(download.id) { mutableStateOf(true) }
    var playbackError by remember(download.id) { mutableStateOf<String?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val targetUri = remember(download.localFilePath, download.localUri) {
        val localFile = download.localFilePath.takeIf { it.isNotBlank() }?.let { File(it) }
        when {
            localFile != null && localFile.exists() -> localFile.toUri()
            download.localUri.isNotBlank() -> download.localUri.toUri()
            else -> download.sourceUrl.toUri()
        }
    }

    BackHandler { onBack() }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
            videoViewRef = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("offline_video_player")
    ) {
        Surface(
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("offline_player_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Offline Playback • ${download.episodeSubtitle} (${download.qualityLabel})",
                        fontSize = 12.sp,
                        color = colors.gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(targetUri, "video/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in external video player",
                        tint = Color.White
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        videoViewRef = this
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        setOnPreparedListener { mp ->
                            isBuffering = false
                            mp.isLooping = false
                            start()
                        }
                        setOnErrorListener { _, what, extra ->
                            isBuffering = false
                            playbackError = "Unable to decode video locally (code $what/$extra). Try opening in an external player."
                            true
                        }
                        setVideoURI(targetUri)
                    }
                },
                update = { view ->
                    videoViewRef = view
                }
            )

            if (isBuffering && playbackError == null) {
                CircularProgressIndicator(color = colors.softRose)
            }

            playbackError?.let { err ->
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = err,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
