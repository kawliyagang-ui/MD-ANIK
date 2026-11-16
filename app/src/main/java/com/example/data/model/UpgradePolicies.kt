package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FilterOption(
    val code: String,
    val label: String
)

data class GenreFilterOption(
    val name: String,
    val movieGenreId: Int,
    val tvGenreId: Int
)

data class BrowseLanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val badgeCode: String = code.uppercase(Locale.US)
)

object TmdbFilterCatalog {
    val genres = listOf(
        GenreFilterOption("Action", 28, 10759),
        GenreFilterOption("Adventure", 12, 10759),
        GenreFilterOption("Animation", 16, 16),
        GenreFilterOption("Comedy", 35, 35),
        GenreFilterOption("Crime", 80, 80),
        GenreFilterOption("Documentary", 99, 99),
        GenreFilterOption("Drama", 18, 18),
        GenreFilterOption("Family", 10751, 10751),
        GenreFilterOption("Fantasy", 14, 10765),
        GenreFilterOption("History", 36, 36),
        GenreFilterOption("Horror", 27, 9648),
        GenreFilterOption("Music", 10402, 10402),
        GenreFilterOption("Mystery", 9648, 9648),
        GenreFilterOption("Romance", 10749, 10749),
        GenreFilterOption("Sci-Fi", 878, 10765),
        GenreFilterOption("Thriller", 53, 80),
        GenreFilterOption("War", 10752, 10768),
        GenreFilterOption("Western", 37, 37)
    )

    val genreNames: List<String> = listOf("All") + genres.map { it.name }

    val yearOptions: List<Int> = listOf(
        2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018, 2015, 2010, 2000, 1990
    )

    val ratingOptions: List<Double> = listOf(0.0, 6.0, 7.0, 8.0, 9.0)

    val browseLanguages: List<BrowseLanguageOption> = listOf(
        BrowseLanguageOption("en", "English", "English", "EN"),
        BrowseLanguageOption("hi", "Hindi", "हिन्दी", "HI"),
        BrowseLanguageOption("ko", "Korean", "한국어", "KO"),
        BrowseLanguageOption("ja", "Japanese", "日本語", "JA"),
        BrowseLanguageOption("es", "Spanish", "Español", "ES"),
        BrowseLanguageOption("fr", "French", "Français", "FR"),
        BrowseLanguageOption("ta", "Tamil", "தமிழ்", "TA"),
        BrowseLanguageOption("te", "Telugu", "తెలుగు", "TE"),
        BrowseLanguageOption("ml", "Malayalam", "മലയാളം", "ML"),
        BrowseLanguageOption("kn", "Kannada", "ಕನ್ನಡ", "KN"),
        BrowseLanguageOption("bn", "Bengali", "বাংলা", "BN"),
        BrowseLanguageOption("mr", "Marathi", "मराठी", "MR"),
        BrowseLanguageOption("pa", "Punjabi", "ਪੰਜਾਬੀ", "PA"),
        BrowseLanguageOption("de", "German", "Deutsch", "DE"),
        BrowseLanguageOption("it", "Italian", "Italiano", "IT"),
        BrowseLanguageOption("zh", "Chinese", "中文", "ZH"),
        BrowseLanguageOption("pt", "Portuguese", "Português", "PT"),
        BrowseLanguageOption("ar", "Arabic", "العربية", "AR"),
        BrowseLanguageOption("tr", "Turkish", "Türkçe", "TR"),
        BrowseLanguageOption("ru", "Russian", "Русский", "RU"),
        BrowseLanguageOption("th", "Thai", "ไทย", "TH"),
        BrowseLanguageOption("id", "Indonesian", "Bahasa Indonesia", "ID"),
        BrowseLanguageOption("nl", "Dutch", "Nederlands", "NL"),
        BrowseLanguageOption("pl", "Polish", "Polski", "PL")
    )

    val originalLanguages: List<FilterOption> =
        listOf(FilterOption("", "Any Language")) +
            browseLanguages.map { FilterOption(it.code, it.name) }

