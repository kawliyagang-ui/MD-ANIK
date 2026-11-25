package com.example.data.model

import java.io.File
import java.util.Locale

sealed interface PlaybackSource {
    data class LocalFile(
        val download: DownloadEntity,
        val filePath: String
    ) : PlaybackSource

    data class DirectStream(
        val url: String
    ) : PlaybackSource

    data object WebEmbed : PlaybackSource
}

data class EpisodeSeasonEligibility(
    val eligibleEpisodes: List<Int>,
    val downloadedEpisodes: List<Int>,
    val noLinkEpisodes: List<Int>
) {
    fun isAllEligibleSelected(selected: Set<Int>): Boolean {
        return eligibleEpisodes.isNotEmpty() && eligibleEpisodes.all { it in selected }
    }
}

data class DownloadSeasonGroup(
    val seriesKey: String,
    val seriesTitle: String,
    val posterUrl: String,
    val seasonNumber: Int,
    val episodes: List<DownloadEntity>,
    val completedCount: Int,
    val overallProgressPercent: Int,
    val totalDownloadedBytes: Long,
    val totalExpectedBytes: Long
)

data class DownloadSeriesGroup(
    val mediaId: String,
    val seriesTitle: String,
    val posterUrl: String,
    val seasons: List<DownloadSeasonGroup>
)

data class DownloadGroupingResult(
    val seriesGroups: List<DownloadSeriesGroup>,
    val movieDownloads: List<DownloadEntity>
)

object PlaybackSourcePolicy {

    fun isHlsStreamUrl(url: String): Boolean {
        val clean = url.trim().lowercase(Locale.US)
        if (clean.isBlank()) return false
        val pathPart = clean.substringBefore('?').substringBefore('#')
        return pathPart.endsWith(".m3u8") || clean.contains("format=m3u8") || clean.contains(".m3u8?")
    }

    fun resolvePlaybackSource(
        media: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        episodesInSeason: List<EpisodeInfo> = emptyList(),
        downloads: List<DownloadEntity> = emptyList(),
        fileExistsChecker: (String) -> Boolean = { path ->
            path.isNotBlank() && File(path).let { it.exists() && it.length() > 0L }
        }
    ): PlaybackSource {
        val isTv = media.mediaType.equals("tv", ignoreCase = true)

        // (a) Completed local download exists -> play local file offline
        val completedDownload = downloads
            .asSequence()
            .filter { dl ->
                dl.matchesMedia(media) &&
                    dl.parsedStatus == DownloadStatus.COMPLETED &&
                    fileExistsChecker(dl.localFilePath) &&
                    (!isTv || (dl.season == season && dl.episode == episode))
            }
            .sortedByDescending { it.completedAt.coerceAtLeast(it.updatedAt) }
            .firstOrNull()

        if (completedDownload != null) {
            return PlaybackSource.LocalFile(
                download = completedDownload,
                filePath = completedDownload.localFilePath
            )
        }

        // (b) Else if a direct link exists -> stream it in the Media3 Direct Player
        val resolvedDirect = if (isTv) {
            media.resolveDirectLinkForEpisode(season, episode, episodesInSeason)
                .effectiveStreamUrl()
                .ifBlank { media.effectiveStreamUrl() }
        } else {
            media.effectiveStreamUrl()
        }

        if (resolvedDirect.startsWith("http://", ignoreCase = true) ||
            resolvedDirect.startsWith("https://", ignoreCase = true)
        ) {
            return PlaybackSource.DirectStream(resolvedDirect)
        }

        // Fallback if vidlyId itself points directly to an .mp4 or .m3u8 file
        val vidlyUrl = media.vidlyId.trim()
        if ((vidlyUrl.startsWith("http://", ignoreCase = true) || vidlyUrl.startsWith("https://", ignoreCase = true)) &&
            (isHlsStreamUrl(vidlyUrl) || vidlyUrl.substringBefore('?').endsWith(".mp4", ignoreCase = true))
        ) {
            return PlaybackSource.DirectStream(vidlyUrl)
        }

        // (c) Else -> existing WebView embed player (Vidly / VidSrc / AutoEmbed / MultiEmbed)
        return PlaybackSource.WebEmbed
    }

