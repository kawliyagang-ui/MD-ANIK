package com.example.ui.screens

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toRect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.example.data.model.DownloadEntity
import com.example.data.model.EpisodeInfo
import com.example.data.model.MediaItem
import com.example.data.model.MovieBoxDownloadOption
import com.example.data.model.PlaybackSourcePolicy
import com.example.player.DirectPlaybackHolder
import com.example.player.DirectPlaybackService
import com.example.ui.components.CineEase
import com.example.ui.components.WebViewFullscreenHost
import com.example.ui.components.rememberVerticalGradientBrush
import com.example.ui.player.DirectAspectRatio
import com.example.ui.player.DirectPlayerActiveSheet
import com.example.ui.player.DirectPlayerBottomBar
import com.example.ui.player.DirectPlayerBufferingOverlay
import com.example.ui.player.DirectPlayerCenterControls
import com.example.ui.player.DirectPlayerErrorCard
import com.example.ui.player.DirectPlayerPipEffects
import com.example.ui.player.DirectPlayerSheetsHost
import com.example.ui.player.DirectPlayerTopBar
import com.example.ui.player.DirectTrackChoice
import com.example.ui.player.DoubleTapSeekOverlay
import com.example.ui.player.HorizontalDragSeekPill
import com.example.ui.player.LongPressSpeedController
import com.example.ui.player.LongPressSpeedPill
import com.example.ui.player.PlayerLockOverlay
import com.example.ui.player.SeriesUpNextCard
import com.example.ui.player.SwipeAxis
import com.example.ui.player.SwipeGestureMath
import com.example.ui.player.VerticalAdjustmentPill
import com.example.ui.player.VerticalSwipeSide
import com.example.ui.player.enterPictureInPictureNow
import com.example.ui.player.isPictureInPictureSupported
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import androidx.media3.common.MediaItem as Media3Item

private enum class SurfaceGestureMode {
    PENDING,
    BOOSTING,
    HORIZONTAL_SEEK,
    VERTICAL_BRIGHTNESS,
    VERTICAL_VOLUME
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun applyWindowBrightness(activity: Activity?, brightness: Float) {
    val win = activity?.window ?: return
    val currentAttrs = win.attributes ?: return
    val updatedAttrs = WindowManager.LayoutParams().apply {
        copyFrom(currentAttrs)
        screenBrightness = brightness
    }
    win.attributes = updatedAttrs
}

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
    loadedEpisodes: List<EpisodeInfo> = emptyList(),
    onSwitchEpisode: ((EpisodeInfo) -> Unit)? = null,
    onSavePosition: (positionMs: Long, durationMs: Long) -> Unit,
    onFallbackToWebEmbed: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hostView = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val pipSupported = remember(context) { isPictureInPictureSupported(context) }
    val latestSavePosition by rememberUpdatedState(onSavePosition)
    val latestOnBack by rememberUpdatedState(onBack)
    val gestureScope = rememberCoroutineScope()
    val ownerToken = remember { UUID.randomUUID().toString() }

    val initialOrientation = remember(activity) {
        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    val initialCutoutMode = remember(activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity?.window?.attributes?.layoutInDisplayCutoutMode
        } else null
    }
    val initialBrightness = remember(activity) {
        SwipeGestureMath.resolveStartBrightness(
            windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        )
    }

