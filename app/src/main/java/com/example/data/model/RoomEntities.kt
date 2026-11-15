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
