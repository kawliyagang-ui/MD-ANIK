package com.example.data.model

data class SeasonInfo(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val airDate: String = "",
    val overview: String = ""
)

data class EpisodeInfo(
    val episodeNumber: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String = "",
    val runtimeMinutes: Int? = null,
    val stillPath: String = "",
    val airDate: String = ""
) {
    val fullStillUrl: String
        get() = when {
            stillPath.isBlank() -> ""
            stillPath.startsWith("http") -> stillPath
            else -> "https://image.tmdb.org/t/p/w500$stillPath"
        }
}

data class MediaItem(
    val id: String,
    val tmdbId: Long? = null,
    val title: String,
    val overview: String,
    val posterPath: String,
    val backdropPath: String = "",
    val mediaType: String = "movie", // "movie" or "tv"
    val releaseDate: String = "",
    val voteAverage: Double = 0.0,
    val genres: List<String> = emptyList(),
    val certification: String = "",
    val vidlyId: String = "",
    val isCustom: Boolean = false,
    val trailerYoutubeKey: String? = null,
    val numberOfSeasons: Int = 1,
    val episodesPerSeason: Int = 10,
    val totalEpisodes: Int? = null,
    val seasons: List<SeasonInfo> = emptyList(),
    val originalLanguage: String = "",
    val originCountry: List<String> = emptyList(),
    val latestEpisodeLabel: String = "",
    val nextAirDate: String = "",
    val popularity: Double = 0.0
) {
    val fullPosterUrl: String
        get() = when {
            posterPath.isBlank() -> ""
            posterPath.startsWith("http") -> posterPath
            else -> "https://image.tmdb.org/t/p/w500$posterPath"
        }

    val fullBackdropUrl: String
        get() = when {
            backdropPath.isNotBlank() && backdropPath.startsWith("http") -> backdropPath
            backdropPath.isNotBlank() -> "https://image.tmdb.org/t/p/w1280$backdropPath"
            posterPath.isNotBlank() && posterPath.startsWith("http") -> posterPath
            posterPath.isNotBlank() -> "https://image.tmdb.org/t/p/w1280$posterPath"
            else -> ""
        }

    val streamId: String
        get() = if (vidlyId.isNotBlank()) vidlyId else (tmdbId?.toString() ?: id)

    fun resolvedTmdbId(): Long? {
        return tmdbId ?: vidlyId.toLongOrNull() ?: id.toLongOrNull()
    }

    fun resolvedSeasonList(): List<SeasonInfo> {
        val valid = seasons.filter { it.episodeCount > 0 }
        if (valid.isNotEmpty()) {
            val regular = valid.filter { it.seasonNumber > 0 }.sortedBy { it.seasonNumber }
            val specials = valid.filter { it.seasonNumber == 0 }
            return regular + specials
        }
        val count = numberOfSeasons.coerceAtLeast(1)
        val epCount = episodesPerSeason.coerceAtLeast(1)
        return (1..count).map { num ->
            SeasonInfo(
                seasonNumber = num,
                name = "Season $num",
                episodeCount = epCount
            )
        }
    }

    fun defaultSeasonNumber(): Int {
        val list = resolvedSeasonList()
        return list.firstOrNull { it.seasonNumber > 0 }?.seasonNumber
            ?: list.firstOrNull()?.seasonNumber
            ?: 1
    }

    fun episodeCountForSeason(seasonNumber: Int): Int {
        return seasons.firstOrNull { it.seasonNumber == seasonNumber }
            ?.episodeCount
            ?.takeIf { it > 0 }
            ?: episodesPerSeason.coerceAtLeast(1)
    }

    fun totalEpisodeCount(): Int {
        return totalEpisodes?.takeIf { it > 0 }
            ?: resolvedSeasonList().sumOf { it.episodeCount }
    }

    fun getStreamUrl(server: PlayerServer = PlayerServer.VIDLY, season: Int = 1, episode: Int = 1): String {
        val sId = streamId.trim()
        val isCustomHttpUrl = sId.startsWith("http://", ignoreCase = true) ||
            sId.startsWith("https://", ignoreCase = true)
        val numericOrFallbackId = if (isCustomHttpUrl) {
            tmdbId?.toString() ?: sId
        } else {
            sId
        }

        return when (server) {
            PlayerServer.VIDLY -> {
                if (isCustomHttpUrl) {
                    sId
                } else if (mediaType == "tv") {
                    "https://vidly.pro/tv/$sId/$season/$episode"
                } else {
                    "https://vidly.pro/movie/$sId"
                }
            }
            PlayerServer.VIDSRC -> {
                if (mediaType == "tv") {
                    "https://vidsrc.to/embed/tv/$numericOrFallbackId/$season/$episode"
                } else {
                    "https://vidsrc.to/embed/movie/$numericOrFallbackId"
                }
            }
            PlayerServer.AUTOEMBED -> {
                if (mediaType == "tv") {
                    "https://player.autoembed.cc/embed/tv/$numericOrFallbackId/$season/$episode"
                } else {
                    "https://player.autoembed.cc/embed/movie/$numericOrFallbackId"
                }
            }
            PlayerServer.MULTIEMBED -> {
                if (mediaType == "tv") {
                    "https://multiembed.mov/?video_id=$numericOrFallbackId&tmdb=1&s=$season&e=$episode"
                } else {
                    "https://multiembed.mov/?video_id=$numericOrFallbackId&tmdb=1"
                }
            }
        }
    }
}

