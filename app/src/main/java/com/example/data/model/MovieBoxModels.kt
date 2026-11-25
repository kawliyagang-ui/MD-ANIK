package com.example.data.model

import java.util.Locale

data class MovieBoxSubject(
    val subjectId: String,
    val subjectType: Int = 1, // 1 = Movie, 2 = TV/Series
    val title: String,
    val description: String = "",
    val releaseDate: String = "",
    val imdbRating: String = "",
    val detailPath: String = "",
    val coverUrl: String = "",
    val hasResource: Boolean = true,
    val sourceHost: String = "",
    val language: String = "",
    val tmdbId: Long? = null,
    val imdbId: String = "",
    val originalTitle: String = "",
    val availableSeasons: List<Int> = emptyList()
) {
    val mediaTypeLabel: String
        get() = if (subjectType == 2) "TV Series" else "Movie"

    val releaseYear: String
        get() = releaseDate.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty()
}

data class MovieBoxDownloadOption(
    val id: String,
    val resolution: Int,
    val resolutionLabel: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val format: String = "MP4",
    val refererHeader: String = DEFAULT_REFERER,
    val userAgentHeader: String = DEFAULT_USER_AGENT,
    val cookieHeader: String = "",
    val language: String = ""
) {
    val formattedSize: String
        get() = formatDownloadBytes(sizeBytes)

    companion object {
        const val DEFAULT_REFERER = "https://videodownloader.site/"
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}

data class MovieBoxSubtitleOption(
    val id: String,
    val languageCode: String,
    val languageName: String,
    val url: String,
    val sizeBytes: Long = 0L
) {
    val formattedSize: String
        get() = formatDownloadBytes(sizeBytes)
}

data class MovieBoxLanguageGroup(
    val language: String,
    val subject: MovieBoxSubject,
    val options: List<MovieBoxDownloadOption>,
    val subtitles: List<MovieBoxSubtitleOption> = emptyList()
)

data class MovieBoxDownloadBundle(
    val subject: MovieBoxSubject,
    val options: List<MovieBoxDownloadOption>,
    val subtitles: List<MovieBoxSubtitleOption> = emptyList(),
    val sourceHost: String = "",
    val season: Int = 1,
    val episode: Int = 1,
    val languageGroups: List<MovieBoxLanguageGroup> = emptyList()
)

enum class DownloadStatus(val storageValue: String, val displayLabel: String) {
    QUEUED("QUEUED", "Queued"),
    RUNNING("RUNNING", "Downloading"),
    PAUSED("PAUSED", "Paused"),
    COMPLETED("COMPLETED", "Completed"),
    FAILED("FAILED", "Failed"),
    CANCELLED("CANCELLED", "Cancelled");

    val isActive: Boolean
        get() = this == QUEUED || this == RUNNING || this == PAUSED

    val isRunningOrQueued: Boolean
        get() = this == QUEUED || this == RUNNING

    companion object {
        fun fromStorage(raw: String?): DownloadStatus {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.equals("DOWNLOADING", ignoreCase = true)) return RUNNING
            return entries.firstOrNull { it.storageValue.equals(trimmed, ignoreCase = true) } ?: QUEUED
        }
    }
}

enum class PreferredDownloadQuality(
    val storageKey: String,
    val displayName: String,
    val targetResolution: Int
) {
    AUTO("auto", "Auto (Best Available)", 2160),
    Q1080P("1080p", "Full HD 1080p", 1080),
    Q720P("720p", "HD 720p (Balanced)", 720),
    Q480P("480p", "SD 480p (Data Saver)", 480),
    Q360P("360p", "Low 360p (Ultra Light)", 360);

    companion object {
        fun fromKey(raw: String?): PreferredDownloadQuality {
            return entries.firstOrNull { it.storageKey.equals(raw, ignoreCase = true) } ?: Q720P
        }
    }
}

fun formatDownloadBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    if (safe <= 0L) return "Unknown size"
    return when {
        safe < 1024L -> "$safe B"
        safe < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", safe / 1024.0)
        safe < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", safe / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.2f GB", safe / (1024.0 * 1024.0 * 1024.0))
    }
}
