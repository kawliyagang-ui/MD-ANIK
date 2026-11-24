package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val tmdbId: Long? = null,
    val title: String,
    val overview: String,
    val posterPath: String,
    val backdropPath: String = "",
    val mediaType: String = "movie",
    val releaseDate: String = "",
    val voteAverage: Double = 0.0,
    val genres: String = "", // Comma-separated
    val certification: String = "",
    val vidlyId: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "continue_watching")
data class ContinueWatchingEntity(
    @PrimaryKey val id: String,
    val tmdbId: Long? = null,
    val title: String,
    val overview: String = "",
    val posterPath: String,
    val backdropPath: String = "",
    val mediaType: String = "movie",
    val releaseDate: String = "",
    val voteAverage: Double = 0.0,
    val genres: String = "",
    val certification: String = "",
    val numberOfSeasons: Int = 1,
    val episodesPerSeason: Int = 10,
    val season: Int = 1,
    val episode: Int = 1,
    val progressPercent: Int = 0,
    val watchedSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val vidlyId: String = ""
)

@Entity(tableName = "custom_media")
data class CustomMediaEntity(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val overview: String = "",
    val posterPath: String = "",
    val backdropPath: String = "",
    val mediaType: String = "movie",
    val releaseDate: String = "",
    val voteAverage: Double = 8.0,
    val genres: String = "Action, Drama",
    val certification: String = "",
    val vidlyId: String = "",
    val tmdbId: Long? = null,
    val trailerYoutubeKey: String? = null,
    val numberOfSeasons: Int = 1,
    val episodesPerSeason: Int = 10,
    val directVideoUrl: String = "",
    val downloadUrl: String = "",
    val episodeLinksJson: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "announcements")
data class AdminAnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // "INFO", "WARNING", "UPDATE"
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tmdb_search_history")
data class TmdbSearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ad_config")
data class AdConfigEntity(
    @PrimaryKey val id: Int = 1,
    val isEnabled: Boolean = false,
    val adNetwork: String = "Adsterra",
    val adFormat: String = "Pop-under",
    val adUrl: String = "",
    val popUnderScript: String = "",
    val headline: String = "Sponsored Access Pass",
    val description: String = "Tap the sponsor ad below and stay on the ad page for 30 seconds to unlock 6 hours of access.",
    val ctaText: String = "Open Sponsor Ad",
    val requiredStaySeconds: Int = 30,
    val cooldownHours: Int = 6,
    val lastCompletedAtMillis: Long = 0L,
    val cooldownResetAtMillis: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_security")
data class AdminSecurityEntity(
    @PrimaryKey val id: Int = 1,
    val passcodeHash: String = "",
    val isDefault: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "issue_reports")
data class IssueReportEntity(
    @PrimaryKey val id: String,
    val category: String,
    val description: String,
    val mediaId: String = "",
    val mediaTitle: String = "",
    val mediaType: String = "",
    val serverName: String = "",
    val episodeLabel: String = "",
    val screenshotPreviewBase64: String = "",
    val status: String = "OPEN", // OPEN, IN_REVIEW, RESOLVED, DISMISSED
    val adminNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tracked_releases")
data class TrackedReleaseEntity(
    @PrimaryKey val releaseKey: String,
    val title: String,
    val notifiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val systemDownloadId: Long = -1L,
    val mediaId: String,
    val tmdbId: Long? = null,
    val title: String,
    val posterPath: String = "",
    val backdropPath: String = "",
    val mediaType: String = "movie",
    val season: Int = 1,
    val episode: Int = 1,
    val episodeTitle: String = "",
    val qualityLabel: String = "720p HD",
    val format: String = "MP4",
    val providerName: String = "Cloud HD",
    val sourceUrl: String,
    val localFilePath: String = "",
    val localUri: String = "",
    val subtitleUrl: String = "",
    val subtitleLabel: String = "",
    val status: String = DownloadStatus.QUEUED.storageValue,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Int = 0,
    val errorReason: String = "",
    val refererHeader: String = "",
    val userAgentHeader: String = "",
    val cookieHeader: String = "",
    val wifiOnly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L
) {
    val parsedStatus: DownloadStatus
        get() = DownloadStatus.fromStorage(status)

    val fullPosterUrl: String
        get() = when {
            posterPath.isBlank() -> ""
            posterPath.startsWith("http") -> posterPath
            else -> "https://image.tmdb.org/t/p/w500$posterPath"
        }

    val episodeSubtitle: String
        get() = if (mediaType.equals("tv", ignoreCase = true)) {
            val epSuffix = if (episodeTitle.isNotBlank()) " • $episodeTitle" else ""
            "S$season:E$episode$epSuffix"
        } else {
            "Movie • $qualityLabel"
        }
}

@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val key: String, // e.g. "${mediaId}_movie" or "${mediaId}_s${season}_e${episode}"
    val mediaId: String,
    val season: Int = 1,
    val episode: Int = 1,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
