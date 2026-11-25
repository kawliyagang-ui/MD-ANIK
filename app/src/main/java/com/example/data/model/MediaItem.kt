package com.example.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

data class EpisodeDirectLink(
    val directVideoUrl: String = "",
    val downloadUrl: String = ""
) {
    fun effectiveStreamUrl(): String = directVideoUrl.trim().ifBlank { downloadUrl.trim() }

    fun effectiveDownloadUrl(): String = downloadUrl.trim().ifBlank { directVideoUrl.trim() }

    fun hasAnyLink(): Boolean =
        effectiveStreamUrl().startsWith("http://", ignoreCase = true) ||
            effectiveStreamUrl().startsWith("https://", ignoreCase = true)
}

object EpisodeDirectLinksCodec {
    private val keyPattern = Regex("^s?(\\d+)[ex:\\-]e?(\\d+)$", RegexOption.IGNORE_CASE)
    private val moshi: Moshi = Moshi.Builder().build()
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    fun keyFor(season: Int, episode: Int): String = "s${season}e${episode}"

    fun decodeJson(rawJson: String): Map<String, EpisodeDirectLink> {
        val trimmed = rawJson.trim()
        if (trimmed.isBlank()) return emptyMap()
        return runCatching {
            val root = mapAdapter.fromJson(trimmed) ?: return@runCatching emptyMap()
            val result = linkedMapOf<String, EpisodeDirectLink>()
            root.forEach { (rawKey, rawVal) ->
                val canonical = normalizeKey(rawKey) ?: return@forEach
                val link = when (rawVal) {
                    is Map<*, *> -> EpisodeDirectLink(
                        directVideoUrl = (rawVal["directVideoUrl"] as? String)?.trim().orEmpty(),
                        downloadUrl = (rawVal["downloadUrl"] as? String)?.trim().orEmpty()
                    )
                    is String -> {
                        val str = rawVal.trim()
                        EpisodeDirectLink(directVideoUrl = str, downloadUrl = str)
                    }
                    else -> EpisodeDirectLink()
                }
                if (link.directVideoUrl.isNotBlank() || link.downloadUrl.isNotBlank()) {
                    result[canonical] = link
                }
            }
            result
        }.getOrDefault(emptyMap())
    }

    fun encodeJson(map: Map<String, EpisodeDirectLink>): String {
        if (map.isEmpty()) return ""
        val encoded = linkedMapOf<String, Map<String, String>>()
        map.forEach { (rawKey, link) ->
            val key = normalizeKey(rawKey) ?: return@forEach
            if (link.directVideoUrl.isBlank() && link.downloadUrl.isBlank()) return@forEach
            encoded[key] = linkedMapOf(
                "directVideoUrl" to link.directVideoUrl.trim(),
                "downloadUrl" to link.downloadUrl.trim()
            )
        }
        return if (encoded.isEmpty()) "" else mapAdapter.toJson(encoded)
    }

    fun parseMultilineInput(input: String): String {
        val parsed = linkedMapOf<String, EpisodeDirectLink>()
        input.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .forEach { line ->
                val parts = line.split("|", limit = 3).map { it.trim() }
                if (parts.size < 2) return@forEach
                val canonical = normalizeKey(parts[0]) ?: return@forEach
                val stream = parts[1]
                val dl = if (parts.size >= 3 && parts[2].isNotBlank()) parts[2] else stream
                if (stream.isNotBlank() || dl.isNotBlank()) {
                    parsed[canonical] = EpisodeDirectLink(directVideoUrl = stream, downloadUrl = dl)
                }
            }
        return encodeJson(parsed)
    }

    fun formatMultilineInput(rawJson: String): String {
        val decoded = decodeJson(rawJson)
        if (decoded.isEmpty()) return ""
        return decoded.entries.joinToString("\n") { (key, link) ->
            val upper = key.uppercase()
            val stream = link.directVideoUrl
            val dl = link.downloadUrl
            when {
                stream.isNotBlank() && dl.isNotBlank() && stream != dl -> "$upper | $stream | $dl"
                else -> "$upper | ${stream.ifBlank { dl }}"
            }
        }
    }

