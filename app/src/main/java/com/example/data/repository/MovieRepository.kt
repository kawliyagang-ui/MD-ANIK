package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.MediaDao
import com.example.data.model.AdConfigEntity
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.AdminSecurityEntity
import com.example.data.model.AdminSecurityPolicy
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.CatalogSyncMappers
import com.example.data.model.CloudSyncStatus
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.CustomMediaEntity
import com.example.data.model.EpisodeInfo
import com.example.data.model.MediaItem
import com.example.data.model.PagedCatalogBatch
import com.example.data.model.SeasonInfo
import com.example.data.model.TmdbItemDto
import com.example.data.model.TmdbPageResponse
import com.example.data.model.TmdbSeasonDetailsDto
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.data.model.TmdbTvDetailsDto
import com.example.data.model.WatchlistEntity
import com.example.data.model.extractBestCertification
import com.example.data.remote.CuratedCatalog
import com.example.data.remote.FirebaseInitializer
import com.example.data.remote.TmdbClient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

class MovieRepository(
    private val mediaDao: MediaDao,
    context: Context? = null,
    firestoreOverride: FirebaseFirestore? = null
) {
    companion object {
        private const val TAG = "MovieRepository"
        private const val CLOUD_ACK_TIMEOUT_MS = 8_000L
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()
    @Volatile
    private var listenersNeedReattach = false
    private val tvDetailsCache = ConcurrentHashMap<Long, TmdbTvDetailsDto>()
    private val tvSeasonEpisodesCache = ConcurrentHashMap<Pair<Long, Int>, List<EpisodeInfo>>()
    private val certificationCache = ConcurrentHashMap<String, String>()

    private val firestoreDb: FirebaseFirestore? = firestoreOverride
        ?: FirebaseInitializer.getFirestore(context)

    private val _cloudSyncStatus = MutableStateFlow(
        if (firestoreDb != null) {
            CloudSyncStatus(isAvailable = true, stateLabel = "CONNECTING...")
        } else {
            CloudSyncStatus(
                isAvailable = false,
                stateLabel = "LOCAL ONLY (Firebase uninitialized)",
                lastError = "Firebase Firestore is not initialized."
            )
        }
    )
    val cloudSyncStatus: StateFlow<CloudSyncStatus> = _cloudSyncStatus.asStateFlow()

    val watchlist: Flow<List<WatchlistEntity>> = mediaDao.getAllWatchlist()
    val continueWatching: Flow<List<ContinueWatchingEntity>> = mediaDao.getAllContinueWatching()
    val customMedia: Flow<List<CustomMediaEntity>> = mediaDao.getAllCustomMedia()
    val allAnnouncements: Flow<List<AdminAnnouncementEntity>> = mediaDao.getAllAnnouncements()
    val activeAnnouncements: Flow<List<AdminAnnouncementEntity>> = mediaDao.getActiveAnnouncements()
    val tmdbSearchHistory: Flow<List<TmdbSearchHistoryEntity>> = mediaDao.getRecentTmdbSearches()
    val adConfig: Flow<AdConfigEntity?> = mediaDao.getAdConfig()
    val adminSecurity: Flow<AdminSecurityEntity?> = mediaDao.getAdminSecurity()

    init {
        startFirestoreRealtimeSync()
    }

    @Synchronized
    private fun startFirestoreRealtimeSync() {
        val db = firestoreDb ?: return
        listenerRegistrations.forEach { runCatching { it.remove() } }
        listenerRegistrations.clear()
        listenersNeedReattach = false

        // 1. Real-time Custom Media & Metadata Overrides Listener
        val mediaReg = db.collection(CatalogSyncMappers.COLLECTION_CUSTOM_MEDIA)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    listenersNeedReattach = true
                    Log.e(TAG, "Firestore custom_media listener failed", error)
                    _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
                        stateLabel = "SYNC ERROR",
                        lastError = error.localizedMessage ?: "Permission or network error"
                    )
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val remoteList = snapshot.documents.mapNotNull { doc ->
                    CatalogSyncMappers.customMediaFromMap(doc.id, doc.data)
                }
                syncScope.launch {
                    runCatching {
                        if (!snapshot.metadata.isFromCache || remoteList.isNotEmpty()) {
                            mediaDao.replaceAllCustomMedia(remoteList)
                        }
                    }.onFailure { err ->
                        Log.e(TAG, "Failed reconciling custom_media into Room", err)
                    }
                }
                _cloudSyncStatus.value = CloudSyncStatus(
                    isAvailable = true,
                    stateLabel = if (snapshot.metadata.isFromCache) "CACHED • Real-time" else "CONNECTED • Real-time",
                    lastSyncedAt = System.currentTimeMillis(),
                    lastError = null
                )
            }
        listenerRegistrations.add(mediaReg)

        // 2. Real-time Announcements Listener
        val annReg = db.collection(CatalogSyncMappers.COLLECTION_ANNOUNCEMENTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    listenersNeedReattach = true
                    Log.e(TAG, "Firestore announcements listener failed", error)
                    _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
                        stateLabel = "SYNC ERROR",
                        lastError = error.localizedMessage ?: "Announcements sync failed"
                    )
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val remoteAnnouncements = snapshot.documents.mapNotNull { doc ->
                    CatalogSyncMappers.announcementFromMap(doc.id, doc.data)
                }
                syncScope.launch {
                    runCatching {
                        if (!snapshot.metadata.isFromCache || remoteAnnouncements.isNotEmpty()) {
                            mediaDao.replaceAllAnnouncements(remoteAnnouncements)
                        }
                    }.onFailure { err ->
                        Log.e(TAG, "Failed reconciling announcements into Room", err)
                    }
                }
                _cloudSyncStatus.value = CloudSyncStatus(
                    isAvailable = true,
                    stateLabel = if (snapshot.metadata.isFromCache) "CACHED • Real-time" else "CONNECTED • Real-time",
                    lastSyncedAt = System.currentTimeMillis(),
                    lastError = null
                )
            }
        listenerRegistrations.add(annReg)

        // 3. Real-time Ad Gate Config Listener
        val adReg = db.collection(CatalogSyncMappers.COLLECTION_AD_CONFIG)
            .document(CatalogSyncMappers.AD_CONFIG_DOC_ID)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    listenersNeedReattach = true
                    Log.e(TAG, "Firestore ad_config listener failed", error)
                    _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
                        stateLabel = "SYNC ERROR",
                        lastError = error.localizedMessage ?: "Ad config sync failed"
                    )
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val data = snapshot.data
                syncScope.launch {
                    runCatching {
                        val existing = mediaDao.getAdConfigOnce()
                        val merged = CatalogSyncMappers.adConfigFromMap(data, existing)
                        if (merged != null) {
                            mediaDao.upsertAdConfig(merged)
                        }
                    }.onFailure { err ->
                        Log.e(TAG, "Failed reconciling ad_config into Room", err)
                    }
                }
            }
        listenerRegistrations.add(adReg)
    }

    suspend fun syncFromCloudNow(): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestoreDb ?: return@withContext Result.failure(
            IllegalStateException("Firebase Firestore is unavailable.")
        )
        syncMutex.withLock {
            try {
                val mediaSnap = db.collection(CatalogSyncMappers.COLLECTION_CUSTOM_MEDIA).get().await()
                val remoteMedia = mediaSnap.documents.mapNotNull { doc ->
                    CatalogSyncMappers.customMediaFromMap(doc.id, doc.data)
                }
                if (!mediaSnap.metadata.isFromCache || remoteMedia.isNotEmpty()) {
                    mediaDao.replaceAllCustomMedia(remoteMedia)
                }

                val annSnap = db.collection(CatalogSyncMappers.COLLECTION_ANNOUNCEMENTS).get().await()
                val remoteAnnouncements = annSnap.documents.mapNotNull { doc ->
                    CatalogSyncMappers.announcementFromMap(doc.id, doc.data)
                }
                if (!annSnap.metadata.isFromCache || remoteAnnouncements.isNotEmpty()) {
                    mediaDao.replaceAllAnnouncements(remoteAnnouncements)
                }

                val adSnap = db.collection(CatalogSyncMappers.COLLECTION_AD_CONFIG)
                    .document(CatalogSyncMappers.AD_CONFIG_DOC_ID)
                    .get()
                    .await()
                if (adSnap.exists()) {
                    val mergedAd = CatalogSyncMappers.adConfigFromMap(adSnap.data, mediaDao.getAdConfigOnce())
                    if (mergedAd != null) {
                        mediaDao.upsertAdConfig(mergedAd)
                    }
                }

                if (listenersNeedReattach) {
                    startFirestoreRealtimeSync()
                }

                _cloudSyncStatus.value = CloudSyncStatus(
                    isAvailable = true,
                    stateLabel = "CONNECTED • Synced",
                    lastSyncedAt = System.currentTimeMillis(),
                    lastError = null
                )
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Manual cloud sync failed", e)
                _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
                    stateLabel = "SYNC ERROR",
                    lastError = e.localizedMessage ?: "Failed to sync from cloud"
                )
                Result.failure(e)
            }
        }
    }

    private suspend fun executeCloudWrite(
        operationLabel: String,
        block: suspend (FirebaseFirestore) -> Unit
    ): Result<Unit> {
        val db = firestoreDb ?: return Result.failure(
            IllegalStateException("Cloud sync unavailable (Firebase not initialized).")
        )
        return try {
            val acked = withTimeoutOrNull(CLOUD_ACK_TIMEOUT_MS) {
                block(db)
                true
            }
            if (acked == true) {
                _cloudSyncStatus.value = CloudSyncStatus(
                    isAvailable = true,
                    stateLabel = "CONNECTED • Synced",
                    lastSyncedAt = System.currentTimeMillis(),
                    lastError = null
                )
            } else {
                _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
                    stateLabel = "QUEUED OFFLINE • Pending sync",
                    lastError = null
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Cloud write failed ($operationLabel)", e)
            _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
                stateLabel = "SYNC ERROR",
                lastError = e.localizedMessage ?: "Cloud sync failed"
            )
            Result.failure(e)
        }
    }

    fun isWatchlisted(id: String): Flow<Boolean> = mediaDao.isWatchlisted(id)

    suspend fun initSeedDataIfEmpty() = withContext(Dispatchers.IO) {
        if (mediaDao.getAdConfigOnce() == null) {
            mediaDao.upsertAdConfig(AdConfigEntity(id = 1, isEnabled = false))
        }

        if (mediaDao.getAdminSecurityOnce() == null) {
            mediaDao.upsertAdminSecurity(
                AdminSecurityEntity(
                    id = 1,
                    passcodeHash = AdminSecurityPolicy.hashPasscode(AdminSecurityPolicy.DEFAULT_PASSCODE),
                    isDefault = true
                )
            )
        }
    }

    private suspend fun fetchMultiPageBatch(
        startPage: Int,
        neededCount: Int,
        fetchPage: suspend (Int) -> TmdbPageResponse,
        mapResponse: (TmdbPageResponse) -> List<MediaItem>
    ): Result<PagedCatalogBatch> = withContext(Dispatchers.IO) {
        try {
            val safeStart = startPage.coerceAtLeast(1)
            val firstPageResponse = fetchPage(safeStart)
            val totalPages = firstPageResponse.totalPages
                .coerceAtLeast(1)
                .coerceAtMost(CatalogBatchPolicy.MAX_TMDB_BROWSABLE_PAGES)
            val totalResults = maxOf(firstPageResponse.totalResults, firstPageResponse.results.size)

            val desiredPages = CatalogBatchPolicy.pageRangeToFetch(
                startPage = safeStart,
                neededCount = neededCount,
                totalPages = totalPages
            )
            val extraPages = if (desiredPages.last > safeStart) {
                coroutineScope {
                    ((safeStart + 1)..desiredPages.last).map { pageNum ->
                        async { runCatching { fetchPage(pageNum) }.getOrNull() }
                    }.awaitAll().filterNotNull()
                }
            } else {
                emptyList()
            }

            val allResponses = listOf(firstPageResponse) + extraPages
            val mergedItems = allResponses
                .flatMap(mapResponse)
                .distinctBy { it.id }
            val lastPage = maxOf(safeStart, allResponses.maxOfOrNull { it.page } ?: safeStart)

            Result.success(
                PagedCatalogBatch(
                    items = mergedItems,
                    lastFetchedPage = lastPage,
                    totalPages = totalPages,
                    totalResults = totalResults
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrendingBatch(
        startPage: Int = 1,
        neededCount: Int = CatalogBatchPolicy.DEFAULT_BATCH_SIZE
    ): Result<PagedCatalogBatch> = fetchMultiPageBatch(
        startPage = startPage,
        neededCount = neededCount,
        fetchPage = { page -> TmdbClient.apiService.getTrending(TmdbClient.apiKey, page) },
        mapResponse = { resp -> resp.results.filter { it.posterPath != null }.map { it.toMediaItem() } }
    )

    suspend fun getPopularMoviesBatch(
        startPage: Int = 1,
        neededCount: Int = CatalogBatchPolicy.DEFAULT_BATCH_SIZE
    ): Result<PagedCatalogBatch> = fetchMultiPageBatch(
        startPage = startPage,
        neededCount = neededCount,
        fetchPage = { page -> TmdbClient.apiService.getPopularMovies(TmdbClient.apiKey, page) },
        mapResponse = { resp ->
            resp.results.filter { it.posterPath != null }.map { it.toMediaItem(defaultType = "movie") }
        }
    )

    suspend fun getTopRatedTvBatch(
        startPage: Int = 1,
        neededCount: Int = CatalogBatchPolicy.DEFAULT_BATCH_SIZE
    ): Result<PagedCatalogBatch> = fetchMultiPageBatch(
        startPage = startPage,
        neededCount = neededCount,
        fetchPage = { page -> TmdbClient.apiService.getTopRatedTv(TmdbClient.apiKey, page) },
        mapResponse = { resp ->
            resp.results.filter { it.posterPath != null }.map { it.toMediaItem(defaultType = "tv") }
        }
    )

    suspend fun searchTitlesBatch(
        query: String,
        startPage: Int = 1,
        neededCount: Int = CatalogBatchPolicy.DEFAULT_BATCH_SIZE
    ): Result<PagedCatalogBatch> {
        if (query.isBlank()) {
            return Result.success(PagedCatalogBatch(emptyList(), 1, 1, 0))
        }
        return fetchMultiPageBatch(
            startPage = startPage,
            neededCount = neededCount,
            fetchPage = { page ->
                TmdbClient.apiService.searchMulti(
                    apiKey = TmdbClient.apiKey,
                    query = query,
                    page = page
                )
            },
            mapResponse = { resp ->
                resp.results
                    .filter { it.posterPath != null && (it.mediaType == "movie" || it.mediaType == "tv" || it.mediaType == null) }
                    .map { it.toMediaItem() }
            }
        )
    }

    suspend fun getTrending(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = TmdbClient.apiService.getTrending(TmdbClient.apiKey)
            val items = response.results.filter { it.posterPath != null }.map { it.toMediaItem() }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularMovies(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = TmdbClient.apiService.getPopularMovies(TmdbClient.apiKey)
            val items = response.results.filter { it.posterPath != null }.map { it.toMediaItem(defaultType = "movie") }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopRatedTv(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = TmdbClient.apiService.getTopRatedTv(TmdbClient.apiKey)
            val items = response.results.filter { it.posterPath != null }.map { it.toMediaItem(defaultType = "tv") }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchTitles(query: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())
        try {
            val response = TmdbClient.apiService.searchMulti(apiKey = TmdbClient.apiKey, query = query)
            val results = response.results
                .filter { it.posterPath != null && (it.mediaType == "movie" || it.mediaType == "tv" || it.mediaType == null) }
                .map { it.toMediaItem() }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkTmdbHealth(): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
        try {
            val response = TmdbClient.apiService.getTrending(TmdbClient.apiKey)
            val count = response.results.count { it.posterPath != null }
            true to count
        } catch (_: Exception) {
            false to 0
        }
    }

    suspend fun fetchTrailerYoutubeKey(id: Long, mediaType: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = if (mediaType == "tv") {
                TmdbClient.apiService.getTvVideos(id, TmdbClient.apiKey)
            } else {
                TmdbClient.apiService.getMovieVideos(id, TmdbClient.apiKey)
            }
            val trailer = response.results.firstOrNull {
                it.site.equals("YouTube", ignoreCase = true) &&
                    (it.type.equals("Trailer", ignoreCase = true) || it.type.equals("Teaser", ignoreCase = true))
            } ?: response.results.firstOrNull { it.site.equals("YouTube", ignoreCase = true) }
            trailer?.key
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchCertification(id: Long, mediaType: String): String = withContext(Dispatchers.IO) {
        val cacheKey = "${mediaType}_$id"
        certificationCache[cacheKey]?.let { return@withContext it }
        try {
            val cert = if (mediaType == "tv") {
                TmdbClient.apiService.getTvContentRatings(id, TmdbClient.apiKey).extractBestCertification()
            } else {
                TmdbClient.apiService.getMovieReleaseDates(id, TmdbClient.apiKey).extractBestCertification()
            }
            if (cert.isNotBlank()) {
                certificationCache[cacheKey] = cert
            }
            cert
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun fetchEnrichedTvSeries(base: MediaItem): MediaItem = withContext(Dispatchers.IO) {
        if (base.mediaType != "tv") return@withContext base
        val tvId = base.resolvedTmdbId() ?: return@withContext base
        try {
            val dto = tvDetailsCache[tvId] ?: TmdbClient.apiService.getTvDetails(tvId, TmdbClient.apiKey).also {
                tvDetailsCache[tvId] = it
            }
            dto.applyToMediaItem(base)
        } catch (_: Exception) {
            base
        }
    }

    suspend fun fetchTvSeasonEpisodes(tmdbId: Long, seasonNumber: Int): List<EpisodeInfo> = withContext(Dispatchers.IO) {
        val key = tmdbId to seasonNumber
        tvSeasonEpisodesCache[key]?.let { return@withContext it }
        try {
            val dto = TmdbClient.apiService.getTvSeasonDetails(tmdbId, seasonNumber, TmdbClient.apiKey)
            val episodes = dto.toEpisodeInfoList(fallbackSeasonNumber = seasonNumber)
            if (episodes.isNotEmpty()) {
                tvSeasonEpisodesCache[key] = episodes
            }
            episodes
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Watchlist
    suspend fun toggleWatchlist(item: MediaItem) = withContext(Dispatchers.IO) {
        val isSaved = mediaDao.isWatchlisted(item.id).firstOrNull() ?: false
        if (isSaved) {
            mediaDao.removeFromWatchlist(item.id)
        } else {
            mediaDao.addToWatchlist(
                WatchlistEntity(
                    id = item.id,
                    tmdbId = item.tmdbId,
                    title = item.title,
                    overview = item.overview,
                    posterPath = item.posterPath,
                    backdropPath = item.backdropPath,
                    mediaType = item.mediaType,
                    releaseDate = item.releaseDate,
                    voteAverage = item.voteAverage,
                    genres = item.genres.joinToString(", "),
                    certification = item.certification,
                    vidlyId = item.vidlyId
                )
            )
        }
    }

    // Continue Watching
    suspend fun saveContinueWatching(
        item: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        watchedSeconds: Long = 0L,
        durationSeconds: Long = 0L,
        progressPercent: Int = ContinueWatchingPolicy.computeProgressPercent(watchedSeconds, durationSeconds)
    ) = withContext(Dispatchers.IO) {
        val (safeSeason, safeEpisode) = ContinueWatchingPolicy.normalizeSeasonEpisode(
            mediaType = item.mediaType,
            season = season,
            episode = episode
        )
        val safeWatched = watchedSeconds.coerceAtLeast(0L)
        val safeDuration = durationSeconds.coerceAtLeast(0L)
        val safePercent = progressPercent.coerceIn(0, 100)
        val existing = mediaDao.getContinueWatchingById(item.id)

        mediaDao.upsertContinueWatching(
            ContinueWatchingEntity(
                id = item.id,
                tmdbId = item.resolvedTmdbId() ?: existing?.tmdbId,
                title = item.title.ifBlank { existing?.title.orEmpty() },
                overview = item.overview.ifBlank { existing?.overview.orEmpty() },
                posterPath = item.posterPath.ifBlank { existing?.posterPath.orEmpty() },
                backdropPath = item.backdropPath.ifBlank { existing?.backdropPath.orEmpty() },
                mediaType = item.mediaType,
                releaseDate = item.releaseDate.ifBlank { existing?.releaseDate.orEmpty() },
                voteAverage = if (item.voteAverage > 0.0) item.voteAverage else (existing?.voteAverage ?: 0.0),
                genres = item.genres.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: existing?.genres.orEmpty(),
                certification = item.certification.ifBlank { existing?.certification.orEmpty() },
                numberOfSeasons = maxOf(item.numberOfSeasons, existing?.numberOfSeasons ?: 1).coerceAtLeast(1),
                episodesPerSeason = if (item.episodesPerSeason > 0) item.episodesPerSeason else (existing?.episodesPerSeason ?: 10),
                season = safeSeason,
                episode = safeEpisode,
                progressPercent = safePercent,
                watchedSeconds = safeWatched,
                durationSeconds = safeDuration,
                lastWatchedAt = System.currentTimeMillis(),
                vidlyId = item.vidlyId.ifBlank { existing?.vidlyId.orEmpty() }
            )
        )
    }

    suspend fun updateContinueWatchingMetadata(item: MediaItem) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getContinueWatchingById(item.id) ?: return@withContext
        mediaDao.upsertContinueWatching(
            existing.copy(
                tmdbId = item.resolvedTmdbId() ?: existing.tmdbId,
                title = item.title.ifBlank { existing.title },
                overview = item.overview.ifBlank { existing.overview },
                posterPath = item.posterPath.ifBlank { existing.posterPath },
                backdropPath = item.backdropPath.ifBlank { existing.backdropPath },
                releaseDate = item.releaseDate.ifBlank { existing.releaseDate },
                voteAverage = if (item.voteAverage > 0.0) item.voteAverage else existing.voteAverage,
                genres = item.genres.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: existing.genres,
                certification = item.certification.ifBlank { existing.certification },
                numberOfSeasons = maxOf(item.numberOfSeasons, existing.numberOfSeasons).coerceAtLeast(1),
                episodesPerSeason = if (item.episodesPerSeason > 0) item.episodesPerSeason else existing.episodesPerSeason
            )
        )
    }

    suspend fun removeContinueWatching(id: String) = withContext(Dispatchers.IO) {
        mediaDao.removeContinueWatching(id)
    }

    // Custom Media (Admin -> Cloud + Room)
    suspend fun insertCustomMedia(entity: CustomMediaEntity): Result<Unit> = withContext(Dispatchers.IO) {
        mediaDao.insertCustomMedia(entity)
        executeCloudWrite("insertCustomMedia:${entity.id}") { db ->
            db.collection(CatalogSyncMappers.COLLECTION_CUSTOM_MEDIA)
                .document(entity.id)
                .set(CatalogSyncMappers.customMediaToMap(entity))
                .await()
        }
    }

    suspend fun updateCustomMedia(entity: CustomMediaEntity): Result<Unit> = withContext(Dispatchers.IO) {
        mediaDao.insertCustomMedia(entity)
        executeCloudWrite("updateCustomMedia:${entity.id}") { db ->
            db.collection(CatalogSyncMappers.COLLECTION_CUSTOM_MEDIA)
                .document(entity.id)
                .set(CatalogSyncMappers.customMediaToMap(entity))
                .await()
        }
    }

    suspend fun deleteCustomMedia(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        mediaDao.deleteCustomMedia(id)
        executeCloudWrite("deleteCustomMedia:$id") { db ->
            db.collection(CatalogSyncMappers.COLLECTION_CUSTOM_MEDIA)
                .document(id)
                .delete()
                .await()
        }
    }

    // Announcements (Admin -> Cloud + Room)
    suspend fun insertAnnouncement(
        title: String,
        message: String,
        type: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val stableId = CatalogSyncMappers.generateStableAnnouncementId()
        val entity = AdminAnnouncementEntity(
            id = stableId,
            title = title,
            message = message,
            type = type,
            active = true,
            createdAt = System.currentTimeMillis()
        )
        mediaDao.insertAnnouncement(entity)
        val docId = CatalogSyncMappers.announcementDocId(stableId)
        executeCloudWrite("insertAnnouncement:$docId") { db ->
            db.collection(CatalogSyncMappers.COLLECTION_ANNOUNCEMENTS)
                .document(docId)
                .set(CatalogSyncMappers.announcementToMap(entity))
                .await()
        }
    }

    suspend fun updateAnnouncement(announcement: AdminAnnouncementEntity): Result<Unit> = withContext(Dispatchers.IO) {
        mediaDao.insertAnnouncement(announcement)
        val docId = CatalogSyncMappers.announcementDocId(announcement.id)
        executeCloudWrite("updateAnnouncement:$docId") { db ->
            db.collection(CatalogSyncMappers.COLLECTION_ANNOUNCEMENTS)
                .document(docId)
                .set(CatalogSyncMappers.announcementToMap(announcement))
                .await()
        }
    }

    suspend fun deleteAnnouncement(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        mediaDao.deleteAnnouncement(id)
        val docId = CatalogSyncMappers.announcementDocId(id)
        executeCloudWrite("deleteAnnouncement:$docId") { db ->
            db.collection(CatalogSyncMappers.COLLECTION_ANNOUNCEMENTS)
                .document(docId)
                .delete()
                .await()
        }
    }

    suspend fun toggleAnnouncementActive(id: Int, active: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = mediaDao.getAnnouncementById(id)
        mediaDao.setAnnouncementActive(id, active)
        val docId = CatalogSyncMappers.announcementDocId(id)
        executeCloudWrite("toggleAnnouncement:$docId") { db ->
            val docRef = db.collection(CatalogSyncMappers.COLLECTION_ANNOUNCEMENTS).document(docId)
            if (existing != null) {
                docRef.set(CatalogSyncMappers.announcementToMap(existing.copy(active = active))).await()
            } else {
                docRef.set(mapOf("id" to id, "active" to active), SetOptions.merge()).await()
            }
        }
    }

    // TMDb Search History (Room Database - Last 5 items)
    suspend fun saveTmdbSearch(query: String) = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext
        mediaDao.insertTmdbSearch(
            TmdbSearchHistoryEntity(
                query = clean,
                timestamp = System.currentTimeMillis()
            )
        )
        mediaDao.trimTmdbSearchHistory()
    }

    suspend fun deleteTmdbSearch(query: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteTmdbSearch(query.trim())
    }

    suspend fun clearTmdbSearchHistory() = withContext(Dispatchers.IO) {
        mediaDao.clearTmdbSearchHistory()
    }

    // Ad Gate Config (Adsterra Pop-under & 6-Hour Cooldown -> Cloud + Room)
    suspend fun saveAdConfig(config: AdConfigEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val updated = config.copy(
            id = 1,
            updatedAt = System.currentTimeMillis()
        )
        mediaDao.upsertAdConfig(updated)
        executeCloudWrite("saveAdConfig") { db ->
            db.collection(CatalogSyncMappers.COLLECTION_AD_CONFIG)
                .document(CatalogSyncMappers.AD_CONFIG_DOC_ID)
                .set(CatalogSyncMappers.adConfigToMap(updated))
                .await()
        }
    }

    suspend fun markAdGateCompleted(timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getAdConfigOnce()
        if (existing == null) {
            mediaDao.upsertAdConfig(AdConfigEntity(id = 1, lastCompletedAtMillis = timestamp))
        } else {
            mediaDao.updateAdLastCompleted(timestamp = timestamp, updatedAt = timestamp)
        }
    }

    suspend fun resetAdCooldown(): Result<Unit> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = mediaDao.getAdConfigOnce() ?: AdConfigEntity(id = 1)
        val updated = existing.copy(
            id = 1,
            lastCompletedAtMillis = 0L,
            cooldownResetAtMillis = now,
            updatedAt = now
        )
        mediaDao.upsertAdConfig(updated)
        executeCloudWrite("resetAdCooldown") { db ->
            db.collection(CatalogSyncMappers.COLLECTION_AD_CONFIG)
                .document(CatalogSyncMappers.AD_CONFIG_DOC_ID)
                .set(CatalogSyncMappers.adConfigToMap(updated))
                .await()
        }
    }

    // Admin Panel Security (Password)
    suspend fun saveAdminPasscodeHash(newHash: String, isDefault: Boolean = false) = withContext(Dispatchers.IO) {
        mediaDao.upsertAdminSecurity(
            AdminSecurityEntity(
                id = 1,
                passcodeHash = newHash,
                isDefault = isDefault,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}


// Extension to map TmdbItemDto to MediaItem
fun TmdbItemDto.toMediaItem(defaultType: String = "movie"): MediaItem {
    val mType = mediaType ?: defaultType
    val mTitle = title ?: name ?: "Untitled"
    val date = releaseDate ?: firstAirDate ?: ""
    val genreList = genreIds
        ?.map { CuratedCatalog.getGenreName(it) }
        ?.filter { it != "Other" }
        .orEmpty()

    return MediaItem(
        id = id.toString(),
        tmdbId = id,
        title = mTitle,
        overview = overview.orEmpty(),
        posterPath = posterPath ?: "",
        backdropPath = backdropPath ?: "",
        mediaType = mType,
        releaseDate = date,
        voteAverage = voteAverage ?: 0.0,
        genres = genreList,
        vidlyId = id.toString(),
        isCustom = false,
        numberOfSeasons = numberOfSeasons ?: 1,
        episodesPerSeason = numberOfEpisodes ?: 10
    )
}

fun TmdbTvDetailsDto.applyToMediaItem(base: MediaItem): MediaItem {
    val mappedSeasons = (seasons ?: emptyList())
        .mapNotNull { dto ->
            val num = dto.seasonNumber ?: return@mapNotNull null
            val epCount = dto.episodeCount ?: 0
            if (epCount <= 0) return@mapNotNull null
            SeasonInfo(
                seasonNumber = num,
                name = dto.name?.takeIf { it.isNotBlank() }
                    ?: if (num == 0) "Specials" else "Season $num",
                episodeCount = epCount,
                airDate = dto.airDate.orEmpty(),
                overview = dto.overview.orEmpty()
            )
        }
        .sortedWith(compareBy<SeasonInfo> { if (it.seasonNumber == 0) Int.MAX_VALUE else it.seasonNumber })

    val regularSeasons = mappedSeasons.filter { it.seasonNumber > 0 }
    val resolvedNumSeasons = when {
        regularSeasons.isNotEmpty() -> regularSeasons.size
        (numberOfSeasons ?: 0) > 0 -> numberOfSeasons!!
        mappedSeasons.isNotEmpty() -> mappedSeasons.size
        else -> base.numberOfSeasons
    }

    val firstRegularEpisodes = regularSeasons.firstOrNull()?.episodeCount
        ?: mappedSeasons.firstOrNull()?.episodeCount
        ?: base.episodesPerSeason

    val resolvedTotalEpisodes = numberOfEpisodes?.takeIf { it > 0 }
        ?: mappedSeasons.sumOf { it.episodeCount }.takeIf { it > 0 }
        ?: base.totalEpisodes

    val resolvedGenres = genres
        ?.mapNotNull { it.name.takeIf { n -> n.isNotBlank() } }
        ?.takeIf { it.isNotEmpty() }
        ?: base.genres

    return base.copy(
        tmdbId = base.tmdbId ?: id,
        title = name?.takeIf { it.isNotBlank() } ?: base.title,
        overview = overview?.takeIf { it.isNotBlank() } ?: base.overview,
        posterPath = posterPath?.takeIf { it.isNotBlank() } ?: base.posterPath,
        backdropPath = backdropPath?.takeIf { it.isNotBlank() } ?: base.backdropPath,
        releaseDate = firstAirDate?.takeIf { it.isNotBlank() } ?: base.releaseDate,
        voteAverage = voteAverage?.takeIf { it > 0.0 } ?: base.voteAverage,
        genres = resolvedGenres,
        numberOfSeasons = resolvedNumSeasons,
        episodesPerSeason = firstRegularEpisodes.coerceAtLeast(1),
        totalEpisodes = resolvedTotalEpisodes,
        seasons = mappedSeasons.ifEmpty { base.seasons }
    )
}

fun TmdbSeasonDetailsDto.toEpisodeInfoList(fallbackSeasonNumber: Int = 1): List<EpisodeInfo> {
    val resolvedSeasonNum = seasonNumber ?: fallbackSeasonNumber
    return (episodes ?: emptyList())
        .map { ep ->
            val epNum = ep.episodeNumber ?: 1
            EpisodeInfo(
                episodeNumber = epNum,
                seasonNumber = ep.seasonNumber ?: resolvedSeasonNum,
                name = ep.name?.takeIf { it.isNotBlank() } ?: "Episode $epNum",
                overview = ep.overview.orEmpty(),
                runtimeMinutes = ep.runtime?.takeIf { it > 0 },
                stillPath = ep.stillPath.orEmpty(),
                airDate = ep.airDate.orEmpty()
            )
        }
        .sortedBy { it.episodeNumber }
}
