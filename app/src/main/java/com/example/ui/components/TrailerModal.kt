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
import java.io.ByteArrayInputStream
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.R
import com.example.ui.theme.CineTheme

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
    val hostView = LocalView.current
    val activity = remember(context) { context.findHostActivity() }
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

    val embedUrl = youtubeKey?.let { "https://www.youtube.com/embed/$it?autoplay=1&playsinline=1&fs=1" }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webViewSlotKey by remember(youtubeKey) { mutableIntStateOf(0) }
    var rendererCrashed by remember(youtubeKey) { mutableStateOf(false) }
    var isWebLoading by remember(youtubeKey) { mutableStateOf(true) }
    var isFullScreen by remember { mutableStateOf(false) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

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

    DisposableEffect(Unit) {
        onDispose {
            val cb = customViewCallback
            customViewCallback = null
            detachCustomViewOnly()
            runCatching { cb?.onCustomViewHidden() }
            WebViewFullscreenHost.applyFullScreenWindowState(
                activity = activity,
                hostView = hostView,
                isFullScreen = false,
                originalOrientation = initialOrientation,
                originalCutoutMode = initialCutoutMode
            )
            webViewRef = null
        }
    }

    val colors = CineTheme.colors

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
                shape = if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFullScreen) Color.Black else colors.surface
                ),
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
                            tint = colors.gold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Official Trailer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (!embedUrl.isNullOrBlank() && !isLoadingTrailer) {
                            IconButton(
                                onClick = { isFullScreen = true },
                                modifier = Modifier.size(48.dp).testTag("trailer_fullscreen_button")
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
                            modifier = Modifier.size(48.dp).testTag("open_yt_app_button")
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
                            modifier = Modifier.size(48.dp).testTag("close_trailer_button")
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
                                    color = colors.softRose,
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
                                    tint = colors.gold,
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
                                                isWebLoading = false
                                                rendererCrashed = false
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
                                                    WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
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
                                                WebViewFullscreenHost.trimHostMemoryForWebView(view?.context ?: context)
                                                WebViewFullscreenHost.safelyDestroyWebView(view)
                                                rendererCrashed = true
                                                return true
                                            }
                                        }
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
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
                                    if (!rendererCrashed) {
                                        runCatching { wv.loadUrl("about:blank") }
                                    }
                                    WebViewFullscreenHost.safelyDestroyWebView(wv)
                                }
                            )
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
                                    Button(
                                        onClick = {
                                            rendererCrashed = false
                                            isWebLoading = true
                                            webViewSlotKey += 1
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                                    ) {
                                        Text("Reload Trailer", color = Color.White)
                                    }
                                }
                            }

                            if (isFullScreen && customFullscreenView == null) {
                                IconButton(
                                    onClick = { exitFullScreen() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(14.dp)
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
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
                                        color = colors.softRose,
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

