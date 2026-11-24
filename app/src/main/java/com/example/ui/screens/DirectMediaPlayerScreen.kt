package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import com.example.data.model.DownloadEntity
import com.example.data.model.MediaItem
import com.example.data.model.MovieBoxDownloadOption
import com.example.data.model.PlaybackSourcePolicy
import com.example.ui.components.WebViewFullscreenHost
import com.example.ui.theme.CineTheme
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import androidx.media3.common.MediaItem as Media3Item

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

data class DirectTrackChoice(
    val trackType: Int,
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
@Composable
fun DirectMediaPlayerScreen(
    media: MediaItem,
    season: Int,
    episode: Int,
    sourceUrl: String,
    isLocalFile: Boolean,
    downloadEntity: DownloadEntity? = null,
    initialPositionMs: Long = 0L,
    onSavePosition: (positionMs: Long, durationMs: Long) -> Unit,
    onFallbackToWebEmbed: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hostView = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = CineTheme.colors
    val activity = remember(context) { context.findActivity() }
    val latestSavePosition by rememberUpdatedState(onSavePosition)

    val initialOrientation = remember(activity) {
        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    val initialCutoutMode = remember(activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity?.window?.attributes?.layoutInDisplayCutoutMode
        } else {
            null
        }
    }

    var isFullScreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var controlInteractionTick by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember(sourceUrl) { mutableStateOf<String?>(null) }
    var currentPositionMs by remember { mutableLongStateOf(initialPositionMs.coerceAtLeast(0L)) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var sliderSeekMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }
    var videoTrackChoices by remember { mutableStateOf<List<DirectTrackChoice>>(emptyList()) }
    var audioTrackChoices by remember { mutableStateOf<List<DirectTrackChoice>>(emptyList()) }
    var subtitleTrackChoices by remember { mutableStateOf<List<DirectTrackChoice>>(emptyList()) }
    var hasAppliedInitialSeek by remember(sourceUrl) { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    val resolvedUri = remember(sourceUrl, isLocalFile) {
        when {
            isLocalFile && !sourceUrl.startsWith("file://") && !sourceUrl.startsWith("content://") ->
                Uri.fromFile(File(sourceUrl))
            else -> Uri.parse(sourceUrl)
        }
    }
    val isHls = remember(sourceUrl) { PlaybackSourcePolicy.isHlsStreamUrl(sourceUrl) }

    val exoPlayer = remember(sourceUrl, reloadKey) {
        val headers = buildMap<String, String> {
            downloadEntity?.refererHeader?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
            val cookie = downloadEntity?.cookieHeader?.takeIf { it.isNotBlank() }
            if (cookie != null) put("Cookie", cookie)
        }
        val ua = downloadEntity?.userAgentHeader?.takeIf { it.isNotBlank() }
            ?: MovieBoxDownloadOption.DEFAULT_USER_AGENT

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(30_000)
            .apply {
                if (headers.isNotEmpty()) {
                    setDefaultRequestProperties(headers)
                }
            }

        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                val itemBuilder = Media3Item.Builder().setUri(resolvedUri)
                if (isHls) {
                    itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                setMediaItem(itemBuilder.build())
                playWhenReady = true
                prepare()
            }
    }

    val mediaSession = remember(exoPlayer) {
        runCatching {
            MediaSession.Builder(context, exoPlayer)
                .setId("cinetra_direct_${media.id}_${System.currentTimeMillis()}")
                .build()
        }.getOrNull()
    }

    LaunchedEffect(isFullScreen) {
        WebViewFullscreenHost.applyFullScreenWindowState(
            activity = activity,
            hostView = hostView,
            isFullScreen = isFullScreen,
            originalOrientation = initialOrientation,
            originalCutoutMode = initialCutoutMode
        )
    }

    DisposableEffect(exoPlayer, mediaSession) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    durationMs = dur
                    if (!hasAppliedInitialSeek && initialPositionMs > 2_000L && (dur <= 0L || initialPositionMs < dur - 5_000L)) {
                        hasAppliedInitialSeek = true
                        exoPlayer.seekTo(initialPositionMs)
                        currentPositionMs = initialPositionMs
                    } else {
                        hasAppliedInitialSeek = true
                    }
                }
                if (playbackState == Player.STATE_ENDED) {
                    latestSavePosition(0L, exoPlayer.duration.coerceAtLeast(0L))
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    if (dur > 0L) latestSavePosition(pos, dur)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val vids = mutableListOf<DirectTrackChoice>()
                val auds = mutableListOf<DirectTrackChoice>()
                val subs = mutableListOf<DirectTrackChoice>()

                tracks.groups.forEach { group ->
                    when (group.type) {
                        C.TRACK_TYPE_VIDEO -> {
                            for (i in 0 until group.length) {
                                if (!group.isTrackSupported(i)) continue
                                val fmt = group.getTrackFormat(i)
                                val resLabel = if (fmt.height > 0) "${fmt.height}p" else "Video ${i + 1}"
                                val bitrateLabel = if (fmt.bitrate > 0) " • ${fmt.bitrate / 1000} kbps" else ""
                                vids += DirectTrackChoice(
                                    trackType = C.TRACK_TYPE_VIDEO,
                                    group = group,
                                    trackIndex = i,
                                    label = "$resLabel$bitrateLabel",
                                    isSelected = group.isTrackSelected(i)
                                )
                            }
                        }
                        C.TRACK_TYPE_AUDIO -> {
                            for (i in 0 until group.length) {
                                if (!group.isTrackSupported(i)) continue
                                val fmt = group.getTrackFormat(i)
                                val lang = fmt.language?.uppercase(Locale.US) ?: "Default"
                                val lbl = fmt.label ?: lang
                                auds += DirectTrackChoice(
                                    trackType = C.TRACK_TYPE_AUDIO,
                                    group = group,
                                    trackIndex = i,
                                    label = lbl,
                                    isSelected = group.isTrackSelected(i)
                                )
                            }
                        }
                        C.TRACK_TYPE_TEXT -> {
                            for (i in 0 until group.length) {
                                if (!group.isTrackSupported(i)) continue
                                val fmt = group.getTrackFormat(i)
                                val lbl = fmt.label ?: fmt.language?.uppercase(Locale.US) ?: "Subtitle ${i + 1}"
                                subs += DirectTrackChoice(
                                    trackType = C.TRACK_TYPE_TEXT,
                                    group = group,
                                    trackIndex = i,
                                    label = lbl,
                                    isSelected = group.isTrackSelected(i)
                                )
                            }
                        }
                    }
                }
                videoTrackChoices = vids.distinctBy { it.label }.sortedByDescending {
                    Regex("(\\d+)p").find(it.label)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                }
                audioTrackChoices = auds
                subtitleTrackChoices = subs
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = if (isLocalFile) {
                    "Unable to play downloaded file (${error.errorCodeName}). The file may be incomplete."
                } else {
                    "Stream error (${error.errorCodeName}): ${error.localizedMessage ?: "Unable to connect"}"
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            val finalPos = runCatching { exoPlayer.currentPosition.coerceAtLeast(0L) }.getOrDefault(0L)
            val finalDur = runCatching { exoPlayer.duration.coerceAtLeast(0L) }.getOrDefault(0L)
            latestSavePosition(finalPos, finalDur)

            exoPlayer.removeListener(listener)
            runCatching { mediaSession?.release() }
            runCatching { exoPlayer.release() }
            WebViewFullscreenHost.applyFullScreenWindowState(
                activity = activity,
                hostView = hostView,
                isFullScreen = false,
                originalOrientation = initialOrientation,
                originalCutoutMode = initialCutoutMode
            )
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(exoPlayer) {
        var tick = 0
        while (true) {
            if (!isSeeking) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            }
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            if (dur > 0L) durationMs = dur

            tick++
            if (tick % 10 == 0 && dur > 0L) {
                latestSavePosition(exoPlayer.currentPosition.coerceAtLeast(0L), dur)
            }
            delay(500L)
        }
    }

    LaunchedEffect(showControls, isPlaying, controlInteractionTick, showSpeedDialog, showTracksDialog) {
        if (showControls && isPlaying && !showSpeedDialog && !showTracksDialog && playbackError == null) {
            delay(3500L)
            showControls = false
        }
    }

    BackHandler {
        when {
            showTracksDialog -> showTracksDialog = false
            showSpeedDialog -> showSpeedDialog = false
            isFullScreen -> isFullScreen = false
            else -> onBack()
        }
    }

    val episodeSubtitle = if (media.mediaType.equals("tv", ignoreCase = true)) {
        "S$season:E$episode • ${if (isLocalFile) "Offline File" else if (isHls) "HLS Stream" else "Direct MP4"}"
    } else {
        if (isLocalFile) "Offline File • ${downloadEntity?.qualityLabel ?: "Local"}"
        else if (isHls) "Direct HLS Stream"
        else "Direct Stream"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
                controlInteractionTick++
            }
            .testTag("direct_media_player_screen")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    keepScreenOn = true
                    player = exoPlayer
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) {
                    view.player = exoPlayer
                }
            }
        )

        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = colors.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        AnimatedVisibility(
            visible = showControls && playbackError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.78f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isFullScreen) isFullScreen = false else onBack()
                        },
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = if (isLocalFile) colors.success.copy(alpha = 0.24f) else colors.primary.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isLocalFile) "OFFLINE" else "DIRECT",
                                    color = if (isLocalFile) colors.success else colors.gold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = episodeSubtitle,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                controlInteractionTick++
                                showSpeedDialog = true
                            }
                            .testTag("direct_player_speed_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Playback speed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (videoTrackChoices.isNotEmpty() || audioTrackChoices.isNotEmpty() || subtitleTrackChoices.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                controlInteractionTick++
                                showTracksDialog = true
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("direct_player_quality_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = "Video quality and tracks",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            controlInteractionTick++
                            isFullScreen = !isFullScreen
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("direct_player_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullScreen) "Exit fullscreen" else "Fullscreen",
                            tint = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            controlInteractionTick++
                            val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            .testTag("direct_player_rewind")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            controlInteractionTick++
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    exoPlayer.seekTo(0L)
                                }
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(colors.primary.copy(alpha = 0.92f), CircleShape)
                            .testTag("direct_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            controlInteractionTick++
                            val dur = exoPlayer.duration.coerceAtLeast(0L)
                            val target = if (dur > 0L) {
                                (exoPlayer.currentPosition + 10_000L).coerceAtMost(dur)
                            } else {
                                exoPlayer.currentPosition + 10_000L
                            }
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            .testTag("direct_player_forward")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    val displayedPos = if (isSeeking) sliderSeekMs else currentPositionMs
                    val maxDur = durationMs.coerceAtLeast(1L)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDurationMs(displayedPos),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (durationMs > 0L) formatDurationMs(durationMs) else "--:--",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 12.sp
                        )
                    }

                    Slider(
                        value = displayedPos.toFloat().coerceIn(0f, maxDur.toFloat()),
                        onValueChange = { value ->
                            controlInteractionTick++
                            isSeeking = true
                            sliderSeekMs = value.toLong()
                        },
                        onValueChangeFinished = {
                            controlInteractionTick++
                            exoPlayer.seekTo(sliderSeekMs)
                            currentPositionMs = sliderSeekMs
                            isSeeking = false
                        },
                        valueRange = 0f..maxDur.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("direct_player_seek_bar")
                    )
                }
            }
        }

        playbackError?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = err,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                playbackError = null
                                isBuffering = true
                                reloadKey++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        if (onFallbackToWebEmbed != null && !isLocalFile) {
                            OutlinedButton(
                                onClick = onFallbackToWebEmbed,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Web Embed", color = colors.gold, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    OutlinedButton(onClick = onBack, modifier = Modifier.height(44.dp)) {
                        Text("Back", color = colors.textPrimary)
                    }
                }
            }
        }
    }

    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        Dialog(onDismissRequest = { showSpeedDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    speeds.forEach { speed ->
                        val selected = kotlin.math.abs(playbackSpeed - speed) < 0.01f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    playbackSpeed = speed
                                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                                    showSpeedDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                color = if (selected) colors.primary else colors.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTracksDialog) {
        Dialog(onDismissRequest = { showTracksDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Quality & Tracks",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (videoTrackChoices.isNotEmpty()) {
                        Text(
                            text = "Video Quality",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.gold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val isAutoVideo = exoPlayer.trackSelectionParameters.overrides
                            .values.none { it.type == C.TRACK_TYPE_VIDEO }

                        TrackOptionRow(
                            label = "Auto (Adaptive)",
                            selected = isAutoVideo,
                            onClick = {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                    .build()
                                showTracksDialog = false
                            }
                        )
                        videoTrackChoices.forEach { choice ->
                            TrackOptionRow(
                                label = choice.label,
                                selected = !isAutoVideo && choice.isSelected,
                                onClick = {
                                    val override = TrackSelectionOverride(
                                        choice.group.mediaTrackGroup,
                                        choice.trackIndex
                                    )
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(override)
                                        .build()
                                    showTracksDialog = false
                                }
                            )
                        }
                    }

                    if (audioTrackChoices.isNotEmpty()) {
                        HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))
                        Text(
                            text = "Audio Track",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.gold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        audioTrackChoices.forEach { choice ->
                            TrackOptionRow(
                                label = choice.label,
                                selected = choice.isSelected,
                                onClick = {
                                    val override = TrackSelectionOverride(
                                        choice.group.mediaTrackGroup,
                                        choice.trackIndex
                                    )
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(override)
                                        .build()
                                    showTracksDialog = false
                                }
                            )
                        }
                    }

                    if (subtitleTrackChoices.isNotEmpty()) {
                        HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))
                        Text(
                            text = "Subtitles",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.gold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val textDisabled = exoPlayer.trackSelectionParameters.disabledTrackTypes
                            .contains(C.TRACK_TYPE_TEXT)
                        TrackOptionRow(
                            label = "Off",
                            selected = textDisabled || subtitleTrackChoices.none { it.isSelected },
                            onClick = {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                    .build()
                                showTracksDialog = false
                            }
                        )
                        subtitleTrackChoices.forEach { choice ->
                            TrackOptionRow(
                                label = choice.label,
                                selected = !textDisabled && choice.isSelected,
                                onClick = {
                                    val override = TrackSelectionOverride(
                                        choice.group.mediaTrackGroup,
                                        choice.trackIndex
                                    )
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(override)
                                        .build()
                                    showTracksDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = CineTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) colors.primary else colors.textPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = colors.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