    val originCountries: List<FilterOption> = listOf(
        FilterOption("", "Any Country"),
        FilterOption("US", "United States"),
        FilterOption("GB", "United Kingdom"),
        FilterOption("KR", "South Korea"),
        FilterOption("JP", "Japan"),
        FilterOption("IN", "India"),
        FilterOption("FR", "France"),
        FilterOption("ES", "Spain"),
        FilterOption("DE", "Germany"),
        FilterOption("CA", "Canada")
    )

    fun browseLanguageByCode(code: String): BrowseLanguageOption? {
        return browseLanguages.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }

    fun movieGenreIdForName(name: String): Int? {
        if (name.equals("All", ignoreCase = true) || name.isBlank()) return null
        return genres.firstOrNull { it.name.equals(name, ignoreCase = true) }?.movieGenreId
    }

    fun tvGenreIdForName(name: String): Int? {
        if (name.equals("All", ignoreCase = true) || name.isBlank()) return null
        return genres.firstOrNull { it.name.equals(name, ignoreCase = true) }?.tvGenreId
    }
}

data class DiscoverFilters(
    val mediaType: String = "All", // "All", "movie", "tv"
    val selectedGenres: Set<String> = emptySet(),
    val releaseYear: Int? = null,
    val minRating: Double = 0.0,
    val originalLanguage: String = "",
    val originCountry: String = "",
    val sortBy: String = "popular" // "popular", "newest", "rating"
) {
    fun hasAdvancedFilters(): Boolean {
        return selectedGenres.isNotEmpty() ||
            releaseYear != null ||
            minRating > 0.0 ||
            originalLanguage.isNotBlank() ||
            originCountry.isNotBlank() ||
            sortBy != "popular" ||
            mediaType != "All"
    }

    fun hasDiscoverApiCriteria(): Boolean {
        return selectedGenres.isNotEmpty() ||
            releaseYear != null ||
            minRating > 0.0 ||
            originalLanguage.isNotBlank() ||
            originCountry.isNotBlank() ||
            sortBy != "popular"
    }

    fun activeFilterCount(): Int {
        var count = 0
        if (mediaType != "All") count++
        if (selectedGenres.isNotEmpty()) count += selectedGenres.size
        if (releaseYear != null) count++
        if (minRating > 0.0) count++
        if (originalLanguage.isNotBlank()) count++
        if (originCountry.isNotBlank()) count++
        if (sortBy != "popular") count++
        return count
    }

    fun movieGenreIdsQuery(): String? {
        val ids = selectedGenres.mapNotNull { TmdbFilterCatalog.movieGenreIdForName(it) }.distinct()
        return ids.takeIf { it.isNotEmpty() }?.joinToString(",")
    }

    fun tvGenreIdsQuery(): String? {
        val ids = selectedGenres.mapNotNull { TmdbFilterCatalog.tvGenreIdForName(it) }.distinct()
        return ids.takeIf { it.isNotEmpty() }?.joinToString(",")
    }

    fun toTmdbMovieSortParam(): String = when (sortBy) {
        "rating" -> "vote_average.desc"
        "newest" -> "primary_release_date.desc"
        else -> "popularity.desc"
    }

    fun toTmdbTvSortParam(): String = when (sortBy) {
        "rating" -> "vote_average.desc"
        "newest" -> "first_air_date.desc"
        else -> "popularity.desc"
    }

    fun matchesItem(item: MediaItem): Boolean {
        if (mediaType != "All" && !item.mediaType.equals(mediaType, ignoreCase = true)) {
            return false
        }
        if (selectedGenres.isNotEmpty()) {
            val itemGenresLower = item.genres.map { it.lowercase(Locale.US) }
            val matchesGenre = selectedGenres.any { g ->
                itemGenresLower.any { it.contains(g.lowercase(Locale.US)) }
            }
            if (!matchesGenre) return false
        }
        if (releaseYear != null) {
            val itemYear = item.releaseDate.take(4).toIntOrNull()
            if (itemYear != releaseYear) return false
        }
        if (minRating > 0.0 && item.voteAverage < minRating) {
            return false
        }
        if (originalLanguage.isNotBlank() && item.originalLanguage.isNotBlank() &&
            !item.originalLanguage.equals(originalLanguage, ignoreCase = true)
        ) {
            return false
        }
        if (originCountry.isNotBlank() && item.originCountry.isNotEmpty() &&
            item.originCountry.none { it.equals(originCountry, ignoreCase = true) }
        ) {
            return false
        }
        return true
    }

    fun sortItems(items: List<MediaItem>): List<MediaItem> = when (sortBy) {
        "rating" -> items.sortedByDescending { it.voteAverage }
        "newest" -> items.sortedByDescending { it.releaseDate }
        else -> items
    }
}

