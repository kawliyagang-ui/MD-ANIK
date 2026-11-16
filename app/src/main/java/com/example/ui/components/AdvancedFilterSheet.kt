package com.example.ui.components

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiscoverFilters
import com.example.data.model.TmdbFilterCatalog
import com.example.ui.theme.CineTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterSheet(
    currentFilters: DiscoverFilters,
    onApplyFilters: (DiscoverFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = CineTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(currentFilters) { mutableStateOf(currentFilters) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = colors.softRose,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Advanced Filters & Sort",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        val activeCount = draft.activeFilterCount()
                        Text(
                            text = if (activeCount == 0) "No filters active" else "$activeCount active filter(s)",
                            fontSize = 11.sp,
                            color = colors.textSecondary
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

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Format Type
                FilterSectionBlock(title = "Content Format") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All" to "All Titles", "movie" to "Movies", "tv" to "TV Series").forEach { (key, label) ->
                            val isSelected = draft.mediaType == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { draft = draft.copy(mediaType = key) },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentMid,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textPrimary
                                )
                            )
                        }
                    }
                }

                // 2. Sort By
                FilterSectionBlock(title = "Sort By") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("popular" to "Popularity", "newest" to "Latest Release", "rating" to "Highest Rated").forEach { (key, label) ->
                            val isSelected = draft.sortBy == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { draft = draft.copy(sortBy = key) },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.gold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textPrimary
                                )
                            )
                        }
                    }
                }

                // 3. Genres (multi-select)
                FilterSectionBlock(title = "Genres (Multi-select)") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TmdbFilterCatalog.genres.forEach { genre ->
                            val isSelected = genre.name in draft.selectedGenres
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val updated = if (isSelected) {
                                        draft.selectedGenres - genre.name
                                    } else {
                                        draft.selectedGenres + genre.name
                                    }
                                    draft = draft.copy(selectedGenres = updated)
                                },
                                label = { Text(genre.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentMid,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textPrimary
                                )
                            )
                        }
                    }
                }

                // 4. Release Year
                FilterSectionBlock(title = "Release Year") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = draft.releaseYear == null,
                            onClick = { draft = draft.copy(releaseYear = null) },
                            label = { Text("Any Year", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentMid,
                                selectedLabelColor = Color.White,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textPrimary
                            )
                        )
                        TmdbFilterCatalog.yearOptions.forEach { year ->
                            val isSelected = draft.releaseYear == year
                            FilterChip(
                                selected = isSelected,
                                onClick = { draft = draft.copy(releaseYear = if (isSelected) null else year) },
                                label = { Text("$year", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentMid,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textPrimary
                                )
                            )
                        }
                    }
                }

                // 5. Minimum Rating
                FilterSectionBlock(title = "Minimum Rating") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TmdbFilterCatalog.ratingOptions.forEach { minRating ->
                            val isSelected = draft.minRating == minRating
                            val label = if (minRating <= 0.0) "Any ★" else String.format(Locale.US, "%.0f+ ★", minRating)
                            FilterChip(
                                selected = isSelected,
                                onClick = { draft = draft.copy(minRating = minRating) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.gold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textPrimary
                                )
                            )
                        }
                    }
                }

                // 6. Original Language
                FilterSectionBlock(title = "Original Language") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TmdbFilterCatalog.originalLanguages.forEach { lang ->
                            val isSelected = draft.originalLanguage == lang.code
                            FilterChip(
                                selected = isSelected,
                                onClick = { draft = draft.copy(originalLanguage = lang.code) },
                                label = { Text(lang.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentMid,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textPrimary
                                )
                            )
                        }
                    }
                }

                // 7. Country of Origin
                FilterSectionBlock(title = "Country of Origin") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TmdbFilterCatalog.originCountries.forEach { country ->
                            val isSelected = draft.originCountry == country.code
                            FilterChip(
                                selected = isSelected,
                                onClick = { draft = draft.copy(originCountry = country.code) },
                                label = { Text(country.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentMid,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textPrimary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { draft = DiscoverFilters() },
                    shape = RoundedCornerShape(12.dp),
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

                CineGradientButton(
                    onClick = {
                        onApplyFilters(draft)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    fillWidth = true,
                    minHeight = 48.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("filter_sheet_apply")
                ) {
                    Text(
                        text = if (draft.activeFilterCount() > 0) {
                            "Apply ${draft.activeFilterCount()} Filter(s)"
                        } else {
                            "Apply Filters"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
    val colors = CineTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .border(0.75.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.gold
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
