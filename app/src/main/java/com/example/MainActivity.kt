package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
        MovieRepository(database.mediaDao())
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
        factory = MovieViewModelFactory(repository, userPreferences)
    )

    val userState by movieViewModel.uiState.collectAsState()
    val adGateState by movieViewModel.adGateState.collectAsState()
    val watchlist by movieViewModel.watchlist.collectAsState()
    val continueWatching by movieViewModel.continueWatching.collectAsState()
    val activeAnnouncements by movieViewModel.activeAnnouncements.collectAsState()
    val searchHistory by movieViewModel.searchHistory.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                movieViewModel.refreshAdGateCheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MyApplicationTheme(palette = userState.selectedPalette) {
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

        BackHandler(
            enabled = userState.backStack.size > 1 &&
                userState.currentScreen !is Screen.Details &&
                userState.currentScreen !is Screen.Player &&
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

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colors.background,
            contentWindowInsets = if (isPlayerScreen) WindowInsets(0, 0, 0, 0) else WindowInsets.statusBars,
            topBar = {
                if (!isPlayerScreen && userState.currentScreen !is Screen.Details) {
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
                if (!isPlayerScreen) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val screen = userState.currentScreen) {
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
                            onSeeAllClick = { section ->
                                when (section) {
                                    "Continue Watching" -> movieViewModel.navigateTo(Screen.ContinueWatching)
                                    "Trending" -> movieViewModel.openCategoryList("Trending Movies", "Trending")
                                    "Movies" -> movieViewModel.openCategoryList("Popular Movies", "Movies")
                                    "TV" -> movieViewModel.openCategoryList("Top-Rated TV Series", "TV")
                                    "Specials" -> movieViewModel.openCategoryList("Cinetra Specials & Admin Picks", "Specials")
                                    else -> movieViewModel.openCategoryList(section, "All")
                                }
                            }
                        )
                    }
                    is Screen.Explore -> {
                        val combinedCatalog = remember(
                            userState.trending,
                            userState.popularMovies,
                            userState.topRatedTv,
                            userState.customMedia
                        ) {
                            (userState.trending + userState.popularMovies + userState.topRatedTv + userState.customMedia)
                                .distinctBy { it.id }
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
                            onResetFilters = {
                                movieViewModel.selectCategory("All")
                                movieViewModel.selectGenre("All")
                                if (userState.searchQuery.isNotBlank()) {
                                    movieViewModel.onSearchQueryChange("")
                                }
                            }
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
                            onBack = { movieViewModel.navigateBack() }
                        )
                    }
                    is Screen.CategoryList -> {
                        val categoryItems = when (screen.categoryKey) {
                            "Trending" -> userState.trending
                            "Movies" -> userState.popularMovies
                            "TV" -> userState.topRatedTv
                            "Specials" -> userState.customMedia
                            else -> (userState.trending + userState.popularMovies + userState.topRatedTv + userState.customMedia).distinctBy { it.id }
                        }
                        CategoryListScreen(
                            title = screen.title,
                            items = categoryItems,
                            onMediaClick = { movieViewModel.openDetails(it) },
                            onBack = { movieViewModel.navigateBack() }
                        )
                    }
                    is Screen.Watchlist -> {
                        WatchlistScreen(
                            watchlist = watchlist,
                            onMediaClick = { movieViewModel.openDetails(it) },
                            onRemoveFromWatchlist = { movieViewModel.toggleWatchlist(it) },
                            onExploreClick = { movieViewModel.navigateTo(Screen.Explore) }
                        )
                    }
                    is Screen.ContinueWatching -> {
                        val combinedCatalog = remember(
                            userState.trending,
                            userState.popularMovies,
                            userState.topRatedTv,
                            userState.customMedia
                        ) {
                            (userState.trending + userState.popularMovies + userState.topRatedTv + userState.customMedia)
                                .distinctBy { it.id }
                        }
                        ContinueWatchingScreen(
                            items = continueWatching,
                            allCatalogItems = combinedCatalog,
                            onResumeWatching = { media, season, ep -> movieViewModel.openPlayer(media, season, ep) },
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
                            onClearSearchHistory = { movieViewModel.clearSearchHistory() }
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
                            onBack = { movieViewModel.navigateBack() }
                        )
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
