package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogBatchPolicy
import com.example.ui.theme.CinetraTheme

private val PillShape = RoundedCornerShape(50)

@Composable
fun CatalogCountChip(
    totalAvailable: Int,
    isFiltered: Boolean = false,
    isLoading: Boolean = false,
    customLabel: String? = null,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val colors = CinetraTheme.colors
    val labelText = customLabel ?: CatalogBatchPolicy.formatTotalChipText(
        totalAvailable = totalAvailable,
        isFiltered = isFiltered,
        isLoading = isLoading
    )

    Box(
        modifier = modifier
            .clip(PillShape)
            .background(if (isFiltered) colors.primary else colors.surfaceElevated)
            .border(
                width = 1.dp,
                color = if (isFiltered) colors.primary else colors.border,
                shape = PillShape
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isFiltered) Icons.Default.FilterList else Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = if (isFiltered) colors.onPrimary else colors.primary,
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isFiltered) colors.onPrimary else colors.textSecondary,
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
    val colors = CinetraTheme.colors
    val normalized = CatalogBatchPolicy.normalizeBatchSize(batchSize)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(PillShape)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, PillShape)
            .clickable { onToggleBatchSize(CatalogBatchPolicy.nextBatchSize(normalized)) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = Icons.Default.ViewModule,
            contentDescription = "Batch size",
            tint = colors.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Batch: $normalized",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = colors.textSecondary
        )
    }
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
    val colors = CinetraTheme.colors
    val safeBatch = CatalogBatchPolicy.normalizeBatchSize(batchSize)
    val summary = CatalogBatchPolicy.formatAvailabilitySummary(
        shownCount = shownCount,
        totalAvailable = totalAvailable
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surface)
            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
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
                        color = colors.primary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Loading next $safeBatch titles...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            } else {
                RedButton(
                    onClick = onLoadMore,
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
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