enum class AppLanguageOption(
    val tmdbCode: String,
    val displayName: String,
    val nativeLabel: String
) {
    ENGLISH("en-US", "English", "English (US)"),
    SPANISH("es-ES", "Spanish", "Español"),
    FRENCH("fr-FR", "French", "Français"),
    GERMAN("de-DE", "German", "Deutsch"),
    HINDI("hi-IN", "Hindi", "हिन्दी"),
    JAPANESE("ja-JP", "Japanese", "日本語"),
    KOREAN("ko-KR", "Korean", "한국어");

    companion object {
        fun fromCode(code: String?): AppLanguageOption {
            return entries.firstOrNull { it.tmdbCode.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

enum class IssueReportCategory(
    val storageKey: String,
    val displayName: String
) {
    VIDEO_NOT_WORKING("video_not_working", "Video Not Working"),
    BROKEN_STREAM_LINK("broken_streaming_link", "Broken Streaming Link"),
    DOWNLOAD_LINK_BROKEN("download_link_not_working", "Download Link Not Working"),
    WRONG_MOVIE_INFO("wrong_movie_information", "Wrong Movie Information"),
    WRONG_SUBTITLE("wrong_subtitle", "Wrong Subtitle"),
    AUDIO_PROBLEM("audio_problem", "Audio Problem"),
    APP_BUG("app_bug", "App Bug"),
    OTHER("other_issues", "Other Issues");

    companion object {
        fun fromKey(key: String?): IssueReportCategory {
            return entries.firstOrNull {
                it.storageKey.equals(key, ignoreCase = true) || it.displayName.equals(key, ignoreCase = true)
            } ?: OTHER
        }
    }
}

enum class IssueReportStatus(
    val storageKey: String,
    val displayName: String
) {
    OPEN("OPEN", "Open"),
    IN_REVIEW("IN_REVIEW", "In Review"),
    RESOLVED("RESOLVED", "Resolved"),
    DISMISSED("DISMISSED", "Dismissed");

    companion object {
        fun fromKey(key: String?): IssueReportStatus {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: OPEN
        }
    }
}

object RecommendationPolicy {
    fun resolveTopPreferredGenre(
        continueWatching: List<ContinueWatchingEntity>,
        watchlist: List<WatchlistEntity>
    ): String {
        val genreCounts = linkedMapOf<String, Int>()
        continueWatching.forEach { item ->
            item.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { genre ->
                genreCounts[genre] = (genreCounts[genre] ?: 0) + 2
            }
        }
        watchlist.forEach { item ->
            item.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { genre ->
                genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
            }
        }
        return genreCounts.maxByOrNull { it.value }?.key ?: "Action"
    }

    fun resolveBecauseYouWatchedAnchor(
        continueWatching: List<ContinueWatchingEntity>,
        watchlist: List<WatchlistEntity>
    ): MediaItem? {
        continueWatching.firstOrNull { it.resolvedTmdbIdOrNull() != null }?.let {
            return it.toMediaItem()
        }
        return watchlist.firstOrNull { it.tmdbId != null || it.id.toLongOrNull() != null }?.toMediaItem()
    }

    private fun ContinueWatchingEntity.resolvedTmdbIdOrNull(): Long? {
        return tmdbId ?: vidlyId.toLongOrNull() ?: id.toLongOrNull()
    }

    fun rankRecommendedForYou(
        candidatePool: List<MediaItem>,
        continueWatching: List<ContinueWatchingEntity>,
        watchlist: List<WatchlistEntity>,
        limit: Int = 20
    ): List<MediaItem> {
        if (candidatePool.isEmpty()) return emptyList()
        val watchedIds = continueWatching.map { it.id }.toSet()
        val favoriteGenres = mutableMapOf<String, Int>()
        continueWatching.forEach { cw ->
            cw.genres.split(",").map { it.trim().lowercase(Locale.US) }.filter { it.isNotBlank() }
                .forEach { g -> favoriteGenres[g] = (favoriteGenres[g] ?: 0) + 3 }
        }
        watchlist.forEach { wl ->
            wl.genres.split(",").map { it.trim().lowercase(Locale.US) }.filter { it.isNotBlank() }
                .forEach { g -> favoriteGenres[g] = (favoriteGenres[g] ?: 0) + 2 }
        }

        val unseen = candidatePool.distinctBy { it.id }.filter { it.id !in watchedIds }
        val poolToRank = if (unseen.isNotEmpty()) unseen else candidatePool.distinctBy { it.id }

        return if (favoriteGenres.isEmpty()) {
            poolToRank.sortedByDescending { it.voteAverage }.take(limit)
        } else {
            poolToRank.sortedByDescending { item ->
                val genreScore = item.genres.sumOf { g -> favoriteGenres[g.lowercase(Locale.US)] ?: 0 }
                (genreScore * 10.0) + item.voteAverage
            }.take(limit)
        }
    }
}

object UpcomingReleasePolicy {
    const val MIN_UPCOMING_POPULARITY = 8.0
    private val ISO_DATE_PREFIX = Regex("""^\d{4}-\d{2}-\d{2}$""")

    fun todayIsoDate(nowMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date(nowMillis))
    }

    fun tomorrowIsoDate(nowMillis: Long = System.currentTimeMillis()): String {
        return offsetDaysIsoDate(days = 1, nowMillis = nowMillis)
    }

    fun daysAgoIsoDate(days: Int, nowMillis: Long = System.currentTimeMillis()): String {
        return offsetDaysIsoDate(days = -days, nowMillis = nowMillis)
    }

    fun offsetDaysIsoDate(days: Int, nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        cal.add(Calendar.DAY_OF_YEAR, days)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(cal.time)
    }

    fun extractIsoDatePrefix(releaseDate: String): String? {
        val candidate = releaseDate.trim().take(10)
        return if (candidate.length == 10 && ISO_DATE_PREFIX.matches(candidate)) {
            candidate
        } else {
            null
        }
    }

    fun isStrictlyFutureRelease(
        releaseDate: String,
        todayIso: String = todayIsoDate()
    ): Boolean {
        val datePrefix = extractIsoDatePrefix(releaseDate) ?: return false
        return datePrefix > todayIso
    }

    fun isReleasedOnOrBeforeToday(
        releaseDate: String,
        todayIso: String = todayIsoDate()
    ): Boolean {
        val datePrefix = extractIsoDatePrefix(releaseDate) ?: return false
        return datePrefix <= todayIso
    }

    fun filterStrictUpcoming(
        items: List<MediaItem>,
        todayIso: String = todayIsoDate(),
        minPopularity: Double = MIN_UPCOMING_POPULARITY
    ): List<MediaItem> {
        return items
            .asSequence()
            .filter { item ->
                isStrictlyFutureRelease(item.releaseDate, todayIso) &&
                    item.popularity >= minPopularity
            }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<MediaItem> { it.popularity }
                    .thenBy { extractIsoDatePrefix(it.releaseDate) ?: "9999-12-31" }
            )
            .toList()
    }
}

object NewReleasePolicy {
    const val MIN_DAILY_POPULARITY = 1.5
    const val DEFAULT_LOOKBACK_DAYS = 14

    fun filterReleasedTitles(
        items: List<MediaItem>,
        todayIso: String = UpcomingReleasePolicy.todayIsoDate(),
        minPopularity: Double = MIN_DAILY_POPULARITY
    ): List<MediaItem> {
        return items
            .asSequence()
            .filter { item ->
                if (!UpcomingReleasePolicy.isReleasedOnOrBeforeToday(item.releaseDate, todayIso)) {
                    return@filter false
                }
                item.isCustom || item.popularity >= minPopularity
            }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<MediaItem> {
                    UpcomingReleasePolicy.extractIsoDatePrefix(it.releaseDate) ?: ""
                }.thenByDescending { it.popularity }
                    .thenByDescending { it.voteAverage }
            )
            .toList()
    }

    fun mergeDailyNewReleases(
        dailyTmdbItems: List<MediaItem>,
        customMediaEntities: List<CustomMediaEntity>,
        todayIso: String = UpcomingReleasePolicy.todayIsoDate(),
        nowMillis: Long = System.currentTimeMillis()
    ): List<MediaItem> {
        val lookbackWindowStart = UpcomingReleasePolicy.daysAgoIsoDate(DEFAULT_LOOKBACK_DAYS, nowMillis)
        val recentAdminUploads = customMediaEntities
            .filter { entity ->
                val uploadDate = UpcomingReleasePolicy.todayIsoDate(entity.createdAt)
                val releasedOk = UpcomingReleasePolicy.extractIsoDatePrefix(entity.releaseDate)
                    ?.let { it <= todayIso } ?: true
                releasedOk && (uploadDate == todayIso || uploadDate >= lookbackWindowStart)
            }
            .sortedByDescending { it.createdAt }
            .map { entity ->
                val uploadIso = UpcomingReleasePolicy.todayIsoDate(entity.createdAt)
                entity.toMediaItem().let { item ->
                    if (UpcomingReleasePolicy.extractIsoDatePrefix(item.releaseDate) == null) {
                        item.copy(releaseDate = uploadIso)
                    } else {
                        item
                    }
                }
            }

        val createdToday = recentAdminUploads.filter { entity ->
            customMediaEntities.firstOrNull { it.id == entity.id }?.let { raw ->
                UpcomingReleasePolicy.todayIsoDate(raw.createdAt) == todayIso
            } ?: false
        }
        val otherRecentCustom = recentAdminUploads.filterNot { item ->
            createdToday.any { it.id == item.id }
        }

        val cleanedTmdb = filterReleasedTitles(dailyTmdbItems, todayIso = todayIso)

        val tmdbReleasedToday = cleanedTmdb.filter {
            UpcomingReleasePolicy.extractIsoDatePrefix(it.releaseDate) == todayIso
        }
        val tmdbEarlierRecent = cleanedTmdb.filterNot {
            UpcomingReleasePolicy.extractIsoDatePrefix(it.releaseDate) == todayIso
        }

        return (createdToday + tmdbReleasedToday + otherRecentCustom + tmdbEarlierRecent)
            .distinctBy { it.id }
    }
}

object SearchSuggestionPolicy {
    const val MIN_QUERY_LENGTH = 1
    const val MAX_SUGGESTIONS = 8

    fun rankSuggestions(
        tmdbItems: List<MediaItem>,
        customMedia: List<MediaItem>,
        query: String,
        limit: Int = MAX_SUGGESTIONS
    ): List<MediaItem> {
        val cleanQuery = query.trim().lowercase(Locale.US)
        if (cleanQuery.length < MIN_QUERY_LENGTH) return emptyList()

        val customMatches = customMedia.filter { item ->
            item.title.lowercase(Locale.US).contains(cleanQuery)
        }
        val combined = (customMatches + tmdbItems)
            .asSequence()
            .filter { it.title.isNotBlank() }
            .distinctBy { it.id }
            .toList()

        return combined
            .sortedWith(
                compareByDescending<MediaItem> { it.title.lowercase(Locale.US) == cleanQuery }
                    .thenByDescending { it.title.lowercase(Locale.US).startsWith(cleanQuery) }
                    .thenByDescending { it.title.lowercase(Locale.US).contains(cleanQuery) }
                    .thenByDescending { it.posterPath.isNotBlank() }
                    .thenByDescending { it.popularity }
                    .thenByDescending { it.voteAverage }
            )
            .take(limit)
    }

    fun filterByMediaType(
        suggestions: List<MediaItem>,
        mediaType: String
    ): List<MediaItem> {
        if (mediaType.equals("All", ignoreCase = true) || mediaType.isBlank()) {
            return suggestions
        }
        return suggestions.filter { it.mediaType.equals(mediaType, ignoreCase = true) }
    }
}


