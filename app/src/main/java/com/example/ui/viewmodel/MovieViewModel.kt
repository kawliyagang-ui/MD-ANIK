package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.model.AdConfigEntity
import com.example.data.model.AdGatePolicy
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.EpisodeInfo
import com.example.data.model.MediaItem
import com.example.data.model.PagedCatalogBatch
import com.example.data.model.PlayerServer
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.data.model.WatchlistEntity
import com.example.data.model.toMediaItem
import com.example.data.repository.MovieRepository
import com.example.ui.theme.DarkThemePalette
import com.example.ui.theme.ThemeTogglePolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

sealed class Screen {
    object Home : Screen()
    object Explore : Screen()
    object Watchlist : Screen()
    object ContinueWatching : Screen()
    object Settings : Screen()
    object Search : Screen()
    data class CategoryList(
        val title: String,
        val categoryKey: String,
        val genreFilter: String = "All"
    ) : Screen()
    data class Details(val media: MediaItem) : Screen()
    data class Player(val media: MediaItem, val season: Int = 1, val episode: Int = 1) : Screen()
}

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
    val popularMovies: List<MediaItem> = emptyList(),
    val topRatedTv: List<MediaItem> = emptyList(),
    val customMedia: List<MediaItem> = emptyList(),
    val searchResults: List<MediaItem> = emptyList(),
    val trendingTotalResults: Int = 0,
    val popularMoviesTotalResults: Int = 0,
    val topRatedTvTotalResults: Int = 0,
    val searchTotalResults: Int = 0,
    val hasMoreTrending: Boolean = false,
    val hasMorePopularMovies: Boolean = false,
    val hasMoreTopRatedTv: Boolean = false,
    val hasMoreSearch: Boolean = false,
    val isLoadingMoreTrending: Boolean = false,
    val isLoadingMoreMovies: Boolean = false,
    val isLoadingMoreTv: Boolean = false,
    val isLoadingMoreSearch: Boolean = false,
    val searchQuery: String = "",
    val selectedHomeGenre: String = "All",
    val selectedGenre: String = "All",
    val selectedCategory: String = "All", // "All", "movie", "tv"
    val sortBy: String = "popular", // "popular", "rating", "newest"
    val selectedMedia: MediaItem? = null,
    val playingMedia: MediaItem? = null,
    val currentSeason: Int = 1,
    val currentEpisode: Int = 1,
    val playerServer: PlayerServer = PlayerServer.VIDLY,
    val selectedPalette: DarkThemePalette = DarkThemePalette.VELVET_CINEMA,
    val preferredHighReadabilityPalette: DarkThemePalette = DarkThemePalette.CRISP_SLATE,
    val trailerYoutubeKey: String? = null,
    val isTrailerLoading: Boolean = false,
    val isTrailerModalOpen: Boolean = false,
    val episodesByMediaAndSeason: Map<String, Map<Int, List<EpisodeInfo>>> = emptyMap(),
    val isLoadingSeasons: Boolean = false,
    val loadingSeasonNumber: Int? = null,
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val searchErrorMessage: String? = null,
    val errorMessage: String? = null
)

