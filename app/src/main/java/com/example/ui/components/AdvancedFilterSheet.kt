package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiscoverFilters
import com.example.data.model.TmdbFilterCatalog
import com.example.ui.theme.CinetraTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterSheet(
    currentFilters: DiscoverFilters,
    onApplyFilters: (DiscoverFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = CinetraTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(currentFilters) { mutableStateOf(currentFilters) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        CineSheetEntryBox(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Filters & Sort",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            val activeCount = draft.activeFilterCount()
                            Text(
                                text = if (activeCount == 0) "No filters active" else "$activeCount active filter(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("filter_sheet_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close filters",
                            tint = colors.textSecondary
                        )
                    }
                }

                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilterSectionBlock(title = "Content Format") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All" to "All Titles", "movie" to "Movies", "tv" to "TV Series").forEach { (key, label) ->
                                CategoryPill(
                                    label = label,
                                    selected = draft.mediaType == key,
                                    onClick = { draft = draft.copy(mediaType = key) }
                                )
                            }
                        }
                    }

                    FilterSectionBlock(title = "Sort By") {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("popular" to "Popularity", "newest" to "Latest Release", "rating" to "Highest Rated").forEach { (key, label) ->
                                CategoryPill(
                                    label = label,
                                    selected = draft.sortBy == key,
                                    onClick = { draft = draft.copy(sortBy = key) }
                                )
                            }
                        }
                    }

                    FilterSectionBlock(title = "Genres") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TmdbFilterCatalog.genres.forEach { genre ->
                                val isSelected = genre.name in draft.selectedGenres
                                CategoryPill(
                                    label = genre.name,
                                    selected = isSelected,
                                    onClick = {
                                        val updated = if (isSelected) {
                                            draft.selectedGenres - genre.name
                                        } else {
                                            draft.selectedGenres + genre.name
                                        }
                                        draft = draft.copy(selectedGenres = updated)
                                    }
                                )
                            }
                        }
                    }

                    FilterSectionBlock(title = "Release Year") {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryPill(
                                label = "Any Year",
                                selected = draft.releaseYear == null,
                                onClick = { draft = draft.copy(releaseYear = null) }
                            )
                            TmdbFilterCatalog.yearOptions.forEach { year ->
                                val isSelected = draft.releaseYear == year
                                CategoryPill(
                                    label = "$year",
                                    selected = isSelected,
                                    onClick = { draft = draft.copy(releaseYear = if (isSelected) null else year) }
                                )
                            }
                        }
                    }

                    FilterSectionBlock(title = "Minimum Rating") {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TmdbFilterCatalog.ratingOptions.forEach { minRating ->
                                val label = if (minRating <= 0.0) "Any" else String.format(Locale.US, "%.0f+ ★", minRating)
                                CategoryPill(
                                    label = label,
                                    selected = draft.minRating == minRating,
                                    onClick = { draft = draft.copy(minRating = minRating) }
                                )
                            }
                        }
                    }

                    FilterSectionBlock(title = "Original Language") {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TmdbFilterCatalog.originalLanguages.forEach { lang ->
                                CategoryPill(
                                    label = lang.label,
                                    selected = draft.originalLanguage == lang.code,
                                    onClick = { draft = draft.copy(originalLanguage = lang.code) }
                                )
                            }
                        }
                    }

                    FilterSectionBlock(title = "Country of Origin") {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TmdbFilterCatalog.originCountries.forEach { country ->
                                CategoryPill(
                                    label = country.label,
                                    selected = draft.originCountry == country.code,
                                    onClick = { draft = draft.copy(originCountry = country.code) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { draft = DiscoverFilters() },
                        shape = MaterialTheme.shapes.small, // 12.dp
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("filter_sheet_reset")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    RedButton(
                        onClick = {
                            onApplyFilters(draft)
                            onDismiss()
                        },
                        fillWidth = true,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_sheet_apply")
                    ) {
                        Text(
                            text = if (draft.activeFilterCount() > 0) {
                                "Apply ${draft.activeFilterCount()} Filter(s)"
                            } else {
                                "Apply Filters"
                            },
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionBlock(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = CinetraTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background.copy(alpha = 0.65f), MaterialTheme.shapes.medium)
            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
