package com.example.data.repository

import com.example.data.local.MediaDao
import com.example.data.model.AdConfigEntity
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.AdminSecurityEntity
import com.example.data.model.AdminSecurityPolicy
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.CustomMediaEntity
import com.example.data.model.EpisodeInfo
import com.example.data.model.MediaItem
import com.example.data.model.SeasonInfo
import com.example.data.model.TmdbItemDto
import com.example.data.model.TmdbSeasonDetailsDto
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.data.model.TmdbTvDetailsDto
import com.example.data.model.WatchlistEntity
import com.example.data.remote.CuratedCatalog
import com.example.data.remote.TmdbClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MovieRepository(private val mediaDao: MediaDao) {

    private val tvDetailsCache = ConcurrentHashMap<Long, TmdbTvDetailsDto>()
    private val tvSeasonEpisodesCache = ConcurrentHashMap<Pair<Long, Int>, List<EpisodeInfo>>()

    private val firestoreDb: FirebaseFirestore? by lazy {
        runCatching {
            FirebaseFirestore.getInstance("ai-studio-android-streamfl-b7ad1e10-a40a-4c27-a9c4-067cca6a6453")
        }.getOrNull()
    }

    val watchlist: Flow<List<WatchlistEntity>> = mediaDao.getAllWatchlist()
    val continueWatching: Flow<List<ContinueWatchingEntity>> = mediaDao.getAllContinueWatching()
    val customMedia: Flow<List<CustomMediaEntity>> = callbackFlow {
        val job = launch(Dispatchers.IO) {
            mediaDao.getAllCustomMedia().collect { list ->
                trySend(list)
            }
        }
        val listener = firestoreDb?.collection("custom_media")?.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val remoteList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CustomMediaEntity::class.java)
                }
                launch(Dispatchers.IO) {
                    remoteList.forEach { entity ->
                        mediaDao.insertCustomMedia(entity)
                    }
                }
                trySend(remoteList)
            }
        }
        awaitClose {
            job.cancel()
            listener?.remove()
        }
    }
    val allAnnouncements: Flow<List<AdminAnnouncementEntity>> = mediaDao.getAllAnnouncements()
    val activeAnnouncements: Flow<List<AdminAnnouncementEntity>> = mediaDao.getActiveAnnouncements()
    val tmdbSearchHistory: Flow<List<TmdbSearchHistoryEntity>> = mediaDao.getRecentTmdbSearches()
    val adConfig: Flow<AdConfigEntity?> = mediaDao.getAdConfig()
    val adminSecurity: Flow<AdminSecurityEntity?> = mediaDao.getAdminSecurity()

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
        } catch (e: Exception) {
            null
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
                numberOfSeasons = maxOf(item.numberOfSeasons, existing.numberOfSeasons).coerceAtLeast(1),
                episodesPerSeason = if (item.episodesPerSeason > 0) item.episodesPerSeason else existing.episodesPerSeason
            )
        )
    }

    suspend fun removeContinueWatching(id: String) = withContext(Dispatchers.IO) {
        mediaDao.removeContinueWatching(id)
    }

    // Custom Media (Admin)
    suspend fun insertCustomMedia(entity: CustomMediaEntity) = withContext(Dispatchers.IO) {
        mediaDao.insertCustomMedia(entity)
        runCatching {
            firestoreDb?.collection("custom_media")?.document(entity.id)?.set(entity)?.await()
        }
    }

    suspend fun updateCustomMedia(entity: CustomMediaEntity) = withContext(Dispatchers.IO) {
        mediaDao.updateCustomMedia(entity)
        runCatching {
            firestoreDb?.collection("custom_media")?.document(entity.id)?.set(entity)?.await()
        }
    }

    suspend fun deleteCustomMedia(id: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteCustomMedia(id)
        runCatching {
            firestoreDb?.collection("custom_media")?.document(id)?.delete()?.await()
        }
    }

    // Announcements (Admin)
    suspend fun insertAnnouncement(title: String, message: String, type: String) = withContext(Dispatchers.IO) {
        mediaDao.insertAnnouncement(
            AdminAnnouncementEntity(
                title = title,
                message = message,
                type = type,
                active = true
            )
        )
    }

    suspend fun updateAnnouncement(announcement: AdminAnnouncementEntity) = withContext(Dispatchers.IO) {
        mediaDao.updateAnnouncement(announcement)
    }

    suspend fun deleteAnnouncement(id: Int) = withContext(Dispatchers.IO) {
        mediaDao.deleteAnnouncement(id)
    }

    suspend fun toggleAnnouncementActive(id: Int, active: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.setAnnouncementActive(id, active)
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

    // Ad Gate Config (Adsterra Pop-under & 6-Hour Cooldown)
    suspend fun saveAdConfig(config: AdConfigEntity) = withContext(Dispatchers.IO) {
        mediaDao.upsertAdConfig(
            config.copy(
                id = 1,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markAdGateCompleted(timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getAdConfigOnce()
        if (existing == null) {
            mediaDao.upsertAdConfig(AdConfigEntity(id = 1, lastCompletedAtMillis = timestamp))
        } else {
            mediaDao.updateAdLastCompleted(timestamp = timestamp, updatedAt = timestamp)
        }
    }

    suspend fun resetAdCooldown() = withContext(Dispatchers.IO) {
        val existing = mediaDao.getAdConfigOnce()
        if (existing != null) {
            mediaDao.updateAdLastCompleted(timestamp = 0L, updatedAt = System.currentTimeMillis())
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

