package com.example.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaItem
import com.example.ui.components.CineEase
import com.example.ui.components.LocalDownloadedMediaIds
import com.example.ui.components.MovieBoxDownloadSheet
import com.example.ui.components.StreamPlayerView
import com.example.ui.components.TrailerModal
import com.example.ui.screens.DirectMediaPlayerScreen
import com.example.ui.screens.OfflineVideoPlayerScreen
import com.example.ui.theme.CineTheme
import com.example.ui.viewmodel.MovieViewModel
import com.example.ui.viewmodel.Screen
import java.io.File

/**
 * Android TV / Leanback root shell reusing MovieViewModel, repositories, players, and download engine.
 * Exposes zero Admin Panel entry points on TV.
 */
@Composable
fun CinetraTvShell(
    movieViewModel: MovieViewModel,
    snackbarHostState: SnackbarHostState
) {
    val userState by movieViewModel.uiState.collectAsState()
    val watchlist by movieViewModel.watchlist.collectAsState()
    val continueWatching by movieViewModel.continueWatching.collectAsState()
    val searchHistory by movieViewModel.searchHistory.collectAsState()
    val downloads by movieViewModel.downloads.collectAsState()
    val focusMemory = remember { TvHomeFocusMemory() }

    val watchlistIds = remember(watchlist) { watchlist.map { it.id }.toSet() }
    val runningDownloadsCount = remember(downloads) {
        downloads.count { it.parsedStatus == DownloadStatus.RUNNING }
    }
    val downloadedMediaIds = remember(downloads) {
        downloads.filter {
            it.parsedStatus == DownloadStatus.COMPLETED &&
                it.localFilePath.isNotBlank() &&
                File(it.localFilePath).exists()
        }.map { it.mediaId }.toSet()
    }
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

    val currentScreen = userState.currentScreen
    val isFullscreenSubScreen = currentScreen is Screen.Details ||
        currentScreen is Screen.Player ||
        currentScreen is Screen.DirectPlayer ||
        currentScreen is Screen.OfflinePlayer

    val selectedTopDestination = when (currentScreen) {
        is Screen.Search -> TvTopDestination.SEARCH
        is Screen.Watchlist -> TvTopDestination.MY_LIST
        is Screen.Downloads -> TvTopDestination.DOWNLOADS
        else -> TvTopDestination.HOME
    }

    val navigateBackWithHomeFocusRestore: () -> Unit = {
        val returningToHome = userState.backStack.dropLast(1).lastOrNull() is Screen.Home
        if (returningToHome) {
            focusMemory.markRestorePending()
        }
        movieViewModel.navigateBack()
    }

    BackHandler(
        enabled = userState.isTrailerModalOpen ||
            userState.downloadSheet.isOpen ||
            (!isFullscreenSubScreen && userState.backStack.size > 1)
    ) {
        when (
            TvBackPolicy.resolveBackAction(
                hasOpenSheetOrMenu = userState.downloadSheet.isOpen,
                hasOpenTrailer = userState.isTrailerModalOpen,
                backStackSize = userState.backStack.size
            )
        ) {
            TvBackAction.CLOSE_OVERLAY -> {
                if (userState.downloadSheet.isOpen) {
                    movieViewModel.dismissMovieBoxDownloadSheet()
                } else if (userState.isTrailerModalOpen) {
                    movieViewModel.closeTrailer()
                }
            }
            TvBackAction.NAVIGATE_BACK -> navigateBackWithHomeFocusRestore()
            TvBackAction.EXIT_APP -> Unit
        }
    }

    CinetraTvTheme {
        val colors = CineTheme.colors
        CompositionLocalProvider(LocalDownloadedMediaIds provides downloadedMediaIds) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .testTag("cinetra_tv_shell")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isFullscreenSubScreen) {
                        TvTopNavigationBar(
                            selectedDestination = selectedTopDestination,
                            watchlistCount = watchlist.size,
                            runningDownloadsCount = runningDownloadsCount,
                            onSelectDestination = { destination ->
                                when (destination) {
                                    TvTopDestination.HOME -> movieViewModel.navigateTo(Screen.Home)
                                    TvTopDestination.SEARCH -> movieViewModel.openSearch()
                                    TvTopDestination.MY_LIST -> movieViewModel.navigateTo(Screen.Watchlist)
                                    TvTopDestination.DOWNLOADS -> movieViewModel.openDownloadsScreen()
                                }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                if (targetState is Screen.Player || initialState is Screen.Player ||
                                    targetState is Screen.DirectPlayer || initialState is Screen.DirectPlayer ||
                                    targetState is Screen.OfflinePlayer || initialState is Screen.OfflinePlayer
                                ) {
                                    EnterTransition.None togetherWith ExitTransition.None
                                } else {
                                    fadeIn(tween(220, easing = CineEase)) togetherWith
                                        fadeOut(tween(160, easing = CineEase))
                                }
                            },
                            contentKey = { screen ->
                                when (screen) {
                                    Screen.Home -> "tv_home"
                                    Screen.Search -> "tv_search"
                                    Screen.Watchlist -> "tv_watchlist"
                                    Screen.Downloads -> "tv_downloads"
                                    is Screen.Details -> "tv_details_${screen.media.id}"
                                    is Screen.Player -> "tv_player_${screen.media.id}"
                                    is Screen.DirectPlayer -> "tv_direct_player_${screen.media.id}_${screen.season}_${screen.episode}"
                                    is Screen.OfflinePlayer -> "tv_offline_player_${screen.download.id}"
                                    else -> "tv_home_fallback"
                                }
                            },
                            label = "cinetra_tv_screen_transition",
                            modifier = Modifier.fillMaxSize()
                        ) { screen ->
                            when (screen) {
                                is Screen.Search -> {
                                    TvSearchScreen(
                                        searchQuery = userState.searchQuery,
                                        searchResults = userState.searchResults,
                                        isSearching = userState.isSearching,
                                        searchHistory = searchHistory,
                                        searchErrorMessage = userState.searchErrorMessage,
                                        hasMore = userState.hasMoreSearch,
                                        isLoadingMore = userState.isLoadingMoreSearch,
                                        allFallbackCatalog = combinedCatalog,
                                        onSearchQueryChange = { movieViewModel.onSearchQueryChange(it) },
                                        onSubmitSearch = { movieViewModel.submitSearchQuery(it) },
                                        onClearSearchHistory = { movieViewModel.clearSearchHistory() },
                                        onRetrySearch = { movieViewModel.onSearchQueryChange(userState.searchQuery) },
                                        onLoadMore = { movieViewModel.loadMoreSearchResults() },
                                        onMediaClick = { movieViewModel.openDetails(it) }
                                    )
                                }
                                is Screen.Watchlist -> {
                                    TvWatchlistScreen(
                                        watchlist = watchlist,
                                        onMediaClick = { movieViewModel.openDetails(it) },
                                        onBrowseHomeClick = { movieViewModel.navigateTo(Screen.Home) }
                                    )
                                }
                                is Screen.Downloads -> {
                                    TvDownloadsScreen(
                                        downloads = downloads,
                                        onPlayOffline = { movieViewModel.openOfflinePlayer(it) },
                                        onPauseDownload = { movieViewModel.pauseDownload(it) },
                                        onResumeDownload = { movieViewModel.resumeDownload(it) },
                                        onRetryDownload = { movieViewModel.retryDownload(it) },
                                        onCancelOrDelete = { movieViewModel.cancelOrDeleteDownload(it) },
                                        onRefreshStatus = { movieViewModel.syncDownloadsNow() }
                                    )
                                }
                                is Screen.Details -> {
                                    val activeMedia = userState.selectedMedia?.takeIf { it.id == screen.media.id } ?: screen.media
                                    val savedContinueItem = remember(continueWatching, activeMedia.id) {
                                        continueWatching.firstOrNull { it.id == activeMedia.id }
                                    }
                                    TvDetailsScreen(
                                        media = activeMedia,
                                        isInWatchlist = activeMedia.id in watchlistIds,
                                        episodesBySeason = userState.episodesByMediaAndSeason[activeMedia.id].orEmpty(),
                                        isLoadingSeasons = userState.isLoadingSeasons,
                                        loadingSeasonNumber = userState.loadingSeasonNumber,
                                        continueWatchingItem = savedContinueItem,
                                        similarMedia = userState.similarForSelectedMedia,
                                        downloads = downloads,
                                        onWatchClick = { s, ep -> movieViewModel.openPlayer(activeMedia, s, ep) },
                                        onWatchlistToggle = { movieViewModel.toggleWatchlist(activeMedia) },
                                        onTrailerClick = { movieViewModel.openTrailer(activeMedia) },
                                        onSeasonSelected = { seasonNum -> movieViewModel.loadSeasonEpisodes(activeMedia, seasonNum) },
                                        onSimilarMediaClick = { movieViewModel.openDetails(it) },
                                        onDownloadClick = { s, ep, epTitle ->
                                            movieViewModel.startMovieOrEpisodeDownload(activeMedia, s, ep, epTitle)
                                        },
                                        onDownloadSeason = { s ->
                                            movieViewModel.enqueueSeasonBatchDownload(activeMedia, s)
                                        },
                                        onPauseDownload = { movieViewModel.pauseDownload(it) },
                                        onResumeDownload = { movieViewModel.resumeDownload(it) },
                                        onCancelDownload = { movieViewModel.cancelOrDeleteDownload(it) },
                                        onRetryDownload = { movieViewModel.retryDownload(it) },
                                        onPlayOffline = { movieViewModel.openOfflinePlayer(it) },
                                        onBack = navigateBackWithHomeFocusRestore
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
                                        onBack = {
                                            movieViewModel.navigateBack()
                                            Unit
                                        },
                                        isTvDevice = true
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
                                        onBack = { movieViewModel.navigateBack() },
                                        isTvDevice = true
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
                                else -> {
                                    TvHomeScreen(
                                        trending = userState.trending,
                                        trendingMovies = userState.trendingMovies,
                                        trendingTv = userState.trendingTv,
                                        newReleases = userState.newReleases,
                                        popularMovies = userState.popularMovies,
                                        upcomingMovies = userState.upcomingMovies,
                                        topRatedMovies = userState.topRatedMovies,
                                        topRatedTv = userState.topRatedTv,
                                        customMedia = userState.customMedia,
                                        continueWatching = continueWatching,
                                        watchlist = watchlist,
                                        recommendedForYou = userState.recommendedForYou,
                                        becauseYouWatchedTitle = userState.becauseYouWatchedTitle,
                                        becauseYouWatchedItems = userState.becauseYouWatchedItems,
                                        selectedHomeGenre = userState.selectedHomeGenre,
                                        isLoading = userState.isLoading,
                                        errorMessage = userState.errorMessage,
                                        isItemInWatchlist = { it in watchlistIds },
                                        focusMemory = focusMemory,
                                        onHomeGenreSelect = { movieViewModel.selectHomeGenre(it) },
                                        onMediaClick = { movieViewModel.openDetails(it) },
                                        onWatchClick = { movieViewModel.openPlayer(it) },
                                        onResumeWatching = { media, s, ep -> movieViewModel.openPlayer(media, s, ep) },
                                        onWatchlistToggle = { movieViewModel.toggleWatchlist(it) },
                                        onRetry = { movieViewModel.loadHomeData() }
                                    )
                                }
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = TvOverscanHorizontal, vertical = 24.dp)
                )

                if (userState.isTrailerModalOpen) {
                    TrailerModal(
                        youtubeKey = userState.trailerYoutubeKey,
                        isLoadingTrailer = userState.isTrailerLoading,
                        title = userState.selectedMedia?.title ?: "Trailer",
                        onDismiss = { movieViewModel.closeTrailer() }
                    )
                }

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
            }
        }
    }
}
