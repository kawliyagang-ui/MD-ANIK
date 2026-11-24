package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.view.ViewGroup
import android.annotation.TargetApi
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.model.AdConfigEntity
import com.example.data.model.AdGatePolicy
import androidx.compose.foundation.layout.PaddingValues
import com.example.ui.theme.CineTheme
import kotlinx.coroutines.delay
import java.util.Locale

private enum class AdGateStage {
    PROMPT,
    VIEWING_AD
}

@Composable
fun AdGateOverlay(
    config: AdConfigEntity,
    onAdCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalStaySeconds = remember(config.requiredStaySeconds) {
        config.requiredStaySeconds.coerceAtLeast(1)
    }
    var stage by remember(config.id, config.updatedAt) { mutableStateOf(AdGateStage.PROMPT) }
    var remainingSeconds by remember(config.id, totalStaySeconds) {
        mutableIntStateOf(totalStaySeconds)
    }
    var hasTriggeredUnlock by remember(config.id, config.updatedAt) { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppResumed by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppResumed = true
                Lifecycle.Event.ON_PAUSE -> isAppResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Countdown runs ONLY while user remains on the Ad Page and app is active in foreground
    LaunchedEffect(stage, isAppResumed, totalStaySeconds) {
        if (stage == AdGateStage.VIEWING_AD && isAppResumed && !hasTriggeredUnlock) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds -= 1
            }
            if (remainingSeconds <= 0 && !hasTriggeredUnlock) {
                hasTriggeredUnlock = true
                onAdCompleted()
            }
        }
    }

    // Prevent hardware back from bypassing the ad gate into the main app
    BackHandler(enabled = true) {
        if (stage == AdGateStage.VIEWING_AD) {
            stage = AdGateStage.PROMPT
            remainingSeconds = totalStaySeconds
        }
    }

    val colors = CineTheme.colors

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
        },
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.systemBars),
        label = "ad_gate_stage_transition"
    ) { currentStage ->
        when (currentStage) {
            AdGateStage.PROMPT -> {
                AdPopupPromptScreen(
                    config = config,
                    totalStaySeconds = totalStaySeconds,
                    onClickAd = {
                        remainingSeconds = totalStaySeconds
                        hasTriggeredUnlock = false
                        stage = AdGateStage.VIEWING_AD
                    }
                )
            }
            AdGateStage.VIEWING_AD -> {
                AdsterraAdPageScreen(
                    config = config,
                    remainingSeconds = remainingSeconds,
                    totalStaySeconds = totalStaySeconds,
                    onBackToPrompt = {
                        stage = AdGateStage.PROMPT
                        remainingSeconds = totalStaySeconds
                    }
                )
            }
        }
    }
}

