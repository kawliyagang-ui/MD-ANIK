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
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R

/**
 * Coordinates mobile full-screen video presentation on top of a Window's root ViewGroup
 * alongside landscape orientation, display-cutout edge-to-edge coverage, keep-screen-on,
 * and sticky immersive system bars.
 */
object WebViewFullscreenHost {

    fun configureVideoWebViewBasics(webView: WebView) {
        webView.setBackgroundColor(Color.BLACK)
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        runCatching {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
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
