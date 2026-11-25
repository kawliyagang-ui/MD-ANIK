package com.example.data.remote

import com.example.data.model.MediaItem
import com.example.data.model.TmdbCanonicalMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object TmdbMetadataResolver {
    private val cache = ConcurrentHashMap<String, TmdbCanonicalMetadata>()

    fun clearCache() {
        cache.clear()
    }

    fun cacheKey(tmdbId: Long, mediaType: String): String {
        val normType = if (mediaType.equals("tv", ignoreCase = true)) "tv" else "movie"
        return "${normType}_$tmdbId"
    }

    fun putCachedForTest(metadata: TmdbCanonicalMetadata) {
        cache[cacheKey(metadata.tmdbId, metadata.mediaType)] = metadata
    }

    suspend fun resolveForMedia(media: MediaItem): TmdbCanonicalMetadata? = withContext(Dispatchers.IO) {
        val tmdbId = media.resolvedTmdbId() ?: return@withContext null
        val isTv = media.mediaType.equals("tv", ignoreCase = true)
        val normType = if (isTv) "tv" else "movie"
        val key = cacheKey(tmdbId, normType)

        cache[key]?.let { cached ->
            return@withContext cached
        }

        val fetched = runCatching {
            if (isTv) {
                val details = TmdbClient.apiService.getTvDetails(
                    tvId = tmdbId,
                    apiKey = TmdbClient.apiKey,
                    language = "en-US",
                    appendToResponse = "external_ids"
                )
                val extImdb = details.externalIds?.imdbId?.trim().orEmpty().ifBlank {
                    runCatching {
                        TmdbClient.apiService.getTvExternalIds(tmdbId, TmdbClient.apiKey).imdbId?.trim().orEmpty()
                    }.getOrDefault("")
                }
                val seasonAirDates = details.seasons.orEmpty()
                    .mapNotNull { s ->
                        val num = s.seasonNumber ?: return@mapNotNull null
                        val air = s.airDate?.trim().orEmpty()
                        if (air.isBlank()) null else num to air
                    }
                    .toMap()
                TmdbCanonicalMetadata(
                    tmdbId = tmdbId,
                    mediaType = "tv",
                    title = details.name?.trim().orEmpty().ifBlank { media.title.trim() },
                    originalTitle = details.originalName?.trim().orEmpty(),
                    releaseDate = details.firstAirDate?.trim().orEmpty().ifBlank { media.releaseDate.trim() },
                    imdbId = extImdb.lowercase(Locale.ROOT),
                    seasonAirDates = seasonAirDates,
                    numberOfSeasons = details.numberOfSeasons?.coerceAtLeast(1) ?: media.numberOfSeasons.coerceAtLeast(1)
                )
            } else {
                val details = TmdbClient.apiService.getMovieDetails(
                    movieId = tmdbId,
                    apiKey = TmdbClient.apiKey,
                    language = "en-US",
                    appendToResponse = "external_ids"
                )
                val extImdb = details.imdbId?.trim().orEmpty()
                    .ifBlank { details.externalIds?.imdbId?.trim().orEmpty() }
                    .ifBlank {
                        runCatching {
                            TmdbClient.apiService.getMovieExternalIds(tmdbId, TmdbClient.apiKey).imdbId?.trim().orEmpty()
                        }.getOrDefault("")
                    }
                TmdbCanonicalMetadata(
                    tmdbId = tmdbId,
                    mediaType = "movie",
                    title = details.title?.trim().orEmpty().ifBlank { media.title.trim() },
                    originalTitle = details.originalTitle?.trim().orEmpty(),
                    releaseDate = details.releaseDate?.trim().orEmpty().ifBlank { media.releaseDate.trim() },
                    imdbId = extImdb.lowercase(Locale.ROOT)
                )
            }
        }.getOrNull()

        if (fetched != null && fetched.title.isNotBlank()) {
            cache[key] = fetched
            return@withContext fetched
        }

        // Offline/fallback canonical record built from the MediaItem itself
        if (media.title.isNotBlank()) {
            val seasonMap = media.seasons
                .filter { it.airDate.isNotBlank() }
                .associate { it.seasonNumber to it.airDate }
            return@withContext TmdbCanonicalMetadata(
                tmdbId = tmdbId,
                mediaType = normType,
                title = media.title.trim(),
                originalTitle = "",
                releaseDate = media.releaseDate.trim(),
                imdbId = "",
                seasonAirDates = seasonMap,
                numberOfSeasons = media.numberOfSeasons.coerceAtLeast(1)
            )
        }

        null
    }

    fun enrichMediaItem(media: MediaItem, canonical: TmdbCanonicalMetadata?): MediaItem {
        val resolvedId = media.resolvedTmdbId() ?: canonical?.tmdbId
        if (canonical == null) {
            return if (resolvedId != null && media.tmdbId == null) {
                media.copy(tmdbId = resolvedId)
            } else {
                media
            }
        }
        return media.copy(
            tmdbId = resolvedId,
            title = media.title.ifBlank { canonical.title },
            releaseDate = media.releaseDate.ifBlank { canonical.releaseDate },
            numberOfSeasons = maxOf(media.numberOfSeasons, canonical.numberOfSeasons)
        )
    }
}