    private fun normalizeKey(raw: String): String? {
        val compact = raw.trim().replace(" ", "")
        val match = keyPattern.matchEntire(compact) ?: return null
        val s = match.groupValues[1].toIntOrNull()?.coerceAtLeast(0) ?: return null
        val e = match.groupValues[2].toIntOrNull()?.coerceAtLeast(1) ?: return null
        return keyFor(s, e)
    }
}

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
    val airDate: String = "",
    val directVideoUrl: String = "",
    val downloadUrl: String = ""
) {
    val fullStillUrl: String
        get() = when {
            stillPath.isBlank() -> ""
            stillPath.startsWith("http") -> stillPath
            else -> "https://image.tmdb.org/t/p/w500$stillPath"
        }

    fun effectiveStreamUrl(): String = directVideoUrl.trim().ifBlank { downloadUrl.trim() }

    fun effectiveDownloadUrl(): String = downloadUrl.trim().ifBlank { directVideoUrl.trim() }

    fun hasAnyDirectLink(): Boolean =
        effectiveStreamUrl().startsWith("http://", ignoreCase = true) ||
            effectiveStreamUrl().startsWith("https://", ignoreCase = true)
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
    val popularity: Double = 0.0,
    val directVideoUrl: String = "",
    val downloadUrl: String = "",
    val episodeLinksJson: String = ""
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

    fun isWebTorrentStream(): Boolean {
        return com.example.ui.components.WebTorrentPolicy.isWebTorrentSource(streamId)
    }

    fun resolvedTmdbId(): Long? {
        return tmdbId?.takeIf { it > 0L }
            ?: vidlyId.trim().toLongOrNull()?.takeIf { it > 0L }
            ?: id.trim().toLongOrNull()?.takeIf { it > 0L }
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

    fun episodesForSeason(seasonNumber: Int): Int = episodeCountForSeason(seasonNumber)

    fun totalEpisodeCount(): Int {
        return totalEpisodes?.takeIf { it > 0 }
            ?: resolvedSeasonList().sumOf { it.episodeCount }
    }

    fun effectiveStreamUrl(): String = directVideoUrl.trim().ifBlank { downloadUrl.trim() }

    fun effectiveDownloadUrl(): String = downloadUrl.trim().ifBlank { directVideoUrl.trim() }

    fun resolveDirectLinkForEpisode(
        season: Int,
        episode: Int,
        episodesInSeason: List<EpisodeInfo> = emptyList()
    ): EpisodeDirectLink {
        val ep = episodesInSeason.firstOrNull { it.seasonNumber == season && it.episodeNumber == episode }
            ?: episodesInSeason.firstOrNull { it.episodeNumber == episode }
        if (ep != null && ep.hasAnyDirectLink()) {
            return EpisodeDirectLink(
                directVideoUrl = ep.directVideoUrl.trim(),
                downloadUrl = ep.downloadUrl.trim()
            )
        }
        val mapped = EpisodeDirectLinksCodec.decodeJson(episodeLinksJson)[EpisodeDirectLinksCodec.keyFor(season, episode)]
        if (mapped != null && mapped.hasAnyLink()) {
            return mapped
        }
        if (!mediaType.equals("tv", ignoreCase = true)) {
            return EpisodeDirectLink(
                directVideoUrl = directVideoUrl.trim(),
                downloadUrl = downloadUrl.trim()
            )
        }
        return EpisodeDirectLink()
    }

    fun getStreamUrl(server: PlayerServer = PlayerServer.VIDLY, season: Int = 1, episode: Int = 1): String {
        val sId = streamId.trim()
        val isWebTorrentId = com.example.ui.components.WebTorrentPolicy.isWebTorrentSource(sId)
        if (isWebTorrentId) {
            return com.example.ui.components.WebTorrentPolicy.normalizeTorrentId(sId, title)
        }

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
            PlayerServer.WEBTORRENT -> {
                if (isCustomHttpUrl) sId else ""
            }
        }
    }
}

enum class PlayerServer(val displayName: String, val speed: String) {
    VIDLY("Vidly Pro (Primary)", "Ultra Fast"),
    VIDSRC("VidSrc Backup", "HD 1080p"),
    AUTOEMBED("AutoEmbed Backup", "Adaptive"),
    MULTIEMBED("MultiEmbed Server", "High Speed"),
    WEBTORRENT("WebTorrent P2P", "Magnet / .torrent")
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
        episodesPerSeason = episodesPerSeason,
        directVideoUrl = directVideoUrl,
        downloadUrl = downloadUrl,
        episodeLinksJson = episodeLinksJson
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
        episodesPerSeason = if (catalogMatch.seasons.isNotEmpty()) catalogMatch.episodesPerSeason else episodesPerSeason,
        directVideoUrl = catalogMatch.directVideoUrl.ifBlank { directVideoUrl },
        downloadUrl = catalogMatch.downloadUrl.ifBlank { downloadUrl },
        episodeLinksJson = catalogMatch.episodeLinksJson.ifBlank { episodeLinksJson }
    )
}
