package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferences
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaItem
import com.example.data.repository.MovieRepository
import com.example.ui.components.AdGateOverlay
import com.example.ui.components.CineEase
import com.example.ui.components.FloatingGlassBar
import com.example.ui.components.LocalDownloadedMediaIds
import com.example.ui.components.MovieBoxDownloadSheet
import com.example.ui.components.StreamPlayerView
import com.example.ui.components.TrailerModal
import com.example.ui.screens.CategoryListScreen
import com.example.ui.screens.ContinueWatchingScreen
import com.example.ui.screens.DetailsScreen
import com.example.ui.screens.DirectMediaPlayerScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LanguageCatalogScreen
import com.example.ui.screens.OfflineVideoPlayerScreen
import com.example.ui.screens.ReportProblemScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.CinetraTheme
import com.example.ui.theme.CinetraTokens
import com.example.ui.theme.LocalBottomBarClearance
import com.example.ui.tv.CinetraTvShell
import com.example.ui.tv.TvDevicePolicy
import com.example.ui.viewmodel.AdGateState
import com.example.ui.viewmodel.MovieViewModel
import com.example.ui.viewmodel.MovieViewModelFactory
import com.example.ui.viewmodel.Screen
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CinetraApp()
        }
    }
}

@Composable
fun CinetraApp() {
    val context = LocalContext.current
    val isTvDevice = remember(context) { TvDevicePolicy.isTvDevice(context) }
    val repository = remember(context) {
        val database = AppDatabase.getDatabase(context.applicationContext)
        MovieRepository(database.mediaDao(), context.applicationContext)
    }
    val userPreferences = remember(context) {
        UserPreferences(context.applicationContext)
    }
    val appVersionName = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        }.getOrDefault("1.0")
    }

    val movieViewModel: MovieViewModel = viewModel(
        factory = MovieViewModelFactory(repository, userPreferences, context.applicationContext)
    )

    val userState by movieViewModel.uiState.collectAsState()
    val adGateState by movieViewModel.adGateState.collectAsState()
    val watchlist by movieViewModel.watchlist.collectAsState()
    val continueWatching by movieViewModel.continueWatching.collectAsState()
    val activeAnnouncements by movieViewModel.activeAnnouncements.collectAsState()
    val searchHistory by movieViewModel.searchHistory.collectAsState()
    val issueReports by movieViewModel.issueReports.collectAsState()
    val downloads by movieViewModel.downloads.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userState.downloadFeedbackMessage) {
        val msg = userState.downloadFeedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            movieViewModel.dismissDownloadFeedback()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                movieViewModel.refreshFromCloud()
                movieViewModel.refreshDailyFeedsIfDateChanged()
                movieViewModel.refreshAdGateCheck()
                movieViewModel.syncDownloadsNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CinetraTheme(
        palette = userState.selectedPalette,
        themeMode = userState.themeMode,
        accentColor = userState.accentColor
    ) {
        val colors = CinetraTheme.colors
        val gradients = CinetraTheme.gradients

        when (val gate = adGateState) {
            is AdGateState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.primary)
                }
                return@CinetraTheme
            }
            is AdGateState.GateRequired -> {
                AdGateOverlay(
                    config = gate.config,
                    onAdCompleted = { movieViewModel.completeAdGate() }
                )
                return@CinetraTheme
            }
            is AdGateState.Unlocked -> Unit
        }

        if (isTvDevice) {
            CinetraTvShell(
                movieViewModel = movieViewModel,
                snackbarHostState = snackbarHostState
            )
            return@CinetraTheme
        }

        val watchlistIds = remember(watchlist) { watchlist.map { it.id }.toSet() }
        val runningDownloadsCount = remember(downloads) {
            downloads.count { it.parsedStatus == DownloadStatus.RUNNING }
        }
        val completedDownloadsCount = remember(downloads) {
            downloads.count { it.parsedStatus == DownloadStatus.COMPLETED }
        }
        val downloadBadgeScale = remember { Animatable(1f) }
        val previousCompletedDownloadsCount = remember { mutableIntStateOf(-1) }
        LaunchedEffect(completedDownloadsCount) {
            val prev = previousCompletedDownloadsCount.intValue
            previousCompletedDownloadsCount.intValue = completedDownloadsCount
            if (prev in 0 until completedDownloadsCount) {
                downloadBadgeScale.animateTo(
                    targetValue = 1.18f,
                    animationSpec = tween(durationMillis = 140, easing = CineEase)
                )
                downloadBadgeScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 180, easing = CineEase)
                )
            }
        }
        val downloadedMediaIds = remember(downloads) {
            downloads.filter {
                it.parsedStatus == DownloadStatus.COMPLETED &&
                    it.localFilePath.isNotBlank() &&
                    File(it.localFilePath).exists()
            }.map { it.mediaId }.toSet()
        }
        val isPlayerScreen = userState.currentScreen is Screen.Player ||
            userState.currentScreen is Screen.DirectPlayer ||
            userState.currentScreen is Screen.OfflinePlayer
        val isDedicatedSubPage = userState.currentScreen is Screen.Details ||
            userState.currentScreen is Screen.Search ||
            userState.currentScreen is Screen.CategoryList ||
            userState.currentScreen is Screen.LanguageCatalog ||
            userState.currentScreen is Screen.Player ||
            userState.currentScreen is Screen.DirectPlayer ||
            userState.currentScreen is Screen.OfflinePlayer ||
            userState.currentScreen is Screen.ReportProblem

        CompositionLocalProvider(LocalDownloadedMediaIds provides downloadedMediaIds) {

        BackHandler(
            enabled = userState.backStack.size > 1 &&
                !isDedicatedSubPage &&
                !userState.isTrailerModalOpen &&
                !userState.downloadSheet.isOpen
        ) {
            movieViewModel.navigateBack()
        }

        val railItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = colors.primary,
            selectedTextColor = colors.primary,
            indicatorColor = colors.primaryContainer,
            unselectedIconColor = colors.textMuted,
            unselectedTextColor = colors.textMuted
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp
            val bottomBarClearance = if (!isDedicatedSubPage && !useNavigationRail) 96.dp else 0.dp

            CompositionLocalProvider(LocalBottomBarClearance provides bottomBarClearance) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = colors.background,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(bottom = bottomBarClearance)
                    )
                },
                contentWindowInsets = when {
                    isPlayerScreen -> WindowInsets(0, 0, 0, 0)
                    userState.currentScreen is Screen.Details -> WindowInsets.navigationBars
                    isDedicatedSubPage -> WindowInsets.systemBars
                    else -> WindowInsets.statusBars
                },
            topBar = {
                AnimatedVisibility(
                    visible = !isDedicatedSubPage,
                    enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    Surface(
                        color = colors.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(top = 12.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Clean bold white CINETRA Wordmark
                                    Box(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "CINETRA",
                                            color = colors.textPrimary,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.4.sp
                                            )
                                        )
                                    }

                                    // Search shortcut
                                    IconButton(
                                        onClick = { movieViewModel.openSearch() },
                                        modifier = Modifier.testTag("topbar_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = colors.textPrimary
                                        )
                                    }

                                    // Downloads shortcut (badge only when active)
                                    IconButton(
                                        onClick = { movieViewModel.openDownloadsScreen() },
                                        modifier = Modifier.testTag("topbar_downloads_button")
                                    ) {
                                        BadgedBox(
                                            modifier = Modifier.graphicsLayer {
                                                val s = downloadBadgeScale.value
                                                scaleX = s
                                                scaleY = s
                                            },
                                            badge = {
                                                if (runningDownloadsCount > 0) {
                                                    Badge(
                                                        containerColor = colors.primary,
                                                        contentColor = colors.onPrimary
                                                    ) {
                                                        Text(
                                                            text = "$runningDownloadsCount",
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (userState.currentScreen is Screen.Downloads) Icons.Filled.Download else Icons.Outlined.Download,
                                                contentDescription = "Downloads",
                                                tint = if (userState.currentScreen is Screen.Downloads) colors.primary else colors.textPrimary
                                            )
                                        }
                                    }

                                    // Watchlist shortcut
                                    IconButton(
                                        onClick = { movieViewModel.navigateTo(Screen.Watchlist) },
                                        modifier = Modifier.testTag("topbar_watchlist_button")
                                    ) {
                                        Icon(
                                            imageVector = if (userState.currentScreen is Screen.Watchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = "Watchlist",
                                            tint = if (userState.currentScreen is Screen.Watchlist) colors.primary else colors.textPrimary
                                        )
                                    }

                                    // Settings shortcut
                                    IconButton(
                                        onClick = { movieViewModel.navigateTo(Screen.Settings) },
                                        modifier = Modifier.testTag("topbar_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = if (userState.currentScreen is Screen.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                            contentDescription = "Settings",
                                            tint = if (userState.currentScreen is Screen.Settings) colors.primary else colors.textPrimary
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.border.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && !isDedicatedSubPage) {
                    NavigationRail(
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = userState.currentScreen is Screen.Home,
                            onClick = { movieViewModel.navigateTo(Screen.Home) },
                            icon = {
                                Icon(
                                    imageVector = if (userState.currentScreen is Screen.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home", fontSize = 11.sp, maxLines = 1) },
                            colors = railItemColors,
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationRailItem(
                            selected = userState.currentScreen is Screen.Explore,
                            onClick = { movieViewModel.navigateTo(Screen.Explore) },
                            icon = {
                                Icon(
                                    imageVector = if (userState.currentScreen is Screen.Explore) Icons.Filled.Search else Icons.Outlined.Search,
                                    contentDescription = "Explore"
                                )
                            },
                            label = { Text("Explore", fontSize = 11.sp, maxLines = 1) },
                            colors = railItemColors,
                            modifier = Modifier.testTag("nav_explore")
                        )
                        NavigationRailItem(
                            selected = userState.currentScreen is Screen.Watchlist,
                            onClick = { movieViewModel.navigateTo(Screen.Watchlist) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (watchlist.isNotEmpty()) {
                                            Badge(containerColor = colors.primary, contentColor = colors.onPrimary) {
                                                Text("${watchlist.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (userState.currentScreen is Screen.Watchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Watchlist"
                                    )
                                }
                            },
                            label = { Text("Saved", fontSize = 11.sp, maxLines = 1) },
                            colors = railItemColors,
                            modifier = Modifier.testTag("nav_watchlist")
                        )
                        NavigationRailItem(
                            selected = userState.currentScreen is Screen.ContinueWatching,
                            onClick = { movieViewModel.navigateTo(Screen.ContinueWatching) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (continueWatching.isNotEmpty()) {
                                            Badge(containerColor = colors.surfaceHover, contentColor = colors.textPrimary) {
                                                Text("${continueWatching.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (userState.currentScreen is Screen.ContinueWatching) Icons.Filled.History else Icons.Outlined.History,
                                        contentDescription = "History"
                                    )
                                }
                            },
                            label = { Text("History", fontSize = 11.sp, maxLines = 1) },
                            colors = railItemColors,
                            modifier = Modifier.testTag("nav_history")
                        )
                        NavigationRailItem(
                            selected = userState.currentScreen is Screen.Downloads,
                            onClick = { movieViewModel.openDownloadsScreen() },
                            icon = {
                                BadgedBox(
                                    modifier = Modifier.graphicsLayer {
                                        val s = downloadBadgeScale.value
                                        scaleX = s
                                        scaleY = s
                                    },
                                    badge = {
                                        if (runningDownloadsCount > 0) {
                                            Badge(containerColor = colors.primary, contentColor = colors.onPrimary) {
                                                Text("$runningDownloadsCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (userState.currentScreen is Screen.Downloads) Icons.Filled.Download else Icons.Outlined.Download,
                                        contentDescription = "Downloads"
                                    )
                                }
                            },
                            label = { Text("Downloads", fontSize = 11.sp, maxLines = 1) },
                            colors = railItemColors,
                            modifier = Modifier.testTag("nav_downloads")
                        )
                        NavigationRailItem(
                            selected = userState.currentScreen is Screen.Settings,
                            onClick = { movieViewModel.navigateTo(Screen.Settings) },
                            icon = {
                                Icon(
                                    imageVector = if (userState.currentScreen is Screen.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings", fontSize = 11.sp, maxLines = 1) },
                            colors = railItemColors,
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = userState.currentScreen,
                        transitionSpec = {
                            val isSecondaryDestination = targetState is Screen.Details ||
                                targetState is Screen.Search ||
                                targetState is Screen.CategoryList ||
                                targetState is Screen.LanguageCatalog ||
                                targetState is Screen.Player ||
                                targetState is Screen.DirectPlayer ||
                                targetState is Screen.OfflinePlayer ||
                                targetState is Screen.ReportProblem
                            val wasSecondaryOrigin = initialState is Screen.Details ||
                                initialState is Screen.Search ||
                                initialState is Screen.CategoryList ||
                                initialState is Screen.LanguageCatalog ||
                                initialState is Screen.Player ||
                                initialState is Screen.DirectPlayer ||
                                initialState is Screen.OfflinePlayer ||
                                initialState is Screen.ReportProblem

                            when {
                                targetState is Screen.Player || initialState is Screen.Player ||
                                    targetState is Screen.DirectPlayer || initialState is Screen.DirectPlayer ||
                                    targetState is Screen.OfflinePlayer || initialState is Screen.OfflinePlayer -> {
                                    // Keep native video/WebView surfaces un-scaled and non-overlapping during transitions
                                    EnterTransition.None togetherWith ExitTransition.None
                                }
                                userState.isNavigatingBack -> {
                                    (slideInHorizontally(
                                        animationSpec = tween(260, easing = CineEase),
                                        initialOffsetX = { -it / 6 }
                                    ) + fadeIn(tween(250, easing = CineEase))) togetherWith
                                        (slideOutHorizontally(
                                            animationSpec = tween(240, easing = CineEase),
                                            targetOffsetX = { it / 5 }
                                        ) + fadeOut(tween(190, easing = CineEase)))
                                }
                                isSecondaryDestination || wasSecondaryOrigin -> {
                                    (slideInHorizontally(
                                        animationSpec = tween(270, easing = CineEase),
                                        initialOffsetX = { it / 6 }
                                    ) + fadeIn(tween(250, easing = CineEase))) togetherWith
                                        (slideOutHorizontally(
                                            animationSpec = tween(240, easing = CineEase),
                                            targetOffsetX = { -it / 7 }
                                        ) + fadeOut(tween(190, easing = CineEase)))
                                }
                                else -> {
                                    fadeIn(tween(220, easing = CineEase)) togetherWith
                                        fadeOut(tween(160, easing = CineEase))
                                }
                            }
                        },
                        contentKey = { screen ->
                            when (screen) {
                                Screen.Home -> "home"
                                Screen.Explore -> "explore"
                                Screen.Watchlist -> "watchlist"
                                Screen.Downloads -> "downloads"
                                Screen.ContinueWatching -> "continue_watching"
                                Screen.Settings -> "settings"
                                Screen.Search -> "search"
                                is Screen.ReportProblem -> "report_${screen.prefilledMedia?.id.orEmpty()}_${screen.serverName}"
                                is Screen.CategoryList -> "category_${screen.categoryKey}_${screen.genreFilter}_${screen.title}"
                                is Screen.LanguageCatalog -> "language_${screen.languageCode}"
                                is Screen.Details -> "details_${screen.media.id}"
                                is Screen.Player -> "player_${screen.media.id}"
                                is Screen.DirectPlayer -> "direct_player_${screen.media.id}_${screen.season}_${screen.episode}"
                                is Screen.OfflinePlayer -> "offline_player_${screen.download.id}"
                            }
                        },
                        label = "cinetra_screen_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { screen ->
                        val combinedCatalog = remember(
                            userState.trending,
                            userState.trendingMovies,
                            userState.trendingTv,
                            userState.newReleases,
                            userState.popularMovies,
                            userState.upcomingMovies,
                            userState.topRatedMovies,
                            userState.topRatedTv,
                            userState.customMedia
                        ) {
                            (
                                userState.trending +
                                    userState.trendingMovies +
                                    userState.trendingTv +
                                    userState.newReleases +
                                    userState.popularMovies +
                                    userState.upcomingMovies +
                                    userState.topRatedMovies +
                                    userState.topRatedTv +
                                    userState.customMedia
                                ).distinctBy { it.id }
                        }

                        when (screen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    trending = userState.trending,
                                    popularMovies = userState.popularMovies,
                                    topRatedTv = userState.topRatedTv,
                                    customMedia = userState.customMedia,
                                    continueWatching = continueWatching,
                                    announcements = activeAnnouncements,
                                    isLoading = userState.isLoading,
                                    errorMessage = userState.errorMessage,
                                    onRetry = { movieViewModel.loadHomeData() },
                                    selectedHomeGenre = userState.selectedHomeGenre,
                                    onHomeGenreSelect = { movieViewModel.selectHomeGenre(it) },
                                    onBrowseGenreInExplore = { movieViewModel.openExploreWithGenre(it) },
                                    isItemInWatchlist = { it in watchlistIds },
                                    onMediaClick = { movieViewModel.openDetails(it) },
                                    onWatchClick = { movieViewModel.openPlayer(it) },
                                    onWatchlistToggle = { movieViewModel.toggleWatchlist(it) },
                                    onTrailerClick = { movieViewModel.openTrailer(it) },
                                    onResumeWatching = { media, s, ep -> movieViewModel.openPlayer(media, s, ep) },
                                    onSearchClick = { movieViewModel.openSearch() },
                                    trendingTotalResults = userState.trendingTotalResults,
                                    popularMoviesTotalResults = userState.popularMoviesTotalResults,
                                    topRatedTvTotalResults = userState.topRatedTvTotalResults,
                                    trendingMovies = userState.trendingMovies,
                                    trendingMoviesTotalResults = userState.trendingMoviesTotalResults,
                                    trendingTv = userState.trendingTv,
                                    trendingTvTotalResults = userState.trendingTvTotalResults,
                                    newReleases = userState.newReleases,
                                    newReleasesTotalResults = userState.newReleasesTotalResults,
                                    upcomingMovies = userState.upcomingMovies,
                                    upcomingMoviesTotalResults = userState.upcomingMoviesTotalResults,
                                    topRatedMovies = userState.topRatedMovies,
                                    topRatedMoviesTotalResults = userState.topRatedMoviesTotalResults,
                                    recommendedForYou = userState.recommendedForYou,
                                    becauseYouWatchedTitle = userState.becauseYouWatchedTitle,
                                    becauseYouWatchedItems = userState.becauseYouWatchedItems,
                                    popularInGenreName = userState.popularInGenreName,
                                    popularInGenreItems = userState.popularInGenreItems,
                                    homeLatestReleased = userState.homeLatestReleased,
                                    homeBollywoodMovies = userState.homeBollywoodMovies,
                                    homeSouthIndianMovies = userState.homeSouthIndianMovies,
                                    homeBanglaMixed = userState.homeBanglaMixed,
                                    homeHollywoodMovies = userState.homeHollywoodMovies,
                                    homeKDramaSeries = userState.homeKDramaSeries,
                                    homeCDramaSeries = userState.homeCDramaSeries,
                                    homeUpcomingSoonest = userState.homeUpcomingSoonest,
                                    onLanguageClick = { code, name -> movieViewModel.openLanguageCatalog(code, name) },
                                    onSeeAllClick = { section ->
                                        when (section) {
                                            "Latest" -> movieViewModel.openCategoryList("Latest Releases", "Latest")
                                            "Continue Watching" -> movieViewModel.navigateTo(Screen.ContinueWatching)
                                            "HotMovies" -> movieViewModel.openCategoryList("Hot Movies", "HotMovies")
                                            "HotSeries" -> movieViewModel.openCategoryList("Hot Series", "HotSeries")
                                            "Bollywood" -> movieViewModel.openLanguageCatalog("hi", "Bollywood")
                                            "SouthIndian" -> movieViewModel.openCategoryList("South Indian", "SouthIndian")
                                            "Bangla" -> movieViewModel.openLanguageCatalog("bn", "Bangla")
                                            "Hollywood" -> movieViewModel.openLanguageCatalog("en", "Hollywood")
                                            "KDrama" -> movieViewModel.openLanguageCatalog("ko", "Original K-Drama")
                                            "CDrama" -> movieViewModel.openLanguageCatalog("zh", "Original C-Drama")
                                            "UpcomingSoonest" -> movieViewModel.openCategoryList("Upcoming", "UpcomingSoonest")
                                            "NewReleases" -> movieViewModel.openCategoryList("New Releases • Daily", "NewReleases")
                                            "Trending" -> {
                                                val activeGenre = userState.selectedHomeGenre
                                                val title = if (activeGenre.equals("All", ignoreCase = true)) {
                                                    "Trending Movies & Series"
                                                } else {
                                                    "Trending • $activeGenre"
                                                }
                                                movieViewModel.openCategoryList(title, "Trending", activeGenre)
                                            }
                                            "TrendingMovies" -> movieViewModel.openCategoryList("Trending Movies", "TrendingMovies")
                                            "TrendingTv" -> movieViewModel.openCategoryList("Trending TV Shows", "TrendingTv")
                                            "Top10Movies" -> movieViewModel.openCategoryList("Top 10 Movies", "Top10Movies")
                                            "Top10Tv" -> movieViewModel.openCategoryList("Top 10 TV Series", "Top10Tv")
                                            "Movies" -> movieViewModel.openCategoryList("Popular Movies", "Movies")
                                            "UpcomingMovies" -> movieViewModel.openCategoryList("Upcoming Movies & Series", "UpcomingMovies")
                                            "TopRatedMovies" -> movieViewModel.openCategoryList("Top Rated Movies", "TopRatedMovies")
                                            "TV" -> movieViewModel.openCategoryList("Top-Rated TV Series", "TV")
                                            "Specials" -> movieViewModel.openCategoryList("Cinetra Specials & Admin Picks", "Specials")
                                            else -> movieViewModel.openCategoryList(section, "All")
                                        }
                                    }
                                )
                            }
                            is Screen.Explore -> {
                                val isExploreSearching = userState.searchQuery.isNotBlank()
                                val exploreTotalCount = remember(
                                    isExploreSearching,
                                    userState.searchTotalResults,
                                    userState.selectedCategory,
                                    userState.popularMoviesTotalResults,
                                    userState.topRatedTvTotalResults,
                                    userState.trendingTotalResults,
                                    userState.customMedia,
                                    combinedCatalog.size
                                ) {
                                    if (isExploreSearching) {
                                        userState.searchTotalResults
                                    } else {
                                        com.example.data.model.CatalogBatchPolicy.resolveExploreTotalCount(
                                            selectedCategory = userState.selectedCategory,
                                            moviesTotalResults = userState.popularMoviesTotalResults,
                                            tvTotalResults = userState.topRatedTvTotalResults,
                                            trendingTotalResults = userState.trendingTotalResults,
                                            customMedia = userState.customMedia,
                                            shownCount = combinedCatalog.size
                                        )
                                    }
                                }
                                val exploreHasMore = if (isExploreSearching) {
                                    userState.hasMoreSearch
                                } else {
                                    when (userState.selectedCategory) {
                                        "movie" -> userState.hasMorePopularMovies
                                        "tv" -> userState.hasMoreTopRatedTv
                                        else -> userState.hasMorePopularMovies || userState.hasMoreTopRatedTv
                                    }
                                }
                                val exploreIsLoadingMore = if (isExploreSearching) {
                                    userState.isLoadingMoreSearch
                                } else {
                                    when (userState.selectedCategory) {
                                        "movie" -> userState.isLoadingMoreMovies
                                        "tv" -> userState.isLoadingMoreTv
                                        else -> userState.isLoadingMoreMovies || userState.isLoadingMoreTv
                                    }
                                }
                                val exploreTargetCategory = when (userState.selectedCategory) {
                                    "movie" -> "Movies"
                                    "tv" -> "TV"
                                    else -> "All"
                                }
                                ExploreScreen(
                                    searchQuery = userState.searchQuery,
                                    onSearchQueryChange = { movieViewModel.onSearchQueryChange(it) },
                                    selectedCategory = userState.selectedCategory,
                                    onCategorySelect = { movieViewModel.selectCategory(it) },
                                    selectedGenre = userState.selectedGenre,
                                    onGenreSelect = { movieViewModel.selectGenre(it) },
                                    sortBy = userState.sortBy,
                                    onSortSelect = { movieViewModel.selectSortBy(it) },
                                    allTitles = combinedCatalog,
                                    searchResults = userState.searchResults,
                                    isSearching = userState.isSearching,
                                    isLoadingCatalog = userState.isLoading,
                                    onMediaClick = { movieViewModel.openDetails(it) },
                                    searchHistory = searchHistory,
                                    onDeleteSearchHistory = { movieViewModel.deleteSearchHistory(it) },
                                    onClearSearchHistory = { movieViewModel.clearSearchHistory() },
                                    searchErrorMessage = userState.searchErrorMessage,
                                    onRetrySearch = { movieViewModel.onSearchQueryChange(userState.searchQuery) },
                                    onOpenSearch = { movieViewModel.openSearch() },
                                    totalAvailableCount = exploreTotalCount,
                                    batchSize = userState.batchSize,
                                    hasMore = exploreHasMore,
                                    isLoadingMore = exploreIsLoadingMore,
                                    onBatchSizeChange = { movieViewModel.setBatchSize(it) },
                                    onLoadMore = {
                                        if (isExploreSearching) {
                                            movieViewModel.loadMoreSearchResults()
                                        } else {
                                            movieViewModel.loadMoreCategory(exploreTargetCategory)
                                        }
                                    },
                                    onResetFilters = { movieViewModel.resetAllExploreFilters() },
                                    discoverFilters = userState.discoverFilters,
                                    discoverResults = userState.discoverResults,
                                    discoverTotalResults = userState.discoverTotalResults,
                                    isLoadingDiscover = userState.isLoadingDiscover,
                                    isLoadingMoreDiscover = userState.isLoadingMoreDiscover,
                                    hasMoreDiscover = userState.hasMoreDiscover,
                                    onApplyDiscoverFilters = { movieViewModel.applyDiscoverFilters(it) },
                                    onLoadMoreDiscover = { movieViewModel.loadMoreDiscover() }
                                )
                            }
                            is Screen.Search -> {
                                SearchScreen(
                                    searchQuery = userState.searchQuery,
                                    onSearchQueryChange = { movieViewModel.onSearchQueryChange(it) },
                                    searchResults = userState.searchResults,
                                    isSearching = userState.isSearching,
                                    searchHistory = searchHistory,
                                    onDeleteSearchHistory = { movieViewModel.deleteSearchHistory(it) },
                                    onClearSearchHistory = { movieViewModel.clearSearchHistory() },
                                    searchErrorMessage = userState.searchErrorMessage,
                                    onRetrySearch = { movieViewModel.onSearchQueryChange(userState.searchQuery) },
                                    onMediaClick = { movieViewModel.openDetails(it) },
                                    onBack = { movieViewModel.navigateBack() },
                                    totalResultsCount = userState.searchTotalResults,
                                    batchSize = userState.batchSize,
                                    hasMore = userState.hasMoreSearch,
                                    isLoadingMore = userState.isLoadingMoreSearch,
                                    onBatchSizeChange = { movieViewModel.setBatchSize(it) },
                                    onLoadMore = { movieViewModel.loadMoreSearchResults() },
                                    searchSuggestions = userState.searchSuggestions,
                                    isFetchingSuggestions = userState.isFetchingSuggestions,
                                    showSuggestions = userState.showSearchSuggestions,
                                    onSubmitSearch = { movieViewModel.submitSearchQuery(it) },
                                    onSuggestionMediaClick = { movieViewModel.selectSearchSuggestion(it) },
                                    onSuggestionFillQuery = { movieViewModel.applySearchSuggestionQuery(it) },
                                    onDismissSuggestions = { movieViewModel.dismissSearchSuggestions() },
                                    onSearchFieldFocusWithQuery = { movieViewModel.showSearchSuggestionsIfAvailable() }
                                )
                            }
                            is Screen.CategoryList -> {
                                val categoryItems = remember(
                                    screen.categoryKey,
                                    screen.genreFilter,
                                    userState.trending,
                                    userState.trendingMovies,
                                    userState.trendingTv,
                                    userState.newReleases,
                                    userState.popularMovies,
                                    userState.upcomingMovies,
                                    userState.topRatedMovies,
                                    userState.topRatedTv,
                                    userState.customMedia,
                                    userState.homeLatestReleased,
                                    userState.homeSouthIndianMovies,
                                    userState.homeUpcomingSoonest
                                ) {
                                    val rawList = when (screen.categoryKey) {
                                        "Latest" -> userState.homeLatestReleased
                                        "HotMovies" -> userState.trendingMovies.ifEmpty { userState.popularMovies }.take(10)
                                        "HotSeries" -> userState.trendingTv.ifEmpty { userState.topRatedTv }
                                        "SouthIndian" -> userState.homeSouthIndianMovies
                                        "UpcomingSoonest" -> userState.homeUpcomingSoonest
                                        "Trending" -> userState.trending
                                        "TrendingMovies" -> userState.trendingMovies
                                        "TrendingTv" -> userState.trendingTv
                                        "NewReleases" -> userState.newReleases
                                        "Top10Movies" -> userState.trendingMovies.ifEmpty { userState.popularMovies }.take(10)
                                        "Top10Tv" -> userState.trendingTv.ifEmpty { userState.topRatedTv }.take(10)
                                        "Movies" -> userState.popularMovies
                                        "UpcomingMovies" -> userState.upcomingMovies
                                        "TopRatedMovies" -> userState.topRatedMovies
                                        "TV" -> userState.topRatedTv
                                        "Specials" -> userState.customMedia
                                        else -> (
                                            userState.trending +
                                                userState.trendingMovies +
                                                userState.trendingTv +
                                                userState.newReleases +
                                                userState.popularMovies +
                                                userState.upcomingMovies +
                                                userState.topRatedMovies +
                                                userState.topRatedTv +
                                                userState.customMedia
                                            ).distinctBy { it.id }
                                    }
                                    com.example.ui.components.HomeGenreCatalog.filterByGenre(rawList, screen.genreFilter)
                                }
                                val categoryTotalCount = when (screen.categoryKey) {
                                    "Trending" -> if (screen.genreFilter.equals("All", ignoreCase = true)) {
                                        maxOf(userState.trendingTotalResults, categoryItems.size)
                                    } else {
                                        categoryItems.size
                                    }
                                    "TrendingMovies" -> maxOf(userState.trendingMoviesTotalResults, categoryItems.size)
                                    "TrendingTv", "HotSeries" -> maxOf(userState.trendingTvTotalResults, categoryItems.size)
                                    "NewReleases" -> maxOf(userState.newReleasesTotalResults, categoryItems.size)
                                    "Latest", "HotMovies", "SouthIndian", "UpcomingSoonest", "Top10Movies", "Top10Tv", "Specials" -> categoryItems.size
                                    "Movies" -> maxOf(userState.popularMoviesTotalResults, categoryItems.size)
                                    "UpcomingMovies" -> maxOf(userState.upcomingMoviesTotalResults, categoryItems.size)
                                    "TopRatedMovies" -> maxOf(userState.topRatedMoviesTotalResults, categoryItems.size)
                                    "TV" -> maxOf(userState.topRatedTvTotalResults, categoryItems.size)
                                    else -> maxOf(
                                        userState.popularMoviesTotalResults + userState.topRatedTvTotalResults + userState.customMedia.size,
                                        categoryItems.size
                                    )
                                }
                                val categoryHasMore = when (screen.categoryKey) {
                                    "Trending" -> userState.hasMoreTrending
                                    "TrendingMovies" -> userState.hasMoreTrendingMovies
                                    "TrendingTv" -> userState.hasMoreTrendingTv
                                    "NewReleases" -> userState.hasMoreNewReleases
                                    "Latest", "HotMovies", "HotSeries", "SouthIndian", "UpcomingSoonest", "Top10Movies", "Top10Tv", "Specials" -> false
                                    "Movies" -> userState.hasMorePopularMovies
                                    "UpcomingMovies" -> userState.hasMoreUpcomingMovies
                                    "TopRatedMovies" -> userState.hasMoreTopRatedMovies
                                    "TV" -> userState.hasMoreTopRatedTv
                                    else -> userState.hasMorePopularMovies || userState.hasMoreTopRatedTv
                                }
                                val categoryIsLoadingMore = when (screen.categoryKey) {
                                    "Trending" -> userState.isLoadingMoreTrending
                                    "TrendingMovies" -> userState.isLoadingMoreTrendingMovies
                                    "TrendingTv" -> userState.isLoadingMoreTrendingTv
                                    "NewReleases" -> userState.isLoadingMoreNewReleases
                                    "Latest", "HotMovies", "HotSeries", "SouthIndian", "UpcomingSoonest", "Top10Movies", "Top10Tv", "Specials" -> false
                                    "Movies" -> userState.isLoadingMoreMovies
                                    "UpcomingMovies" -> userState.isLoadingMoreUpcomingMovies
                                    "TopRatedMovies" -> userState.isLoadingMoreTopRatedMovies
                                    "TV" -> userState.isLoadingMoreTv
                                    else -> userState.isLoadingMoreMovies || userState.isLoadingMoreTv
                                }
                                CategoryListScreen(
                                    title = screen.title,
                                    mediaItems = categoryItems,
                                    onMediaClick = { movieViewModel.openDetails(it) },
                                    onBack = { movieViewModel.navigateBack() },
                                    totalAvailableCount = categoryTotalCount,
                                    batchSize = userState.batchSize,
                                    hasMore = categoryHasMore,
                                    isLoadingMore = categoryIsLoadingMore,
                                    onBatchSizeChange = { movieViewModel.setBatchSize(it) },
                                    onLoadMore = { movieViewModel.loadMoreCategory(screen.categoryKey) }
                                )
                            }
                            is Screen.LanguageCatalog -> {
                                LanguageCatalogScreen(
                                    languageCode = screen.languageCode,
                                    languageName = screen.languageName,
                                    mediaItems = userState.languageCatalogItems,
                                    selectedMediaType = userState.languageCatalogMediaType,
                                    sortBy = userState.languageCatalogSortBy,
                                    isLoading = userState.isLoadingLanguageCatalog,
                                    errorMessage = userState.languageCatalogError,
                                    totalAvailableCount = userState.languageCatalogTotalResults,
                                    batchSize = userState.batchSize,
                                    hasMore = userState.hasMoreLanguageCatalog,
                                    isLoadingMore = userState.isLoadingMoreLanguageCatalog,
                                    onMediaTypeChange = { movieViewModel.setLanguageCatalogMediaType(it) },
                                    onSortByChange = { movieViewModel.setLanguageCatalogSortBy(it) },
                                    onMediaClick = { movieViewModel.openDetails(it) },
                                    onBatchSizeChange = { movieViewModel.setBatchSize(it) },
                                    onLoadMore = { movieViewModel.loadMoreLanguageCatalog() },
                                    onRetry = { movieViewModel.retryLanguageCatalog() },
                                    onBack = { movieViewModel.navigateBack() }
                                )
                            }
                            is Screen.Watchlist -> {
                                WatchlistScreen(
                                    watchlist = watchlist,
                                    onMediaClick = { movieViewModel.openDetails(it) },
                                    onWatchClick = { movieViewModel.openPlayer(it) },
                                    onRemoveFromWatchlist = { movieViewModel.toggleWatchlist(it) },
                                    onExploreClick = { movieViewModel.navigateTo(Screen.Explore) }
                                )
                            }
                            is Screen.ContinueWatching -> {
                                ContinueWatchingScreen(
                                    items = continueWatching,
                                    allCatalogItems = combinedCatalog,
                                    onResumeWatching = { media, season, ep -> movieViewModel.openPlayer(media, season, ep) },
                                    onMediaClick = { movieViewModel.openDetails(it) },
                                    onRemoveItem = { movieViewModel.removeContinueWatching(it) },
                                    onBrowseClick = { movieViewModel.navigateTo(Screen.Explore) }
                                )
                            }
                            is Screen.Settings -> {
                                SettingsScreen(
                                    selectedPalette = userState.selectedPalette,
                                    defaultServer = userState.playerServer,
                                    watchlistCount = watchlist.size,
                                    continueWatchingCount = continueWatching.size,
                                    searchHistoryCount = searchHistory.size,
                                    appVersionName = appVersionName,
                                    onToggleHighReadability = { movieViewModel.toggleHighReadabilityPalette() },
                                    onSelectPalette = { movieViewModel.selectDarkThemePalette(it) },
                                    onSelectDefaultServer = { movieViewModel.setDefaultPlayerServer(it) },
                                    onClearSearchHistory = { movieViewModel.clearSearchHistory() },
                                    themeMode = userState.themeMode,
                                    accentColor = userState.accentColor,
                                    appLanguage = userState.appLanguage,
                                    notificationPrefs = userState.notificationPrefs,
                                    cacheSizeLabel = userState.cacheSizeLabel,
                                    feedbackMessage = userState.settingsFeedbackMessage,
                                    onSelectThemeMode = { movieViewModel.selectThemeMode(it) },
                                    onSelectAccentColor = { movieViewModel.selectAccentColor(it) },
                                    onSelectAppLanguage = { movieViewModel.selectAppLanguage(it) },
                                    onUpdateNotifications = { enabled, ep, mov ->
                                        movieViewModel.updateNotificationPreferences(enabled, ep, mov)
                                    },
                                    onCheckReleasesNow = { movieViewModel.checkNewReleasesAndNotify(silentIfNoPermission = false) },
                                    onClearCache = { movieViewModel.clearAppCache() },
                                    onReportProblemClick = { movieViewModel.openReportProblem() },
                                    onLaunchMagnetStream = { torrentUri -> movieViewModel.openMagnetStream(torrentUri) },
                                    onDismissFeedback = { movieViewModel.dismissSettingsFeedback() },
                                    downloadsCount = downloads.size,
                                    downloadPreferences = userState.downloadPrefs,
                                    onOpenDownloads = { movieViewModel.openDownloadsScreen() },
                                    onSaveDownloadPreferences = { host, quality, wifiOnly ->
                                        movieViewModel.updateDownloadPreferences(host, quality, wifiOnly)
                                    },
                                    onTestMovieBoxHost = { host -> movieViewModel.testMovieBoxHostConnection(host) }
                                )
                            }
                            is Screen.Downloads -> {
                                DownloadsScreen(
                                    downloads = downloads,
                                    movieBoxSearchQuery = userState.movieBoxSearchQuery,
                                    movieBoxSearchResults = userState.movieBoxSearchResults,
                                    isSearchingMovieBox = userState.isSearchingMovieBox,
                                    movieBoxSearchError = userState.movieBoxSearchError,
                                    feedbackMessage = userState.downloadFeedbackMessage,
                                    movieBoxHostUrl = userState.downloadPrefs.movieBoxHostUrl,
                                    onSearchMovieBox = { movieViewModel.searchMovieBoxHub(it) },
                                    onOpenMovieBoxSubject = { subject, season, ep ->
                                        movieViewModel.openDownloadSheetForMovieBoxSubject(subject, season, ep)
                                    },
                                    onPlayOffline = { movieViewModel.openOfflinePlayer(it) },
                                    onExportToDevice = { movieViewModel.exportDownloadToPublicFolder(it) },
                                    onPauseDownload = { movieViewModel.pauseDownload(it) },
                                    onResumeDownload = { movieViewModel.resumeDownload(it) },
                                    onRetryDownload = { movieViewModel.retryDownload(it) },
                                    onCancelOrDelete = { movieViewModel.cancelOrDeleteDownload(it) },
                                    onRefreshStatus = { movieViewModel.syncDownloadsNow() },
                                    onDismissFeedback = { movieViewModel.dismissDownloadFeedback() },
                                    onBack = { movieViewModel.navigateBack() }
                                )
                            }
                            is Screen.ReportProblem -> {
                                ReportProblemScreen(
                                    prefilledMedia = screen.prefilledMedia,
                                    serverName = screen.serverName,
                                    episodeLabel = screen.episodeLabel,
                                    reportHistory = issueReports,
                                    isSubmitting = userState.isSubmittingReport,
                                    submitMessage = userState.reportSubmitMessage,
                                    isError = userState.isReportError,
                                    onSubmitReport = { category, description, media, server, epLabel, screenshot ->
                                        movieViewModel.submitProblemReport(
                                            category = category,
                                            description = description,
                                            media = media,
                                            serverName = server,
                                            episodeLabel = epLabel,
                                            screenshotBase64 = screenshot
                                        )
                                    },
                                    onClearFeedback = { movieViewModel.clearReportFeedback() },
                                    onBack = { movieViewModel.navigateBack() }
                                )
                            }
                            is Screen.Details -> {
                                val activeMedia = userState.selectedMedia?.takeIf { it.id == screen.media.id } ?: screen.media
                                val savedContinueItem = remember(continueWatching, activeMedia.id) {
                                    continueWatching.firstOrNull { it.id == activeMedia.id }
                                }
                                DetailsScreen(
                                    media = activeMedia,
                                    isInWatchlist = activeMedia.id in watchlistIds,
                                    episodesBySeason = userState.episodesByMediaAndSeason[activeMedia.id].orEmpty(),
                                    isLoadingSeasons = userState.isLoadingSeasons,
                                    loadingSeasonNumber = userState.loadingSeasonNumber,
                                    onSeasonSelected = { seasonNum -> movieViewModel.loadSeasonEpisodes(activeMedia, seasonNum) },
                                    continueWatchingItem = savedContinueItem,
                                    onWatchClick = { s, ep -> movieViewModel.openPlayer(activeMedia, s, ep) },
                                    onWatchlistToggle = { movieViewModel.toggleWatchlist(activeMedia) },
                                    onTrailerClick = { movieViewModel.openTrailer(activeMedia) },
                                    similarMedia = userState.similarForSelectedMedia,
                                    onSimilarMediaClick = { movieViewModel.openDetails(it) },
                                    onReportProblemClick = { movieViewModel.openReportProblem(media = activeMedia) },
                                    downloads = downloads,
                                    onDownloadClick = { s, ep, epTitle ->
                                        movieViewModel.startMovieOrEpisodeDownload(activeMedia, s, ep, epTitle)
                                    },
                                    onDownloadSeason = { s ->
                                        movieViewModel.enqueueSeasonBatchDownload(activeMedia, s)
                                    },
                                    onDownloadSelectedEpisodes = { s, selectedEps ->
                                        movieViewModel.enqueueSeasonBatchDownload(activeMedia, s, selectedEps)
                                    },
                                    onPauseDownload = { movieViewModel.pauseDownload(it) },
                                    onResumeDownload = { movieViewModel.resumeDownload(it) },
                                    onCancelDownload = { movieViewModel.cancelOrDeleteDownload(it) },
                                    onRetryDownload = { movieViewModel.retryDownload(it) },
                                    onPlayOffline = { movieViewModel.openOfflinePlayer(it) },
                                    onBack = { movieViewModel.navigateBack() }
                                )
                            }
                            is Screen.Player -> {
                                val activePlayerMedia = userState.playingMedia?.takeIf { it.id == screen.media.id } ?: screen.media
                                val savedProgress = remember(continueWatching, activePlayerMedia.id, userState.currentSeason, userState.currentEpisode) {
                                    continueWatching.firstOrNull {
                                        it.id == activePlayerMedia.id &&
                                            (activePlayerMedia.mediaType != "tv" ||
                                                (it.season == userState.currentSeason && it.episode == userState.currentEpisode))
                                    }
                                }
                                StreamPlayerView(
                                    media = activePlayerMedia,
                                    currentSeason = userState.currentSeason,
                                    currentEpisode = userState.currentEpisode,
                                    selectedServer = userState.playerServer,
                                    episodesBySeason = userState.episodesByMediaAndSeason[activePlayerMedia.id].orEmpty(),
                                    initialWatchedSeconds = savedProgress?.watchedSeconds ?: 0L,
                                    initialDurationSeconds = savedProgress?.durationSeconds ?: 0L,
                                    onProgressUpdate = { watchedSec, durationSec ->
                                        movieViewModel.updateWatchProgress(
                                            media = activePlayerMedia,
                                            season = userState.currentSeason,
                                            episode = userState.currentEpisode,
                                            watchedSeconds = watchedSec,
                                            durationSeconds = durationSec
                                        )
                                    },
                                    onServerSelect = { movieViewModel.switchPlayerServer(it) },
                                    onEpisodeSelect = { s, ep -> movieViewModel.switchEpisode(s, ep) },
                                    onDownloadClick = {
                                        val epTitle = userState.episodesByMediaAndSeason[activePlayerMedia.id]
                                            ?.get(userState.currentSeason)
                                            ?.firstOrNull { it.episodeNumber == userState.currentEpisode }
                                            ?.name
                                            .orEmpty()
                                        movieViewModel.openMovieBoxDownloadSheet(
                                            media = activePlayerMedia,
                                            season = userState.currentSeason,
                                            episode = userState.currentEpisode,
                                            episodeTitle = epTitle
                                        )
                                    },
                                    onReportIssue = {
                                        movieViewModel.openReportProblem(
                                            media = activePlayerMedia,
                                            serverName = userState.playerServer.displayName,
                                            episodeLabel = if (activePlayerMedia.mediaType == "tv") {
                                                "S${userState.currentSeason}:E${userState.currentEpisode}"
                                            } else ""
                                        )
                                    },
                                    onBack = { movieViewModel.navigateBack() }
                                )
                            }
                            is Screen.DirectPlayer -> {
                                val loadedSeasonEpisodes = userState.episodesByMediaAndSeason[screen.media.id]
                                    ?.get(screen.season)
                                    .orEmpty()
                                DirectMediaPlayerScreen(
                                    media = screen.media,
                                    season = screen.season,
                                    episode = screen.episode,
                                    sourceUrl = screen.sourceUrl,
                                    isLocalFile = screen.isLocalFile,
                                    downloadEntity = screen.downloadEntity,
                                    initialPositionMs = screen.initialPositionMs,
                                    loadedEpisodes = loadedSeasonEpisodes,
                                    onSwitchEpisode = if (screen.media.mediaType.equals("tv", ignoreCase = true)) {
                                        { targetEp ->
                                            movieViewModel.switchDirectEpisode(
                                                media = screen.media,
                                                season = screen.season,
                                                episode = targetEp.episodeNumber
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onSavePosition = { pos, dur ->
                                        movieViewModel.saveDirectPlaybackPosition(
                                            media = screen.media,
                                            season = screen.season,
                                            episode = screen.episode,
                                            positionMs = pos,
                                            durationMs = dur
                                        )
                                    },
                                    onFallbackToWebEmbed = if (!screen.isLocalFile) {
                                        { movieViewModel.openWebEmbedPlayer(screen.media, screen.season, screen.episode) }
                                    } else {
                                        null
                                    },
                                    onBack = { movieViewModel.navigateBack() }
                                )
                            }
                            is Screen.OfflinePlayer -> {
                                OfflineVideoPlayerScreen(
                                    download = screen.download,
                                    initialPositionMs = screen.initialPositionMs,
                                    onSavePosition = { pos, dur ->
                                        val offlineMedia = MediaItem(
                                            id = screen.download.mediaId,
                                            tmdbId = screen.download.tmdbId,
                                            title = screen.download.title,
                                            overview = "",
                                            posterPath = screen.download.posterPath,
                                            backdropPath = screen.download.backdropPath,
                                            mediaType = screen.download.mediaType
                                        )
                                        movieViewModel.saveDirectPlaybackPosition(
                                            media = offlineMedia,
                                            season = screen.download.season,
                                            episode = screen.download.episode,
                                            positionMs = pos,
                                            durationMs = dur
                                        )
                                    },
                                    onBack = { movieViewModel.navigateBack() }
                                )
                            }
                        }
                    }

                    // Official YouTube Trailer Modal
                    if (userState.isTrailerModalOpen) {
                        TrailerModal(
                            youtubeKey = userState.trailerYoutubeKey,
                            isLoadingTrailer = userState.isTrailerLoading,
                            title = userState.selectedMedia?.title ?: "Trailer",
                            onDismiss = { movieViewModel.closeTrailer() }
                        )
                    }

                    // MovieBox API Download Bottom Sheet
                    MovieBoxDownloadSheet(
                        sheetState = userState.downloadSheet,
                        preferredQuality = userState.downloadPrefs.preferredQuality,
                        onDismiss = { movieViewModel.dismissMovieBoxDownloadSheet() },
                        onSearchQueryChange = { movieViewModel.searchMovieBoxCandidatesInSheet(it) },
                        onSelectSubject = { movieViewModel.selectMovieBoxSubjectInSheet(it) },
                        onDownloadOptionClick = { opt, sub ->
                            movieViewModel.enqueueMovieBoxOptionDownload(opt, sub)
                        },
                        onRetry = {
                            val sheet = userState.downloadSheet
                            val target = sheet.targetMedia
                            if (target != null) {
                                movieViewModel.openMovieBoxDownloadSheet(
                                    media = target,
                                    season = sheet.season,
                                    episode = sheet.episode,
                                    episodeTitle = sheet.episodeTitle
                                )
                            }
                        }
                    )

                    // Floating Translucent Glass Bottom Navigation Pill (content scrolls behind it)
                    FloatingBottomNavOverlay(
                        visible = !isDedicatedSubPage && !useNavigationRail,
                        currentScreen = userState.currentScreen,
                        runningDownloadsCount = runningDownloadsCount,
                        downloadBadgeScale = downloadBadgeScale.value,
                        onNavigate = { movieViewModel.navigateTo(it) },
                        onOpenDownloads = { movieViewModel.openDownloadsScreen() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
        }
        }
        }
    }
}

@Composable
private fun FloatingBottomNavOverlay(
    visible: Boolean,
    currentScreen: Screen,
    runningDownloadsCount: Int,
    downloadBadgeScale: Float,
    onNavigate: (Screen) -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(tween(220, easing = CineEase)) { it / 2 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(180, easing = CineEase)) { it / 2 },
        modifier = modifier
    ) {
        FloatingGlassBar {
            FloatingGlassNavTab(
                label = "Home",
                selected = currentScreen is Screen.Home,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                onClick = { onNavigate(Screen.Home) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_home")
            )
            FloatingGlassNavTab(
                label = "Explore",
                selected = currentScreen is Screen.Explore,
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search,
                onClick = { onNavigate(Screen.Explore) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_explore")
            )
            FloatingGlassNavTab(
                label = "Saved",
                selected = currentScreen is Screen.Watchlist,
                selectedIcon = Icons.Filled.Bookmark,
                unselectedIcon = Icons.Outlined.BookmarkBorder,
                onClick = { onNavigate(Screen.Watchlist) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_watchlist")
            )
            FloatingGlassNavTab(
                label = "Downloads",
                selected = currentScreen is Screen.Downloads,
                selectedIcon = Icons.Filled.Download,
                unselectedIcon = Icons.Outlined.Download,
                badgeCount = runningDownloadsCount,
                badgeScale = downloadBadgeScale,
                onClick = onOpenDownloads,
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_downloads")
            )
            FloatingGlassNavTab(
                label = "History",
                selected = currentScreen is Screen.ContinueWatching,
                selectedIcon = Icons.Filled.History,
                unselectedIcon = Icons.Outlined.History,
                onClick = { onNavigate(Screen.ContinueWatching) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_history")
            )
            FloatingGlassNavTab(
                label = "Settings",
                selected = currentScreen is Screen.Settings,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                onClick = { onNavigate(Screen.Settings) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_settings")
            )
        }
    }
}

@Composable
private fun FloatingGlassNavTab(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    badgeScale: Float = 1f
) {
    val colors = CinetraTheme.colors
    val iconTint = if (selected) colors.onPrimary else CinetraTokens.NavInactiveIcon
    val pillBg = if (selected) colors.primary else Color.Transparent
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(50.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                role = Role.Tab,
                onClick = onClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 44.dp, height = 32.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pillBg)
        ) {
            BadgedBox(
                modifier = Modifier.graphicsLayer {
                    scaleX = badgeScale
                    scaleY = badgeScale
                },
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ) {
                            Text(
                                text = "$badgeCount",
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}
