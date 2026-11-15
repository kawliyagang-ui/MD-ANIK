package com.example.data.model

import java.text.NumberFormat
import java.util.Locale

data class PagedCatalogBatch(
    val items: List<MediaItem>,
    val lastFetchedPage: Int,
    val totalPages: Int,
    val totalResults: Int
)

/**
 * Pure policy & formatting helpers for 25/50 batch pagination and TMDB total-count display.
 */
object CatalogBatchPolicy {
    const val DEFAULT_BATCH_SIZE = 50
    const val MAX_TMDB_BROWSABLE_PAGES = 500
    val BATCH_SIZE_OPTIONS = listOf(25, 50)

    fun normalizeBatchSize(requested: Int): Int {
        return if (requested <= 25) 25 else 50
    }

    fun nextBatchSize(current: Int): Int {
        return if (normalizeBatchSize(current) == 50) 25 else 50
    }

    /**
     * TMDB returns up to 20 items per API page.
     * Compute how many API pages to request in parallel to satisfy [neededCount] items.
     */
    fun pagesCountForNeededItems(neededCount: Int): Int {
        val safeNeeded = neededCount.coerceAtLeast(1)
        return ((safeNeeded + 19) / 20).coerceIn(1, 5)
    }

    fun pageRangeToFetch(
        startPage: Int,
        neededCount: Int,
        totalPages: Int
    ): IntRange {
        val cappedTotalPages = totalPages.coerceIn(1, MAX_TMDB_BROWSABLE_PAGES)
        val safeStart = startPage.coerceAtLeast(1)
        if (safeStart > cappedTotalPages) return IntRange.EMPTY
        val count = pagesCountForNeededItems(neededCount)
        val endPage = minOf(safeStart + count - 1, cappedTotalPages)
        return safeStart..endPage
    }

    fun formatCount(count: Int): String {
        return NumberFormat.getIntegerInstance(Locale.US).format(count.coerceAtLeast(0))
    }

    fun formatAvailabilitySummary(
        shownCount: Int,
        totalAvailable: Int,
        singular: String = "title",
        plural: String = "titles"
    ): String {
        val safeShown = shownCount.coerceAtLeast(0)
        val safeTotal = totalAvailable.coerceAtLeast(0)
        return when {
            safeTotal > safeShown ->
                "Showing ${formatCount(safeShown)} of ${formatCount(safeTotal)} available"
            safeShown > 0 ->
                "Showing all ${formatCount(safeShown)} ${if (safeShown == 1) singular else plural}"
            else ->
                "0 $plural"
        }
    }

    fun formatCompactHeaderTotal(shownCount: Int, totalAvailable: Int): String {
        val safeShown = shownCount.coerceAtLeast(0)
        val safeTotal = totalAvailable.coerceAtLeast(safeShown)
        return if (safeTotal > safeShown) {
            "${formatCount(safeShown)} / ${formatCount(safeTotal)}"
        } else {
            formatCount(safeShown)
        }
    }

    fun resolveExploreTotalCount(
        selectedCategory: String,
        moviesTotalResults: Int,
        tvTotalResults: Int,
        trendingTotalResults: Int,
        customMedia: List<MediaItem>,
        shownCount: Int
    ): Int {
        val customMovies = customMedia.count { it.mediaType.equals("movie", ignoreCase = true) }
        val customTv = customMedia.count { it.mediaType.equals("tv", ignoreCase = true) }
        val estimatedTotal = when {
            selectedCategory.equals("movie", ignoreCase = true) ->
                moviesTotalResults + customMovies
            selectedCategory.equals("tv", ignoreCase = true) ->
                tvTotalResults + customTv
            else -> {
                val sum = moviesTotalResults + tvTotalResults + customMedia.size
                if (sum > 0) sum else trendingTotalResults + customMedia.size
            }
        }
        return maxOf(shownCount, estimatedTotal)
    }
}
