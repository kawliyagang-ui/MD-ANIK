package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.download.BatchEpisodeDownloadRequest
import com.example.data.download.DownloadEngine
import com.example.data.local.DownloadPreferences
import com.example.data.local.NotificationPreferences
import com.example.data.local.UserPreferences
import com.example.data.model.AdConfigEntity
import com.example.data.model.AdGatePolicy
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.AppLanguageOption
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.CatalogOverridePolicy
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.CustomMediaEntity
import com.example.data.model.DiscoverFilters
import com.example.data.model.DownloadEntity
import com.example.data.model.EpisodeInfo
import com.example.data.model.IssueReportCategory
import com.example.data.model.IssueReportEntity
import com.example.data.model.MediaItem
import com.example.data.model.MovieBoxDownloadBundle
import com.example.data.model.MovieBoxDownloadOption
import com.example.data.model.MovieBoxSubject
import com.example.data.model.MovieBoxSubtitleOption
import com.example.data.model.NewReleasePolicy
import com.example.data.model.PagedCatalogBatch
import com.example.data.model.PlaybackSource
import com.example.data.model.PlaybackSourcePolicy
import com.example.data.model.PlayerServer
import com.example.data.model.PreferredDownloadQuality
import com.example.data.model.RecommendationPolicy
import com.example.data.model.SearchSuggestionPolicy
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.data.model.UpcomingReleasePolicy
import com.example.data.model.WatchlistEntity
import com.example.data.model.toMediaItem
import com.example.data.notifications.EpisodeNotificationHelper
import com.example.data.remote.MovieBoxApiClient
import com.example.data.repository.MovieRepository
import com.example.ui.theme.AccentColorOption
import com.example.ui.theme.DarkThemePalette
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeTogglePolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.Locale
import java.util.UUID

sealed class Screen {
    object Home : Screen()
    object Explore : Screen()
    object Watchlist : Screen()
    object Downloads : Screen()
    object ContinueWatching : Screen()
    object Settings : Screen()
    object Search : Screen()
    data class ReportProblem(
        val prefilledMedia: MediaItem? = null,
        val serverName: String = "",
        val episodeLabel: String = ""
    ) : Screen()
    data class CategoryList(
        val title: String,
        val categoryKey: String,
        val genreFilter: String = "All"
    ) : Screen()
    data class LanguageCatalog(
        val languageCode: String,
        val languageName: String
    ) : Screen()
    data class Details(val media: MediaItem) : Screen()
    data class Player(val media: MediaItem, val season: Int = 1, val episode: Int = 1) : Screen()
    data class DirectPlayer(
        val media: MediaItem,
        val season: Int = 1,
        val episode: Int = 1,
        val sourceUrl: String,
        val isLocalFile: Boolean,
        val downloadEntity: DownloadEntity? = null,
        val initialPositionMs: Long = 0L
    ) : Screen()
    data class OfflinePlayer(val download: DownloadEntity, val initialPositionMs: Long = 0L) : Screen()
}

data class DownloadSheetUiState(
    val isOpen: Boolean = false,
    val targetMedia: MediaItem? = null,
    val season: Int = 1,
    val episode: Int = 1,
    val episodeTitle: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val bundle: MovieBoxDownloadBundle? = null,
    val searchQuery: String = "",
    val searchCandidates: List<MovieBoxSubject> = emptyList(),
    val isSearchingCandidates: Boolean = false
)

sealed interface AdGateState {
    data object Loading : AdGateState
    data object Unlocked : AdGateState
    data class GateRequired(val config: AdConfigEntity) : AdGateState
}

private data class FeedPaginationState(
    val nextPage: Int = 1,
    val totalPages: Int = 1,
    val totalResults: Int = 0,
    val buffer: List<MediaItem> = emptyList()
) {
    fun hasMore(): Boolean = buffer.isNotEmpty() || nextPage <= totalPages
}

data class UserUiState(
    val currentScreen: Screen = Screen.Home,
    val backStack: List<Screen> = listOf(Screen.Home),
    val isNavigatingBack: Boolean = false,
    val batchSize: Int = CatalogBatchPolicy.DEFAULT_BATCH_SIZE,
    val trending: List<MediaItem> = emptyList(),
    val trendingMovies: List<MediaItem> = emptyList(),
    val trendingTv: List<MediaItem> = emptyList(),
    val newReleases: List<MediaItem> = emptyList(),
    val popularMovies: List<MediaItem> = emptyList(),
    val upcomingMovies: List<MediaItem> = emptyList(),
    val topRatedMovies: List<MediaItem> = emptyList(),
    val topRatedTv: List<MediaItem> = emptyList(),
    val customMedia: List<MediaItem> = emptyList(),
    val searchResults: List<MediaItem> = emptyList(),
    val discoverResults: List<MediaItem> = emptyList(),
    val discoverFilters: DiscoverFilters = DiscoverFilters(),
    val isLoadingDiscover: Boolean = false,
    val isLoadingMoreDiscover: Boolean = false,
    val hasMoreDiscover: Boolean = false,
    val discoverTotalResults: Int = 0,
    val selectedLanguageCode: String = "",
    val selectedLanguageName: String = "",
    val languageCatalogItems: List<MediaItem> = emptyList(),
    val languageCatalogTotalResults: Int = 0,
    val languageCatalogMediaType: String = "All",
    val languageCatalogSortBy: String = "popular",
    val isLoadingLanguageCatalog: Boolean = false,
    val isLoadingMoreLanguageCatalog: Boolean = false,
    val hasMoreLanguageCatalog: Boolean = false,
    val languageCatalogError: String? = null,
    val trendingTotalResults: Int = 0,
    val trendingMoviesTotalResults: Int = 0,
    val trendingTvTotalResults: Int = 0,
    val newReleasesTotalResults: Int = 0,
    val popularMoviesTotalResults: Int = 0,
    val upcomingMoviesTotalResults: Int = 0,
    val topRatedMoviesTotalResults: Int = 0,
    val topRatedTvTotalResults: Int = 0,
    val searchTotalResults: Int = 0,
    val hasMoreTrending: Boolean = false,
    val hasMoreTrendingMovies: Boolean = false,
    val hasMoreTrendingTv: Boolean = false,
    val hasMoreNewReleases: Boolean = false,
    val hasMorePopularMovies: Boolean = false,
    val hasMoreUpcomingMovies: Boolean = false,
    val hasMoreTopRatedMovies: Boolean = false,
    val hasMoreTopRatedTv: Boolean = false,
    val hasMoreSearch: Boolean = false,
    val isLoadingMoreTrending: Boolean = false,
    val isLoadingMoreTrendingMovies: Boolean = false,
    val isLoadingMoreTrendingTv: Boolean = false,
    val isLoadingMoreNewReleases: Boolean = false,
    val isLoadingMoreMovies: Boolean = false,
    val isLoadingMoreUpcomingMovies: Boolean = false,
    val isLoadingMoreTopRatedMovies: Boolean = false,
    val isLoadingMoreTv: Boolean = false,
    val isLoadingMoreSearch: Boolean = false,
    // Smart Recommendations
    val recommendedForYou: List<MediaItem> = emptyList(),
    val becauseYouWatchedTitle: String = "",
    val becauseYouWatchedItems: List<MediaItem> = emptyList(),
    val popularInGenreName: String = "Action",
    val popularInGenreItems: List<MediaItem> = emptyList(),
    val similarForSelectedMedia: List<MediaItem> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    // Explore/Search Simple State
    val searchQuery: String = "",
    val selectedHomeGenre: String = "All",
    val selectedGenre: String = "All",
    val selectedCategory: String = "All", // "All", "movie", "tv"
    val sortBy: String = "popular", // "popular", "rating", "newest"
    // Active Media & Player
    val selectedMedia: MediaItem? = null,
    val playingMedia: MediaItem? = null,
    val currentSeason: Int = 1,
    val currentEpisode: Int = 1,
    val playerServer: PlayerServer = PlayerServer.VIDLY,
    // Customization & Notifications
    val selectedPalette: DarkThemePalette = DarkThemePalette.VELVET_CINEMA,
    val preferredHighReadabilityPalette: DarkThemePalette = DarkThemePalette.CRISP_SLATE,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: AccentColorOption = AccentColorOption.VELVET_ROSE,
    val appLanguage: AppLanguageOption = AppLanguageOption.ENGLISH,
    val notificationPrefs: NotificationPreferences = NotificationPreferences(),
    val cacheSizeLabel: String = "0 KB",
    val settingsFeedbackMessage: String? = null,
    // Report Problem State
    val isSubmittingReport: Boolean = false,
    val reportSubmitMessage: String? = null,
    val isReportError: Boolean = false,
    // Trailer & Seasons
    val trailerYoutubeKey: String? = null,
    val isTrailerLoading: Boolean = false,
    val isTrailerModalOpen: Boolean = false,
    val episodesByMediaAndSeason: Map<String, Map<Int, List<EpisodeInfo>>> = emptyMap(),
    val isLoadingSeasons: Boolean = false,
    val loadingSeasonNumber: Int? = null,
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val searchErrorMessage: String? = null,
    val searchSuggestions: List<MediaItem> = emptyList(),
    val isFetchingSuggestions: Boolean = false,
    val showSearchSuggestions: Boolean = false,
    val errorMessage: String? = null,
    // MovieBox API & Download System
    val downloadPrefs: DownloadPreferences = DownloadPreferences(),
    val downloadSheet: DownloadSheetUiState = DownloadSheetUiState(),
    val movieBoxSearchQuery: String = "",
    val movieBoxSearchResults: List<MovieBoxSubject> = emptyList(),
    val isSearchingMovieBox: Boolean = false,
    val movieBoxSearchError: String? = null,
    val downloadFeedbackMessage: String? = null
)