class MovieViewModel(
    private val repository: MovieRepository,
    private val userPreferences: UserPreferences? = null
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

    private val _adGateState = MutableStateFlow<AdGateState>(AdGateState.Loading)
    val adGateState: StateFlow<AdGateState> = _adGateState.asStateFlow()

    private var latestAdConfig: AdConfigEntity? = null
    private var hasLoadedAdConfig: Boolean = false
    private var searchJob: Job? = null

    private var trendingFeed = FeedPaginationState()
    private var moviesFeed = FeedPaginationState()
    private var tvFeed = FeedPaginationState()
    private var searchFeed = FeedPaginationState()

    init {
        viewModelScope.launch {
            repository.initSeedDataIfEmpty()
        }
        observeUserPreferences()
        observeAdConfig()
        loadHomeData()
        observeCustomMedia()
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

    private fun observeCustomMedia() {
        viewModelScope.launch {
            repository.customMedia.collect { list ->
                _uiState.value = _uiState.value.copy(
                    customMedia = list.map { it.toMediaItem() }
                )
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val trendingDeferred = async {
                    repository.getTrendingBatch(startPage = 1, neededCount = targetBatchSize)
                }
                val popularDeferred = async {
                    repository.getPopularMoviesBatch(startPage = 1, neededCount = targetBatchSize)
                }
                val tvDeferred = async {
                    repository.getTopRatedTvBatch(startPage = 1, neededCount = targetBatchSize)
                }

                val trendingResult = trendingDeferred.await()
                val popularResult = popularDeferred.await()
                val tvResult = tvDeferred.await()

                val allFailed = trendingResult.isFailure && popularResult.isFailure && tvResult.isFailure

                val trendingBatch = trendingResult.getOrNull()
                val popularBatch = popularResult.getOrNull()
                val tvBatch = tvResult.getOrNull()

                val trendingItems = trendingBatch?.items?.take(targetBatchSize).orEmpty()
                val popularItems = popularBatch?.items?.take(targetBatchSize).orEmpty()
                val tvItems = tvBatch?.items?.take(targetBatchSize).orEmpty()

                if (trendingBatch != null) {
                    trendingFeed = FeedPaginationState(
                        nextPage = trendingBatch.lastFetchedPage + 1,
                        totalPages = trendingBatch.totalPages,
                        totalResults = trendingBatch.totalResults,
                        buffer = trendingBatch.items.drop(trendingItems.size)
                    )
                }
                if (popularBatch != null) {
                    moviesFeed = FeedPaginationState(
                        nextPage = popularBatch.lastFetchedPage + 1,
                        totalPages = popularBatch.totalPages,
                        totalResults = popularBatch.totalResults,
                        buffer = popularBatch.items.drop(popularItems.size)
                    )
                }
                if (tvBatch != null) {
                    tvFeed = FeedPaginationState(
                        nextPage = tvBatch.lastFetchedPage + 1,
                        totalPages = tvBatch.totalPages,
                        totalResults = tvBatch.totalResults,
                        buffer = tvBatch.items.drop(tvItems.size)
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        trending = trendingItems.ifEmpty { state.trending },
                        popularMovies = popularItems.ifEmpty { state.popularMovies },
                        topRatedTv = tvItems.ifEmpty { state.topRatedTv },
                        trendingTotalResults = trendingBatch?.totalResults ?: state.trendingTotalResults,
                        popularMoviesTotalResults = popularBatch?.totalResults ?: state.popularMoviesTotalResults,
                        topRatedTvTotalResults = tvBatch?.totalResults ?: state.topRatedTvTotalResults,
                        hasMoreTrending = trendingFeed.hasMore(),
                        hasMorePopularMovies = moviesFeed.hasMore(),
                        hasMoreTopRatedTv = tvFeed.hasMore(),
                        isLoading = false,
                        errorMessage = if (allFailed) {
                            "Unable to reach TMDb right now. Please check your internet connection and tap Retry."
                        } else {
                            null
                        }
                    )
                }

                val tvItemsToEnrich = (trendingItems + tvItems)
                    .filter { it.mediaType == "tv" && it.seasons.isEmpty() }
                    .take(20)
                if (tvItemsToEnrich.isNotEmpty()) {
                    enrichTvItemsInBackground(tvItemsToEnrich)
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

    fun setBatchSize(newSize: Int) {
        val normalized = CatalogBatchPolicy.normalizeBatchSize(newSize)
        _uiState.update { it.copy(batchSize = normalized) }
    }

    fun loadMoreCategory(categoryKey: String) {
        when (categoryKey) {
            "Trending" -> loadMoreTrending()
            "Movies" -> loadMoreMovies()
            "TV" -> loadMoreTv()
            "Specials" -> Unit
            else -> {
                loadMoreMovies()
                loadMoreTv()
            }
        }
    }

    private fun loadMoreTrending() {
        val state = _uiState.value
        if (state.isLoadingMoreTrending || !trendingFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreTrending = true) }
            val previousSize = state.trending.size
            val (updated, newFeed) = consumeNextBatch(
                existingVisible = _uiState.value.trending,
                currentFeed = trendingFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getTrendingBatch(page, needed) }
            )
            trendingFeed = newFeed
            _uiState.update {
                it.copy(
                    trending = updated,
                    trendingTotalResults = maxOf(newFeed.totalResults, updated.size),
                    hasMoreTrending = newFeed.hasMore(),
                    isLoadingMoreTrending = false
                )
            }
            val newlyAddedTv = updated.drop(previousSize)
                .filter { it.mediaType == "tv" && it.seasons.isEmpty() }
                .take(20)
            if (newlyAddedTv.isNotEmpty()) {
                enrichTvItemsInBackground(newlyAddedTv)
            }
        }
    }

    private fun loadMoreMovies() {
        val state = _uiState.value
        if (state.isLoadingMoreMovies || !moviesFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreMovies = true) }
            val (updated, newFeed) = consumeNextBatch(
                existingVisible = _uiState.value.popularMovies,
                currentFeed = moviesFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getPopularMoviesBatch(page, needed) }
            )
            moviesFeed = newFeed
            _uiState.update {
                it.copy(
                    popularMovies = updated,
                    popularMoviesTotalResults = maxOf(newFeed.totalResults, updated.size),
                    hasMorePopularMovies = newFeed.hasMore(),
                    isLoadingMoreMovies = false
                )
            }
        }
    }

    private fun loadMoreTv() {
        val state = _uiState.value
        if (state.isLoadingMoreTv || !tvFeed.hasMore()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreTv = true) }
            val previousSize = state.topRatedTv.size
            val (updated, newFeed) = consumeNextBatch(
                existingVisible = _uiState.value.topRatedTv,
                currentFeed = tvFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.getTopRatedTvBatch(page, needed) }
            )
            tvFeed = newFeed
            _uiState.update {
                it.copy(
                    topRatedTv = updated,
                    topRatedTvTotalResults = maxOf(newFeed.totalResults, updated.size),
                    hasMoreTopRatedTv = newFeed.hasMore(),
                    isLoadingMoreTv = false
                )
            }
            val newlyAddedTv = updated.drop(previousSize)
                .filter { it.mediaType == "tv" && it.seasons.isEmpty() }
                .take(20)
            if (newlyAddedTv.isNotEmpty()) {
                enrichTvItemsInBackground(newlyAddedTv)
            }
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
            isNavigatingBack = false
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

    fun onSearchQueryChange(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            searchFeed = FeedPaginationState()
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    searchResults = emptyList(),
                    searchTotalResults = 0,
                    hasMoreSearch = false,
                    isLoadingMoreSearch = false,
                    isSearching = false,
                    searchErrorMessage = null
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                searchQuery = query,
                isSearching = true,
                searchResults = emptyList(),
                searchTotalResults = 0,
                hasMoreSearch = false,
                isLoadingMoreSearch = false,
                searchErrorMessage = null
            )
        }
        searchJob = viewModelScope.launch {
            delay(350) // Debounce
            val trimmedQuery = query.trim()
            if (trimmedQuery.length >= 2) {
                repository.saveTmdbSearch(trimmedQuery)
            }
            val targetBatch = _uiState.value.batchSize
            val result = repository.searchTitlesBatch(
                query = trimmedQuery,
                startPage = 1,
                neededCount = targetBatch
            )
            result.fold(
                onSuccess = { batch ->
                    val initialItems = batch.items.take(targetBatch)
                    searchFeed = FeedPaginationState(
                        nextPage = batch.lastFetchedPage + 1,
                        totalPages = batch.totalPages,
                        totalResults = batch.totalResults,
                        buffer = batch.items.drop(initialItems.size)
                    )
                    _uiState.update {
                        it.copy(
                            searchResults = initialItems,
                            searchTotalResults = maxOf(batch.totalResults, initialItems.size),
                            hasMoreSearch = searchFeed.hasMore(),
                            isSearching = false,
                            searchErrorMessage = null
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
    }

    fun loadMoreSearchResults() {
        val state = _uiState.value
        val trimmedQuery = state.searchQuery.trim()
        if (trimmedQuery.isBlank() || state.isSearching || state.isLoadingMoreSearch || !searchFeed.hasMore()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreSearch = true) }
            val previousSize = _uiState.value.searchResults.size
            val (updated, newFeed) = consumeNextBatch(
                existingVisible = _uiState.value.searchResults,
                currentFeed = searchFeed,
                batchSize = _uiState.value.batchSize,
                fetcher = { page, needed -> repository.searchTitlesBatch(trimmedQuery, page, needed) }
            )
            searchFeed = newFeed
            _uiState.update {
                it.copy(
                    searchResults = updated,
                    searchTotalResults = maxOf(newFeed.totalResults, updated.size),
                    hasMoreSearch = newFeed.hasMore(),
                    isLoadingMoreSearch = false
                )
            }
            val newlyAddedTv = updated.drop(previousSize)
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
        viewModelScope.launch { repository.clearTmdbSearchHistory() }
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
        _uiState.update {
            it.copy(
                selectedGenre = genre.ifBlank { "All" },
                selectedCategory = "All"
            )
        }
        navigateTo(Screen.Explore)
    }

    fun openExploreSection(category: String = "All", genre: String = "All") {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                selectedGenre = genre
            )
        }
        navigateTo(Screen.Explore)
    }

    fun openSearch() {
        navigateTo(Screen.Search)
    }

    fun openCategoryList(title: String, categoryKey: String, genreFilter: String = "All") {
        navigateTo(Screen.CategoryList(title, categoryKey, genreFilter))
    }

    fun selectGenre(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectSortBy(sort: String) {
        _uiState.update { it.copy(sortBy = sort) }
    }

    fun openDetails(media: MediaItem) {
        _uiState.update { it.copy(selectedMedia = media) }
        navigateTo(Screen.Details(media))
        // Fetch trailer in background
        val resolvedId = media.resolvedTmdbId()
        if (resolvedId != null && media.trailerYoutubeKey == null) {
            viewModelScope.launch {
                val key = repository.fetchTrailerYoutubeKey(resolvedId, media.mediaType)
                if (key != null) {
                    val currentSelected = _uiState.value.selectedMedia
                    if (currentSelected?.id == media.id) {
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
        // Automatically fetch all seasons and episodes for TV series / web series / anime
        if (media.mediaType == "tv") {
            ensureTvSeriesLoaded(media, initialSeason = media.defaultSeasonNumber())
        }
    }

    fun openPlayer(media: MediaItem, season: Int = 1, episode: Int = 1) {
        val (safeSeason, safeEpisode) = ContinueWatchingPolicy.normalizeSeasonEpisode(
            mediaType = media.mediaType,
            season = season,
            episode = episode
        )
        val existing = continueWatching.value.firstOrNull { it.id == media.id }
        val preserve = ContinueWatchingPolicy.shouldPreserveProgress(
            existing = existing,
            mediaType = media.mediaType,
            targetSeason = safeSeason,
            targetEpisode = safeEpisode
        )
        val initialWatched = if (preserve) (existing?.watchedSeconds ?: 0L) else 0L
        val initialDuration = if (preserve) (existing?.durationSeconds ?: 0L) else 0L
        val initialPercent = if (preserve) (existing?.progressPercent ?: 0) else 0

        _uiState.value = _uiState.value.copy(
            playingMedia = media,
            currentSeason = safeSeason,
            currentEpisode = safeEpisode
        )
        viewModelScope.launch {
            repository.saveContinueWatching(
                item = media,
                season = safeSeason,
                episode = safeEpisode,
                watchedSeconds = initialWatched,
                durationSeconds = initialDuration,
                progressPercent = initialPercent
            )
        }
        navigateTo(Screen.Player(media, safeSeason, safeEpisode))
        if (media.mediaType == "tv") {
            ensureTvSeriesLoaded(media, initialSeason = safeSeason)
        }
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

            // Preload remaining seasons' episode lists in background so every season opens instantly
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
            val current = _uiState.value
            _uiState.value = current.copy(
                trending = current.trending.map { enrichedMap[it.id] ?: it },
                topRatedTv = current.topRatedTv.map { enrichedMap[it.id] ?: it },
                searchResults = current.searchResults.map { enrichedMap[it.id] ?: it },
                selectedMedia = current.selectedMedia?.let { enrichedMap[it.id] ?: it },
                playingMedia = current.playingMedia?.let { enrichedMap[it.id] ?: it }
            )
        }
    }

    private fun applyEnrichedMediaToState(enriched: MediaItem) {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedMedia = if (current.selectedMedia?.id == enriched.id) {
                enriched.copy(trailerYoutubeKey = enriched.trailerYoutubeKey ?: current.selectedMedia.trailerYoutubeKey)
            } else {
                current.selectedMedia
            },
            playingMedia = if (current.playingMedia?.id == enriched.id) {
                enriched.copy(trailerYoutubeKey = enriched.trailerYoutubeKey ?: current.playingMedia.trailerYoutubeKey)
            } else {
                current.playingMedia
            },
            trending = current.trending.map { if (it.id == enriched.id) enriched else it },
            topRatedTv = current.topRatedTv.map { if (it.id == enriched.id) enriched else it },
            searchResults = current.searchResults.map { if (it.id == enriched.id) enriched else it }
        )
        viewModelScope.launch {
            repository.updateContinueWatchingMetadata(enriched)
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
}

class MovieViewModelFactory(
    private val repository: MovieRepository,
    private val userPreferences: UserPreferences? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovieViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MovieViewModel(repository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
