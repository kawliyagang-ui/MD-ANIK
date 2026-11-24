package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.imageLoader
import com.example.R
import java.io.ByteArrayInputStream
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinates mobile full-screen video presentation on top of a Window's root ViewGroup
 * alongside landscape orientation, display-cutout edge-to-edge coverage, keep-screen-on,
 * sticky immersive system bars, and low-memory WebView lifecycle management.
 */
object WebViewFullscreenHost {

    private val swFilterInstalled = AtomicBoolean(false)
    private val destroyedWebViews = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<WebView, Boolean>())
    )

    fun trimHostMemoryForWebView(context: Context?) {
        val appContext = context?.applicationContext ?: return
        runCatching {
            appContext.imageLoader.memoryCache?.clear()
        }
    }

    fun emptyBlockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            403,
            "Blocked",
            mapOf("Access-Control-Allow-Origin" to "*"),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun isDestroyed(webView: WebView?): Boolean {
        return webView == null || destroyedWebViews.contains(webView)
    }

    private fun installServiceWorkerFilterOnce() {
        if (!swFilterInstalled.compareAndSet(false, true)) return
        runCatching {
            val controller = ServiceWorkerController.getInstance()
            controller.serviceWorkerWebSettings.apply {
                allowContentAccess = false
                allowFileAccess = false
            }
            controller.setServiceWorkerClient(object : ServiceWorkerClient() {
                override fun shouldInterceptRequest(request: WebResourceRequest?): WebResourceResponse? {
                    val targetUrl = request?.url?.toString() ?: return null
                    return if (PlayerControlsPolicy.shouldBlockServiceWorkerRequest(targetUrl)) {
                        emptyBlockedResponse()
                    } else {
                        null
                    }
                }
            })
        }
    }

    @Suppress("DEPRECATION")
    fun configureVideoWebViewBasics(webView: WebView) {
        installServiceWorkerFilterOnce()
        webView.setBackgroundColor(Color.BLACK)
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        // Use default accelerated compositing without allocating an extra offscreen hardware FBO layer
        webView.setLayerType(View.LAYER_TYPE_NONE, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
            }
        }
        runCatching {
            webView.settings.apply {
                allowFileAccess = false
                allowContentAccess = false
                databaseEnabled = false
                setGeolocationEnabled(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    offscreenPreRaster = false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = false
                }
            }
        }
        runCatching {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }
    }

    fun safelyDestroyWebView(webView: WebView?) {
        if (webView == null) return
        if (!destroyedWebViews.add(webView)) return
        runCatching {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.removeJavascriptInterface("CinetraPlayerBridge")
            webView.removeJavascriptInterface("CinetraWebTorrentBridge")
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.onPause()
            webView.stopLoading()
            webView.clearCache(false)
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
    }

    fun applyFullScreenWindowState(
        activity: Activity?,
        hostView: View,
        isFullScreen: Boolean,
        originalOrientation: Int,
        originalCutoutMode: Int?
    ) {
        val window = activity?.window
        if (isFullScreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val attrs = window.attributes
                    attrs.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    window.attributes = attrs
                }
                WindowCompat.getInsetsController(window, hostView).apply {
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        } else {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && originalCutoutMode != null) {
                    val attrs = window.attributes
                    attrs.layoutInDisplayCutoutMode = originalCutoutMode
                    window.attributes = attrs
                }
                WindowCompat.getInsetsController(window, hostView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    fun attachCustomFullscreenView(
        targetRoot: ViewGroup,
        context: Context,
        customView: View,
        onExitClicked: () -> Unit
    ): FrameLayout {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density + 0.5f).toInt()

        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        (customView.parent as? ViewGroup)?.removeView(customView)
        container.addView(
            customView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val pillBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(175, 16, 16, 22))
            setStroke(dp(1), Color.argb(110, 255, 255, 255))
        }

        val exitButton = ImageButton(context).apply {
            setImageResource(R.drawable.ic_exit_fullscreen)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = pillBackground
            contentDescription = context.getString(R.string.player_fullscreen_exit_desc)
            val pad = dp(11)
            setPadding(pad, pad, pad, pad)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val outValue = TypedValue()
                context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackgroundBorderless,
                    outValue,
                    true
                )
                foreground = context.getDrawable(outValue.resourceId)
            }
            setOnClickListener { onExitClicked() }
        }

        val buttonSize = dp(48)
        val baseMargin = dp(14)
        val exitParams = FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.TOP or Gravity.END).apply {
            topMargin = baseMargin
            marginEnd = baseMargin
        }
        container.addView(exitButton, exitParams)

        ViewCompat.setOnApplyWindowInsetsListener(container) { _, insets ->
            val safeInsets = insets.getInsets(
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.systemBars()
            )
            val lp = exitButton.layoutParams as FrameLayout.LayoutParams
            lp.topMargin = baseMargin + safeInsets.top
            lp.marginEnd = baseMargin + safeInsets.right
            exitButton.layoutParams = lp
            insets
        }

        targetRoot.addView(
            container,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        container.bringToFront()
        ViewCompat.requestApplyInsets(container)
        return container
    }

    fun detachCustomFullscreenView(container: FrameLayout?, customView: View?) {
        (customView?.parent as? ViewGroup)?.removeView(customView)
        container?.removeAllViews()
        (container?.parent as? ViewGroup)?.removeView(container)
    }
}
