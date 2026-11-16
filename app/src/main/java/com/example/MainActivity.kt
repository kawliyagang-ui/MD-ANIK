package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferences
import com.example.data.repository.MovieRepository
import com.example.ui.components.AdGateOverlay
import com.example.ui.components.StreamPlayerView
import com.example.ui.components.TrailerModal
import com.example.ui.screens.CategoryListScreen
import com.example.ui.screens.ContinueWatchingScreen
import com.example.ui.screens.DetailsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LanguageCatalogScreen
import com.example.ui.screens.ReportProblemScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.CineTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AdGateState
import com.example.ui.viewmodel.MovieViewModel
import com.example.ui.viewmodel.MovieViewModelFactory
import com.example.ui.viewmodel.Screen

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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                movieViewModel.refreshFromCloud()
                movieViewModel.refreshDailyFeedsIfDateChanged()
                movieViewModel.refreshAdGateCheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MyApplicationTheme(
        palette = userState.selectedPalette,
        themeMode = userState.themeMode,
        accentColor = userState.accentColor
    ) {
        val colors = CineTheme.colors
        val gradients = CineTheme.gradients

        when (val gate = adGateState) {
            is AdGateState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradients.appBackdrop),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.softRose)
                }
                return@MyApplicationTheme
            }
            is AdGateState.GateRequired -> {
                AdGateOverlay(
                    config = gate.config,
                    onAdCompleted = { movieViewModel.completeAdGate() }
                )
                return@MyApplicationTheme
            }
            is AdGateState.Unlocked -> Unit
        }

        val watchlistIds = remember(watchlist) { watchlist.map { it.id }.toSet() }
        val isPlayerScreen = userState.currentScreen is Screen.Player
        val isDedicatedSubPage = userState.currentScreen is Screen.Details ||
            userState.currentScreen is Screen.Search ||
            userState.currentScreen is Screen.CategoryList ||
            userState.currentScreen is Screen.LanguageCatalog ||
            userState.currentScreen is Screen.Player ||
            userState.currentScreen is Screen.ReportProblem

        BackHandler(
            enabled = userState.backStack.size > 1 &&
                !isDedicatedSubPage &&
                !userState.isTrailerModalOpen
        ) {
            movieViewModel.navigateBack()
        }

        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.textPrimary,
            selectedTextColor = colors.softRose,
            indicatorColor = colors.navIndicator,
            unselectedIconColor = colors.textMuted,
            unselectedTextColor = colors.textMuted
        )
        val railItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = colors.textPrimary,
            selectedTextColor = colors.softRose,
            indicatorColor = colors.navIndicator,
            unselectedIconColor = colors.textMuted,
            unselectedTextColor = colors.textMuted
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = colors.background,
                contentWindowInsets = when {
                    isPlayerScreen -> WindowInsets(0, 0, 0, 0)
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
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(gradients.topBarBackdrop)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cinetra Brand Logo & Title
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_cinetra_logo),
                                        contentDescription = "Cinetra Logo",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "CINETRA",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.2.sp,
                                            brush = gradients.wordmark
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

                                // Watchlist shortcut
                                IconButton(
                                    onClick = { movieViewModel.navigateTo(Screen.Watchlist) },
                                    modifier = Modifier.testTag("topbar_watchlist_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (watchlist.isNotEmpty()) {
                                                Badge(containerColor = colors.accentMid) {
                                                    Text("${watchlist.size}", color = Color.White, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "Watchlist",
                                            tint = if (watchlist.isNotEmpty()) colors.gold else colors.textPrimary
                                        )
                                    }
                                }

                                // Settings shortcut
                                IconButton(
                                    onClick = { movieViewModel.navigateTo(Screen.Settings) },
                                    modifier = Modifier.testTag("topbar_settings_button")
                                ) {
                                    Icon(
                                        imageVector = if (userState.currentScreen is Screen.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings",
                                        tint = if (userState.currentScreen is Screen.Settings) colors.softRose else colors.textPrimary
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.border.copy(alpha = 0.55f))
                            )
                        }
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !isDedicatedSubPage && !useNavigationRail,
                    enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(gradients.bottomBarBackdrop)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.border.copy(alpha = 0.45f))
                        )
                        NavigationBar(
                            containerColor = Color.Transparent,
                            contentColor = colors.textPrimary,
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            NavigationBarItem(
                                selected = userState.currentScreen is Screen.Home,
                                onClick = { movieViewModel.navigateTo(Screen.Home) },
                                icon = {
                                    Icon(
                                        imageVector = if (userState.currentScreen is Screen.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                label = { Text("Home", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_home")
                            )

                            NavigationBarItem(
                                selected = userState.currentScreen is Screen.Explore,
                                onClick = { movieViewModel.navigateTo(Screen.Explore) },
                                icon = {
                                    Icon(
                                        imageVector = if (userState.currentScreen is Screen.Explore) Icons.Filled.Search else Icons.Outlined.Search,
                                        contentDescription = "Explore"
                                    )
                                },
                                label = { Text("Explore", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_explore")
                            )

                            NavigationBarItem(
                                selected = userState.currentScreen is Screen.Watchlist,
                                onClick = { movieViewModel.navigateTo(Screen.Watchlist) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (watchlist.isNotEmpty()) {
                                                Badge(containerColor = colors.accentMid) {
                                                    Text("${watchlist.size}", color = Color.White)
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
                                label = { Text("Saved", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_watchlist")
                            )

                            NavigationBarItem(
                                selected = userState.currentScreen is Screen.ContinueWatching,
                                onClick = { movieViewModel.navigateTo(Screen.ContinueWatching) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (continueWatching.isNotEmpty()) {
                                                Badge(containerColor = colors.gold) {
                                                    Text("${continueWatching.size}", color = Color.Black)
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
                                label = { Text("History", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_history")
                            )

                            NavigationBarItem(
                                selected = userState.currentScreen is Screen.Settings,
                                onClick = { movieViewModel.navigateTo(Screen.Settings) },
                                icon = {
                                    Icon(
                                        imageVector = if (userState.currentScreen is Screen.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text("Settings", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_settings")
                            )
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
                                            Badge(containerColor = colors.accentMid) {
                                                Text("${watchlist.size}", color = Color.White)
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
                                            Badge(containerColor = colors.gold) {
                                                Text("${continueWatching.size}", color = Color.Black)
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
                                targetState is Screen.ReportProblem
                            val wasSecondaryOrigin = initialState is Screen.Details ||
                                initialState is Screen.Search ||
                                initialState is Screen.CategoryList ||
                                initialState is Screen.LanguageCatalog ||
                                initialState is Screen.Player ||
                                initialState is Screen.ReportProblem

                            when {
                                userState.isNavigatingBack -> {
                                    (slideInHorizontally(
                                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                                        initialOffsetX = { -it / 5 }
                                    ) + fadeIn(tween(240))) togetherWith
                                        (slideOutHorizontally(
                                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                                            targetOffsetX = { it / 3 }
                                        ) + fadeOut(tween(200)))
                                }
                                isSecondaryDestination || wasSecondaryOrigin -> {
                                    (slideInHorizontally(
                                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                                        initialOffsetX = { it / 4 }
                                    ) + fadeIn(tween(250)) + scaleIn(
                                        initialScale = 0.97f,
                                        animationSpec = tween(260)
                                    )) togetherWith
                                        (slideOutHorizontally(
                                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                                            targetOffsetX = { -it / 6 }
                                        ) + fadeOut(tween(200)))
                                }
                                else -> {
                                    (fadeIn(tween(220)) + scaleIn(
                                        initialScale = 0.985f,
                                        animationSpec = tween(220)
                                    )) togetherWith fadeOut(tween(160))
                                }
                            }
                        },
                        contentKey = { screen ->
                            when (screen) {
                                Screen.Home -> "home"
                                Screen.Explore -> "explore"
                                Screen.Watchlist -> "watchlist"
                                Screen.ContinueWatching -> "continue_watching"
                                Screen.Settings -> "settings"
                                Screen.Search -> "search"
                                is Screen.ReportProblem -> "report_${screen.prefilledMedia?.id.orEmpty()}_${screen.serverName}"
                                is Screen.CategoryList -> "category_${screen.categoryKey}_${screen.genreFilter}_${screen.title}"
                                is Screen.LanguageCatalog -> "language_${screen.languageCode}"
                                is Screen.Details -> "details_${screen.media.id}"
                                is Screen.Player -> "player_${screen.media.id}_${screen.season}_${screen.episode}"
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
                                    onLanguageClick = { code, name -> movieViewModel.openLanguageCatalog(code, name) },
                                    onSeeAllClick = { section ->
                                        when (section) {
                                            "Continue Watching" -> movieViewModel.navigateTo(Screen.ContinueWatching)
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
                                    onLoadMore = { movieViewModel.loadMoreSearchResults() }
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
                                    userState.customMedia
                                ) {
                                    val rawList = when (screen.categoryKey) {
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
                                    "TrendingTv" -> maxOf(userState.trendingTvTotalResults, categoryItems.size)
                                    "NewReleases" -> maxOf(userState.newReleasesTotalResults, categoryItems.size)
                                    "Top10Movies", "Top10Tv", "Specials" -> categoryItems.size
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
                                    "Top10Movies", "Top10Tv", "Specials" -> false
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
                                    "Top10Movies", "Top10Tv", "Specials" -> false
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
                                    onDismissFeedback = { movieViewModel.dismissSettingsFeedback() }
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
                }
            }
        }
        }
    }
}
