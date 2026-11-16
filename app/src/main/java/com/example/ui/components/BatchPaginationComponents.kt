package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogBatchPolicy
import com.example.ui.theme.CineTheme

@Composable
fun CatalogCountChip(
    totalAvailable: Int,
    isFiltered: Boolean = false,
    isLoading: Boolean = false,
    customLabel: String? = null,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val labelText = customLabel ?: CatalogBatchPolicy.formatTotalChipText(
        totalAvailable = totalAvailable,
        isFiltered = isFiltered,
        isLoading = isLoading
    )
    val borderColor = if (isFiltered) {
        colors.softRose.copy(alpha = 0.55f)
    } else {
        colors.gold.copy(alpha = 0.45f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isFiltered) gradients.softBadge else gradients.topBarBackdrop
            )
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isFiltered) Icons.Default.FilterList else Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = if (isFiltered) Color.White else colors.gold,
                modifier = Modifier.size(13.dp)
            )
            AnimatedContent(
                targetState = labelText,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "catalog_count_chip_text"
            ) { currentText ->
                Text(
                    text = currentText,
                    color = if (isFiltered) Color.White else colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BatchSizeChip(
    batchSize: Int,
    onToggleBatchSize: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val normalized = CatalogBatchPolicy.normalizeBatchSize(batchSize)
    FilterChip(
        selected = normalized == 50,
        onClick = { onToggleBatchSize(CatalogBatchPolicy.nextBatchSize(normalized)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.ViewModule,
                contentDescription = "Batch size",
                tint = colors.gold,
                modifier = Modifier.size(14.dp)
            )
        },
        label = {
            Text(
                text = "Batch: $normalized",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.surfaceElevated,
            selectedLabelColor = colors.gold,
            containerColor = colors.surfaceElevated,
            labelColor = colors.textPrimary
        ),
        modifier = modifier.testTag(testTag)
    )
}

@Composable
fun LoadMoreBatchFooter(
    shownCount: Int,
    totalAvailable: Int,
    batchSize: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val safeBatch = CatalogBatchPolicy.normalizeBatchSize(batchSize)
    val summary = CatalogBatchPolicy.formatAvailabilitySummary(
        shownCount = shownCount,
        totalAvailable = totalAvailable
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceElevated.copy(alpha = 0.72f))
            .border(1.dp, colors.border.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = summary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        if (hasMore) {
            if (isLoadingMore) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.height(48.dp)
                ) {
                    CircularProgressIndicator(
                        color = colors.softRose,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Loading next $safeBatch titles...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.gold
                    )
                }
            } else {
                CineGradientButton(
                    onClick = onLoadMore,
                    shape = RoundedCornerShape(50),
                    minHeight = 48.dp,
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                    modifier = Modifier.testTag(testTag)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Load Next $safeBatch",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
