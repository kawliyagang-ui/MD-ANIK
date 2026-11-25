package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.annotation.TargetApi
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.model.EpisodeInfo
import com.example.data.model.MediaItem
import com.example.data.model.PlayerServer
import com.example.ui.theme.CineTheme
import kotlinx.coroutines.delay

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamPlayerView(
    media: MediaItem,
    currentSeason: Int,
    currentEpisode: Int,
    selectedServer: PlayerServer,
    onServerSelect: (PlayerServer) -> Unit,
    onEpisodeSelect: (Int, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    episodesBySeason: Map<Int, List<EpisodeInfo>> = emptyMap(),
    initialWatchedSeconds: Long = 0L,
    initialDurationSeconds: Long = 0L,
    onProgressUpdate: (watchedSeconds: Long, durationSeconds: Long) -> Unit = { _, _ -> },
    onReportIssue: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val hostView = LocalView.current
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val maxStreamVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
    }
    var volumeFraction by remember(audioManager, maxStreamVolume) {
        mutableFloatStateOf(
            audioManager?.let {
                PlayerControlsPolicy.streamIndexToVolumeFraction(
                    currentVolume = it.getStreamVolume(AudioManager.STREAM_MUSIC),
                    maxVolume = maxStreamVolume
                )
            } ?: 0.7f
        )
    }
    var lastNonZeroVolume by remember {
        mutableFloatStateOf(if (volumeFraction > 0.05f) volumeFraction else 0.7f)
    }
    var isMuted by remember { mutableStateOf(volumeFraction <= 0.005f) }

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

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webViewSlotKey by remember { mutableIntStateOf(0) }
    var rendererCrashed by remember(media.id, selectedServer, currentSeason, currentEpisode) {
        mutableStateOf(false)
    }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenHostContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    var isLoadingPage by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showFullScreenHud by remember { mutableStateOf(true) }
    var hudInteractionTick by remember { mutableIntStateOf(0) }
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var seekFeedbackTick by remember { mutableIntStateOf(0) }
    var trackedWatchedSeconds by remember(media.id, currentSeason, currentEpisode) {
        mutableLongStateOf(initialWatchedSeconds.coerceAtLeast(0L))
    }
    var trackedDurationSeconds by remember(media.id, currentSeason, currentEpisode) {
        mutableLongStateOf(initialDurationSeconds.coerceAtLeast(0L))
    }
    var manualTorrentUri by remember(media.id, currentSeason, currentEpisode) {
        mutableStateOf("")
    }
    var webTorrentTelemetry by remember(media.id, currentSeason, currentEpisode) {
        mutableStateOf(WebTorrentTelemetry())
    }
    var webTorrentFiles by remember(media.id, currentSeason, currentEpisode) {
        mutableStateOf<List<TorrentFileEntry>>(emptyList())
    }
    val latestOnProgressUpdate by rememberUpdatedState(onProgressUpdate)
    val currentStreamUrl = remember(media, selectedServer, currentSeason, currentEpisode) {
        media.getStreamUrl(selectedServer, currentSeason, currentEpisode)
    }
    val latestStreamUrl by rememberUpdatedState(currentStreamUrl)
    val effectiveStreamUrl = remember(manualTorrentUri, currentStreamUrl, media.title) {
        if (manualTorrentUri.isNotBlank()) {
            WebTorrentPolicy.normalizeTorrentId(manualTorrentUri, media.title)
        } else {
            currentStreamUrl
        }
    }
    val hasPlayableWebTorrentSource = remember(effectiveStreamUrl) {
        WebTorrentPolicy.isWebTorrentSource(effectiveStreamUrl)
    }
    val isWebTorrentMode = remember(hasPlayableWebTorrentSource, selectedServer, effectiveStreamUrl) {
        hasPlayableWebTorrentSource ||
            (selectedServer == PlayerServer.WEBTORRENT &&
                !PlayerControlsPolicy.isDirectMediaFileUrl(effectiveStreamUrl) &&
                !effectiveStreamUrl.startsWith("http", ignoreCase = true))
    }
    val playerLoadKey = remember(effectiveStreamUrl, currentSeason, currentEpisode, isWebTorrentMode) {
        if (isWebTorrentMode) {
            "wt::$effectiveStreamUrl::$currentSeason::$currentEpisode"
        } else {
            effectiveStreamUrl
        }
    }

    val subtitleText = remember(media.mediaType, currentSeason, currentEpisode, selectedServer, hasPlayableWebTorrentSource) {
        when {
            media.mediaType == "tv" -> "Season $currentSeason • Episode $currentEpisode"
            hasPlayableWebTorrentSource -> "Now Streaming • P2P Stream"
            else -> "Now Streaming • ${selectedServer.displayName}"
        }
    }

    // Listen for physical hardware volume button changes
    DisposableEffect(context, audioManager, maxStreamVolume) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val mgr = audioManager ?: return
                val updated = PlayerControlsPolicy.streamIndexToVolumeFraction(
                    currentVolume = mgr.getStreamVolume(AudioManager.STREAM_MUSIC),
                    maxVolume = maxStreamVolume
                )
                volumeFraction = updated
                if (updated > 0.005f) {
                    lastNonZeroVolume = updated
                    isMuted = false
                } else {
                    isMuted = true
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // Sync WebView lifecycle and renderer priority on background/foreground transitions
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewInstance, rendererCrashed) {
        val observer = LifecycleEventObserver { _, event ->
            val activeWebView = webViewInstance
            if (!rendererCrashed && !WebViewFullscreenHost.isDestroyed(activeWebView)) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        WebViewFullscreenHost.updateRendererPriority(activeWebView, isForeground = true)
                        runCatching { activeWebView?.onResume() }
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        WebViewFullscreenHost.updateRendererPriority(activeWebView, isForeground = false)
                        runCatching { activeWebView?.onPause() }
                    }
                    else -> Unit
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Manage system bars, cutout edge-to-edge, keep-screen-on & landscape orientation during full-screen playback
    DisposableEffect(isFullScreen, activity, hostView) {
        WebViewFullscreenHost.applyFullScreenWindowState(
            activity = activity,
            hostView = hostView,
            isFullScreen = isFullScreen,
            originalOrientation = initialOrientation,
            originalCutoutMode = initialCutoutMode
        )

        onDispose {
            if (isFullScreen) {
                WebViewFullscreenHost.applyFullScreenWindowState(
                    activity = activity,
                    hostView = hostView,
                    isFullScreen = false,
                    originalOrientation = initialOrientation,
                    originalCutoutMode = initialCutoutMode
                )
            }
        }
    }

    // Softly dim full-screen exit button after 4 seconds so it never obscures the video
    LaunchedEffect(isFullScreen, showFullScreenHud, hudInteractionTick) {
        if (isFullScreen && showFullScreenHud) {
            delay(4000L)
            showFullScreenHud = false
        }
    }

    // Auto-dismiss seek delta badge (-10s / +10s)
    LaunchedEffect(seekFeedbackTick) {
        if (seekFeedbackTick > 0) {
            delay(900L)
            seekFeedbackText = null
        }
    }

    // Track real active watch seconds & probe player duration/position every 5s
    LaunchedEffect(media.id, currentSeason, currentEpisode, isPlaying, isLoadingPage, rendererCrashed) {
        if (!isLoadingPage && isPlaying && !rendererCrashed) {
            while (true) {
                delay(1000L)
                trackedWatchedSeconds += 1L
                val activeWebView = webViewInstance
                if (trackedWatchedSeconds % 5L == 0L && !rendererCrashed && !WebViewFullscreenHost.isDestroyed(activeWebView)) {
                    runCatching {
                        activeWebView?.evaluateJavascript(
                            PlayerControlsPolicy.buildProgressProbeScript(),
                            null
                        )
                    }
                    latestOnProgressUpdate(trackedWatchedSeconds, trackedDurationSeconds)
                }
            }
        }
    }

    val performSeek: (Int) -> Unit = { deltaSeconds ->
        val activeWebView = webViewInstance
        if (!rendererCrashed && !WebViewFullscreenHost.isDestroyed(activeWebView)) {
            runCatching {
                activeWebView?.evaluateJavascript(PlayerControlsPolicy.buildSeekScript(deltaSeconds), null)
            }
        }
        val prefix = if (deltaSeconds < 0) "↺ " else "↻ "
        seekFeedbackText = "$prefix${PlayerControlsPolicy.seekDeltaLabel(deltaSeconds)}"
        seekFeedbackTick += 1
        hudInteractionTick += 1
    }

    val togglePlayPause: () -> Unit = {
        val nextPlaying = !isPlaying
        isPlaying = nextPlaying
        val activeWebView = webViewInstance
        if (!rendererCrashed && !WebViewFullscreenHost.isDestroyed(activeWebView)) {
            runCatching {
                activeWebView?.evaluateJavascript(PlayerControlsPolicy.buildPlayPauseScript(nextPlaying), null)
            }
        }
        hudInteractionTick += 1
    }

    val applyVolume: (Float) -> Unit = { newFraction ->
        val clamped = newFraction.coerceIn(0f, 1f)
        volumeFraction = clamped
        val mutedNow = clamped <= 0.005f
        isMuted = mutedNow
        if (!mutedNow) {
            lastNonZeroVolume = clamped
        }
        audioManager?.let { mgr ->
            val streamIndex = PlayerControlsPolicy.volumeFractionToStreamIndex(clamped, maxStreamVolume)
            runCatching { mgr.setStreamVolume(AudioManager.STREAM_MUSIC, streamIndex, 0) }
        }
        val activeWebView = webViewInstance
        if (!rendererCrashed && !WebViewFullscreenHost.isDestroyed(activeWebView)) {
            runCatching {
                activeWebView?.evaluateJavascript(
                    PlayerControlsPolicy.buildVolumeScript(clamped, mutedNow),
                    null
                )
            }
        }
        hudInteractionTick += 1
    }

    val toggleMute: () -> Unit = {
        if (isMuted || volumeFraction <= 0.005f) {
            val restored = lastNonZeroVolume.coerceIn(0.15f, 1f)
            applyVolume(restored)
        } else {
            lastNonZeroVolume = volumeFraction
            isMuted = true
            audioManager?.let { mgr ->
                runCatching { mgr.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0) }
            }
            val activeWebView = webViewInstance
            if (!rendererCrashed && !WebViewFullscreenHost.isDestroyed(activeWebView)) {
                runCatching {
                    activeWebView?.evaluateJavascript(
                        PlayerControlsPolicy.buildVolumeScript(0f, true),
                        null
                    )
                }
            }
            hudInteractionTick += 1
        }
    }

    val detachCustomFullscreenOnly: () -> Unit = {
        val activeContainer = fullscreenHostContainer
        val activeCustomView = customFullscreenView
        fullscreenHostContainer = null
        customFullscreenView = null
        WebViewFullscreenHost.detachCustomFullscreenView(activeContainer, activeCustomView)
    }

    val setFullScreenMode: (Boolean) -> Unit = { enter ->
        if (!enter) {
            val cb = customViewCallback
            customViewCallback = null
            detachCustomFullscreenOnly()
            runCatching { cb?.onCustomViewHidden() }
        }
        isFullScreen = enter
        showFullScreenHud = true
        hudInteractionTick += 1
    }

    BackHandler {
        if (isFullScreen || customFullscreenView != null) {
            setFullScreenMode(false)
        } else {
            latestOnProgressUpdate(trackedWatchedSeconds, trackedDurationSeconds)
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            latestOnProgressUpdate(trackedWatchedSeconds, trackedDurationSeconds)
            val cb = customViewCallback
            customViewCallback = null
            detachCustomFullscreenOnly()
            runCatching { cb?.onCustomViewHidden() }
            WebViewFullscreenHost.applyFullScreenWindowState(
                activity = activity,
                hostView = hostView,
                isFullScreen = false,
                originalOrientation = initialOrientation,
                originalCutoutMode = initialCutoutMode
            )
            webViewInstance = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isFullScreen) {
                    Modifier.background(Color.Black)
                } else {
                    Modifier.background(gradients.appBackdrop)
                }
            )
    ) {
        // Player Header Bar (hidden in Full-Screen mode)
        if (!isFullScreen) {
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
                        onClick = {
                            latestOnProgressUpdate(trackedWatchedSeconds, trackedDurationSeconds)
                            onBack()
                        },
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = media.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (media.mediaType == "tv") colors.softRose else colors.gold,
                            fontWeight = if (media.mediaType == "tv") FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    if (onDownloadClick != null) {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.testTag("player_download_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Download",
                                tint = colors.gold
                            )
                        }
                    }

                    IconButton(
                        onClick = { setFullScreenMode(true) },
                        modifier = Modifier.testTag("player_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = stringResource(R.string.player_fullscreen_enter_desc),
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            if (rendererCrashed) {
                                rendererCrashed = false
                                isLoadingPage = true
                                webViewSlotKey += 1
                            } else if (!WebViewFullscreenHost.isDestroyed(webViewInstance)) {
                                webViewInstance?.reload()
                            }
                        },
                        modifier = Modifier.testTag("player_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Player",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            val targetUri = effectiveStreamUrl.ifBlank { WebTorrentPolicy.WEBTORRENT_BASE_URL }
                            runCatching {
                                val browserIntent = Intent(Intent.ACTION_VIEW, targetUri.toUri())
                                context.startActivity(browserIntent)
                            }
                        },
                        modifier = Modifier.testTag("player_open_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open in browser",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Video Player Surface (16:9 in normal mode, full-bleed fillMaxSize in Full-Screen mode)
        Box(
            modifier = if (isFullScreen) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            }
        ) {
            if (!rendererCrashed) {
            key(webViewSlotKey) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebViewFullscreenHost.trimHostMemoryForWebView(ctx)
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        WebViewFullscreenHost.configureVideoWebViewBasics(this)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadsImagesAutomatically = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString =
                                "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                if (rendererCrashed || WebViewFullscreenHost.isDestroyed(view)) return
                                isLoadingPage = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (rendererCrashed || WebViewFullscreenHost.isDestroyed(view)) return
                                isLoadingPage = false
                                if (url != "about:blank") {
                                    view?.evaluateJavascript(
                                        PlayerControlsPolicy.buildProgressMessageBridgeScript(),
                                        null
                                    )
                                    view?.evaluateJavascript(
                                        PlayerControlsPolicy.buildVolumeScript(volumeFraction, isMuted),
                                        null
                                    )
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val targetUrl = request?.url?.toString() ?: return false
                                val allow = PlayerControlsPolicy.shouldAllowNavigation(
                                    url = targetUrl,
                                    currentStreamUrl = latestStreamUrl,
                                    isForMainFrame = request.isForMainFrame,
                                    isRedirect = request.isRedirect
                                )
                                return !allow
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val targetUrl = request?.url?.toString() ?: return null
                                return if (PlayerControlsPolicy.shouldBlockPlayerSubresource(targetUrl)) {
                                    WebViewFullscreenHost.emptyBlockedResponse()
                                } else {
                                    null
                                }
                            }

                            @TargetApi(Build.VERSION_CODES.O)
                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                if (webViewInstance === view) {
                                    webViewInstance = null
                                }
                                val cb = customViewCallback
                                customViewCallback = null
                                detachCustomFullscreenOnly()
                                runCatching { cb?.onCustomViewHidden() }
                                isFullScreen = false
                                isLoadingPage = false
                                rendererCrashed = true
                                WebViewFullscreenHost.trimHostMemoryForWebView(view?.context ?: context)
                                WebViewFullscreenHost.safelyDestroyWebView(view, rendererAlreadyGone = true)
                                return true
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                if (rendererCrashed || WebViewFullscreenHost.isDestroyed(view)) return
                                if (newProgress >= 85) {
                                    isLoadingPage = false
                                }
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                if (view == null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                customViewCallback = null
                                detachCustomFullscreenOnly()

                                val targetRoot = (activity?.window?.decorView as? ViewGroup)
                                    ?: (hostView.rootView as? ViewGroup)
                                if (targetRoot != null) {
                                    WebViewFullscreenHost.applyFullScreenWindowState(
                                        activity = activity,
                                        hostView = hostView,
                                        isFullScreen = true,
                                        originalOrientation = initialOrientation,
                                        originalCutoutMode = initialCutoutMode
                                    )
                                    fullscreenHostContainer = WebViewFullscreenHost.attachCustomFullscreenView(
                                        targetRoot = targetRoot,
                                        context = ctx,
                                        customView = view,
                                        onExitClicked = { setFullScreenMode(false) }
                                    )
                                }
                                customFullscreenView = view
                                customViewCallback = callback
                                isFullScreen = true
                                showFullScreenHud = true
                                hudInteractionTick += 1
                            }

                            override fun onHideCustomView() {
                                customViewCallback = null
                                detachCustomFullscreenOnly()
                                isFullScreen = false
                            }
                        }
                        val jsBridge = object {
                            @JavascriptInterface
                            fun onPlaybackProgress(currentSec: Long, durationSec: Long) {
                                if (durationSec > 0L && currentSec >= 0L) {
                                    post {
                                        trackedWatchedSeconds = currentSec.coerceAtMost(durationSec)
                                        trackedDurationSeconds = durationSec
                                        latestOnProgressUpdate(trackedWatchedSeconds, trackedDurationSeconds)
                                    }
                                }
                            }
                        }
                        addJavascriptInterface(jsBridge, "CinetraPlayerBridge")
                        val wtBridge = object {
                            @JavascriptInterface
                            fun onTorrentStatus(
                                statusText: String,
                                peers: Int,
                                downloadSpeedBytes: Long,
                                uploadSpeedBytes: Long,
                                progressPercent: Int,
                                activeFileName: String,
                                errorMsg: String,
                                ready: Boolean
                            ) {
                                post {
                                    webTorrentTelemetry = WebTorrentTelemetry(
                                        statusText = statusText,
                                        peers = peers.coerceAtLeast(0),
                                        downloadSpeedBytes = downloadSpeedBytes.coerceAtLeast(0L),
                                        uploadSpeedBytes = uploadSpeedBytes.coerceAtLeast(0L),
                                        progressPercent = progressPercent.coerceIn(0, 100),
                                        activeFileName = activeFileName,
                                        errorMessage = errorMsg.takeIf { it.isNotBlank() },
                                        isReady = ready
                                    )
                                }
                            }

                            @JavascriptInterface
                            fun onTorrentFiles(filesJson: String) {
                                val parsed = WebTorrentPolicy.parseTorrentFilesJson(filesJson)
                                post {
                                    webTorrentFiles = parsed
                                }
                            }
                        }
                        addJavascriptInterface(wtBridge, "CinetraWebTorrentBridge")

                        when {
                            hasPlayableWebTorrentSource -> {
                                loadDataWithBaseURL(
                                    WebTorrentPolicy.WEBTORRENT_BASE_URL,
                                    WebTorrentPolicy.buildWebTorrentPlayerHtml(
                                        torrentId = effectiveStreamUrl,
                                        title = media.title,
                                        season = currentSeason,
                                        episode = currentEpisode,
                                        isTv = media.mediaType == "tv"
                                    ),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                            isWebTorrentMode -> {
                                isLoadingPage = false
                                loadUrl("about:blank")
                            }
                            PlayerControlsPolicy.isDirectMediaFileUrl(effectiveStreamUrl) -> {
                                loadDataWithBaseURL(
                                    effectiveStreamUrl,
                                    PlayerControlsPolicy.buildDirectVideoHtml(effectiveStreamUrl),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                            else -> {
                                loadUrl(effectiveStreamUrl)
                            }
                        }
                        tag = playerLoadKey
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    if (rendererCrashed || WebViewFullscreenHost.isDestroyed(webView)) {
                        return@AndroidView
                    }
                    if (webView.tag != playerLoadKey) {
                        webView.tag = playerLoadKey
                        runCatching {
                            webView.evaluateJavascript("window.__wtDestroy && window.__wtDestroy()", null)
                        }
                        webTorrentTelemetry = WebTorrentTelemetry()
                        webTorrentFiles = emptyList()
                        isPlaying = true
                        isLoadingPage = !isWebTorrentMode || hasPlayableWebTorrentSource
                        when {
                            hasPlayableWebTorrentSource -> {
                                webView.loadDataWithBaseURL(
                                    WebTorrentPolicy.WEBTORRENT_BASE_URL,
                                    WebTorrentPolicy.buildWebTorrentPlayerHtml(
                                        torrentId = effectiveStreamUrl,
                                        title = media.title,
                                        season = currentSeason,
                                        episode = currentEpisode,
                                        isTv = media.mediaType == "tv"
                                    ),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                            isWebTorrentMode -> {
                                isLoadingPage = false
                                webView.loadUrl("about:blank")
                            }
                            PlayerControlsPolicy.isDirectMediaFileUrl(effectiveStreamUrl) -> {
                                webView.loadDataWithBaseURL(
                                    effectiveStreamUrl,
                                    PlayerControlsPolicy.buildDirectVideoHtml(effectiveStreamUrl),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                            else -> {
                                webView.loadUrl(effectiveStreamUrl)
                            }
                        }
                    }
                },
                onRelease = { wv ->
                    if (webViewInstance === wv) {
                        webViewInstance = null
                    }
                    if (isWebTorrentMode && !rendererCrashed && !WebViewFullscreenHost.isDestroyed(wv)) {
                        runCatching {
                            wv.evaluateJavascript("window.__wtDestroy && window.__wtDestroy()", null)
                        }
                    }
                    WebViewFullscreenHost.safelyDestroyWebView(wv)
                }
            )
            }
            }

            if (rendererCrashed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Stream player stopped unexpectedly.",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    rendererCrashed = false
                                    isLoadingPage = true
                                    webViewSlotKey += 1
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                            ) {
                                Text("Reload Player", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val servers = PlayerServer.entries.filter { it != PlayerServer.WEBTORRENT }
                                    val idx = servers.indexOf(selectedServer)
                                    val nextServer = servers[(idx + 1) % servers.size]
                                    rendererCrashed = false
                                    isLoadingPage = true
                                    onServerSelect(nextServer)
                                }
                            ) {
                                Text("Switch Server", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (isWebTorrentMode && !hasPlayableWebTorrentSource) {
                WebTorrentIdleStage(
                    onLaunchDemo = {
                        manualTorrentUri = WebTorrentPolicy.SAMPLE_SINTEL_MAGNET
                    }
                )
            }

            // Non-blocking corner exit-fullscreen control when in app full-screen mode
            if (isFullScreen && customFullscreenView == null) {
                val pillAlpha by animateFloatAsState(
                    targetValue = if (showFullScreenHud) 0.92f else 0.42f,
                    animationSpec = tween(350),
                    label = "fullscreen_exit_alpha"
                )
                IconButton(
                    onClick = { setFullScreenMode(false) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(14.dp)
                        .size(48.dp)
                        .alpha(pillAlpha)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.68f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .testTag("player_exit_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = stringResource(R.string.player_fullscreen_exit_desc),
                        tint = Color.White
                    )
                }
            }

            if (isLoadingPage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = colors.softRose,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Connecting to ${selectedServer.displayName}...",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Seek visual feedback pill (-10s / +10s)
            SeekFeedbackBadge(
                feedbackText = seekFeedbackText,
                modifier = Modifier.align(Alignment.Center)
            )
        }


        // Normal Mode: WebTorrent Deck (when active) + Servers + Episodes
        if (!isFullScreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            ) {
                if (isWebTorrentMode) {
                    WebTorrentControlDeck(
                        activeTorrentSource = if (hasPlayableWebTorrentSource) effectiveStreamUrl else "",
                        telemetry = webTorrentTelemetry,
                        torrentFiles = webTorrentFiles,
                        onStartTorrent = { newSource ->
                            manualTorrentUri = newSource.trim()
                        },
                        onSelectTorrentFile = { fileIndex ->
                            webViewInstance?.evaluateJavascript(
                                "window.__wtSelectFile && window.__wtSelectFile($fileIndex)",
                                null
                            )
                        },
                        onClearManualTorrent = {
                            manualTorrentUri = ""
                        },
                        canClearManualTorrent = manualTorrentUri.isNotBlank()
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }

                // Server Switcher Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = colors.softRose,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Streaming Server (Switch if video fails to load)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerServer.entries
                        .filter { it != PlayerServer.WEBTORRENT || selectedServer == PlayerServer.WEBTORRENT }
                        .forEach { server ->
                        val isSelected = selectedServer == server
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                rendererCrashed = false
                                isLoadingPage = true
                                onServerSelect(server)
                            },
                            label = {
                                Column {
                                    Text(
                                        text = server.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = server.speed,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else colors.textSecondary
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentMid,
                                selectedLabelColor = Color.White,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textPrimary
                            ),
                            modifier = Modifier.testTag("server_chip_${server.name}")
                        )
                    }
                }

                // TV Series / Anime Season & Episode Selector (Auto-loaded from TMDB)
                if (media.mediaType == "tv") {
                    val seasonList = remember(media.id, media.seasons, media.numberOfSeasons, media.episodesPerSeason) {
                        media.resolvedSeasonList()
                    }
                    val currentSeasonEpisodes = episodesBySeason[currentSeason].orEmpty()
                    val maxEpisodesInSeason = maxOf(
                        currentSeasonEpisodes.size,
                        media.episodeCountForSeason(currentSeason)
                    )
                    val activeEpisodeInfo = remember(currentSeasonEpisodes, currentEpisode) {
                        currentSeasonEpisodes.firstOrNull { it.episodeNumber == currentEpisode }
                    }
                    val episodeRowState = rememberLazyListState()

                    LaunchedEffect(currentSeason, currentEpisode, maxEpisodesInSeason) {
                        val targetIndex = (currentEpisode - 1).coerceIn(0, (maxEpisodesInSeason - 1).coerceAtLeast(0))
                        episodeRowState.animateScrollToItem(targetIndex)
                    }

                    Spacer(modifier = Modifier.size(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Episodes (Season $currentSeason • $maxEpisodesInSeason Eps)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            if (activeEpisodeInfo != null) {
                                Text(
                                    text = "EP $currentEpisode: ${activeEpisodeInfo.name}",
                                    color = colors.softRose,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    if (currentEpisode > 1) {
                                        onEpisodeSelect(currentSeason, currentEpisode - 1)
                                    }
                                },
                                enabled = currentEpisode > 1,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("prev_episode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Episode",
                                    tint = if (currentEpisode > 1) colors.textPrimary else colors.textMuted
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (currentEpisode < maxEpisodesInSeason) {
                                        onEpisodeSelect(currentSeason, currentEpisode + 1)
                                    }
                                },
                                enabled = currentEpisode < maxEpisodesInSeason,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("next_episode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Episode",
                                    tint = if (currentEpisode < maxEpisodesInSeason) colors.textPrimary else colors.textMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    if (seasonList.size > 1 || media.seasons.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            seasonList.forEach { seasonInfo ->
                                val sNum = seasonInfo.seasonNumber
                                val isSeasonSelected = sNum == currentSeason
                                val labelText = if (sNum == 0) {
                                    "Specials (${seasonInfo.episodeCount})"
                                } else {
                                    "Season $sNum (${seasonInfo.episodeCount})"
                                }
                                FilterChip(
                                    selected = isSeasonSelected,
                                    onClick = { onEpisodeSelect(sNum, 1) },
                                    label = { Text(labelText, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.accentMid,
                                        selectedLabelColor = Color.White,
                                        containerColor = colors.surfaceElevated,
                                        labelColor = colors.textPrimary
                                    ),
                                    modifier = Modifier.testTag("player_season_chip_$sNum")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                    }

                    val episodeNumbers = remember(maxEpisodesInSeason) { (1..maxEpisodesInSeason).toList() }
                    LazyRow(
                        state = episodeRowState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_episodes_lazy_row"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = episodeNumbers,
                            key = { it },
                            contentType = { "player_episode_pill" }
                        ) { ep ->
                            val isEpSelected = ep == currentEpisode
                            if (isEpSelected) {
                                CineGradientButton(
                                    onClick = { onEpisodeSelect(currentSeason, ep) },
                                    shape = RoundedCornerShape(8.dp),
                                    minHeight = 40.dp,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("episode_button_$ep")
                                ) {
                                    Text(
                                        text = "EP $ep",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onEpisodeSelect(currentSeason, ep) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.surfaceElevated,
                                        contentColor = colors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("episode_button_$ep")
                                ) {
                                    Text(
                                        text = "EP $ep",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(18.dp))

                // Information Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (isWebTorrentMode) "P2P Stream Source:" else "Source Stream Link:",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        Text(
                            text = effectiveStreamUrl.ifBlank { "Awaiting magnet:? or .torrent link" },
                            fontSize = 12.sp,
                            color = colors.gold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (isWebTorrentMode) {
                                "Cinetra streams magnet:? and .torrent links peer-to-peer directly in the player."
                            } else {
                                "Cinetra embeds Vidly Pro and backup streaming providers. If ads appear in the embed, use an ad blocker or try server 2/3."
                            },
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                        if (onReportIssue != null) {
                            Spacer(modifier = Modifier.size(10.dp))
                            OutlinedButton(
                                onClick = onReportIssue,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("player_report_issue_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = "Report Stream Issue",
                                    tint = colors.softRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Report Broken Stream / Subtitle / Audio Issue",
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
