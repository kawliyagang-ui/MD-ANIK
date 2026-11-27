package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaItem

object HomeGenreCatalog {
    val defaultGenres = listOf(
        "All",
        "Action",
        "Comedy",
        "Sci-Fi",
        "Drama",
        "Adventure",
        "Horror",
        "Thriller",
        "Animation",
        "Fantasy",
        "Crime"
    )

    fun availableGenres(items: List<MediaItem>): List<String> {
        val extraGenres = items
            .flatMap { it.genres }
            .map { it.trim() }
            .filter { genre ->
                genre.isNotBlank() &&
                    !genre.equals("Cinema", ignoreCase = true) &&
                    defaultGenres.none { base ->
                        !base.equals("All", ignoreCase = true) &&
                            genre.contains(base, ignoreCase = true)
                    }
            }
            .distinct()
        return defaultGenres + extraGenres
    }

    fun filterByGenre(items: List<MediaItem>, genre: String): List<MediaItem> {
        if (genre.isBlank() || genre.equals("All", ignoreCase = true)) {
            return items
        }
        return items.filter { item ->
            item.genres.any { it.contains(genre, ignoreCase = true) }
        }
    }
}

@Composable
fun GenreFilterBar(
    genres: List<String>,
    selectedGenre: String,
    onGenreSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_genre_filter_bar"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = genres,
            key = { it },
            contentType = { "genre_pill" }
        ) { genre ->
            val isSelected = selectedGenre.equals(genre, ignoreCase = true)
            val tagSuffix = remember(genre) {
                genre.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            }
            CategoryPill(
                label = genre,
                selected = isSelected,
                onClick = {
                    if (isSelected && !genre.equals("All", ignoreCase = true)) {
                        onGenreSelect("All")
                    } else {
                        onGenreSelect(genre)
                    }
                },
                accessibilityLabel = if (isSelected) {
                    "$genre genre filter selected"
                } else {
                    "Filter trending by $genre"
                },
                modifier = Modifier.testTag("home_genre_chip_$tagSuffix")
            )
        }
    }
}