    fun computeSeasonEligibility(
        media: MediaItem,
        seasonNumber: Int,
        episodeNumbers: List<Int>,
        episodesInSeason: List<EpisodeInfo> = emptyList(),
        downloads: List<DownloadEntity> = emptyList(),
        fileExistsChecker: (String) -> Boolean = { path ->
            path.isNotBlank() && File(path).let { it.exists() && it.length() > 0L }
        }
    ): EpisodeSeasonEligibility {
        val eligible = mutableListOf<Int>()
        val downloaded = mutableListOf<Int>()
        val noLink = mutableListOf<Int>()

        episodeNumbers.distinct().sorted().forEach { epNum ->
            val existing = downloads
                .filter { it.matchesMedia(media) && it.season == seasonNumber && it.episode == epNum }
                .maxByOrNull { it.updatedAt }

            val isCompleted = existing?.parsedStatus == DownloadStatus.COMPLETED &&
                fileExistsChecker(existing.localFilePath)

            if (isCompleted) {
                downloaded += epNum
                return@forEach
            }

            val isAlreadyQueuedOrRunning = existing?.parsedStatus?.isActive == true
            val link = media.resolveDirectLinkForEpisode(seasonNumber, epNum, episodesInSeason)
            if (!link.hasAnyLink()) {
                noLink += epNum
            } else if (!isAlreadyQueuedOrRunning) {
                eligible += epNum
            }
        }

        return EpisodeSeasonEligibility(
            eligibleEpisodes = eligible,
            downloadedEpisodes = downloaded,
            noLinkEpisodes = noLink
        )
    }

    fun groupDownloadsForDisplay(downloads: List<DownloadEntity>): DownloadGroupingResult {
        val tvDownloads = downloads.filter { it.mediaType.equals("tv", ignoreCase = true) }
        val movieDownloads = downloads
            .filterNot { it.mediaType.equals("tv", ignoreCase = true) }
            .sortedByDescending { it.createdAt }

        val seriesGroups = tvDownloads
            .groupBy { dl ->
                dl.tmdbId?.takeIf { it > 0L }?.let { "tmdb_$it" } ?: dl.mediaId
            }
            .map { (groupKey, seriesItems) ->
                val sortedItems = seriesItems.sortedByDescending { it.createdAt }
                val sample = sortedItems.first()
                val mediaId = sample.mediaId
                val seasons = seriesItems
                    .groupBy { it.season }
                    .toSortedMap()
                    .map { (seasonNum, eps) ->
                        val sortedEps = eps.sortedWith(compareBy<DownloadEntity> { it.episode }.thenByDescending { it.createdAt })
                        val completedCount = sortedEps.count { it.parsedStatus == DownloadStatus.COMPLETED }
                        val downloadedBytes = sortedEps.sumOf { dl ->
                            if (dl.parsedStatus == DownloadStatus.COMPLETED) {
                                maxOf(dl.bytesDownloaded, dl.totalBytes)
                            } else {
                                dl.bytesDownloaded.coerceAtLeast(0L)
                            }
                        }
                        val expectedBytes = sortedEps.sumOf { dl ->
                            maxOf(dl.totalBytes, if (dl.parsedStatus == DownloadStatus.COMPLETED) dl.bytesDownloaded else 0L)
                        }
                        val overallPercent = when {
                            sortedEps.isEmpty() -> 0
                            expectedBytes > 0L -> ((downloadedBytes * 100L) / expectedBytes).toInt().coerceIn(0, 100)
                            else -> sortedEps.sumOf { dl ->
                                if (dl.parsedStatus == DownloadStatus.COMPLETED) 100 else dl.progressPercent.coerceIn(0, 100)
                            } / sortedEps.size
                        }
                        DownloadSeasonGroup(
                            seriesKey = "${mediaId}_s$seasonNum",
                            seriesTitle = sample.title,
                            posterUrl = sample.fullPosterUrl,
                            seasonNumber = seasonNum,
                            episodes = sortedEps,
                            completedCount = completedCount,
                            overallProgressPercent = overallPercent,
                            totalDownloadedBytes = downloadedBytes,
                            totalExpectedBytes = expectedBytes
                        )
                    }

                DownloadSeriesGroup(
                    mediaId = mediaId,
                    seriesTitle = sample.title,
                    posterUrl = sample.fullPosterUrl,
                    seasons = seasons
                )
            }
            .sortedByDescending { grp -> grp.seasons.flatMap { it.episodes }.maxOfOrNull { it.createdAt } ?: 0L }

        return DownloadGroupingResult(
            seriesGroups = seriesGroups,
            movieDownloads = movieDownloads
        )
    }
}

fun DownloadEntity.matchesMedia(media: MediaItem): Boolean {
    if (mediaId == media.id) return true
    val targetTmdbId = media.resolvedTmdbId() ?: return false
    val dlTmdbId = tmdbId?.takeIf { it > 0L } ?: mediaId.trim().toLongOrNull()?.takeIf { it > 0L } ?: return false
    return dlTmdbId == targetTmdbId && mediaType.equals(media.mediaType, ignoreCase = true)
}