    var isFullScreen by remember { mutableStateOf(false) }
    var isInPipMode by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var controlInteractionTick by remember { mutableIntStateOf(0) }
    var activeSheet by remember { mutableStateOf(DirectPlayerActiveSheet.NONE) }
    var aspectRatio by remember { mutableStateOf(DirectAspectRatio.FIT) }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var isEnded by remember { mutableStateOf(false) }
    var bufferedPercent by remember { mutableIntStateOf(0) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var playbackError by remember(sourceUrl) { mutableStateOf<String?>(null) }
    var currentPositionMs by remember { mutableLongStateOf(initialPositionMs.coerceAtLeast(0L)) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var pipSourceRect by remember { mutableStateOf<Rect?>(null) }

    var isScrubbingBar by remember { mutableStateOf(false) }
    var barScrubMs by remember { mutableLongStateOf(0L) }
    var baseSpeed by remember { mutableFloatStateOf(1.0f) }
    var isSpeedBoosted by remember { mutableStateOf(false) }
    var hasAppliedInitialSeek by remember(sourceUrl) { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var showPortraitPausedDetails by remember { mutableStateOf(false) }
    var dismissedUpNextForEpisode by remember(media.id, season, episode) { mutableStateOf(false) }

    // Gesture visual indicator states
    var doubleTapForward by remember { mutableStateOf<Boolean?>(null) }
    var doubleTapSeconds by remember { mutableIntStateOf(0) }
    var doubleTapTargetMs by remember { mutableLongStateOf(0L) }
    var doubleTapResetJob by remember { mutableStateOf<Job?>(null) }
    var pendingSingleTapJob by remember { mutableStateOf<Job?>(null) }
    var pendingTapIsRight by remember { mutableStateOf<Boolean?>(null) }

    var isHorizontalGestureScrubbing by remember { mutableStateOf(false) }
    var horizontalGestureTargetMs by remember { mutableLongStateOf(0L) }
    var horizontalGestureDeltaMs by remember { mutableLongStateOf(0L) }

    var brightnessFraction by remember { mutableFloatStateOf(initialBrightness) }
    var showBrightnessPill by remember { mutableStateOf(false) }
    var brightnessHideJob by remember { mutableStateOf<Job?>(null) }

    var volumeFraction by remember {
        mutableFloatStateOf(
            audioManager?.let {
                val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                it.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.toFloat()
            } ?: 0.6f
        )
    }
    var showVolumePill by remember { mutableStateOf(false) }
    var volumeHideJob by remember { mutableStateOf<Job?>(null) }

    var videoTrackChoices by remember { mutableStateOf<List<DirectTrackChoice>>(emptyList()) }
    var audioTrackChoices by remember { mutableStateOf<List<DirectTrackChoice>>(emptyList()) }
    var subtitleTrackChoices by remember { mutableStateOf<List<DirectTrackChoice>>(emptyList()) }
    var trackRevision by remember { mutableIntStateOf(0) }

    val speedController = remember {
        LongPressSpeedController(initialBaseSpeed = 1.0f) { _ -> }
    }

    val sortedEpisodes = remember(loadedEpisodes) {
        loadedEpisodes.sortedBy { it.episodeNumber }
    }
    val currentEpisodeIndex = remember(sortedEpisodes, episode) {
        sortedEpisodes.indexOfFirst { it.episodeNumber == episode }
    }
    val prevEpisode = remember(sortedEpisodes, currentEpisodeIndex) {
        if (currentEpisodeIndex > 0) sortedEpisodes[currentEpisodeIndex - 1] else null
    }
    val nextEpisode = remember(sortedEpisodes, currentEpisodeIndex) {
        if (currentEpisodeIndex >= 0 && currentEpisodeIndex + 1 < sortedEpisodes.size) {
            sortedEpisodes[currentEpisodeIndex + 1]
        } else null
    }
    val currentEpisodeTitle = remember(sortedEpisodes, currentEpisodeIndex) {
        if (currentEpisodeIndex >= 0) sortedEpisodes[currentEpisodeIndex].name else ""
    }

    val resolvedUri = remember(sourceUrl, isLocalFile) {
        if (isLocalFile && !sourceUrl.startsWith("file://") && !sourceUrl.startsWith("content://")) {
            Uri.fromFile(File(sourceUrl))
        } else {
            Uri.parse(sourceUrl)
        }
    }
    val isHls = remember(sourceUrl) { PlaybackSourcePolicy.isHlsStreamUrl(sourceUrl) }
    val notificationSubtitle = remember(media.mediaType, season, episode, currentEpisodeTitle) {
        if (media.mediaType.equals("tv", ignoreCase = true)) {
            if (currentEpisodeTitle.isNotBlank()) "S$season · E$episode — $currentEpisodeTitle" else "S$season · E$episode"
        } else {
            media.releaseDate.take(4).ifBlank { "Now Playing" }
        }
    }

    val exoPlayer = remember(sourceUrl, reloadKey) {
        val headers = buildMap {
            downloadEntity?.refererHeader?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
            downloadEntity?.cookieHeader?.takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
        }
        val ua = downloadEntity?.userAgentHeader?.takeIf { it.isNotBlank() }
            ?: MovieBoxDownloadOption.DEFAULT_USER_AGENT
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(30_000)
            .apply { if (headers.isNotEmpty()) setDefaultRequestProperties(headers) }
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                val artworkUri = media.fullPosterUrl.takeIf { it.isNotBlank() }?.let(Uri::parse)
                val itemBuilder = Media3Item.Builder()
                    .setUri(resolvedUri)
                    .setMediaMetadata(
                        DirectPlaybackService.buildMediaMetadata(
                            title = media.title,
                            subtitle = notificationSubtitle,
                            artworkUri = artworkUri
                        )
                    )
                if (isHls) itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                setMediaItem(itemBuilder.build())
                playbackParameters = PlaybackParameters(baseSpeed)
                playWhenReady = true
                prepare()
            }
    }

    // Bind player to DirectPlaybackHolder & connect Media3 MediaSessionService for background playback
    DisposableEffect(exoPlayer) {
        DirectPlaybackHolder.setPlayer(
            ownerToken = ownerToken,
            sourceKey = "${media.id}_${season}_$episode",
            player = exoPlayer,
            onStopRequested = { latestOnBack() }
        )
        val sessionToken = SessionToken(context, ComponentName(context, DirectPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({ runCatching { controllerFuture.get() } }, MoreExecutors.directExecutor())

        onDispose {
            val finalPos = runCatching { exoPlayer.currentPosition.coerceAtLeast(0L) }.getOrDefault(0L)
            val finalDur = runCatching { exoPlayer.duration.coerceAtLeast(0L) }.getOrDefault(0L)
            latestSavePosition(finalPos, finalDur)
            MediaController.releaseFuture(controllerFuture)
            DirectPlaybackHolder.releaseIfOwner(ownerToken)
            runCatching { context.stopService(Intent(context, DirectPlaybackService::class.java)) }
        }
    }

    // Smooth 200ms ramp to 2.0x boost & clean restore to previous baseSpeed
    LaunchedEffect(isSpeedBoosted, baseSpeed, exoPlayer) {
        val target = if (isSpeedBoosted) speedController.boostSpeed else baseSpeed
        val start = exoPlayer.playbackParameters.speed
        if (abs(start - target) < 0.01f) {
            exoPlayer.playbackParameters = PlaybackParameters(target)
            return@LaunchedEffect
        }
        val duration = if (isSpeedBoosted) {
            LongPressSpeedController.RAMP_DURATION_MS
        } else {
            LongPressSpeedController.RELEASE_RAMP_DURATION_MS
        }
        val steps = 8
        val stepMs = (duration / steps).coerceAtLeast(12L)
        for (step in 1..steps) {
            val frac = step.toFloat() / steps.toFloat()
            val nextSpeed = LongPressSpeedController.interpolateSpeed(start, target, frac)
            exoPlayer.playbackParameters = PlaybackParameters(nextSpeed)
            if (step < steps) delay(stepMs)
        }
        exoPlayer.playbackParameters = PlaybackParameters(target)
    }

    // Disengage speed boost immediately if playback stops/buffers/locks/opens menu/enters PiP
    LaunchedEffect(
        isPlaying,
        isBuffering,
        isEnded,
        playbackError,
        isLocked,
        activeSheet,
        isScrubbingBar,
        isInPipMode
    ) {
        val eligible = speedController.canBoost(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            isEnded = isEnded,
            hasError = playbackError != null,
            isLocked = isLocked,
            hasOpenMenu = activeSheet != DirectPlayerActiveSheet.NONE,
            isScrubbing = isScrubbingBar,
            isInPipMode = isInPipMode
        )
        if (speedController.syncEligibility(eligible)) {
            isSpeedBoosted = false
        }
    }

    // Orientation + keepScreenOn strictly tied to playing state
    LaunchedEffect(isFullScreen) {
        WebViewFullscreenHost.applyFullScreenWindowState(
            activity = activity,
            hostView = hostView,
            isFullScreen = isFullScreen,
            originalOrientation = initialOrientation,
            originalCutoutMode = initialCutoutMode
        )
        if (!isPlaying) {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    LaunchedEffect(isPlaying, isFullScreen) {
        if (!isPlaying) {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else if (isFullScreen) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            WebViewFullscreenHost.applyFullScreenWindowState(
                activity = activity,
                hostView = hostView,
                isFullScreen = false,
                originalOrientation = initialOrientation,
                originalCutoutMode = initialCutoutMode
            )
            applyWindowBrightness(activity, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        }
    }

    DirectPlayerPipEffects(
        activity = activity,
        isPlaying = isPlaying,
        shouldAutoEnterPip = isPlaying && playbackError == null && !isEnded,
        videoWidth = videoWidth,
        videoHeight = videoHeight,
        sourceRect = pipSourceRect,
        onPipModeChanged = { inPip ->
            isInPipMode = inPip
            if (inPip) activeSheet = DirectPlayerActiveSheet.NONE
        },
        onPipClosedByUser = { exoPlayer.pause() },
        onPipPlay = {
            if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0L)
            exoPlayer.play()
        },
        onPipPause = { exoPlayer.pause() }
    )

    // ExoPlayer listener for tracks, state, errors, and video dimensions
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                isEnded = playbackState == Player.STATE_ENDED
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
                    showControls = true
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

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val vids = mutableListOf<DirectTrackChoice>()
                val auds = mutableListOf<DirectTrackChoice>()
                val subs = mutableListOf<DirectTrackChoice>()
                tracks.groups.forEach { group ->
                    for (i in 0 until group.length) {
                        if (!group.isTrackSupported(i)) continue
                        val fmt = group.getTrackFormat(i)
                        when (group.type) {
                            C.TRACK_TYPE_VIDEO -> {
                                val res = if (fmt.height > 0) "${fmt.height}p" else "Video ${i + 1}"
                                val kbps = if (fmt.bitrate > 0) " • ${fmt.bitrate / 1000} kbps" else ""
                                vids += DirectTrackChoice(C.TRACK_TYPE_VIDEO, group, i, "$res$kbps", group.isTrackSelected(i))
                            }
                            C.TRACK_TYPE_AUDIO -> {
                                val lbl = fmt.label ?: fmt.language?.uppercase(Locale.US) ?: "Audio ${i + 1}"
                                auds += DirectTrackChoice(C.TRACK_TYPE_AUDIO, group, i, lbl, group.isTrackSelected(i))
                            }
                            C.TRACK_TYPE_TEXT -> {
                                val lbl = fmt.label ?: fmt.language?.uppercase(Locale.US) ?: "Subtitle ${i + 1}"
                                subs += DirectTrackChoice(C.TRACK_TYPE_TEXT, group, i, lbl, group.isTrackSelected(i))
                            }
                        }
                    }
                }
                videoTrackChoices = vids.distinctBy { it.label }.sortedByDescending {
                    Regex("(\\d+)p").find(it.label)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                }
                audioTrackChoices = auds
                subtitleTrackChoices = subs
                trackRevision++
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = if (isLocalFile) {
                    "This downloaded file could not be played. Try playing again or re-downloading it."
                } else {
                    "Unable to stream this link right now. Check your connection and tap Retry."
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Periodic position / buffer polling & progress autosave
    LaunchedEffect(exoPlayer) {
        var tick = 0
        while (true) {
            if (!isScrubbingBar && !isHorizontalGestureScrubbing) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            }
            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            bufferedPercent = exoPlayer.bufferedPercentage.coerceIn(0, 100)
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            if (dur > 0L) durationMs = dur
            tick++
            if (tick % 10 == 0 && dur > 0L) {
                latestSavePosition(exoPlayer.currentPosition.coerceAtLeast(0L), dur)
            }
            delay(400L)
        }
    }

    // Auto-hide controls after 3s while playing
    LaunchedEffect(showControls, isPlaying, controlInteractionTick, activeSheet, isLocked, isScrubbingBar) {
        if (showControls && isPlaying && activeSheet == DirectPlayerActiveSheet.NONE &&
            !isLocked && !isScrubbingBar && playbackError == null
        ) {
            delay(3000L)
            showControls = false
        }
    }

    // Portrait paused > 2 seconds metadata fade-in
    LaunchedEffect(isPlaying, isFullScreen, isBuffering, playbackError, isInPipMode) {
        if (!isPlaying && !isFullScreen && !isBuffering && playbackError == null && !isInPipMode) {
            delay(2000L)
            showPortraitPausedDetails = true
        } else {
            showPortraitPausedDetails = false
        }
    }

    BackHandler {
        when {
            activeSheet != DirectPlayerActiveSheet.NONE -> activeSheet = DirectPlayerActiveSheet.NONE
            isLocked -> Unit // Hold Unlock button to exit lock mode
            isFullScreen -> isFullScreen = false
            else -> onBack()
        }
    }

    val performSeekDelta: (Long) -> Long = { deltaMs ->
        val base = exoPlayer.currentPosition.coerceAtLeast(0L)
        val dur = exoPlayer.duration.coerceAtLeast(0L)
        val target = if (dur > 0L) (base + deltaMs).coerceIn(0L, dur) else (base + deltaMs).coerceAtLeast(0L)
        exoPlayer.seekTo(target)
        currentPositionMs = target
        target
    }

    val showUpNextCard = remember(
        nextEpisode,
        dismissedUpNextForEpisode,
        isInPipMode,
        playbackError,
        durationMs,
        currentPositionMs,
        isEnded,
        isLocked
    ) {
        nextEpisode != null &&
            !dismissedUpNextForEpisode &&
            !isInPipMode &&
            playbackError == null &&
            !isLocked &&
            (isEnded || (durationMs > 15_000L && currentPositionMs >= durationMs - 10_000L))
    }

    val topScrimBrush = rememberVerticalGradientBrush(Color.Black.copy(alpha = 0.78f), Color.Transparent)
    val bottomScrimBrush = rememberVerticalGradientBrush(Color.Transparent, Color.Black.copy(alpha = 0.86f))

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { coords ->
                pipSourceRect = coords.boundsInWindow().toAndroidRectF().toRect()
            }
            .testTag("direct_media_player_screen")
    ) {
        val isPortraitLayout = maxHeight > maxWidth && !isFullScreen

        // Video surface with Aspect Ratio framing
        val videoContainerModifier = when (aspectRatio) {
            DirectAspectRatio.RATIO_16_9 -> Modifier
                .align(Alignment.Center)
                .then(
                    if (maxWidth / maxHeight > 16f / 9f) {
                        Modifier.fillMaxHeight().aspectRatio(16f / 9f)
                    } else {
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    }
                )
            DirectAspectRatio.RATIO_4_3 -> Modifier
                .align(Alignment.Center)
                .then(
                    if (maxWidth / maxHeight > 4f / 3f) {
                        Modifier.fillMaxHeight().aspectRatio(4f / 3f)
                    } else {
                        Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                    }
                )
            else -> Modifier.fillMaxSize()
        }

        AndroidView(
            modifier = videoContainerModifier,
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    isClickable = false
                    isFocusable = false
                    keepScreenOn = isPlaying
                    resizeMode = aspectRatio.toResizeMode()
                    player = exoPlayer
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) view.player = exoPlayer
                if (view.keepScreenOn != isPlaying) view.keepScreenOn = isPlaying
                val targetResize = aspectRatio.toResizeMode()
                if (view.resizeMode != targetResize) view.resizeMode = targetResize
            }
        )

        val currentExoPlayer by rememberUpdatedState(exoPlayer)
        val currentDurationMs by rememberUpdatedState(durationMs)
        val currentIsPlaying by rememberUpdatedState(isPlaying)
        val currentIsBuffering by rememberUpdatedState(isBuffering)
        val currentIsEnded by rememberUpdatedState(isEnded)
        val currentHasError by rememberUpdatedState(playbackError != null)
        val currentIsLocked by rememberUpdatedState(isLocked)
        val currentHasOpenMenu by rememberUpdatedState(activeSheet != DirectPlayerActiveSheet.NONE)
        val currentIsScrubbingBar by rememberUpdatedState(isScrubbingBar)
        val currentIsInPipMode by rememberUpdatedState(isInPipMode)

        // Unified gesture layer placed above the video surface and below the chrome
        // (disabled in PiP, Lock mode, playback error, or while a bottom-sheet menu is open)
        if (!isInPipMode && !isLocked && activeSheet == DirectPlayerActiveSheet.NONE && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        val dragSlopPx = maxOf(
                            viewConfiguration.touchSlop,
                            SwipeGestureMath.DRAG_SLOP_DP.dp.toPx()
                        )
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            if (currentIsLocked || currentIsInPipMode || currentHasOpenMenu || currentHasError) {
                                return@awaitEachGesture
                            }

                            val downPos = down.position
                            val surfaceW = size.width.toFloat().coerceAtLeast(1f)
                            val surfaceH = size.height.toFloat().coerceAtLeast(1f)

                            var mode = SurfaceGestureMode.PENDING
                            var seekOriginMs = 0L
                            var dragStartBrightness = SwipeGestureMath.DEFAULT_FALLBACK_BRIGHTNESS
                            var dragStartVolume = 0
                            var dragMaxVolume = 15
                            var lastAppliedVolume = -1
                            var gestureFinishedByUp = false
                            var gestureCancelled = false

                            val holdJob = gestureScope.launch {
                                delay(LongPressSpeedController.HOLD_THRESHOLD_MS)
                                if (mode == SurfaceGestureMode.PENDING && !gestureFinishedByUp && !gestureCancelled) {
                                    val activated = speedController.onHoldThresholdReached(
                                        isPlaying = currentIsPlaying,
                                        isBuffering = currentIsBuffering,
                                        isEnded = currentIsEnded,
                                        hasError = currentHasError,
                                        isLocked = currentIsLocked,
                                        hasOpenMenu = currentHasOpenMenu,
                                        isScrubbing = currentIsScrubbingBar,
                                        isInPipMode = currentIsInPipMode
                                    )
                                    if (activated) {
                                        mode = SurfaceGestureMode.BOOSTING
                                        isSpeedBoosted = true
                                        hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                }
                            }

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || change.isConsumed) {
                                        gestureCancelled = true
                                        break
                                    }
                                    if (!change.pressed) {
                                        gestureFinishedByUp = true
                                        break
                                    }
                                    if (currentIsLocked || currentIsInPipMode || currentHasOpenMenu || currentHasError) {
                                        gestureCancelled = true
                                        break
                                    }

                                    val totalDx = change.position.x - downPos.x
                                    val totalDy = change.position.y - downPos.y

                                    if (mode == SurfaceGestureMode.PENDING) {
                                        val axis = SwipeGestureMath.decideSwipeAxis(
                                            dx = totalDx,
                                            dy = totalDy,
                                            touchSlopPx = dragSlopPx
                                        )
                                        if (axis != null) {
                                            holdJob.cancel()
                                            when (axis) {
                                                SwipeAxis.HORIZONTAL -> {
                                                    mode = SurfaceGestureMode.HORIZONTAL_SEEK
                                                    val dur = currentDurationMs
                                                    if (dur > 0L) {
                                                        seekOriginMs = currentExoPlayer.currentPosition.coerceIn(0L, dur)
                                                        val initial = SwipeGestureMath.mapSeekTarget(
                                                            startPositionMs = seekOriginMs,
                                                            durationMs = dur,
                                                            dragDxPx = totalDx,
                                                            screenWidthPx = surfaceW
                                                        )
                                                        horizontalGestureTargetMs = initial.targetPositionMs
                                                        horizontalGestureDeltaMs = initial.deltaMs
                                                        isHorizontalGestureScrubbing = true
                                                    }
                                                }
                                                SwipeAxis.VERTICAL -> {
                                                    when (SwipeGestureMath.decideVerticalSide(downPos.x, surfaceW)) {
                                                        VerticalSwipeSide.LEFT_BRIGHTNESS -> {
                                                            mode = SurfaceGestureMode.VERTICAL_BRIGHTNESS
                                                            val winBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                                                            dragStartBrightness = SwipeGestureMath.resolveStartBrightness(
                                                                windowBrightness = winBrightness,
                                                                fallbackBrightness = brightnessFraction
                                                            )
                                                            brightnessHideJob?.cancel()
                                                            brightnessFraction = dragStartBrightness
                                                            showBrightnessPill = true
                                                        }
                                                        VerticalSwipeSide.RIGHT_VOLUME -> {
                                                            mode = SurfaceGestureMode.VERTICAL_VOLUME
                                                            audioManager?.let { am ->
                                                                dragMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                                                dragStartVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, dragMaxVolume)
                                                                lastAppliedVolume = dragStartVolume
                                                                volumeFraction = dragStartVolume.toFloat() / dragMaxVolume.toFloat()
                                                            }
                                                            volumeHideJob?.cancel()
                                                            showVolumePill = true
                                                        }
                                                    }
                                                }
                                            }
                                            change.consume()
                                        }
                                    }

                                    when (mode) {
                                        SurfaceGestureMode.BOOSTING -> {
                                            // Stationary hold remains in boost mode until release; never converts to a drag
                                            change.consume()
                                        }
                                        SurfaceGestureMode.HORIZONTAL_SEEK -> {
                                            change.consume()
                                            val dur = currentDurationMs
                                            if (dur > 0L) {
                                                val mapped = SwipeGestureMath.mapSeekTarget(
                                                    startPositionMs = seekOriginMs,
                                                    durationMs = dur,
                                                    dragDxPx = totalDx,
                                                    screenWidthPx = surfaceW
                                                )
                                                horizontalGestureTargetMs = mapped.targetPositionMs
                                                horizontalGestureDeltaMs = mapped.deltaMs
                                                isHorizontalGestureScrubbing = true
                                            }
                                        }
                                        SurfaceGestureMode.VERTICAL_BRIGHTNESS -> {
                                            change.consume()
                                            val dragUpPx = downPos.y - change.position.y
                                            val updated = SwipeGestureMath.mapBrightness(
                                                startBrightness = dragStartBrightness,
                                                dragUpPx = dragUpPx,
                                                screenHeightPx = surfaceH
                                            )
                                            brightnessFraction = updated
                                            applyWindowBrightness(activity, updated)
                                            showBrightnessPill = true
                                        }
                                        SurfaceGestureMode.VERTICAL_VOLUME -> {
                                            change.consume()
                                            val dragUpPx = downPos.y - change.position.y
                                            val mapped = SwipeGestureMath.mapVolume(
                                                startVolume = dragStartVolume,
                                                maxVolume = dragMaxVolume,
                                                dragUpPx = dragUpPx,
                                                screenHeightPx = surfaceH
                                            )
                                            volumeFraction = mapped.fraction
                                            if (mapped.step != lastAppliedVolume) {
                                                lastAppliedVolume = mapped.step
                                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, mapped.step, 0)
                                            }
                                            showVolumePill = true
                                        }
                                        SurfaceGestureMode.PENDING -> Unit
                                    }
                                }
                            } finally {
                                holdJob.cancel()
                                if (speedController.onReleaseOrCancel() || isSpeedBoosted) {
                                    isSpeedBoosted = false
                                }
                                if (mode == SurfaceGestureMode.HORIZONTAL_SEEK) {
                                    isHorizontalGestureScrubbing = false
                                    if (gestureFinishedByUp && !gestureCancelled && currentDurationMs > 0L) {
                                        currentExoPlayer.seekTo(horizontalGestureTargetMs)
                                        currentPositionMs = horizontalGestureTargetMs
                                    }
                                }
                                if (mode == SurfaceGestureMode.VERTICAL_BRIGHTNESS) {
                                    brightnessHideJob?.cancel()
                                    brightnessHideJob = gestureScope.launch {
                                        delay(SwipeGestureMath.INDICATOR_HIDE_DELAY_MS)
                                        showBrightnessPill = false
                                    }
                                }
                                if (mode == SurfaceGestureMode.VERTICAL_VOLUME) {
                                    volumeHideJob?.cancel()
                                    volumeHideJob = gestureScope.launch {
                                        delay(SwipeGestureMath.INDICATOR_HIDE_DELAY_MS)
                                        showVolumePill = false
                                    }
                                }
                                if (mode == SurfaceGestureMode.PENDING && gestureFinishedByUp && !gestureCancelled) {
                                    val isLeftZone = downPos.x < surfaceW * 0.35f
                                    val isRightZone = downPos.x > surfaceW * 0.65f
                                    if (!isLeftZone && !isRightZone) {
                                        pendingSingleTapJob?.cancel()
                                        pendingTapIsRight = null
                                        showControls = !showControls
                                        controlInteractionTick++
                                    } else {
                                        val isRight = isRightZone
                                        val activeDouble = doubleTapForward
                                        if (activeDouble == isRight) {
                                            doubleTapSeconds += 10
                                            doubleTapTargetMs = performSeekDelta(if (isRight) 10_000L else -10_000L)
                                            doubleTapResetJob?.cancel()
                                            doubleTapResetJob = gestureScope.launch {
                                                delay(680L)
                                                doubleTapForward = null
                                                doubleTapSeconds = 0
                                            }
                                        } else if (pendingTapIsRight == isRight && pendingSingleTapJob?.isActive == true) {
                                            pendingSingleTapJob?.cancel()
                                            pendingTapIsRight = null
                                            doubleTapForward = isRight
                                            doubleTapSeconds = 10
                                            doubleTapTargetMs = performSeekDelta(if (isRight) 10_000L else -10_000L)
                                            doubleTapResetJob?.cancel()
                                            doubleTapResetJob = gestureScope.launch {
                                                delay(680L)
                                                doubleTapForward = null
                                                doubleTapSeconds = 0
                                            }
                                        } else {
                                            pendingSingleTapJob?.cancel()
                                            pendingTapIsRight = isRight
                                            pendingSingleTapJob = gestureScope.launch {
                                                delay(230L)
                                                pendingTapIsRight = null
                                                showControls = !showControls
                                                controlInteractionTick++
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            )
        }

        // Translucent top & bottom scrims + controls (hidden in PiP & Lock mode)
        AnimatedVisibility(
            visible = !isInPipMode && !isLocked && showControls && playbackError == null,
            enter = fadeIn(tween(220, easing = CineEase)),
            exit = fadeOut(tween(220, easing = CineEase)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(topScrimBrush)
                ) {
                    DirectPlayerTopBar(
                        media = media,
                        season = season,
                        episode = episode,
                        episodeTitle = currentEpisodeTitle,
                        qualityLabel = downloadEntity?.qualityLabel.orEmpty(),
                        isLocalFile = isLocalFile,
                        showPortraitPausedDetails = isPortraitLayout && showPortraitPausedDetails,
                        onBack = { if (isFullScreen) isFullScreen = false else onBack() }
                    )
                }

                DirectPlayerCenterControls(
                    isPlaying = isPlaying,
                    isSeries = media.mediaType.equals("tv", ignoreCase = true) && onSwitchEpisode != null && sortedEpisodes.isNotEmpty(),
                    hasPrevEpisode = prevEpisode != null,
                    hasNextEpisode = nextEpisode != null,
                    onRewind10 = {
                        controlInteractionTick++
                        performSeekDelta(-10_000L)
                    },
                    onPlayPause = {
                        controlInteractionTick++
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0L)
                            exoPlayer.play()
                        }
                    },
                    onForward10 = {
                        controlInteractionTick++
                        performSeekDelta(10_000L)
                    },
                    onPrevEpisode = {
                        prevEpisode?.let { ep -> onSwitchEpisode?.invoke(ep) }
                    },
                    onNextEpisode = {
                        nextEpisode?.let { ep -> onSwitchEpisode?.invoke(ep) }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )

                val activeSubEnabled = remember(trackRevision, subtitleTrackChoices) {
                    val disabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                    !disabled && subtitleTrackChoices.any { it.isSelected }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(bottomScrimBrush)
                ) {
                    DirectPlayerBottomBar(
                        currentPositionMs = if (isScrubbingBar) barScrubMs else currentPositionMs,
                        bufferedPositionMs = bufferedPositionMs,
                        durationMs = durationMs,
                        currentBaseSpeed = baseSpeed,
                        aspectRatio = aspectRatio,
                        hasMultipleVideoQualities = videoTrackChoices.size > 1,
                        hasActiveSubtitles = activeSubEnabled,
                        isPipSupported = pipSupported,
                        isFullScreen = isFullScreen,
                        onScrubStart = { target ->
                            controlInteractionTick++
                            isScrubbingBar = true
                            barScrubMs = target
                        },
                        onScrubChange = { target ->
                            controlInteractionTick++
                            barScrubMs = target
                        },
                        onScrubFinished = { target ->
                            controlInteractionTick++
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                            isScrubbingBar = false
                        },
                        onOpenSpeed = { activeSheet = DirectPlayerActiveSheet.SPEED },
                        onOpenSubtitles = { activeSheet = DirectPlayerActiveSheet.SUBTITLES },
                        onOpenAudio = { activeSheet = DirectPlayerActiveSheet.AUDIO },
                        onOpenQuality = { activeSheet = DirectPlayerActiveSheet.QUALITY },
                        onOpenAspectRatio = { activeSheet = DirectPlayerActiveSheet.ASPECT_RATIO },
                        onEnterPip = {
                            enterPictureInPictureNow(
                                activity = activity,
                                isPlaying = isPlaying,
                                videoWidth = videoWidth,
                                videoHeight = videoHeight,
                                sourceRect = pipSourceRect
                            )
                        },
                        onLock = {
                            isLocked = true
                            showControls = false
                        },
                        onToggleFullScreen = {
                            controlInteractionTick++
                            isFullScreen = !isFullScreen
                        }
                    )
                }
            }
        }

        // Double-tap seek ripples
        doubleTapForward?.let { isForward ->
            if (!isInPipMode && !isLocked) {
                DoubleTapSeekOverlay(
                    isForward = isForward,
                    accumulatedSeconds = doubleTapSeconds,
                    targetPositionMs = doubleTapTargetMs
                )
            }
        }

        // Horizontal drag seek pill
        AnimatedVisibility(
            visible = isHorizontalGestureScrubbing && !isInPipMode && !isLocked,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            HorizontalDragSeekPill(
                targetPositionMs = horizontalGestureTargetMs,
                deltaMs = horizontalGestureDeltaMs
            )
        }

        // Left Brightness / Right Volume vertical pills
        AnimatedVisibility(
            visible = showBrightnessPill && !isInPipMode && !isLocked,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
        ) {
            VerticalAdjustmentPill(isBrightness = true, fraction = brightnessFraction)
        }
        AnimatedVisibility(
            visible = showVolumePill && !isInPipMode && !isLocked,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        ) {
            VerticalAdjustmentPill(isBrightness = false, fraction = volumeFraction)
        }

        // Top-Center "▶▶ 2.0x" speed boost pill (~12dp below top edge)
        LongPressSpeedPill(
            visible = isSpeedBoosted && !isInPipMode && !isLocked,
            speedLabel = "2.0x",
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Series Up Next card
        AnimatedVisibility(
            visible = showUpNextCard,
            enter = fadeIn(tween(240, easing = CineEase)),
            exit = fadeOut(tween(180, easing = CineEase)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 16.dp, bottom = if (showControls) 108.dp else 20.dp)
        ) {
            nextEpisode?.let { nextEp ->
                SeriesUpNextCard(
                    season = season,
                    nextEpisode = nextEp,
                    fallbackThumbUrl = media.fullBackdropUrl.ifBlank { media.fullPosterUrl },
                    onPlayNow = { onSwitchEpisode?.invoke(nextEp) },
                    onDismiss = { dismissedUpNextForEpisode = true }
                )
            }
        }

        // Buffering overlay
        if (isBuffering && playbackError == null && !isInPipMode) {
            DirectPlayerBufferingOverlay(
                bufferedPercent = bufferedPercent,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Lock overlay
        if (isLocked && !isInPipMode) {
            PlayerLockOverlay(
                onUnlock = {
                    isLocked = false
                    showControls = true
                    controlInteractionTick++
                }
            )
        }

        // Playback error card
        playbackError?.let { err ->
            if (!isInPipMode) {
                DirectPlayerErrorCard(
                    message = err,
                    canFallbackToEmbed = onFallbackToWebEmbed != null && !isLocalFile,
                    onRetry = {
                        playbackError = null
                        isBuffering = true
                        reloadKey++
                    },
                    onFallbackToWebEmbed = onFallbackToWebEmbed,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Bottom sheets for Speed / CC / Audio / Quality / Aspect Ratio
        if (!isInPipMode) {
            val isAutoVideo = remember(trackRevision, videoTrackChoices) {
                exoPlayer.trackSelectionParameters.overrides.values.none { it.type == C.TRACK_TYPE_VIDEO }
            }
            val textDisabled = remember(trackRevision, subtitleTrackChoices) {
                exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
            }
            DirectPlayerSheetsHost(
                activeSheet = activeSheet,
                currentBaseSpeed = baseSpeed,
                currentAspectRatio = aspectRatio,
                videoTrackChoices = videoTrackChoices,
                isAutoVideoSelected = isAutoVideo,
                audioTrackChoices = audioTrackChoices,
                subtitleTrackChoices = subtitleTrackChoices,
                areSubtitlesDisabled = textDisabled,
                onDismiss = {
                    activeSheet = DirectPlayerActiveSheet.NONE
                    controlInteractionTick++
                },
                onSelectSpeed = { selected ->
                    baseSpeed = selected
                    speedController.setUserSpeed(selected)
                    exoPlayer.playbackParameters = PlaybackParameters(selected)
                },
                onSelectAspectRatio = { aspectRatio = it },
                onSelectAutoVideo = {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        .build()
                    trackRevision++
                },
                onSelectVideoTrack = { choice ->
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(TrackSelectionOverride(choice.group.mediaTrackGroup, choice.trackIndex))
                        .build()
                    trackRevision++
                },
                onSelectAudioTrack = { choice ->
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(TrackSelectionOverride(choice.group.mediaTrackGroup, choice.trackIndex))
                        .build()
                    trackRevision++
                },
                onDisableSubtitles = {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                    trackRevision++
                },
                onSelectSubtitleTrack = { choice ->
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(TrackSelectionOverride(choice.group.mediaTrackGroup, choice.trackIndex))
                        .build()
                    trackRevision++
                }
            )
        }
    }
}
