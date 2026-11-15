package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure domain helpers for Continue Watching normalization, progress calculation,
 * elapsed-time formatting, and resume preservation.
 */
object ContinueWatchingPolicy {

    fun normalizeSeasonEpisode(mediaType: String, season: Int, episode: Int): Pair<Int, Int> {
        return if (mediaType.equals("tv", ignoreCase = true)) {
            season.coerceAtLeast(0) to episode.coerceAtLeast(1)
        } else {
            1 to 1
        }
    }

    fun computeProgressPercent(watchedSeconds: Long, durationSeconds: Long): Int {
        if (watchedSeconds <= 0L) return 0
        if (durationSeconds > 0L && watchedSeconds >= durationSeconds) return 100
        val safeDuration = if (durationSeconds <= 0L) {
            maxOf(watchedSeconds * 2L, 1800L)
        } else {
            durationSeconds
        }
        return ((watchedSeconds.toDouble() / safeDuration.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(1, 99)
    }

    fun shouldPreserveProgress(
        existing: ContinueWatchingEntity?,
        mediaType: String,
        targetSeason: Int,
        targetEpisode: Int
    ): Boolean {
        if (existing == null) return false
        return if (mediaType.equals("tv", ignoreCase = true)) {
            val (safeSeason, safeEp) = normalizeSeasonEpisode("tv", targetSeason, targetEpisode)
            existing.season == safeSeason && existing.episode == safeEp
        } else {
            true
        }
    }

    fun formatWatchedDuration(watchedSeconds: Long): String {
        if (watchedSeconds <= 0L) return ""
        if (watchedSeconds < 60L) return "${watchedSeconds}s"
        val totalMinutes = watchedSeconds / 60L
        if (totalMinutes < 60L) return "${totalMinutes}m"
        val hours = totalMinutes / 60L
        val mins = totalMinutes % 60L
        return if (mins > 0L) "${hours}h ${mins}m" else "${hours}h"
    }

    fun formatRelativeTime(
        lastWatchedAt: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        if (lastWatchedAt <= 0L || lastWatchedAt > nowMillis) return "Just now"
        val diffMillis = nowMillis - lastWatchedAt
        val minutes = diffMillis / 60_000L
        if (minutes < 1L) return "Just now"
        if (minutes < 60L) return "${minutes}m ago"
        val hours = diffMillis / 3_600_000L
        if (hours < 24L) return "${hours}h ago"
        val days = diffMillis / 86_400_000L
        if (days < 7L) return "${days}d ago"
        return SimpleDateFormat("MMM d", Locale.US).format(Date(lastWatchedAt))
    }
}