enum class PlayerServer(val displayName: String, val speed: String) {
    VIDLY("Vidly Pro (Primary)", "Ultra Fast"),
    VIDSRC("VidSrc Backup", "HD 1080p"),
    AUTOEMBED("AutoEmbed Backup", "Adaptive"),
    MULTIEMBED("MultiEmbed Server", "High Speed")
}

// Extension to map CustomMediaEntity to MediaItem
fun CustomMediaEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = id,
        tmdbId = tmdbId,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        mediaType = mediaType,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        genres = genres.split(",").map { it.trim() }.filter { it.isNotBlank() },
        certification = certification.trim(),
        vidlyId = vidlyId,
        isCustom = true,
        trailerYoutubeKey = trailerYoutubeKey,
        numberOfSeasons = numberOfSeasons,
        episodesPerSeason = episodesPerSeason
    )
}

// Extension to map WatchlistEntity to MediaItem
fun WatchlistEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = id,
        tmdbId = tmdbId,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        mediaType = mediaType,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        genres = genres.split(",").map { it.trim() }.filter { it.isNotBlank() },
        certification = certification.trim(),
        vidlyId = vidlyId,
        isCustom = false
    )
}

// Extension to map ContinueWatchingEntity to MediaItem with full metadata preserved
fun ContinueWatchingEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = id,
        tmdbId = tmdbId,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        mediaType = mediaType,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        genres = genres.split(",").map { it.trim() }.filter { it.isNotBlank() },
        certification = certification.trim(),
        vidlyId = vidlyId,
        isCustom = id.startsWith("custom_"),
        numberOfSeasons = numberOfSeasons.coerceAtLeast(1),
        episodesPerSeason = episodesPerSeason.coerceAtLeast(1)
    )
}

fun MediaItem.mergeWithCatalog(catalogMatch: MediaItem?): MediaItem {
    if (catalogMatch == null) return this
    return catalogMatch.copy(
        tmdbId = catalogMatch.tmdbId ?: tmdbId,
        overview = catalogMatch.overview.ifBlank { overview },
        posterPath = catalogMatch.posterPath.ifBlank { posterPath },
        backdropPath = catalogMatch.backdropPath.ifBlank { backdropPath },
        releaseDate = catalogMatch.releaseDate.ifBlank { releaseDate },
        voteAverage = if (catalogMatch.voteAverage > 0.0) catalogMatch.voteAverage else voteAverage,
        genres = catalogMatch.genres.ifEmpty { genres },
        certification = catalogMatch.certification.ifBlank { certification },
        vidlyId = catalogMatch.vidlyId.ifBlank { vidlyId },
        numberOfSeasons = maxOf(catalogMatch.numberOfSeasons, numberOfSeasons),
        episodesPerSeason = if (catalogMatch.seasons.isNotEmpty()) catalogMatch.episodesPerSeason else episodesPerSeason
    )
}
