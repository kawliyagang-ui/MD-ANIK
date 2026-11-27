package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.annotation.TargetApi
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.CinetraTheme
import com.example.ui.tv.TvDevicePolicy
import kotlinx.coroutines.delay

private tailrec fun Context.findHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findHostActivity()
        else -> null
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrailerModal(
    youtubeKey: String?,
    isLoadingTrailer: Boolean = false,
    title: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isTvDevice = remember(context) { TvDevicePolicy.isTvDevice(context) }
    val closeFocusRequester = remember { FocusRequester() }
    val hostView = LocalView.current
    val activity = remember(context) { context.findHostActivity() }
    LaunchedEffect(isTvDevice) {
        if (isTvDevice) {
            delay(60L)
            runCatching { closeFocusRequester.requestFocus() }
        }
    }
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

    val embedUrl = youtubeKey?.let {
        "https://www.youtube-nocookie.com/embed/$it?autoplay=1&playsinline=1&fs=1&rel=0&modestbranding=1"
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webViewSlotKey by remember(youtubeKey) { mutableIntStateOf(0) }
    var rendererCrashed by remember(youtubeKey) { mutableStateOf(false) }
    var isWebLoading by remember(youtubeKey) { mutableStateOf(true) }
    var isFullScreen by remember { mutableStateOf(false) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewRef, rendererCrashed) {
        val observer = LifecycleEventObserver { _, event ->
            val activeWebView = webViewRef
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

    val detachCustomViewOnly: () -> Unit = {
        val c = fullscreenContainer
        val v = customFullscreenView
        fullscreenContainer = null
        customFullscreenView = null
        WebViewFullscreenHost.detachCustomFullscreenView(c, v)
    }

    val exitFullScreen: () -> Unit = {
        val cb = customViewCallback
        customViewCallback = null
        detachCustomViewOnly()
        runCatching { cb?.onCustomViewHidden() }
        isFullScreen = false
    }

    DisposableEffect(isFullScreen, activity, hostView, isTvDevice) {
        if (!isTvDevice) {
            WebViewFullscreenHost.applyFullScreenWindowState(
                activity = activity,
                hostView = hostView,
                isFullScreen = isFullScreen,
                originalOrientation = initialOrientation,
                originalCutoutMode = initialCutoutMode
            )
        }
        onDispose {
            if (isFullScreen && !isTvDevice) {
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

    DisposableEffect(isTvDevice) {
        onDispose {
            val cb = customViewCallback
            customViewCallback = null
            detachCustomViewOnly()
            runCatching { cb?.onCustomViewHidden() }
            if (!isTvDevice) {
                WebViewFullscreenHost.applyFullScreenWindowState(
                    activity = activity,
                    hostView = hostView,
                    isFullScreen = false,
                    originalOrientation = initialOrientation,
                    originalCutoutMode = initialCutoutMode
                )
            }
            webViewRef = null
        }
    }

    val colors = CinetraTheme.colors

    Dialog(
        onDismissRequest = {
            if (isFullScreen || customFullscreenView != null) {
                exitFullScreen()
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler {
            if (isFullScreen || customFullscreenView != null) {
                exitFullScreen()
            } else {
                onDismiss()
            }
        }

        val rootBackdropModifier = if (isFullScreen) {
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        } else {
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        }

        Box(
            modifier = rootBackdropModifier,
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = if (isFullScreen) RoundedCornerShape(0.dp) else MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (isFullScreen) Color.Black else colors.surface
                ),
                border = if (isFullScreen) null else androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = (if (isFullScreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                }).testTag("trailer_dialog")
            ) {
            Column(modifier = if (isFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
                // Header
                if (!isFullScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Official Trailer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        var fullscreenBtnFocused by remember { mutableStateOf(false) }
                        var ytBtnFocused by remember { mutableStateOf(false) }
                        var closeBtnFocused by remember { mutableStateOf(false) }
                        if (!embedUrl.isNullOrBlank() && !isLoadingTrailer) {
                            IconButton(
                                onClick = { isFullScreen = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .onFocusChanged { fullscreenBtnFocused = it.isFocused }
                                    .then(
                                        if (fullscreenBtnFocused) {
                                            Modifier
                                                .border(2.dp, Color.White, CircleShape)
                                                .background(colors.primary.copy(alpha = 0.35f), CircleShape)
                                        } else Modifier
                                    )
                                    .testTag("trailer_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = stringResource(R.string.player_fullscreen_enter_desc),
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val ytUri = if (!youtubeKey.isNullOrBlank()) {
                                    "https://www.youtube.com/watch?v=$youtubeKey"
                                } else {
                                    "https://www.youtube.com/results?search_query=${Uri.encode("$title official trailer")}"
                                }
                                val ytIntent = Intent(Intent.ACTION_VIEW, ytUri.toUri())
                                context.startActivity(ytIntent)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .onFocusChanged { ytBtnFocused = it.isFocused }
                                .then(
                                    if (ytBtnFocused) {
                                        Modifier
                                            .border(2.dp, Color.White, CircleShape)
                                            .background(colors.primary.copy(alpha = 0.35f), CircleShape)
                                    } else Modifier
                                )
                                .testTag("open_yt_app_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in YouTube",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .focusRequester(closeFocusRequester)
                                .onFocusChanged { closeBtnFocused = it.isFocused }
                                .then(
                                    if (closeBtnFocused) {
                                        Modifier
                                            .border(2.dp, Color.White, CircleShape)
                                            .background(colors.primary.copy(alpha = 0.35f), CircleShape)
                                    } else Modifier
                                )
                                .testTag("close_trailer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Video iframe or loading / fallback
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
                    },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoadingTrailer -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = colors.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Fetching official trailer...",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        embedUrl.isNullOrBlank() -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No embedded trailer found. Tap the YouTube icon above to search YouTube.",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            val dialogRootView = LocalView.current
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
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            javaScriptCanOpenWindowsAutomatically = false
                                            setSupportMultipleWindows(false)
                                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                if (rendererCrashed || WebViewFullscreenHost.isDestroyed(view)) return
                                                isWebLoading = false
                                            }

                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): Boolean {
                                                val url = request?.url?.toString() ?: return false
                                                return !PlayerControlsPolicy.shouldAllowNavigation(
                                                    url = url,
                                                    currentStreamUrl = embedUrl,
                                                    isForMainFrame = request.isForMainFrame,
                                                    isRedirect = request.isRedirect
                                                )
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
                                                if (webViewRef === view) {
                                                    webViewRef = null
                                                }
                                                val cb = customViewCallback
                                                customViewCallback = null
                                                detachCustomViewOnly()
                                                runCatching { cb?.onCustomViewHidden() }
                                                isFullScreen = false
                                                isWebLoading = false
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
                                                    isWebLoading = false
                                                }
                                            }

                                            override fun onShowCustomView(
                                                view: View?,
                                                callback: CustomViewCallback?
                                            ) {
                                                if (view == null) {
                                                    callback?.onCustomViewHidden()
                                                    return
                                                }
                                                customViewCallback = null
                                                detachCustomViewOnly()
                                                val targetRoot = (dialogRootView.rootView as? ViewGroup)
                                                    ?: (activity?.window?.decorView as? ViewGroup)
                                                if (targetRoot != null) {
                                                    WebViewFullscreenHost.applyFullScreenWindowState(
                                                        activity = activity,
                                                        hostView = dialogRootView,
                                                        isFullScreen = true,
                                                        originalOrientation = initialOrientation,
                                                        originalCutoutMode = initialCutoutMode
                                                    )
                                                    fullscreenContainer = WebViewFullscreenHost.attachCustomFullscreenView(
                                                        targetRoot = targetRoot,
                                                        context = ctx,
                                                        customView = view,
                                                        onExitClicked = { exitFullScreen() }
                                                    )
                                                }
                                                customFullscreenView = view
                                                customViewCallback = callback
                                                isFullScreen = true
                                            }

                                            override fun onHideCustomView() {
                                                customViewCallback = null
                                                detachCustomViewOnly()
                                                isFullScreen = false
                                            }
                                        }
                                        loadUrl(embedUrl)
                                        tag = embedUrl
                                        webViewRef = this
                                    }
                                },
                                update = { webView ->
                                    if (rendererCrashed || WebViewFullscreenHost.isDestroyed(webView)) {
                                        return@AndroidView
                                    }
                                    if (webView.tag != embedUrl) {
                                        webView.tag = embedUrl
                                        isWebLoading = true
                                        webView.loadUrl(embedUrl)
                                    }
                                },
                                onRelease = { wv ->
                                    if (webViewRef === wv) {
                                        webViewRef = null
                                    }
                                    WebViewFullscreenHost.safelyDestroyWebView(wv)
                                }
                            )
                            }
                            }

                            if (rendererCrashed) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.85f))
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                ) {
                                    Text(
                                        text = "Trailer player interrupted.",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    RedButton(
                                        onClick = {
                                            rendererCrashed = false
                                            isWebLoading = true
                                            webViewSlotKey += 1
                                        }
                                    ) {
                                        Text("Reload Trailer", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (isFullScreen && customFullscreenView == null) {
                                var exitFocus by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { exitFullScreen() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(14.dp)
                                        .size(48.dp)
                                        .onFocusChanged { exitFocus = it.isFocused }
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                                        .then(
                                            if (exitFocus) Modifier.border(2.dp, Color.White, RoundedCornerShape(24.dp))
                                            else Modifier
                                        )
                                        .testTag("trailer_exit_fullscreen_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = stringResource(R.string.player_fullscreen_exit_desc),
                                        tint = Color.White
                                    )
                                }
                            }

                            if (isWebLoading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = colors.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Title label at bottom
                if (!isFullScreen) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        }
    }
}