@Composable
private fun AdPopupPromptScreen(
    config: AdConfigEntity,
    totalStaySeconds: Int,
    onClickAd: () -> Unit
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradients.adGateGlow)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                .testTag("ad_gate_popup_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Sponsored & Cooldown Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(gradients.softBadge)
                            .border(1.dp, colors.softRose.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.ad_gate_sponsored_badge),
                            color = colors.gold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.ad_gate_frequency_badge, config.cooldownHours),
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cinetra Brand Emblem
                Image(
                    painter = painterResource(id = R.drawable.ic_cinetra_logo),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = config.headline.ifBlank { stringResource(id = R.string.ad_gate_default_headline) },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = config.description.ifBlank {
                        stringResource(
                            id = R.string.ad_gate_default_description,
                            totalStaySeconds,
                            config.cooldownHours
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Sponsor Ad Banner Preview (Clicking this opens the Ad Page)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(gradients.adBannerSurface)
                        .border(1.dp, colors.softRose.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onClickAd)
                        .padding(16.dp)
                        .testTag("ad_gate_banner_preview")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(colors.accentEnd.copy(alpha = 0.28f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdsClick,
                                contentDescription = null,
                                tint = colors.gold,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${config.adNetwork} • ${config.adFormat}",
                                color = colors.gold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(id = R.string.ad_gate_tap_banner_title),
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(id = R.string.ad_gate_stay_requirement, totalStaySeconds),
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary CTA Button
                CineGradientButton(
                    onClick = onClickAd,
                    shape = RoundedCornerShape(12.dp),
                    fillWidth = true,
                    minHeight = 52.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("ad_gate_click_ad_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${config.ctaText.ifBlank { "Open Sponsor Ad" }} (${totalStaySeconds}s)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = colors.success,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.ad_gate_unlock_note, config.cooldownHours),
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AdsterraAdPageScreen(
    config: AdConfigEntity,
    remainingSeconds: Int,
    totalStaySeconds: Int,
    onBackToPrompt: () -> Unit
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pendingChildWebView by remember { mutableStateOf<WebView?>(null) }
    var webViewSlotKey by remember { mutableIntStateOf(0) }
    var rendererCrashCount by remember(config.adUrl, config.popUnderScript) { mutableIntStateOf(0) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var pendingRendererRecovery by remember { mutableStateOf(false) }
    var pendingForegroundAutoRecover by remember { mutableStateOf(false) }
    var isPageLoading by remember { mutableStateOf(true) }
    var currentPageTitle by remember { mutableStateOf("${config.adNetwork} ${config.adFormat}") }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewRef, rendererCrashed, pendingRendererRecovery) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (pendingRendererRecovery && !rendererCrashed) {
                        pendingRendererRecovery = false
                        isPageLoading = true
                        webViewSlotKey += 1
                    } else if (!rendererCrashed && !WebViewFullscreenHost.isDestroyed(webViewRef)) {
                        runCatching { webViewRef?.onResume() }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (!rendererCrashed && !WebViewFullscreenHost.isDestroyed(webViewRef)) {
                        runCatching { webViewRef?.onPause() }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(pendingForegroundAutoRecover) {
        if (pendingForegroundAutoRecover) {
            delay(450L)
            pendingForegroundAutoRecover = false
            if (!rendererCrashed && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                isPageLoading = true
                webViewSlotKey += 1
            } else if (!rendererCrashed) {
                pendingRendererRecovery = true
            }
        }
    }

    val progressFraction by animateFloatAsState(
        targetValue = ((totalStaySeconds - remainingSeconds).coerceIn(0, totalStaySeconds)).toFloat() /
            totalStaySeconds.coerceAtLeast(1).toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "ad_stay_progress"
    )

    val formattedTimer = remember(remainingSeconds) {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("ad_gate_page_screen")
    ) {
        // Top Ad Page Header with Live 30s Countdown Timer
        Surface(
            color = colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToPrompt,
                        modifier = Modifier.testTag("ad_page_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.ad_page_back_desc),
                            tint = colors.textPrimary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = currentPageTitle,
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (remainingSeconds > 0) {
                                stringResource(id = R.string.ad_page_stay_instruction, remainingSeconds)
                            } else {
                                stringResource(id = R.string.ad_page_unlocked)
                            },
                            color = if (remainingSeconds > 0) colors.textSecondary else colors.success,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Countdown Timer Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .then(
                                if (remainingSeconds > 0) {
                                    Modifier.background(gradients.softBadge)
                                } else {
                                    Modifier.background(colors.success.copy(alpha = 0.2f))
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (remainingSeconds > 0) colors.softRose.copy(alpha = 0.7f) else colors.success,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("ad_gate_countdown_timer")
                    ) {
                        Icon(
                            imageVector = if (remainingSeconds > 0) Icons.Default.Timer else Icons.Default.CheckCircle,
                            contentDescription = stringResource(id = R.string.ad_page_timer_desc),
                            tint = if (remainingSeconds > 0) colors.gold else colors.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedTimer,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = {
                            if (rendererCrashed || pendingRendererRecovery || pendingForegroundAutoRecover) {
                                rendererCrashed = false
                                pendingRendererRecovery = false
                                pendingForegroundAutoRecover = false
                                rendererCrashCount = 0
                                isPageLoading = true
                                webViewSlotKey += 1
                            } else {
                                webViewRef?.reload()
                            }
                        },
                        modifier = Modifier.testTag("ad_page_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.ad_page_reload_desc),
                            tint = colors.textPrimary
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = if (remainingSeconds > 0) colors.gold else colors.success,
                    trackColor = colors.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .testTag("ad_gate_progress_bar")
                )
            }
        }

        // In-App Adsterra WebView Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(colors.background)
        ) {
            val contentKey = remember(config.adUrl, config.popUnderScript) {
                "${config.adUrl}|${config.popUnderScript}"
            }

            if (!rendererCrashed && !pendingRendererRecovery && !pendingForegroundAutoRecover) {
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
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(true)
                            loadsImagesAutomatically = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isPageLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isPageLoading = false
                                val title = view?.title?.takeIf { it.isNotBlank() && it != "about:blank" }
                                if (title != null) {
                                    currentPageTitle = title
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val target = request?.url?.toString() ?: return false
                                // Keep HTTP/HTTPS browsing inside the in-app Ad Page so 30s stay is tracked cleanly
                                return !(target.startsWith("http://", ignoreCase = true) ||
                                    target.startsWith("https://", ignoreCase = true))
                            }

                            @TargetApi(Build.VERSION_CODES.O)
                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                val crashedBySite = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                    detail?.didCrash() == true
                                if (webViewRef === view) {
                                    webViewRef = null
                                }
                                WebViewFullscreenHost.safelyDestroyWebView(pendingChildWebView)
                                pendingChildWebView = null
                                isPageLoading = false
                                WebViewFullscreenHost.trimHostMemoryForWebView(view?.context)
                                WebViewFullscreenHost.safelyDestroyWebView(view)
                                if (!crashedBySite && rendererCrashCount < 1) {
                                    rendererCrashCount += 1
                                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                        pendingForegroundAutoRecover = true
                                    } else {
                                        pendingRendererRecovery = true
                                    }
                                } else {
                                    rendererCrashed = true
                                }
                                return true
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                val directTarget = view?.hitTestResult?.extra
                                if (!directTarget.isNullOrBlank() &&
                                    (directTarget.startsWith("http://", ignoreCase = true) ||
                                        directTarget.startsWith("https://", ignoreCase = true))
                                ) {
                                    this@apply.loadUrl(directTarget)
                                    return false
                                }
                                // Capture Adsterra pop-under window.open() and render inside this WebView
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                val parentWebView = this@apply
                                WebViewFullscreenHost.safelyDestroyWebView(pendingChildWebView)
                                val childWebView = WebView(ctx).apply {
                                    WebViewFullscreenHost.configureVideoWebViewBasics(this)
                                    settings.javaScriptEnabled = true
                                }
                                pendingChildWebView = childWebView
                                childWebView.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        childView: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val popUrl = request?.url?.toString()
                                        if (!popUrl.isNullOrBlank() &&
                                            (popUrl.startsWith("http://", ignoreCase = true) ||
                                                popUrl.startsWith("https://", ignoreCase = true)) &&
                                            !WebViewFullscreenHost.isDestroyed(parentWebView)
                                        ) {
                                            parentWebView.loadUrl(popUrl)
                                        }
                                        childWebView.postDelayed({
                                            if (pendingChildWebView === childWebView) {
                                                pendingChildWebView = null
                                            }
                                            WebViewFullscreenHost.safelyDestroyWebView(childWebView)
                                        }, 150L)
                                        return true
                                    }

                                    @TargetApi(Build.VERSION_CODES.O)
                                    override fun onRenderProcessGone(
                                        childView: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        if (pendingChildWebView === childView) {
                                            pendingChildWebView = null
                                        }
                                        WebViewFullscreenHost.safelyDestroyWebView(childView)
                                        return true
                                    }
                                }
                                childWebView.postDelayed({
                                    if (pendingChildWebView === childWebView) {
                                        pendingChildWebView = null
                                    }
                                    WebViewFullscreenHost.safelyDestroyWebView(childWebView)
                                }, 3000L)
                                transport.webView = childWebView
                                resultMsg.sendToTarget()
                                return true
                            }
                        }

                        loadAdsterraContent(this, config)
                        tag = contentKey
                        webViewRef = this
                    }
                },
                update = { webView ->
                    if (rendererCrashed || WebViewFullscreenHost.isDestroyed(webView)) {
                        return@AndroidView
                    }
                    if (webView.tag != contentKey) {
                        webView.tag = contentKey
                        loadAdsterraContent(webView, config)
                    }
                },
                onRelease = { wv ->
                    if (webViewRef === wv) {
                        webViewRef = null
                    }
                    WebViewFullscreenHost.safelyDestroyWebView(pendingChildWebView)
                    pendingChildWebView = null
                    if (!rendererCrashed && !WebViewFullscreenHost.isDestroyed(wv)) {
                        runCatching { wv.loadUrl("about:blank") }
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
                        .background(colors.background.copy(alpha = 0.92f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Sponsor page stopped unexpectedly.",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                rendererCrashed = false
                                pendingRendererRecovery = false
                                pendingForegroundAutoRecover = false
                                rendererCrashCount = 0
                                isPageLoading = true
                                webViewSlotKey += 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Reload Page", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isPageLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = colors.softRose,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(id = R.string.ad_page_loading),
                            color = colors.textPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun loadAdsterraContent(webView: WebView, config: AdConfigEntity) {
    val script = config.popUnderScript.trim()
    val url = AdGatePolicy.normalizeUrl(config.adUrl)
    if (script.isNotBlank()) {
        val html = AdGatePolicy.buildAdsterraPopUnderHtml(
            popUnderScript = script,
            adUrl = url
        )
        webView.loadDataWithBaseURL("https://cinetra.app/", html, "text/html", "UTF-8", null)
    } else if (url.isNotBlank()) {
        webView.loadUrl(url)
    }
}