class MovieViewModel(
    private val repository: MovieRepository,
    private val userPreferences: UserPreferences? = null,
    private val appContext: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    val watchlist: StateFlow<List<WatchlistEntity>> = repository.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueWatching: StateFlow<List<ContinueWatchingEntity>> = repository.continueWatching
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAnnouncements: StateFlow<List<AdminAnnouncementEntity>> = repository.activeAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<TmdbSearchHistoryEntity>> = repository.tmdbSearchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val issueReports: StateFlow<List<IssueReportEntity>> = repository.issueReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = repository.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _adGateState = MutableStateFlow<AdGateState>(AdGateState.Loading)
    val adGateState: StateFlow<AdGateState> = _adGateState.asStateFlow()

    private var latestAdConfig: AdConfigEntity? = null
    private var hasLoadedAdConfig: Boolean = false
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private var searchHistoryCommitJob: Job? = null
    private var languageCatalogJob: Job? = null
    private var downloadFetchJob: Job? = null
    private var movieBoxHubSearchJob: Job? = null
    private var downloadSyncJob: Job? = null
    private var hasCheckedReleasesThisSession = false

    private var rawTrending: List<MediaItem> = emptyList()
    private var rawTrendingMovies: List<MediaItem> = emptyList()
    private var rawTrendingTv: List<MediaItem> = emptyList()
    private var rawNewReleases: List<MediaItem> = emptyList()
    private var rawCustomEntities: List<CustomMediaEntity> = emptyList()
    private var rawPopularMovies: List<MediaItem> = emptyList()
    private var rawUpcomingMovies: List<MediaItem> = emptyList()
    private var rawTopRatedMovies: List<MediaItem> = emptyList()
    private var rawTopRatedTv: List<MediaItem> = emptyList()
    private var rawSearchResults: List<MediaItem> = emptyList()
    private var rawSearchSuggestions: List<MediaItem> = emptyList()
    private var rawDiscoverResults: List<MediaItem> = emptyList()
    private var rawLanguageCatalog: List<MediaItem> = emptyList()
    private var rawRecommendedForYou: List<MediaItem> = emptyList()
    private var rawBecauseYouWatched: List<MediaItem> = emptyList()
    private var rawPopularInGenre: List<MediaItem> = emptyList()
    private var rawSimilarForSelected: List<MediaItem> = emptyList()

    private var trendingFeed = FeedPaginationState()
    private var trendingMoviesFeed = FeedPaginationState()
    private var trendingTvFeed = FeedPaginationState()
    private var newReleasesFeed = FeedPaginationState()
    private var moviesFeed = FeedPaginationState()
    private var upcomingFeed = FeedPaginationState()
    private var topRatedMoviesFeed = FeedPaginationState()
    private var tvFeed = FeedPaginationState()
    private var searchFeed = FeedPaginationState()
    private var discoverFeed = FeedPaginationState()
    private var languageCatalogFeed = FeedPaginationState()
    private var lastDailyFeedDate: String = UpcomingReleasePolicy.todayIsoDate()

    init {
        viewModelScope.launch {
            repository.initSeedDataIfEmpty()
            repository.syncFromCloudNow()
        }
        observeUserPreferences()
        observeAdConfig()
        observeCustomMedia()
        observeHistoryForRecommendations()
        observeDownloadsProgress()
        refreshCacheSize()
        loadHomeData()
    }

    fun refreshFromCloud() {
        viewModelScope.launch {
            repository.syncFromCloudNow()
            refreshDailyFeedsIfDateChanged()
            if (!hasCheckedReleasesThisSession) {
                hasCheckedReleasesThisSession = true
                checkNewReleasesAndNotify(silentIfNoPermission = true)
            }
        }
    }

    fun refreshDailyFeedsIfDateChanged() {
        val currentDay = UpcomingReleasePolicy.todayIsoDate()
        if (currentDay != lastDailyFeedDate) {
            lastDailyFeedDate = currentDay
            loadHomeData()
        }
    }

    private fun observeUserPreferences() {
        val prefs = userPreferences ?: return
        viewModelScope.launch {
            prefs.darkThemePaletteFlow.collect { palette ->
                _uiState.update { it.copy(selectedPalette = palette) }
            }
        }
        viewModelScope.launch {
            prefs.preferredHighReadabilityPaletteFlow.collect { readable ->
                _uiState.update { it.copy(preferredHighReadabilityPalette = readable) }
            }
        }
        viewModelScope.launch {
            prefs.defaultPlayerServerFlow.collect { server ->
                _uiState.update { it.copy(playerServer = server) }
            }
        }
        viewModelScope.launch {
            prefs.themeModeFlow.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            prefs.accentColorFlow.collect { accent ->
                _uiState.update { it.copy(accentColor = accent) }
            }
        }
        viewModelScope.launch {
            var firstEmit = true
            prefs.appLanguageFlow.collect { lang ->
                val changed = repository.currentLanguageCode != lang.tmdbCode
                repository.currentLanguageCode = lang.tmdbCode
                _uiState.update { it.copy(appLanguage = lang) }
                if (!firstEmit && changed) {
                    loadHomeData()
                }
                firstEmit = false
            }
        }
        viewModelScope.launch {
            prefs.notificationPreferencesFlow.collect { nPrefs ->
                _uiState.update { it.copy(notificationPrefs = nPrefs) }
            }
        }
        viewModelScope.launch {
            prefs.downloadPreferencesFlow.collect { dPrefs ->
                _uiState.update { it.copy(downloadPrefs = dPrefs) }
            }
        }
    }

    private fun observeHistoryForRecommendations() {
        viewModelScope.launch {
            continueWatching.collect {
                refreshSmartRecommendations()
            }
        }
        viewModelScope.launch {
            watchlist.collect {
                refreshSmartRecommendations()
            }
        }
    }

    private fun observeAdConfig() {
        viewModelScope.launch {
            repository.adConfig.collect { config ->
                latestAdConfig = config
                hasLoadedAdConfig = true
                evaluateAdGate(config)
            }
        }
    }

    private fun evaluateAdGate(config: AdConfigEntity?) {
        _adGateState.value = if (config != null && AdGatePolicy.shouldShowAdGate(config)) {
            AdGateState.GateRequired(config)
        } else {
            AdGateState.Unlocked
        }
    }

    fun refreshAdGateCheck() {
        if (!hasLoadedAdConfig) return
        evaluateAdGate(latestAdConfig)
    }

    fun completeAdGate() {
        val now = System.currentTimeMillis()
        latestAdConfig = latestAdConfig?.copy(lastCompletedAtMillis = now)
        _adGateState.value = AdGateState.Unlocked
        viewModelScope.launch {
            repository.markAdGateCompleted(now)
        }
    }

    private fun buildDailyNewReleasesUiList(
        tmdbNewReleases: List<MediaItem>,
        customEntities: List<CustomMediaEntity>,
        mappedCustom: List<MediaItem>
    ): List<MediaItem> {
        val merged = NewReleasePolicy.mergeDailyNewReleases(
            dailyTmdbItems = tmdbNewReleases,
            customMediaEntities = customEntities
        )
        return CatalogOverridePolicy.applyAdminOverrides(merged, mappedCustom)
    }

    private fun buildStrictUpcomingUiList(
        rawUpcoming: List<MediaItem>,
        mappedCustom: List<MediaItem>
    ): List<MediaItem> {
        val overridden = CatalogOverridePolicy.applyAdminOverrides(rawUpcoming, mappedCustom)
        return UpcomingReleasePolicy.filterStrictUpcoming(overridden)
    }

    private fun buildLanguageCatalogUiList(
        rawTmdbItems: List<MediaItem>,
        languageCode: String,
        mediaType: String,
        sortBy: String,
        mappedCustom: List<MediaItem>
    ): List<MediaItem> {
        if (languageCode.isBlank()) return emptyList()
        val matchingCustom = mappedCustom.filter { custom ->
            custom.originalLanguage.equals(languageCode, ignoreCase = true) &&
                (mediaType == "All" || custom.mediaType.equals(mediaType, ignoreCase = true))
        }
        val merged = (matchingCustom + rawTmdbItems).distinctBy { it.id }
        val overridden = CatalogOverridePolicy.applyAdminOverrides(merged, mappedCustom)
        return DiscoverFilters(
            mediaType = mediaType,
            originalLanguage = languageCode,
            sortBy = sortBy
        ).sortItems(overridden)
    }

    private fun observeCustomMedia() {
        viewModelScope.launch {
            repository.customMedia.collect { list ->
                rawCustomEntities = list
                val mappedCustom = list.map { it.toMediaItem() }
                _uiState.update { current ->
                    current.copy(
                        customMedia = mappedCustom,
                        trending = CatalogOverridePolicy.applyAdminOverrides(rawTrending, mappedCustom),
                        trendingMovies = CatalogOverridePolicy.applyAdminOverrides(rawTrendingMovies, mappedCustom),
                        trendingTv = CatalogOverridePolicy.applyAdminOverrides(rawTrendingTv, mappedCustom),
                        newReleases = buildDailyNewReleasesUiList(rawNewReleases, list, mappedCustom),
                        popularMovies = CatalogOverridePolicy.applyAdminOverrides(rawPopularMovies, mappedCustom),
                        upcomingMovies = buildStrictUpcomingUiList(rawUpcomingMovies, mappedCustom),
                        topRatedMovies = CatalogOverridePolicy.applyAdminOverrides(rawTopRatedMovies, mappedCustom),
                        topRatedTv = CatalogOverridePolicy.applyAdminOverrides(rawTopRatedTv, mappedCustom),
                        searchResults = CatalogOverridePolicy.applyAdminOverrides(rawSearchResults, mappedCustom),
                        searchSuggestions = CatalogOverridePolicy.applyAdminOverrides(
                            SearchSuggestionPolicy.rankSuggestions(
                                tmdbItems = rawSearchSuggestions,
                                customMedia = mappedCustom,
                                query = current.searchQuery
                            ),
                            mappedCustom
                        ),
                        discoverResults = CatalogOverridePolicy.applyAdminOverrides(rawDiscoverResults, mappedCustom),
                        languageCatalogItems = buildLanguageCatalogUiList(
                            rawTmdbItems = rawLanguageCatalog,
                            languageCode = current.selectedLanguageCode,
                            mediaType = current.languageCatalogMediaType,
                            sortBy = current.languageCatalogSortBy,
                            mappedCustom = mappedCustom
                        ),
                        recommendedForYou = CatalogOverridePolicy.applyAdminOverrides(rawRecommendedForYou, mappedCustom),
                        becauseYouWatchedItems = CatalogOverridePolicy.applyAdminOverrides(rawBecauseYouWatched, mappedCustom),
                        popularInGenreItems = CatalogOverridePolicy.applyAdminOverrides(rawPopularInGenre, mappedCustom),
                        similarForSelectedMedia = CatalogOverridePolicy.applyAdminOverrides(rawSimilarForSelected, mappedCustom),
                        selectedMedia = current.selectedMedia?.let {
                            CatalogOverridePolicy.resolveItemWithOverrides(it, mappedCustom)
                        },
                        playingMedia = current.playingMedia?.let {
                            CatalogOverridePolicy.resolveItemWithOverrides(it, mappedCustom)
                        }
                    )
                }
            }
        }
    }

    private suspend fun consumeNextBatch(
        existingVisible: List<MediaItem>,
        currentFeed: FeedPaginationState,
        batchSize: Int,
        fetcher: suspend (startPage: Int, neededCount: Int) -> Result<PagedCatalogBatch>
    ): Pair<List<MediaItem>, FeedPaginationState> {
        val safeBatch = CatalogBatchPolicy.normalizeBatchSize(batchSize)
        val existingIds = existingVisible.mapTo(HashSet(existingVisible.size + safeBatch)) { it.id }
        val filteredBuffer = currentFeed.buffer.filter { it.id !in existingIds }

        val candidatePool = filteredBuffer.toMutableList()
        var nextPage = currentFeed.nextPage
        var totalPages = currentFeed.totalPages
        var totalResults = currentFeed.totalResults

        if (candidatePool.size < safeBatch && nextPage <= totalPages) {
            val missing = safeBatch - candidatePool.size
            val result = fetcher(nextPage, missing)
            result.onSuccess { batch ->
                val seenInPool = candidatePool.mapTo(HashSet(candidatePool.size)) { it.id }
                batch.items.forEach { item ->
                    if (item.id !in existingIds && item.id !in seenInPool) {
                        candidatePool.add(item)
                        seenInPool.add(item.id)
                    }
                }
                nextPage = batch.lastFetchedPage + 1
                totalPages = batch.totalPages
                totalResults = batch.totalResults
            }
        }

        val toEmit = candidatePool.take(safeBatch)
        val remainingBuffer = if (candidatePool.size > toEmit.size) {
            candidatePool.drop(toEmit.size)
        } else {
            emptyList()
        }
        val updatedFeed = FeedPaginationState(
            nextPage = nextPage,
            totalPages = totalPages,
            totalResults = totalResults,
            buffer = remainingBuffer
        )
        return (existingVisible + toEmit) to updatedFeed
    }

    fun loadHomeData() {
        viewModelScope.launch {
            val targetBatchSize = _uiState.value.batchSize
            lastDailyFeedDate = UpcomingReleasePolicy.todayIsoDate()
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                supervisorScope {
                    val trendingDeferred = async { repository.getTrendingBatch(1, targetBatchSize) }
                    val trendingMoviesDef = async { repository.getTrendingMoviesBatch(1, targetBatchSize) }
                    val trendingTvDef = async { repository.getTrendingTvBatch(1, targetBatchSize) }
                    val newReleasesDef = async { repository.getNewReleasesBatch(1, targetBatchSize) }
                    val popularDeferred = async { repository.getPopularMoviesBatch(1, targetBatchSize) }
                    val upcomingDeferred = async { repository.getUpcomingMoviesBatch(1, targetBatchSize) }
                    val topRatedMoviesDef = async { repository.getTopRatedMoviesBatch(1, targetBatchSize) }
                    val tvDeferred = async { repository.getTopRatedTvBatch(1, targetBatchSize) }

                    val trendingResult = trendingDeferred.await()
                    val trendingMoviesRes = trendingMoviesDef.await()
                    val trendingTvRes = trendingTvDef.await()
                    val newReleasesRes = newReleasesDef.await()
                    val popularResult = popularDeferred.await()
                    val upcomingRes = upcomingDeferred.await()
                    val topRatedMoviesRes = topRatedMoviesDef.await()
                    val tvResult = tvDeferred.await()

                    val allFailed = trendingResult.isFailure && popularResult.isFailure && tvResult.isFailure

                    val trendingBatch = trendingResult.getOrNull()
                    val trendingMoviesBatch = trendingMoviesRes.getOrNull()
                    val trendingTvBatch = trendingTvRes.getOrNull()
                    val newReleasesBatch = newReleasesRes.getOrNull()
                    val popularBatch = popularResult.getOrNull()
                    val upcomingBatch = upcomingRes.getOrNull()
                    val topRatedMoviesBatch = topRatedMoviesRes.getOrNull()
                    val tvBatch = tvResult.getOrNull()

                    val trendingItems = trendingBatch?.items?.take(targetBatchSize).orEmpty()
                    val trendingMovieItems = trendingMoviesBatch?.items?.take(targetBatchSize).orEmpty()
                    val trendingTvItems = trendingTvBatch?.items?.take(targetBatchSize).orEmpty()
                    val newReleaseItems = newReleasesBatch?.items?.take(targetBatchSize).orEmpty()
                    val popularItems = popularBatch?.items?.take(targetBatchSize).orEmpty()
                    val upcomingItems = upcomingBatch?.items?.take(targetBatchSize).orEmpty()
                    val topRatedMovieItems = topRatedMoviesBatch?.items?.take(targetBatchSize).orEmpty()
                    val tvItems = tvBatch?.items?.take(targetBatchSize).orEmpty()

                    if (trendingItems.isNotEmpty()) rawTrending = trendingItems
                    if (trendingMovieItems.isNotEmpty()) rawTrendingMovies = trendingMovieItems
                    if (trendingTvItems.isNotEmpty()) rawTrendingTv = trendingTvItems
                    if (newReleaseItems.isNotEmpty()) rawNewReleases = newReleaseItems
                    if (popularItems.isNotEmpty()) rawPopularMovies = popularItems
                    if (upcomingBatch != null) rawUpcomingMovies = upcomingItems
                    if (topRatedMovieItems.isNotEmpty()) rawTopRatedMovies = topRatedMovieItems
                    if (tvItems.isNotEmpty()) rawTopRatedTv = tvItems

                    trendingBatch?.let {
                        trendingFeed = FeedPaginationState(it.lastFetchedPage + 1, it.totalPages, it.totalResults, it.items.drop(trendingItems.size))
                    }
                    trendingMoviesBatch?.let {
                        trendingMoviesFeed = FeedPaginationState(it.lastFetchedPage + 1, it.totalPages, it.totalResults, it.items.drop(trendingMovieItems.size))
                    }
                    trendingTvBatch?.let {
                        trendingTvFeed = FeedPaginationState(it.lastFetchedPage + 1, it.totalPages, it.totalResults, it.items.drop(trendingTvItems.size))
                    }
                    newReleasesBatch?.let {
                        newReleasesFeed = FeedPaginationState(it.lastFetchedPage + 1, it.totalPages, it.totalResults, it.items.drop(newReleaseItems.size))
                    }
                    popularBatch?.let {
                        moviesFeed = FeedPaginationState(it.lastFetchedPage + 1, it.totalPages, it.totalResults, it.items.drop(popularItems.size))
                    }
                    upcomingBatch?.let {
                        upcomingFeed = FeedPaginationState(it.lastFetchedPage + 1, it.totalPages, it.totalResults, it.items.drop(upcomingItems.size))
                    }
                    topRatedMoviesBatch?.let {
                        topRatedMoviesFeed = FeedPaginationState(it.lastFetchedPage + 1, it.totalPages, it.totalResults, it.items.drop(topRatedMovieItems.size))
                    }
                    tvBatch?.let {
                        tvFeed = FeedPaginationState(it.lastFetchedPage + 1, it.totalPages, it.totalResults, it.items.drop(tvItems.size))
                    }

                    _uiState.update { state ->
                        val overrides = state.customMedia
                        val resolvedNewReleases = buildDailyNewReleasesUiList(rawNewReleases, rawCustomEntities, overrides)
                        val resolvedUpcoming = buildStrictUpcomingUiList(rawUpcomingMovies, overrides)
                        state.copy(
                            trending = CatalogOverridePolicy.applyAdminOverrides(rawTrending, overrides),
                            trendingMovies = CatalogOverridePolicy.applyAdminOverrides(rawTrendingMovies, overrides),
                            trendingTv = CatalogOverridePolicy.applyAdminOverrides(rawTrendingTv, overrides),
                            newReleases = resolvedNewReleases,
                            popularMovies = CatalogOverridePolicy.applyAdminOverrides(rawPopularMovies, overrides),
                            upcomingMovies = resolvedUpcoming,
                            topRatedMovies = CatalogOverridePolicy.applyAdminOverrides(rawTopRatedMovies, overrides),
                            topRatedTv = CatalogOverridePolicy.applyAdminOverrides(rawTopRatedTv, overrides),
                            trendingTotalResults = trendingBatch?.totalResults ?: state.trendingTotalResults,
                            trendingMoviesTotalResults = trendingMoviesBatch?.totalResults ?: state.trendingMoviesTotalResults,
                            trendingTvTotalResults = trendingTvBatch?.totalResults ?: state.trendingTvTotalResults,
                            newReleasesTotalResults = maxOf(newReleasesBatch?.totalResults ?: state.newReleasesTotalResults, resolvedNewReleases.size),
                            popularMoviesTotalResults = popularBatch?.totalResults ?: state.popularMoviesTotalResults,
                            upcomingMoviesTotalResults = maxOf(upcomingBatch?.totalResults ?: state.upcomingMoviesTotalResults, resolvedUpcoming.size),
                            topRatedMoviesTotalResults = topRatedMoviesBatch?.totalResults ?: state.topRatedMoviesTotalResults,
                            topRatedTvTotalResults = tvBatch?.totalResults ?: state.topRatedTvTotalResults,
                            hasMoreTrending = trendingFeed.hasMore(),
                            hasMoreTrendingMovies = trendingMoviesFeed.hasMore(),
                            hasMoreTrendingTv = trendingTvFeed.hasMore(),
                            hasMoreNewReleases = newReleasesFeed.hasMore(),
                            hasMorePopularMovies = moviesFeed.hasMore(),
                            hasMoreUpcomingMovies = upcomingFeed.hasMore(),
                            hasMoreTopRatedMovies = topRatedMoviesFeed.hasMore(),
                            hasMoreTopRatedTv = tvFeed.hasMore(),
                            isLoading = false,
                            errorMessage = if (allFailed) {
                                "Unable to reach TMDb right now. Please check your internet connection and tap Retry."
                            } else {
                                null
                            }
                        )
                    }

                    refreshSmartRecommendations()
                    val tvItemsToEnrich = (trendingItems + trendingTvItems + tvItems)
                        .filter { it.mediaType == "tv" && it.seasons.isEmpty() }
                        .distinctBy { it.id }
                        .take(20)
                    if (tvItemsToEnrich.isNotEmpty()) {
                        enrichTvItemsInBackground(tvItemsToEnrich)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load catalog: ${e.localizedMessage ?: "Network error"}"
                    )
                }
            }
        }
    }

    fun refreshSmartRecommendations() {
        viewModelScope.launch {
            val cw = continueWatching.value
            val wl = watchlist.value
            val topGenre = RecommendationPolicy.resolveTopPreferredGenre(cw, wl)
            val anchor = RecommendationPolicy.resolveBecauseYouWatchedAnchor(cw, wl)

            val candidatePool = (rawTrending + rawPopularMovies + rawTopRatedMovies + rawTopRatedTv + rawTrendingTv)
                .distinctBy { it.id }
            val immediateRanked = RecommendationPolicy.rankRecommendedForYou(
                candidatePool = candidatePool,
                continueWatching = cw,
                watchlist = wl
            )
            if (immediateRanked.isNotEmpty()) {
                rawRecommendedForYou = immediateRanked
                _uiState.update {
                    it.copy(
                        recommendedForYou = CatalogOverridePolicy.applyAdminOverrides(rawRecommendedForYou, it.customMedia),
                        popularInGenreName = topGenre
                    )
                }
            }

            _uiState.update { it.copy(isLoadingRecommendations = true, popularInGenreName = topGenre) }
            try {
                supervisorScope {
                    val becauseDef = async {
                        if (anchor != null) {
                            repository.fetchRecommendationsForMedia(anchor)
                        } else {
                            emptyList()
                        }
                    }
                    val genreDef = async {
                        repository.fetchPopularInGenre(topGenre)
                    }

                    val becauseFetched = becauseDef.await()
                    val genreFetched = genreDef.await()

                    val combinedForYou = RecommendationPolicy.rankRecommendedForYou(
                        candidatePool = (becauseFetched + genreFetched + candidatePool).distinctBy { it.id },
                        continueWatching = cw,
                        watchlist = wl
                    )

                    if (combinedForYou.isNotEmpty()) rawRecommendedForYou = combinedForYou
                    rawBecauseYouWatched = if (becauseFetched.isNotEmpty()) {
                        becauseFetched
                    } else {
                        // Useful fallback when watch history is empty
                        rawTopRatedMovies.take(12)
                    }
                    if (genreFetched.isNotEmpty()) rawPopularInGenre = genreFetched

                    _uiState.update { state ->
                        val overrides = state.customMedia
                        state.copy(
                            recommendedForYou = CatalogOverridePolicy.applyAdminOverrides(rawRecommendedForYou, overrides),
                            becauseYouWatchedTitle = anchor?.title.orEmpty(),
                            becauseYouWatchedItems = CatalogOverridePolicy.applyAdminOverrides(rawBecauseYouWatched, overrides),
                            popularInGenreName = topGenre,
                            popularInGenreItems = CatalogOverridePolicy.applyAdminOverrides(rawPopularInGenre, overrides),
                            isLoadingRecommendations = false
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingRecommendations = false) }
            }
        }
    }

    // Advanced Search & Discover Filters
    fun applyDiscoverFilters(filters: DiscoverFilters) {
        _uiState.update {
            it.copy(
                discoverFilters = filters,
                selectedCategory = filters.mediaType,
                sortBy = filters.sortBy,
                selectedGenre = if (filters.selectedGenres.size == 1) filters.selectedGenres.first() else "All"
            )
        }
        if (filters.hasDiscoverApiCriteria() || filters.mediaType != "All") {
            fetchDiscoverBatch(filters)
        } else {
            rawDiscoverResults = emptyList()
            discoverFeed = FeedPaginationState()
            _uiState.update {
                it.copy(
                    discoverResults = emptyList(),
                    discoverTotalResults = 0,
                    hasMoreDiscover = false,
                    isLoadingDiscover = false
                )
            }
        }
    }

    fun resetAllExploreFilters() {
        applyDiscoverFilters(DiscoverFilters())
        if (_uiState.value.searchQuery.isNotBlank()) {
            onSearchQueryChange("")
        }
    }

    private fun fetchDiscoverBatch(filters: DiscoverFilters) {
        viewModelScope.launch {
            val targetBatch = _uiState.value.batchSize
            _uiState.update { it.copy(isLoadingDiscover = true, searchErrorMessage = null) }
            val result = repository.discoverCatalogBatch(
                filters = filters,
                startPage = 1,
                neededCount = targetBatch
            )
            result.fold(
                onSuccess = { batch ->
                    val items = batch.items.take(targetBatch)
                    rawDiscoverResults = items
                    discoverFeed = FeedPaginationState(
                        nextPage = batch.lastFetchedPage + 1,
                        totalPages = batch.totalPages,
                        totalResults = batch.totalResults,
                        buffer = batch.items.drop(items.size)
                    )
                    _uiState.update { current ->
                        current.copy(
                            discoverResults = CatalogOverridePolicy.applyAdminOverrides(items, current.customMedia),
                            discoverTotalResults = maxOf(batch.totalResults, items.size),
                            hasMoreDiscover = discoverFeed.hasMore(),
                            isLoadingDiscover = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingDiscover = false) }
                }
            )
        }
    }

    fun loadMoreDiscover() {
        val state = _uiState.value
        if (state.isLoadingMoreDiscover || !discoverFeed.hasMore()) return
        val activeFilters = state.discoverFilters
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreDiscover = true) }
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawDiscoverResults,
                currentFeed = discoverFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.discoverCatalogBatch(activeFilters, page, needed) }
            )
            rawDiscoverResults = updatedRaw
            discoverFeed = newFeed
            _uiState.update { current ->
                current.copy(
                    discoverResults = CatalogOverridePolicy.applyAdminOverrides(updatedRaw, current.customMedia),
                    discoverTotalResults = maxOf(newFeed.totalResults, updatedRaw.size),
                    hasMoreDiscover = newFeed.hasMore(),
                    isLoadingMoreDiscover = false
                )
            }
        }
    }

    fun setBatchSize(newSize: Int) {
        val normalized = CatalogBatchPolicy.normalizeBatchSize(newSize)
        _uiState.update { it.copy(batchSize = normalized) }
    }

    fun loadMoreCategory(categoryKey: String) {
        when (categoryKey) {
            "Trending" -> loadMoreTrending()
            "TrendingMovies" -> loadMoreTrendingMovies()
            "TrendingTv" -> loadMoreTrendingTv()
            "NewReleases" -> loadMoreNewReleases()
            "Movies", "PopularMovies", "Top10Movies" -> loadMoreMovies()
            "UpcomingMovies" -> loadMoreUpcomingMovies()
            "TopRatedMovies" -> loadMoreTopRatedMovies()
            "TV", "TopRatedTv", "Top10Tv" -> loadMoreTv()
            "Discover" -> loadMoreDiscover()
            "Specials" -> Unit
            else -> {
                loadMoreMovies()
                loadMoreTv()
            }
        }
    }

    private fun loadMoreNewReleases() {
        val state = _uiState.value
        if (state.isLoadingMoreNewReleases || !newReleasesFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreNewReleases = true) }
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawNewReleases,
                currentFeed = newReleasesFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getNewReleasesBatch(page, needed) }
            )
            rawNewReleases = updatedRaw
            newReleasesFeed = newFeed
            _uiState.update { current ->
                val resolved = buildDailyNewReleasesUiList(updatedRaw, rawCustomEntities, current.customMedia)
                current.copy(
                    newReleases = resolved,
                    newReleasesTotalResults = maxOf(newFeed.totalResults, resolved.size),
                    hasMoreNewReleases = newFeed.hasMore(),
                    isLoadingMoreNewReleases = false
                )
            }
        }
    }

    private fun loadMoreTrending() {
        val state = _uiState.value
        if (state.isLoadingMoreTrending || !trendingFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreTrending = true) }
            val previousSize = rawTrending.size
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawTrending,
                currentFeed = trendingFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getTrendingBatch(page, needed) }
            )
            rawTrending = updatedRaw
            trendingFeed = newFeed
            _uiState.update { current ->
                current.copy(
                    trending = CatalogOverridePolicy.applyAdminOverrides(updatedRaw, current.customMedia),
                    trendingTotalResults = maxOf(newFeed.totalResults, updatedRaw.size),
                    hasMoreTrending = newFeed.hasMore(),
                    isLoadingMoreTrending = false
                )
            }
            val newlyAddedTv = updatedRaw.drop(previousSize).filter { it.mediaType == "tv" && it.seasons.isEmpty() }.take(20)
            if (newlyAddedTv.isNotEmpty()) enrichTvItemsInBackground(newlyAddedTv)
        }
    }

    private fun loadMoreTrendingMovies() {
        val state = _uiState.value
        if (state.isLoadingMoreTrendingMovies || !trendingMoviesFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreTrendingMovies = true) }
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawTrendingMovies,
                currentFeed = trendingMoviesFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getTrendingMoviesBatch(page, needed) }
            )
            rawTrendingMovies = updatedRaw
            trendingMoviesFeed = newFeed
            _uiState.update { current ->
                current.copy(
                    trendingMovies = CatalogOverridePolicy.applyAdminOverrides(updatedRaw, current.customMedia),
                    trendingMoviesTotalResults = maxOf(newFeed.totalResults, updatedRaw.size),
                    hasMoreTrendingMovies = newFeed.hasMore(),
                    isLoadingMoreTrendingMovies = false
                )
            }
        }
    }

    private fun loadMoreTrendingTv() {
        val state = _uiState.value
        if (state.isLoadingMoreTrendingTv || !trendingTvFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreTrendingTv = true) }
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawTrendingTv,
                currentFeed = trendingTvFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getTrendingTvBatch(page, needed) }
            )
            rawTrendingTv = updatedRaw
            trendingTvFeed = newFeed
            _uiState.update { current ->
                current.copy(
                    trendingTv = CatalogOverridePolicy.applyAdminOverrides(updatedRaw, current.customMedia),
                    trendingTvTotalResults = maxOf(newFeed.totalResults, updatedRaw.size),
                    hasMoreTrendingTv = newFeed.hasMore(),
                    isLoadingMoreTrendingTv = false
                )
            }
        }
    }

    private fun loadMoreMovies() {
        val state = _uiState.value
        if (state.isLoadingMoreMovies || !moviesFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreMovies = true) }
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawPopularMovies,
                currentFeed = moviesFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getPopularMoviesBatch(page, needed) }
            )
            rawPopularMovies = updatedRaw
            moviesFeed = newFeed
            _uiState.update { current ->
                current.copy(
                    popularMovies = CatalogOverridePolicy.applyAdminOverrides(updatedRaw, current.customMedia),
                    popularMoviesTotalResults = maxOf(newFeed.totalResults, updatedRaw.size),
                    hasMorePopularMovies = newFeed.hasMore(),
                    isLoadingMoreMovies = false
                )
            }
        }
    }

    private fun loadMoreUpcomingMovies() {
        val state = _uiState.value
        if (state.isLoadingMoreUpcomingMovies || !upcomingFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreUpcomingMovies = true) }
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawUpcomingMovies,
                currentFeed = upcomingFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getUpcomingMoviesBatch(page, needed) }
            )
            rawUpcomingMovies = updatedRaw
            upcomingFeed = newFeed
            _uiState.update { current ->
                val strictUpcoming = buildStrictUpcomingUiList(updatedRaw, current.customMedia)
                current.copy(
                    upcomingMovies = strictUpcoming,
                    upcomingMoviesTotalResults = maxOf(newFeed.totalResults, strictUpcoming.size),
                    hasMoreUpcomingMovies = newFeed.hasMore(),
                    isLoadingMoreUpcomingMovies = false
                )
            }
        }
    }

    private fun loadMoreTopRatedMovies() {
        val state = _uiState.value
        if (state.isLoadingMoreTopRatedMovies || !topRatedMoviesFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreTopRatedMovies = true) }
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawTopRatedMovies,
                currentFeed = topRatedMoviesFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getTopRatedMoviesBatch(page, needed) }
            )
            rawTopRatedMovies = updatedRaw
            topRatedMoviesFeed = newFeed
            _uiState.update { current ->
                current.copy(
                    topRatedMovies = CatalogOverridePolicy.applyAdminOverrides(updatedRaw, current.customMedia),
                    topRatedMoviesTotalResults = maxOf(newFeed.totalResults, updatedRaw.size),
                    hasMoreTopRatedMovies = newFeed.hasMore(),
                    isLoadingMoreTopRatedMovies = false
                )
            }
        }
    }

    private fun loadMoreTv() {
        val state = _uiState.value
        if (state.isLoadingMoreTv || !tvFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreTv = true) }
            val previousSize = rawTopRatedTv.size
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawTopRatedTv,
                currentFeed = tvFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getTopRatedTvBatch(page, needed) }
            )
            rawTopRatedTv = updatedRaw
            tvFeed = newFeed
            _uiState.update { current ->
                current.copy(
                    topRatedTv = CatalogOverridePolicy.applyAdminOverrides(updatedRaw, current.customMedia),
                    topRatedTvTotalResults = maxOf(newFeed.totalResults, updatedRaw.size),
                    hasMoreTopRatedTv = newFeed.hasMore(),
                    isLoadingMoreTv = false
                )
            }
            val newlyAddedTv = updatedRaw.drop(previousSize).filter { it.mediaType == "tv" && it.seasons.isEmpty() }.take(20)
            if (newlyAddedTv.isNotEmpty()) enrichTvItemsInBackground(newlyAddedTv)
        }
    }

    fun navigateTo(screen: Screen) {
        val current = _uiState.value
        if (current.currentScreen == screen) return
        val newStack = if (screen is Screen.Home) {
            listOf(Screen.Home)
        } else {
            current.backStack + screen
        }
        _uiState.value = current.copy(
            currentScreen = screen,
            backStack = newStack,
            isNavigatingBack = false,
            reportSubmitMessage = if (screen is Screen.ReportProblem) null else current.reportSubmitMessage
        )
    }

    fun navigateBack(): Boolean {
        val stack = _uiState.value.backStack
        if (stack.size > 1) {
            val poppedStack = stack.dropLast(1)
            val previousScreen = poppedStack.last()
            _uiState.value = _uiState.value.copy(
                currentScreen = previousScreen,
                backStack = poppedStack,
                isNavigatingBack = true
            )
            return true
        }
        return false
    }

    fun openReportProblem(
        media: MediaItem? = null,
        serverName: String = "",
        episodeLabel: String = ""
    ) {
        navigateTo(Screen.ReportProblem(prefilledMedia = media, serverName = serverName, episodeLabel = episodeLabel))
    }

    fun submitProblemReport(
        category: IssueReportCategory,
        description: String,
        media: MediaItem?,
        serverName: String,
        episodeLabel: String,
        screenshotBase64: String
    ) {
        if (description.trim().length < 5) {
            _uiState.update {
                it.copy(
                    reportSubmitMessage = "Please enter at least 5 characters describing the issue.",
                    isReportError = true
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReport = true, reportSubmitMessage = null, isReportError = false) }
            val now = System.currentTimeMillis()
            val entity = IssueReportEntity(
                id = "rep_${UUID.randomUUID().toString().take(10)}",
                category = category.storageKey,
                description = description.trim(),
                mediaId = media?.id.orEmpty(),
                mediaTitle = media?.title.orEmpty(),
                mediaType = media?.mediaType.orEmpty(),
                serverName = serverName.trim(),
                episodeLabel = episodeLabel.trim(),
                screenshotPreviewBase64 = screenshotBase64,
                status = "OPEN",
                adminNotes = "",
                createdAt = now,
                updatedAt = now
            )
            val result = repository.submitIssueReport(entity)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubmittingReport = false,
                            reportSubmitMessage = "✓ Report submitted! Track status updates below.",
                            isReportError = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSubmittingReport = false,
                            reportSubmitMessage = "Saved locally; cloud upload pending (${err.localizedMessage ?: "offline"}).",
                            isReportError = false
                        )
                    }
                }
            )
        }
    }

    fun clearReportFeedback() {
        _uiState.update { it.copy(reportSubmitMessage = null, isReportError = false) }
    }

    fun onSearchQueryChange(query: String) {
        searchJob?.cancel()
        suggestionJob?.cancel()
        searchHistoryCommitJob?.cancel()
        if (query.isBlank()) {
            rawSearchResults = emptyList()
            rawSearchSuggestions = emptyList()
            searchFeed = FeedPaginationState()
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    searchResults = emptyList(),
                    searchTotalResults = 0,
                    hasMoreSearch = false,
                    isLoadingMoreSearch = false,
                    isSearching = false,
                    searchErrorMessage = null,
                    searchSuggestions = emptyList(),
                    isFetchingSuggestions = false,
                    showSearchSuggestions = false
                )
            }
            return
        }
        val trimmedQuery = query.trim()
        val instantSeed = SearchSuggestionPolicy.rankSuggestions(
            tmdbItems = (rawSearchSuggestions + rawTrending + rawPopularMovies + rawTopRatedMovies + rawTopRatedTv),
            customMedia = _uiState.value.customMedia,
            query = trimmedQuery
        )
        _uiState.update {
            it.copy(
                searchQuery = query,
                isSearching = true,
                searchResults = emptyList(),
                searchTotalResults = 0,
                hasMoreSearch = false,
                isLoadingMoreSearch = false,
                searchErrorMessage = null,
                searchSuggestions = instantSeed,
                isFetchingSuggestions = true,
                showSearchSuggestions = true
            )
        }

        // Fast real-time TMDB autocomplete suggestions (150ms debounce)
        suggestionJob = viewModelScope.launch {
            delay(150)
            repository.fetchSearchSuggestions(trimmedQuery).fold(
                onSuccess = { remoteSuggestions ->
                    val ranked = SearchSuggestionPolicy.rankSuggestions(
                        tmdbItems = remoteSuggestions,
                        customMedia = _uiState.value.customMedia,
                        query = trimmedQuery
                    )
                    rawSearchSuggestions = ranked
                    _uiState.update { current ->
                        if (current.searchQuery.trim() != trimmedQuery) {
                            current
                        } else {
                            current.copy(
                                searchSuggestions = CatalogOverridePolicy.applyAdminOverrides(ranked, current.customMedia),
                                isFetchingSuggestions = false
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update { current ->
                        current.copy(isFetchingSuggestions = false)
                    }
                }
            )
        }

        // Full paginated search results grid (380ms debounce)
        searchJob = viewModelScope.launch {
            delay(380)
            executeFullSearchBatch(trimmedQuery)
        }

        // Commit query to search history once typing settles (prevents partial keystroke spam)
        if (trimmedQuery.length >= 2) {
            searchHistoryCommitJob = viewModelScope.launch {
                delay(1100)
                repository.saveTmdbSearch(trimmedQuery)
            }
        }
    }

    fun submitSearchQuery(query: String = _uiState.value.searchQuery) {
        val trimmed = query.trim()
        searchHistoryCommitJob?.cancel()
        _uiState.update { it.copy(showSearchSuggestions = false) }
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            if (trimmed.length >= 2) {
                repository.saveTmdbSearch(trimmed)
            }
            if (rawSearchResults.isEmpty() || _uiState.value.searchQuery.trim() != trimmed) {
                searchJob?.cancel()
                _uiState.update { it.copy(searchQuery = trimmed, isSearching = true, searchErrorMessage = null) }
                executeFullSearchBatch(trimmed)
            }
        }
    }

    fun applySearchSuggestionQuery(suggestedTitle: String) {
        val clean = suggestedTitle.trim()
        if (clean.isBlank()) return
        suggestionJob?.cancel()
        searchJob?.cancel()
        searchHistoryCommitJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = clean,
                showSearchSuggestions = false,
                isFetchingSuggestions = false,
                isSearching = true,
                searchErrorMessage = null
            )
        }
        viewModelScope.launch {
            if (clean.length >= 2) {
                repository.saveTmdbSearch(clean)
            }
            executeFullSearchBatch(clean)
        }
    }

    fun selectSearchSuggestion(media: MediaItem) {
        searchHistoryCommitJob?.cancel()
        _uiState.update { it.copy(showSearchSuggestions = false) }
        if (media.title.trim().length >= 2) {
            viewModelScope.launch {
                repository.saveTmdbSearch(media.title.trim())
            }
        }
        openDetails(media)
    }

    fun dismissSearchSuggestions() {
        _uiState.update { it.copy(showSearchSuggestions = false) }
    }

    fun showSearchSuggestionsIfAvailable() {
        if (_uiState.value.searchQuery.isNotBlank()) {
            _uiState.update { it.copy(showSearchSuggestions = true) }
        }
    }

    private suspend fun executeFullSearchBatch(trimmedQuery: String) {
        val targetBatch = _uiState.value.batchSize
        val result = repository.searchTitlesBatch(
            query = trimmedQuery,
            startPage = 1,
            neededCount = targetBatch
        )
        result.fold(
            onSuccess = { batch ->
                val initialItems = batch.items.take(targetBatch)
                rawSearchResults = initialItems
                searchFeed = FeedPaginationState(
                    nextPage = batch.lastFetchedPage + 1,
                    totalPages = batch.totalPages,
                    totalResults = batch.totalResults,
                    buffer = batch.items.drop(initialItems.size)
                )
                _uiState.update { current ->
                    val updatedSuggestions = if (current.searchSuggestions.isEmpty()) {
                        SearchSuggestionPolicy.rankSuggestions(
                            tmdbItems = initialItems,
                            customMedia = current.customMedia,
                            query = trimmedQuery
                        )
                    } else {
                        current.searchSuggestions
                    }
                    current.copy(
                        searchResults = CatalogOverridePolicy.applyAdminOverrides(initialItems, current.customMedia),
                        searchTotalResults = maxOf(batch.totalResults, initialItems.size),
                        hasMoreSearch = searchFeed.hasMore(),
                        isSearching = false,
                        searchErrorMessage = null,
                        searchSuggestions = CatalogOverridePolicy.applyAdminOverrides(updatedSuggestions, current.customMedia)
                    )
                }
                val tvSearchItems = initialItems
                    .filter { it.mediaType == "tv" && it.seasons.isEmpty() }
                    .take(15)
                if (tvSearchItems.isNotEmpty()) {
                    enrichTvItemsInBackground(tvSearchItems)
                }
            },
            onFailure = { err ->
                rawSearchResults = emptyList()
                searchFeed = FeedPaginationState()
                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        searchTotalResults = 0,
                        hasMoreSearch = false,
                        isSearching = false,
                        searchErrorMessage = "TMDb search failed (${err.localizedMessage ?: "offline"}). Check connection and try again."
                    )
                }
            }
        )
    }

    fun loadMoreSearchResults() {
        val state = _uiState.value
        val trimmedQuery = state.searchQuery.trim()
        if (trimmedQuery.isBlank() || state.isSearching || state.isLoadingMoreSearch || !searchFeed.hasMore()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreSearch = true) }
            val previousSize = rawSearchResults.size
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawSearchResults,
                currentFeed = searchFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.searchTitlesBatch(trimmedQuery, page, needed) }
            )
            rawSearchResults = updatedRaw
            searchFeed = newFeed
            _uiState.update { current ->
                current.copy(
                    searchResults = CatalogOverridePolicy.applyAdminOverrides(updatedRaw, current.customMedia),
                    searchTotalResults = maxOf(newFeed.totalResults, updatedRaw.size),
                    hasMoreSearch = newFeed.hasMore(),
                    isLoadingMoreSearch = false
                )
            }
            val newlyAddedTv = updatedRaw.drop(previousSize)
                .filter { it.mediaType == "tv" && it.seasons.isEmpty() }
                .take(15)
            if (newlyAddedTv.isNotEmpty()) {
                enrichTvItemsInBackground(newlyAddedTv)
            }
        }
    }

    fun deleteSearchHistory(query: String) {
        viewModelScope.launch { repository.deleteTmdbSearch(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearTmdbSearchHistory()
            _uiState.update { it.copy(settingsFeedbackMessage = "Search history cleared.") }
        }
    }

    fun selectDarkThemePalette(palette: DarkThemePalette) {
        _uiState.update {
            it.copy(
                selectedPalette = palette,
                preferredHighReadabilityPalette = if (palette.isHighReadability) palette else it.preferredHighReadabilityPalette
            )
        }
        viewModelScope.launch {
            userPreferences?.saveDarkThemePalette(palette)
        }
    }

    fun toggleHighReadabilityPalette() {
        val current = _uiState.value.selectedPalette
        val preferred = _uiState.value.preferredHighReadabilityPalette
        val next = ThemeTogglePolicy.toggleHighReadability(current, preferred)
        selectDarkThemePalette(next)
    }

    fun selectThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch { userPreferences?.saveThemeMode(mode) }
    }

    fun selectAccentColor(option: AccentColorOption) {
        _uiState.update { it.copy(accentColor = option) }
        viewModelScope.launch { userPreferences?.saveAccentColor(option) }
    }

    fun selectAppLanguage(language: AppLanguageOption) {
        val changed = repository.currentLanguageCode != language.tmdbCode
        repository.currentLanguageCode = language.tmdbCode
        _uiState.update {
            it.copy(
                appLanguage = language,
                settingsFeedbackMessage = "Content language set to ${language.displayName}."
            )
        }
        viewModelScope.launch {
            userPreferences?.saveAppLanguage(language)
            if (changed) {
                loadHomeData()
            }
        }
    }

    fun updateNotificationPreferences(
        enabled: Boolean = _uiState.value.notificationPrefs.enabled,
        newEpisodesEnabled: Boolean = _uiState.value.notificationPrefs.newEpisodesEnabled,
        newMoviesEnabled: Boolean = _uiState.value.notificationPrefs.newMoviesEnabled
    ) {
        val updated = NotificationPreferences(
            enabled = enabled,
            newEpisodesEnabled = newEpisodesEnabled,
            newMoviesEnabled = newMoviesEnabled
        )
        _uiState.update { it.copy(notificationPrefs = updated) }
        viewModelScope.launch {
            userPreferences?.saveNotificationPreferences(updated)
        }
    }

    fun checkNewReleasesAndNotify(silentIfNoPermission: Boolean = false) {
        val ctx = appContext ?: return
        val nPrefs = _uiState.value.notificationPrefs
        if (!nPrefs.enabled) {
            if (!silentIfNoPermission) {
                _uiState.update { it.copy(settingsFeedbackMessage = "Notifications are disabled in Settings.") }
            }
            return
        }
        if (!EpisodeNotificationHelper.hasNotificationPermission(ctx)) {
            if (!silentIfNoPermission) {
                _uiState.update { it.copy(settingsFeedbackMessage = "Grant notification permission above to receive release alerts.") }
            }
            return
        }
        viewModelScope.launch {
            val alerts = repository.checkForNewReleaseAlerts(
                watchlist = watchlist.value,
                customMedia = repository.customMedia.firstOrNull().orEmpty(),
                checkEpisodes = nPrefs.newEpisodesEnabled,
                checkNewMovies = nPrefs.newMoviesEnabled
            )
            var posted = 0
            alerts.forEach { alert ->
                if (EpisodeNotificationHelper.postReleaseNotification(ctx, alert)) {
                    posted++
                }
            }
            if (!silentIfNoPermission) {
                val msg = if (posted > 0) {
                    "Sent $posted new release alert(s) to your notification shade."
                } else {
                    "You're all caught up! No new unreleased episode alerts right now."
                }
                _uiState.update { it.copy(settingsFeedbackMessage = msg) }
            }
        }
    }

    fun refreshCacheSize() {
        val ctx = appContext ?: return
        val bytes = repository.getImageCacheSizeBytes(ctx)
        _uiState.update { it.copy(cacheSizeLabel = formatByteSize(bytes)) }
    }

    fun clearAppCache() {
        val ctx = appContext ?: return
        viewModelScope.launch {
            val clearedBytes = repository.clearCaches(ctx)
            val afterBytes = repository.getImageCacheSizeBytes(ctx)
            _uiState.update {
                it.copy(
                    cacheSizeLabel = formatByteSize(afterBytes),
                    settingsFeedbackMessage = "Cleared ${formatByteSize(clearedBytes)} of cached posters & API data."
                )
            }
        }
    }

    fun dismissSettingsFeedback() {
        _uiState.update { it.copy(settingsFeedbackMessage = null) }
    }

    private fun formatByteSize(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        return when {
            safe < 1024L -> "$safe B"
            safe < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", safe / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", safe / (1024.0 * 1024.0))
        }
    }

    fun setDefaultPlayerServer(server: PlayerServer) {
        _uiState.update { it.copy(playerServer = server) }
        viewModelScope.launch {
            userPreferences?.saveDefaultPlayerServer(server)
        }
    }

    fun selectHomeGenre(genre: String) {
        _uiState.update { it.copy(selectedHomeGenre = genre) }
    }

    fun openExploreWithGenre(genre: String) {
        val updatedFilters = DiscoverFilters(
            mediaType = "All",
            selectedGenres = if (genre.equals("All", ignoreCase = true) || genre.isBlank()) emptySet() else setOf(genre)
        )
        applyDiscoverFilters(updatedFilters)
        navigateTo(Screen.Explore)
    }

    fun openExploreSection(category: String = "All", genre: String = "All") {
        val updatedFilters = DiscoverFilters(
            mediaType = category,
            selectedGenres = if (genre.equals("All", ignoreCase = true) || genre.isBlank()) emptySet() else setOf(genre)
        )
        applyDiscoverFilters(updatedFilters)
        navigateTo(Screen.Explore)
    }

    fun openSearch() {
        navigateTo(Screen.Search)
    }

    fun openCategoryList(title: String, categoryKey: String, genreFilter: String = "All") {
        navigateTo(Screen.CategoryList(title, categoryKey, genreFilter))
    }

    fun openLanguageCatalog(languageCode: String, languageName: String) {
        val cleanCode = languageCode.trim().lowercase(Locale.US)
        if (cleanCode.isBlank()) return
        val current = _uiState.value
        val sameLanguage = current.selectedLanguageCode.equals(cleanCode, ignoreCase = true) &&
            rawLanguageCatalog.isNotEmpty() &&
            current.languageCatalogError == null
        navigateTo(Screen.LanguageCatalog(cleanCode, languageName))
        if (sameLanguage) return

        rawLanguageCatalog = emptyList()
        languageCatalogFeed = FeedPaginationState()
        _uiState.update {
            it.copy(
                selectedLanguageCode = cleanCode,
                selectedLanguageName = languageName,
                languageCatalogMediaType = "All",
                languageCatalogSortBy = "popular",
                languageCatalogItems = buildLanguageCatalogUiList(
                    rawTmdbItems = emptyList(),
                    languageCode = cleanCode,
                    mediaType = "All",
                    sortBy = "popular",
                    mappedCustom = it.customMedia
                ),
                languageCatalogTotalResults = 0,
                hasMoreLanguageCatalog = false,
                isLoadingMoreLanguageCatalog = false,
                languageCatalogError = null
            )
        }
        fetchLanguageCatalogInitial(
            languageCode = cleanCode,
            mediaType = "All",
            sortBy = "popular"
        )
    }

    fun setLanguageCatalogMediaType(mediaType: String) {
        val current = _uiState.value
        if (current.selectedLanguageCode.isBlank() || current.languageCatalogMediaType == mediaType) return
        rawLanguageCatalog = emptyList()
        languageCatalogFeed = FeedPaginationState()
        _uiState.update {
            it.copy(
                languageCatalogMediaType = mediaType,
                languageCatalogItems = emptyList(),
                languageCatalogTotalResults = 0,
                hasMoreLanguageCatalog = false,
                languageCatalogError = null
            )
        }
        fetchLanguageCatalogInitial(
            languageCode = current.selectedLanguageCode,
            mediaType = mediaType,
            sortBy = current.languageCatalogSortBy
        )
    }

    fun setLanguageCatalogSortBy(sortBy: String) {
        val current = _uiState.value
        if (current.selectedLanguageCode.isBlank() || current.languageCatalogSortBy == sortBy) return
        rawLanguageCatalog = emptyList()
        languageCatalogFeed = FeedPaginationState()
        _uiState.update {
            it.copy(
                languageCatalogSortBy = sortBy,
                languageCatalogItems = emptyList(),
                languageCatalogTotalResults = 0,
                hasMoreLanguageCatalog = false,
                languageCatalogError = null
            )
        }
        fetchLanguageCatalogInitial(
            languageCode = current.selectedLanguageCode,
            mediaType = current.languageCatalogMediaType,
            sortBy = sortBy
        )
    }

    fun retryLanguageCatalog() {
        val current = _uiState.value
        if (current.selectedLanguageCode.isBlank()) return
        fetchLanguageCatalogInitial(
            languageCode = current.selectedLanguageCode,
            mediaType = current.languageCatalogMediaType,
            sortBy = current.languageCatalogSortBy
        )
    }

    private fun fetchLanguageCatalogInitial(
        languageCode: String,
        mediaType: String,
        sortBy: String
    ) {
        languageCatalogJob?.cancel()
        languageCatalogJob = viewModelScope.launch {
            val targetBatch = _uiState.value.batchSize
            val filters = DiscoverFilters(
                mediaType = mediaType,
                originalLanguage = languageCode,
                sortBy = sortBy
            )
            _uiState.update {
                it.copy(
                    isLoadingLanguageCatalog = true,
                    languageCatalogError = null
                )
            }
            val result = repository.discoverCatalogBatch(
                filters = filters,
                startPage = 1,
                neededCount = targetBatch
            )
            result.fold(
                onSuccess = { batch ->
                    val items = batch.items.take(targetBatch)
                    rawLanguageCatalog = items
                    languageCatalogFeed = FeedPaginationState(
                        nextPage = batch.lastFetchedPage + 1,
                        totalPages = batch.totalPages,
                        totalResults = batch.totalResults,
                        buffer = batch.items.drop(items.size)
                    )
                    _uiState.update { state ->
                        val resolved = buildLanguageCatalogUiList(
                            rawTmdbItems = items,
                            languageCode = languageCode,
                            mediaType = mediaType,
                            sortBy = sortBy,
                            mappedCustom = state.customMedia
                        )
                        state.copy(
                            languageCatalogItems = resolved,
                            languageCatalogTotalResults = maxOf(batch.totalResults, resolved.size),
                            hasMoreLanguageCatalog = languageCatalogFeed.hasMore(),
                            isLoadingLanguageCatalog = false,
                            languageCatalogError = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { state ->
                        val customFallback = buildLanguageCatalogUiList(
                            rawTmdbItems = emptyList(),
                            languageCode = languageCode,
                            mediaType = mediaType,
                            sortBy = sortBy,
                            mappedCustom = state.customMedia
                        )
                        state.copy(
                            languageCatalogItems = customFallback,
                            languageCatalogTotalResults = customFallback.size,
                            hasMoreLanguageCatalog = false,
                            isLoadingLanguageCatalog = false,
                            languageCatalogError = if (customFallback.isEmpty()) {
                                "Unable to load ${state.selectedLanguageName} titles (${err.localizedMessage ?: "offline"})."
                            } else {
                                null
                            }
                        )
                    }
                }
            )
        }
    }

    fun loadMoreLanguageCatalog() {
        val state = _uiState.value
        if (state.selectedLanguageCode.isBlank() ||
            state.isLoadingLanguageCatalog ||
            state.isLoadingMoreLanguageCatalog ||
            !languageCatalogFeed.hasMore()
        ) return

        val langCode = state.selectedLanguageCode
        val mediaType = state.languageCatalogMediaType
        val sortBy = state.languageCatalogSortBy
        val filters = DiscoverFilters(
            mediaType = mediaType,
            originalLanguage = langCode,
            sortBy = sortBy
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreLanguageCatalog = true) }
            val (updatedRaw, newFeed) = consumeNextBatch(
                existingVisible = rawLanguageCatalog,
                currentFeed = languageCatalogFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.discoverCatalogBatch(filters, page, needed) }
            )
            rawLanguageCatalog = updatedRaw
            languageCatalogFeed = newFeed
            _uiState.update { current ->
                val resolved = buildLanguageCatalogUiList(
                    rawTmdbItems = updatedRaw,
                    languageCode = langCode,
                    mediaType = mediaType,
                    sortBy = sortBy,
                    mappedCustom = current.customMedia
                )
                current.copy(
                    languageCatalogItems = resolved,
                    languageCatalogTotalResults = maxOf(newFeed.totalResults, resolved.size),
                    hasMoreLanguageCatalog = newFeed.hasMore(),
                    isLoadingMoreLanguageCatalog = false
                )
            }
        }
    }

    fun selectGenre(genre: String) {
        val current = _uiState.value.discoverFilters
        val updatedGenres = if (genre.equals("All", ignoreCase = true)) {
            emptySet()
        } else {
            setOf(genre)
        }
        applyDiscoverFilters(current.copy(selectedGenres = updatedGenres))
    }

    fun selectCategory(category: String) {
        val current = _uiState.value.discoverFilters
        applyDiscoverFilters(current.copy(mediaType = category))
    }

    fun selectSortBy(sort: String) {
        val current = _uiState.value.discoverFilters
        applyDiscoverFilters(current.copy(sortBy = sort))
    }

    fun openDetails(media: MediaItem) {
        val resolvedMedia = CatalogOverridePolicy.resolveItemWithOverrides(media, _uiState.value.customMedia)
        rawSimilarForSelected = emptyList()
        _uiState.update { it.copy(selectedMedia = resolvedMedia, similarForSelectedMedia = emptyList()) }
        navigateTo(Screen.Details(resolvedMedia))

        val resolvedId = resolvedMedia.resolvedTmdbId()
        if (resolvedId != null && resolvedMedia.trailerYoutubeKey == null) {
            viewModelScope.launch {
                val key = repository.fetchTrailerYoutubeKey(resolvedId, resolvedMedia.mediaType)
                if (key != null) {
                    val currentSelected = _uiState.value.selectedMedia
                    if (currentSelected?.id == resolvedMedia.id) {
                        _uiState.update {
                            it.copy(
                                selectedMedia = currentSelected.copy(trailerYoutubeKey = key),
                                trailerYoutubeKey = key
                            )
                        }
                    }
                }
            }
        }
        if (resolvedId != null && resolvedMedia.certification.isBlank()) {
            viewModelScope.launch {
                val cert = repository.fetchCertification(resolvedId, resolvedMedia.mediaType)
                if (cert.isNotBlank()) {
                    _uiState.update { current ->
                        val curSelected = current.selectedMedia
                        if (curSelected?.id == resolvedMedia.id && curSelected.certification.isBlank()) {
                            current.copy(selectedMedia = curSelected.copy(certification = cert))
                        } else {
                            current
                        }
                    }
                }
            }
        }
        if (resolvedId != null) {
            viewModelScope.launch {
                val similar = repository.fetchSimilarMedia(resolvedMedia)
                if (similar.isNotEmpty() && _uiState.value.selectedMedia?.id == resolvedMedia.id) {
                    rawSimilarForSelected = similar
                    _uiState.update { current ->
                        current.copy(
                            similarForSelectedMedia = CatalogOverridePolicy.applyAdminOverrides(similar, current.customMedia)
                        )
                    }
                }
            }
        }
        if (resolvedMedia.mediaType == "tv") {
            ensureTvSeriesLoaded(resolvedMedia, initialSeason = resolvedMedia.defaultSeasonNumber())
        }
    }

    fun openPlayer(media: MediaItem, season: Int = 1, episode: Int = 1) {
        val resolvedMedia = CatalogOverridePolicy.resolveItemWithOverrides(media, _uiState.value.customMedia)
        val (safeSeason, safeEpisode) = ContinueWatchingPolicy.normalizeSeasonEpisode(
            mediaType = resolvedMedia.mediaType,
            season = season,
            episode = episode
        )
        val episodesInSeason = _uiState.value.episodesByMediaAndSeason[resolvedMedia.id]?.get(safeSeason).orEmpty()
        val playbackSource = PlaybackSourcePolicy.resolvePlaybackSource(
            media = resolvedMedia,
            season = safeSeason,
            episode = safeEpisode,
            episodesInSeason = episodesInSeason,
            downloads = downloads.value
        )

        when (playbackSource) {
            is PlaybackSource.LocalFile -> {
                viewModelScope.launch {
                    val savedPosMs = repository.getPlaybackPosition(
                        mediaId = resolvedMedia.id,
                        mediaType = resolvedMedia.mediaType,
                        season = safeSeason,
                        episode = safeEpisode
                    )
                    repository.saveContinueWatching(
                        item = resolvedMedia,
                        season = safeSeason,
                        episode = safeEpisode,
                        watchedSeconds = savedPosMs / 1000L,
                        durationSeconds = 0L,
                        progressPercent = 0
                    )
                    _uiState.update {
                        it.copy(
                            playingMedia = resolvedMedia,
                            currentSeason = safeSeason,
                            currentEpisode = safeEpisode
                        )
                    }
                    navigateTo(
                        Screen.DirectPlayer(
                            media = resolvedMedia,
                            season = safeSeason,
                            episode = safeEpisode,
                            sourceUrl = playbackSource.filePath,
                            isLocalFile = true,
                            downloadEntity = playbackSource.download,
                            initialPositionMs = savedPosMs
                        )
                    )
                }
            }
            is PlaybackSource.DirectStream -> {
                viewModelScope.launch {
                    val savedPosMs = repository.getPlaybackPosition(
                        mediaId = resolvedMedia.id,
                        mediaType = resolvedMedia.mediaType,
                        season = safeSeason,
                        episode = safeEpisode
                    )
                    repository.saveContinueWatching(
                        item = resolvedMedia,
                        season = safeSeason,
                        episode = safeEpisode,
                        watchedSeconds = savedPosMs / 1000L,
                        durationSeconds = 0L,
                        progressPercent = 0
                    )
                    _uiState.update {
                        it.copy(
                            playingMedia = resolvedMedia,
                            currentSeason = safeSeason,
                            currentEpisode = safeEpisode
                        )
                    }
                    navigateTo(
                        Screen.DirectPlayer(
                            media = resolvedMedia,
                            season = safeSeason,
                            episode = safeEpisode,
                            sourceUrl = playbackSource.url,
                            isLocalFile = false,
                            downloadEntity = null,
                            initialPositionMs = savedPosMs
                        )
                    )
                }
                if (resolvedMedia.mediaType == "tv") {
                    ensureTvSeriesLoaded(resolvedMedia, initialSeason = safeSeason)
                }
            }
            is PlaybackSource.WebEmbed -> {
                openWebEmbedPlayer(resolvedMedia, safeSeason, safeEpisode)
            }
        }
    }

    fun switchDirectEpisode(media: MediaItem, season: Int, episode: Int) {
        val resolvedMedia = CatalogOverridePolicy.resolveItemWithOverrides(media, _uiState.value.customMedia)
        val (safeSeason, safeEpisode) = ContinueWatchingPolicy.normalizeSeasonEpisode(
            mediaType = resolvedMedia.mediaType,
            season = season,
            episode = episode
        )
        val episodesInSeason = _uiState.value.episodesByMediaAndSeason[resolvedMedia.id]?.get(safeSeason).orEmpty()
        val playbackSource = PlaybackSourcePolicy.resolvePlaybackSource(
            media = resolvedMedia,
            season = safeSeason,
            episode = safeEpisode,
            episodesInSeason = episodesInSeason,
            downloads = downloads.value
        )

        fun replaceTopPlayerScreen(nextScreen: Screen) {
            _uiState.update { current ->
                val trimmedStack = if (current.backStack.lastOrNull() is Screen.DirectPlayer) {
                    current.backStack.dropLast(1)
                } else {
                    current.backStack
                }
                current.copy(
                    playingMedia = resolvedMedia,
                    currentSeason = safeSeason,
                    currentEpisode = safeEpisode,
                    currentScreen = nextScreen,
                    backStack = trimmedStack + nextScreen,
                    isNavigatingBack = false
                )
            }
        }

        when (playbackSource) {
            is PlaybackSource.LocalFile -> {
                viewModelScope.launch {
                    val savedPosMs = repository.getPlaybackPosition(
                        mediaId = resolvedMedia.id,
                        mediaType = resolvedMedia.mediaType,
                        season = safeSeason,
                        episode = safeEpisode
                    )
                    replaceTopPlayerScreen(
                        Screen.DirectPlayer(
                            media = resolvedMedia,
                            season = safeSeason,
                            episode = safeEpisode,
                            sourceUrl = playbackSource.filePath,
                            isLocalFile = true,
                            downloadEntity = playbackSource.download,
                            initialPositionMs = savedPosMs
                        )
                    )
                }
            }
            is PlaybackSource.DirectStream -> {
                viewModelScope.launch {
                    val savedPosMs = repository.getPlaybackPosition(
                        mediaId = resolvedMedia.id,
                        mediaType = resolvedMedia.mediaType,
                        season = safeSeason,
                        episode = safeEpisode
                    )
                    replaceTopPlayerScreen(
                        Screen.DirectPlayer(
                            media = resolvedMedia,
                            season = safeSeason,
                            episode = safeEpisode,
                            sourceUrl = playbackSource.url,
                            isLocalFile = false,
                            downloadEntity = null,
                            initialPositionMs = savedPosMs
                        )
                    )
                }
                if (resolvedMedia.mediaType == "tv") {
                    ensureTvSeriesLoaded(resolvedMedia, initialSeason = safeSeason)
                }
            }
            is PlaybackSource.WebEmbed -> {
                val basePlayerScreen = Screen.Player(resolvedMedia, safeSeason, safeEpisode)
                replaceTopPlayerScreen(basePlayerScreen)
                if (resolvedMedia.mediaType == "tv") {
                    ensureTvSeriesLoaded(resolvedMedia, initialSeason = safeSeason)
                }
            }
        }
    }

    fun openWebEmbedPlayer(media: MediaItem, season: Int = 1, episode: Int = 1) {
        val resolvedMedia = CatalogOverridePolicy.resolveItemWithOverrides(media, _uiState.value.customMedia)
        val (safeSeason, safeEpisode) = ContinueWatchingPolicy.normalizeSeasonEpisode(
            mediaType = resolvedMedia.mediaType,
            season = season,
            episode = episode
        )
        val existing = continueWatching.value.firstOrNull { it.id == resolvedMedia.id }
        val preserve = ContinueWatchingPolicy.shouldPreserveProgress(
            existing = existing,
            mediaType = resolvedMedia.mediaType,
            targetSeason = safeSeason,
            targetEpisode = safeEpisode
        )
        val initialWatched = if (preserve) (existing?.watchedSeconds ?: 0L) else 0L
        val initialDuration = if (preserve) (existing?.durationSeconds ?: 0L) else 0L
        val initialPercent = if (preserve) (existing?.progressPercent ?: 0) else 0

        _uiState.value = _uiState.value.copy(
            playingMedia = resolvedMedia,
            currentSeason = safeSeason,
            currentEpisode = safeEpisode,
            playerServer = if (resolvedMedia.isWebTorrentStream()) PlayerServer.WEBTORRENT else _uiState.value.playerServer
        )
        viewModelScope.launch {
            repository.saveContinueWatching(
                item = resolvedMedia,
                season = safeSeason,
                episode = safeEpisode,
                watchedSeconds = initialWatched,
                durationSeconds = initialDuration,
                progressPercent = initialPercent
            )
        }
        navigateTo(Screen.Player(resolvedMedia, safeSeason, safeEpisode))
        if (resolvedMedia.mediaType == "tv") {
            ensureTvSeriesLoaded(resolvedMedia, initialSeason = safeSeason)
        }
    }

    fun saveDirectPlaybackPosition(
        media: MediaItem,
        season: Int,
        episode: Int,
        positionMs: Long,
        durationMs: Long
    ) {
        viewModelScope.launch {
            repository.savePlaybackPosition(
                mediaId = media.id,
                mediaType = media.mediaType,
                season = season,
                episode = episode,
                positionMs = positionMs,
                durationMs = durationMs
            )
            if (durationMs > 0L) {
                repository.saveContinueWatching(
                    item = media,
                    season = season,
                    episode = episode,
                    watchedSeconds = positionMs / 1000L,
                    durationSeconds = durationMs / 1000L
                )
            }
        }
    }

    fun openMagnetStream(torrentUri: String, title: String = "") {
        val normalized = com.example.ui.components.WebTorrentPolicy.normalizeTorrentId(torrentUri, title)
        if (!com.example.ui.components.WebTorrentPolicy.isWebTorrentSource(normalized)) return
        val resolvedTitle = title.trim().ifBlank {
            if (normalized == com.example.ui.components.WebTorrentPolicy.SAMPLE_SINTEL_MAGNET) {
                "Sintel (WebTorrent Open Demo)"
            } else {
                "WebTorrent P2P Stream"
            }
        }
        val media = MediaItem(
            id = "wt_${normalized.hashCode().toUInt()}",
            title = resolvedTitle,
            overview = "Peer-to-peer WebTorrent stream",
            posterPath = "",
            mediaType = "movie",
            vidlyId = normalized,
            isCustom = true
        )
        _uiState.update {
            it.copy(
                playingMedia = media,
                currentSeason = 1,
                currentEpisode = 1,
                playerServer = PlayerServer.WEBTORRENT
            )
        }
        navigateTo(Screen.Player(media, 1, 1))
    }

    fun switchPlayerServer(server: PlayerServer) {
        _uiState.value = _uiState.value.copy(playerServer = server)
    }

    fun switchEpisode(season: Int, episode: Int) {
        val media = _uiState.value.playingMedia ?: return
        val (safeSeason, safeEpisode) = ContinueWatchingPolicy.normalizeSeasonEpisode(
            mediaType = media.mediaType,
            season = season,
            episode = episode
        )
        _uiState.value = _uiState.value.copy(
            currentSeason = safeSeason,
            currentEpisode = safeEpisode
        )
        loadSeasonEpisodes(media, safeSeason)
        viewModelScope.launch {
            repository.saveContinueWatching(
                item = media,
                season = safeSeason,
                episode = safeEpisode,
                watchedSeconds = 0L,
                durationSeconds = 0L,
                progressPercent = 0
            )
        }
    }

    fun updateWatchProgress(
        media: MediaItem,
        season: Int,
        episode: Int,
        watchedSeconds: Long,
        durationSeconds: Long
    ) {
        viewModelScope.launch {
            repository.saveContinueWatching(
                item = media,
                season = season,
                episode = episode,
                watchedSeconds = watchedSeconds,
                durationSeconds = durationSeconds
            )
        }
    }

    fun loadSeasonEpisodes(media: MediaItem, seasonNumber: Int) {
        if (media.mediaType != "tv") return
        val tmdbId = media.resolvedTmdbId() ?: return
        val existing = _uiState.value.episodesByMediaAndSeason[media.id]?.get(seasonNumber)
        if (!existing.isNullOrEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingSeasonNumber = seasonNumber)
            val episodes = repository.fetchTvSeasonEpisodes(tmdbId, seasonNumber)
            if (episodes.isNotEmpty()) {
                storeLoadedSeasonEpisodes(media.id, seasonNumber, episodes)
            }
            if (_uiState.value.loadingSeasonNumber == seasonNumber) {
                _uiState.value = _uiState.value.copy(loadingSeasonNumber = null)
            }
        }
    }

    private fun ensureTvSeriesLoaded(media: MediaItem, initialSeason: Int) {
        val tmdbId = media.resolvedTmdbId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSeasons = true)
            val enriched = repository.fetchEnrichedTvSeries(media)
            applyEnrichedMediaToState(enriched)

            val seasonList = enriched.resolvedSeasonList()
            val primarySeason = if (seasonList.any { it.seasonNumber == initialSeason }) {
                initialSeason
            } else {
                enriched.defaultSeasonNumber()
            }

            if (_uiState.value.episodesByMediaAndSeason[enriched.id]?.get(primarySeason).isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(loadingSeasonNumber = primarySeason)
                val primaryEpisodes = repository.fetchTvSeasonEpisodes(tmdbId, primarySeason)
                if (primaryEpisodes.isNotEmpty()) {
                    storeLoadedSeasonEpisodes(enriched.id, primarySeason, primaryEpisodes)
                }
            }

            _uiState.value = _uiState.value.copy(
                isLoadingSeasons = false,
                loadingSeasonNumber = null
            )

            val remainingSeasons = seasonList
                .map { it.seasonNumber }
                .filter { it != primarySeason }
                .take(12)
            if (remainingSeasons.isNotEmpty()) {
                supervisorScope {
                    remainingSeasons.map { seasonNum ->
                        async {
                            if (_uiState.value.episodesByMediaAndSeason[enriched.id]?.get(seasonNum).isNullOrEmpty()) {
                                val eps = repository.fetchTvSeasonEpisodes(tmdbId, seasonNum)
                                if (eps.isNotEmpty()) {
                                    storeLoadedSeasonEpisodes(enriched.id, seasonNum, eps)
                                }
                            }
                        }
                    }.awaitAll()
                }
            }
        }
    }

    private fun enrichTvItemsInBackground(items: List<MediaItem>) {
        val distinctItems = items.distinctBy { it.id }
        viewModelScope.launch {
            val enrichedMap = supervisorScope {
                distinctItems.map { item ->
                    async {
                        val enriched = repository.fetchEnrichedTvSeries(item)
                        enriched.id to enriched
                    }
                }.awaitAll().toMap()
            }
            rawTrending = rawTrending.map { enrichedMap[it.id] ?: it }
            rawTrendingTv = rawTrendingTv.map { enrichedMap[it.id] ?: it }
            rawTopRatedTv = rawTopRatedTv.map { enrichedMap[it.id] ?: it }
            rawSearchResults = rawSearchResults.map { enrichedMap[it.id] ?: it }
            rawLanguageCatalog = rawLanguageCatalog.map { enrichedMap[it.id] ?: it }
            _uiState.update { current ->
                val overrides = current.customMedia
                current.copy(
                    trending = CatalogOverridePolicy.applyAdminOverrides(rawTrending, overrides),
                    trendingTv = CatalogOverridePolicy.applyAdminOverrides(rawTrendingTv, overrides),
                    topRatedTv = CatalogOverridePolicy.applyAdminOverrides(rawTopRatedTv, overrides),
                    searchResults = CatalogOverridePolicy.applyAdminOverrides(rawSearchResults, overrides),
                    languageCatalogItems = buildLanguageCatalogUiList(
                        rawTmdbItems = rawLanguageCatalog,
                        languageCode = current.selectedLanguageCode,
                        mediaType = current.languageCatalogMediaType,
                        sortBy = current.languageCatalogSortBy,
                        mappedCustom = overrides
                    ),
                    selectedMedia = current.selectedMedia?.let { sel ->
                        CatalogOverridePolicy.resolveItemWithOverrides(enrichedMap[sel.id] ?: sel, overrides)
                    },
                    playingMedia = current.playingMedia?.let { pl ->
                        CatalogOverridePolicy.resolveItemWithOverrides(enrichedMap[pl.id] ?: pl, overrides)
                    }
                )
            }
        }
    }

    private fun applyEnrichedMediaToState(enriched: MediaItem) {
        rawTrending = rawTrending.map { if (it.id == enriched.id) enriched else it }
        rawTrendingTv = rawTrendingTv.map { if (it.id == enriched.id) enriched else it }
        rawTopRatedTv = rawTopRatedTv.map { if (it.id == enriched.id) enriched else it }
        rawSearchResults = rawSearchResults.map { if (it.id == enriched.id) enriched else it }
        rawLanguageCatalog = rawLanguageCatalog.map { if (it.id == enriched.id) enriched else it }
        _uiState.update { current ->
            val overrides = current.customMedia
            val resolvedEnriched = CatalogOverridePolicy.resolveItemWithOverrides(enriched, overrides)
            current.copy(
                selectedMedia = if (current.selectedMedia?.id == enriched.id) {
                    resolvedEnriched.copy(trailerYoutubeKey = resolvedEnriched.trailerYoutubeKey ?: current.selectedMedia.trailerYoutubeKey)
                } else {
                    current.selectedMedia
                },
                playingMedia = if (current.playingMedia?.id == enriched.id) {
                    resolvedEnriched.copy(trailerYoutubeKey = resolvedEnriched.trailerYoutubeKey ?: current.playingMedia.trailerYoutubeKey)
                } else {
                    current.playingMedia
                },
                trending = CatalogOverridePolicy.applyAdminOverrides(rawTrending, overrides),
                trendingTv = CatalogOverridePolicy.applyAdminOverrides(rawTrendingTv, overrides),
                topRatedTv = CatalogOverridePolicy.applyAdminOverrides(rawTopRatedTv, overrides),
                searchResults = CatalogOverridePolicy.applyAdminOverrides(rawSearchResults, overrides),
                languageCatalogItems = buildLanguageCatalogUiList(
                    rawTmdbItems = rawLanguageCatalog,
                    languageCode = current.selectedLanguageCode,
                    mediaType = current.languageCatalogMediaType,
                    sortBy = current.languageCatalogSortBy,
                    mappedCustom = overrides
                )
            )
        }
        viewModelScope.launch {
            repository.updateContinueWatchingMetadata(
                CatalogOverridePolicy.resolveItemWithOverrides(enriched, _uiState.value.customMedia)
            )
        }
    }

    private fun storeLoadedSeasonEpisodes(mediaId: String, seasonNumber: Int, episodes: List<EpisodeInfo>) {
        _uiState.update { current ->
            val existingForMedia = current.episodesByMediaAndSeason[mediaId].orEmpty()
            val updatedForMedia = existingForMedia + (seasonNumber to episodes)
            val updatedMap = current.episodesByMediaAndSeason + (mediaId to updatedForMedia)
            current.copy(episodesByMediaAndSeason = updatedMap)
        }
    }

    fun openTrailer(media: MediaItem) {
        val currentKey = media.trailerYoutubeKey
            ?: _uiState.value.selectedMedia?.takeIf { it.id == media.id }?.trailerYoutubeKey
        val resolvedId = media.resolvedTmdbId()
        _uiState.update {
            it.copy(
                selectedMedia = media,
                trailerYoutubeKey = currentKey,
                isTrailerLoading = currentKey == null && resolvedId != null,
                isTrailerModalOpen = true
            )
        }
        if (resolvedId != null && currentKey == null) {
            viewModelScope.launch {
                val key = repository.fetchTrailerYoutubeKey(resolvedId, media.mediaType)
                _uiState.update {
                    it.copy(
                        trailerYoutubeKey = key,
                        isTrailerLoading = false
                    )
                }
            }
        }
    }

    fun closeTrailer() {
        _uiState.update {
            it.copy(
                isTrailerModalOpen = false,
                isTrailerLoading = false
            )
        }
    }

    fun toggleWatchlist(media: MediaItem) {
        viewModelScope.launch {
            repository.toggleWatchlist(media)
        }
    }

    fun isItemInWatchlist(id: String): Boolean {
        return watchlist.value.any { it.id == id }
    }

    fun removeContinueWatching(id: String) {
        viewModelScope.launch {
            repository.removeContinueWatching(id)
        }
    }

    // =========================================================================
    // MovieBox API & Offline Download Manager
    // =========================================================================

    private fun observeDownloadsProgress() {
        viewModelScope.launch {
            repository.syncActiveDownloads()
            downloads.collect { list ->
                val hasRunningOrQueued = list.any { it.parsedStatus.isRunningOrQueued }
                if (hasRunningOrQueued && downloadSyncJob?.isActive != true) {
                    downloadSyncJob = viewModelScope.launch {
                        while (true) {
                            delay(4000L)
                            val synced = repository.syncActiveDownloads()
                            if (synced.none { it.parsedStatus.isRunningOrQueued }) break
                        }
                    }
                }
            }
        }
    }

    fun syncDownloadsNow() {
        viewModelScope.launch {
            repository.syncActiveDownloads()
        }
    }

    fun openDownloadsScreen() {
        syncDownloadsNow()
        navigateTo(Screen.Downloads)
    }

    fun openOfflinePlayer(download: DownloadEntity) {
        viewModelScope.launch {
            val savedPosMs = repository.getPlaybackPosition(
                mediaId = download.mediaId,
                mediaType = download.mediaType,
                season = download.season,
                episode = download.episode
            )
            navigateTo(Screen.OfflinePlayer(download, savedPosMs))
        }
    }

    fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            repository.pauseDownload(downloadId)
        }
    }

    fun resumeDownload(downloadId: String) {
        viewModelScope.launch {
            repository.resumeDownload(downloadId)
        }
    }

    fun startMovieOrEpisodeDownload(
        media: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        episodeTitle: String = ""
    ) {
        val resolvedMedia = CatalogOverridePolicy.resolveItemWithOverrides(media, _uiState.value.customMedia)
        val episodesInSeason = _uiState.value.episodesByMediaAndSeason[resolvedMedia.id]?.get(season).orEmpty()
        val link = resolvedMedia.resolveDirectLinkForEpisode(
            season = season,
            episode = episode,
            episodesInSeason = episodesInSeason
        )
        val directDownloadUrl = link.effectiveDownloadUrl()
        if (directDownloadUrl.startsWith("http://", ignoreCase = true) ||
            directDownloadUrl.startsWith("https://", ignoreCase = true)
        ) {
            val wifiOnly = _uiState.value.downloadPrefs.wifiOnly
            viewModelScope.launch {
                repository.startDirectUrlDownload(
                    media = resolvedMedia,
                    season = season,
                    episode = episode,
                    episodeTitle = episodeTitle,
                    directUrl = directDownloadUrl,
                    wifiOnly = wifiOnly
                ).fold(
                    onSuccess = { entry ->
                        val label = if (resolvedMedia.mediaType.equals("tv", true)) {
                            "Queued ${entry.title} S${entry.season}E${entry.episode}"
                        } else {
                            "Queued ${entry.title}"
                        }
                        _uiState.update { it.copy(downloadFeedbackMessage = label) }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(downloadFeedbackMessage = err.localizedMessage ?: "Download failed.")
                        }
                    }
                )
            }
        } else {
            openMovieBoxDownloadSheet(
                media = resolvedMedia,
                season = season,
                episode = episode,
                episodeTitle = episodeTitle
            )
        }
    }

    fun enqueueSeasonBatchDownload(
        media: MediaItem,
        seasonNumber: Int,
        selectedEpisodesOnly: Set<Int>? = null
    ) {
        val resolvedMedia = CatalogOverridePolicy.resolveItemWithOverrides(media, _uiState.value.customMedia)
        val episodesInSeason = _uiState.value.episodesByMediaAndSeason[resolvedMedia.id]?.get(seasonNumber).orEmpty()
        val episodeCount = resolvedMedia.episodesForSeason(seasonNumber)
        val allEpisodeNums = if (episodesInSeason.isNotEmpty()) {
            episodesInSeason.map { it.episodeNumber }
        } else {
            (1..episodeCount).toList()
        }

        val eligibility = PlaybackSourcePolicy.computeSeasonEligibility(
            media = resolvedMedia,
            seasonNumber = seasonNumber,
            episodeNumbers = allEpisodeNums,
            episodesInSeason = episodesInSeason,
            downloads = downloads.value
        )

        val targetEpisodes = if (selectedEpisodesOnly != null) {
            eligibility.eligibleEpisodes.filter { it in selectedEpisodesOnly }
        } else {
            eligibility.eligibleEpisodes
        }

        if (targetEpisodes.isEmpty()) {
            _uiState.update {
                it.copy(
                    downloadFeedbackMessage = if (eligibility.downloadedEpisodes.isNotEmpty() && eligibility.noLinkEpisodes.isEmpty()) {
                        "All episodes in Season $seasonNumber are already downloaded or queued."
                    } else {
                        "No episodes with direct download links available in Season $seasonNumber."
                    }
                )
            }
            return
        }

        val requests = targetEpisodes.mapNotNull { epNum ->
            val epInfo = episodesInSeason.firstOrNull { it.episodeNumber == epNum }
            val link = resolvedMedia.resolveDirectLinkForEpisode(
                season = seasonNumber,
                episode = epNum,
                episodesInSeason = episodesInSeason
            )
            val dlUrl = link.effectiveDownloadUrl()
            if (!dlUrl.startsWith("http", ignoreCase = true)) null
            else BatchEpisodeDownloadRequest(
                media = resolvedMedia,
                season = seasonNumber,
                episode = epNum,
                episodeTitle = epInfo?.name.orEmpty(),
                sourceUrl = dlUrl
            )
        }

        val wifiOnly = _uiState.value.downloadPrefs.wifiOnly
        viewModelScope.launch {
            val count = repository.enqueueBatchEpisodeDownloads(requests, wifiOnly)
            _uiState.update {
                it.copy(
                    downloadFeedbackMessage = if (count > 0) {
                        "Queued $count episode(s) from Season $seasonNumber (max 2 running at once)."
                    } else {
                        "Episodes already downloaded or queued."
                    }
                )
            }
        }
    }

    fun openMovieBoxDownloadSheet(
        media: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        episodeTitle: String = ""
    ) {
        val (safeSeason, safeEpisode) = ContinueWatchingPolicy.normalizeSeasonEpisode(
            mediaType = media.mediaType,
            season = season,
            episode = episode
        )
        downloadFetchJob?.cancel()
        _uiState.update {
            it.copy(
                downloadSheet = DownloadSheetUiState(
                    isOpen = true,
                    targetMedia = media,
                    season = safeSeason,
                    episode = safeEpisode,
                    episodeTitle = episodeTitle,
                    isLoading = true,
                    errorMessage = null,
                    bundle = null,
                    searchQuery = media.title,
                    searchCandidates = emptyList(),
                    isSearchingCandidates = false
                )
            )
        }

        val customHost = _uiState.value.downloadPrefs.movieBoxHostUrl
        downloadFetchJob = viewModelScope.launch {
            val bundleResult = repository.resolveMovieBoxDownloadBundle(
                media = media,
                season = safeSeason,
                episode = safeEpisode,
                customHost = customHost
            )

            bundleResult.fold(
                onSuccess = { bundle ->
                    _uiState.update { current ->
                        current.copy(
                            downloadSheet = current.downloadSheet.copy(
                                isLoading = false,
                                errorMessage = null,
                                bundle = bundle,
                                searchCandidates = emptyList()
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { current ->
                        current.copy(
                            downloadSheet = current.downloadSheet.copy(
                                isLoading = false,
                                errorMessage = err.localizedMessage
                                    ?: "Could not fetch download links for \"${media.title}\".",
                                bundle = null,
                                searchCandidates = emptyList()
                            )
                        )
                    }
                }
            )
        }
    }

    fun dismissMovieBoxDownloadSheet() {
        downloadFetchJob?.cancel()
        _uiState.update { it.copy(downloadSheet = DownloadSheetUiState()) }
    }

    fun searchMovieBoxCandidatesInSheet(query: String) {
        val clean = query.trim()
        _uiState.update {
            it.copy(
                downloadSheet = it.downloadSheet.copy(
                    searchQuery = query,
                    isSearchingCandidates = clean.isNotBlank()
                )
            )
        }
        if (clean.isBlank()) return
        downloadFetchJob?.cancel()
        val customHost = _uiState.value.downloadPrefs.movieBoxHostUrl
        downloadFetchJob = viewModelScope.launch {
            val res = repository.searchMovieBoxSubjects(clean, "all", customHost)
            res.fold(
                onSuccess = { items ->
                    _uiState.update { current ->
                        current.copy(
                            downloadSheet = current.downloadSheet.copy(
                                isSearchingCandidates = false,
                                searchCandidates = items,
                                errorMessage = if (items.isEmpty()) "No titles matched \"$clean\"." else null
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { current ->
                        current.copy(
                            downloadSheet = current.downloadSheet.copy(
                                isSearchingCandidates = false,
                                errorMessage = err.localizedMessage ?: "Search failed."
                            )
                        )
                    }
                }
            )
        }
    }

    fun selectMovieBoxSubjectInSheet(subject: MovieBoxSubject) {
        val sheet = _uiState.value.downloadSheet
        val targetMedia = sheet.targetMedia ?: return
        val customHost = _uiState.value.downloadPrefs.movieBoxHostUrl
        val isTv = targetMedia.mediaType.equals("tv", ignoreCase = true) || subject.subjectType == 2
        downloadFetchJob?.cancel()
        _uiState.update {
            it.copy(
                downloadSheet = it.downloadSheet.copy(
                    isLoading = true,
                    errorMessage = null
                )
            )
        }
        downloadFetchJob = viewModelScope.launch {
            val res = repository.fetchMovieBoxDownloadBundleForSubject(
                subject = subject,
                season = sheet.season,
                episode = sheet.episode,
                isTv = isTv,
                customHost = customHost
            )
            res.fold(
                onSuccess = { bundle ->
                    _uiState.update { current ->
                        current.copy(
                            downloadSheet = current.downloadSheet.copy(
                                isLoading = false,
                                errorMessage = null,
                                bundle = bundle
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { current ->
                        current.copy(
                            downloadSheet = current.downloadSheet.copy(
                                isLoading = false,
                                errorMessage = err.localizedMessage ?: "No downloadable files found."
                            )
                        )
                    }
                }
            )
        }
    }

    fun enqueueMovieBoxOptionDownload(
        option: MovieBoxDownloadOption,
        subtitle: MovieBoxSubtitleOption? = null
    ) {
        val sheet = _uiState.value.downloadSheet
        val media = sheet.targetMedia ?: return
        val wifiOnly = _uiState.value.downloadPrefs.wifiOnly

        viewModelScope.launch {
            val res = repository.startMovieBoxDownload(
                media = media,
                season = sheet.season,
                episode = sheet.episode,
                episodeTitle = sheet.episodeTitle,
                option = option,
                subtitle = subtitle,
                providerName = DownloadEngine.DEFAULT_CLOUD_PROVIDER,
                wifiOnly = wifiOnly
            )
            res.fold(
                onSuccess = { entry ->
                    _uiState.update {
                        it.copy(
                            downloadSheet = DownloadSheetUiState(),
                            downloadFeedbackMessage = "Downloading ${entry.title} (${entry.qualityLabel})"
                        )
                    }
                    syncDownloadsNow()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            downloadSheet = it.downloadSheet.copy(
                                errorMessage = err.localizedMessage ?: "Unable to start download."
                            )
                        )
                    }
                }
            )
        }
    }

    fun searchMovieBoxHub(query: String) {
        val clean = query.trim()
        movieBoxHubSearchJob?.cancel()
        _uiState.update {
            it.copy(
                movieBoxSearchQuery = query,
                isSearchingMovieBox = clean.isNotBlank(),
                movieBoxSearchError = null
            )
        }
        if (clean.isBlank()) {
            _uiState.update { it.copy(movieBoxSearchResults = emptyList(), isSearchingMovieBox = false) }
            return
        }
        val customHost = _uiState.value.downloadPrefs.movieBoxHostUrl
        movieBoxHubSearchJob = viewModelScope.launch {
            val res = repository.searchMovieBoxSubjects(clean, "all", customHost)
            res.fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            movieBoxSearchResults = items,
                            isSearchingMovieBox = false,
                            movieBoxSearchError = if (items.isEmpty()) "No titles found for \"$clean\"." else null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSearchingMovieBox = false,
                            movieBoxSearchError = err.localizedMessage ?: "Download catalog search failed."
                        )
                    }
                }
            )
        }
    }

    fun openDownloadSheetForMovieBoxSubject(subject: MovieBoxSubject, season: Int = 1, episode: Int = 1) {
        val media = MediaItem(
            id = "mb_${subject.subjectId}",
            title = subject.title,
            overview = subject.description,
            posterPath = subject.coverUrl,
            backdropPath = subject.coverUrl,
            mediaType = if (subject.subjectType == 2) "tv" else "movie",
            releaseDate = subject.releaseDate,
            voteAverage = subject.imdbRating.toDoubleOrNull() ?: 0.0,
            isCustom = true
        )
        downloadFetchJob?.cancel()
        _uiState.update {
            it.copy(
                downloadSheet = DownloadSheetUiState(
                    isOpen = true,
                    targetMedia = media,
                    season = season,
                    episode = episode,
                    isLoading = true,
                    searchQuery = subject.title,
                    searchCandidates = _uiState.value.movieBoxSearchResults
                )
            )
        }
        val customHost = _uiState.value.downloadPrefs.movieBoxHostUrl
        downloadFetchJob = viewModelScope.launch {
            val res = repository.fetchMovieBoxDownloadBundleForSubject(
                subject = subject,
                season = season,
                episode = episode,
                isTv = subject.subjectType == 2,
                customHost = customHost
            )
            res.fold(
                onSuccess = { bundle ->
                    _uiState.update { current ->
                        current.copy(
                            downloadSheet = current.downloadSheet.copy(
                                isLoading = false,
                                bundle = bundle,
                                errorMessage = null
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { current ->
                        current.copy(
                            downloadSheet = current.downloadSheet.copy(
                                isLoading = false,
                                errorMessage = err.localizedMessage ?: "No downloads available for this title."
                            )
                        )
                    }
                }
            )
        }
    }

    fun cancelOrDeleteDownload(downloadId: String) {
        viewModelScope.launch {
            repository.cancelAndDeleteDownload(downloadId)
            _uiState.update { it.copy(downloadFeedbackMessage = "Download removed.") }
        }
    }

    fun retryDownload(download: DownloadEntity) {
        val wifiOnly = _uiState.value.downloadPrefs.wifiOnly
        viewModelScope.launch {
            repository.retryDownload(download, wifiOnly).fold(
                onSuccess = {
                    _uiState.update { it.copy(downloadFeedbackMessage = "Retrying ${download.title}...") }
                    syncDownloadsNow()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(downloadFeedbackMessage = err.localizedMessage ?: "Retry failed.")
                    }
                }
            )
        }
    }

    fun exportDownloadToPublicFolder(download: DownloadEntity) {
        viewModelScope.launch {
            repository.exportDownloadToPublicFolder(download).fold(
                onSuccess = { msg ->
                    _uiState.update { it.copy(downloadFeedbackMessage = msg) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(downloadFeedbackMessage = err.localizedMessage ?: "Export failed.")
                    }
                }
            )
        }
    }

    fun dismissDownloadFeedback() {
        _uiState.update { it.copy(downloadFeedbackMessage = null) }
    }

    fun updateDownloadPreferences(
        movieBoxHostUrl: String = _uiState.value.downloadPrefs.movieBoxHostUrl,
        preferredQuality: PreferredDownloadQuality = _uiState.value.downloadPrefs.preferredQuality,
        wifiOnly: Boolean = _uiState.value.downloadPrefs.wifiOnly
    ) {
        val updated = DownloadPreferences(
            movieBoxHostUrl = MovieBoxApiClient.normalizeHostUrl(movieBoxHostUrl),
            preferredQuality = preferredQuality,
            wifiOnly = wifiOnly
        )
        _uiState.update {
            it.copy(
                downloadPrefs = updated,
                settingsFeedbackMessage = "Download settings saved (${updated.preferredQuality.displayName})."
            )
        }
        viewModelScope.launch {
            userPreferences?.saveDownloadPreferences(updated)
        }
    }

    fun testMovieBoxHostConnection(hostUrl: String) {
        viewModelScope.launch {
            MovieBoxApiClient.testHostConnection(hostUrl).fold(
                onSuccess = { msg ->
                    _uiState.update { it.copy(settingsFeedbackMessage = msg) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            settingsFeedbackMessage = "Connection failed: ${err.localizedMessage ?: "Network error"}"
                        )
                    }
                }
            )
        }
    }
}

class MovieViewModelFactory(
    private val repository: MovieRepository,
    private val userPreferences: UserPreferences? = null,
    private val appContext: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovieViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MovieViewModel(repository, userPreferences, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
