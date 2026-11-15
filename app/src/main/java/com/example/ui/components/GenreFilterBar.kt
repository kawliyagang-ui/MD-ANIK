package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.theme.CineTheme

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
        items(genres, key = { it }) { genre ->
            val isSelected = selectedGenre.equals(genre, ignoreCase = true)
            GenreFilterPill(
                genre = genre,
                isSelected = isSelected,
                onClick = {
                    if (isSelected && !genre.equals("All", ignoreCase = true)) {
                        onGenreSelect("All")
                    } else {
                        onGenreSelect(genre)
                    }
                }
            )
        }
    }
}

@Composable
private fun GenreFilterPill(
    genre: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val backgroundColor by animateColorAsState(
        targetValue = colors.surfaceElevated,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "genre_pill_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.softRose.copy(alpha = 0.65f) else colors.border,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "genre_pill_border"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else colors.textPrimary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "genre_pill_content"
    )

    val pillShape = RoundedCornerShape(50)
    val tagSuffix = genre.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(pillShape)
            .background(
                brush = if (isSelected) {
                    gradients.primaryCta
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(backgroundColor, backgroundColor)
                    )
                }
            )
            .border(width = 1.dp, color = borderColor, shape = pillShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                selected = isSelected
                contentDescription = if (isSelected) {
                    "$genre genre filter selected"
                } else {
                    "Filter trending by $genre"
                }
            }
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag("home_genre_chip_$tagSuffix"),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = genreIconFor(genre),
                contentDescription = null,
                tint = if (isSelected) Color.White else if (genre == "All") colors.gold else colors.textSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = genre,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor
            )
        }
    }
}

private fun genreIconFor(genre: String): ImageVector {
    return when (genre.lowercase()) {
        "all" -> Icons.Default.LocalFireDepartment
        "action" -> Icons.Default.Bolt
        "comedy" -> Icons.Default.SentimentVerySatisfied
        "sci-fi" -> Icons.Default.RocketLaunch
        "drama" -> Icons.Default.TheaterComedy
        "adventure" -> Icons.Default.Explore
        "horror" -> Icons.Default.NightsStay
        "thriller" -> Icons.Default.Psychology
        "animation" -> Icons.Default.Animation
        "fantasy" -> Icons.Default.AutoAwesome
        "crime" -> Icons.Default.Gavel
        else -> Icons.Default.MovieFilter
    }
}
