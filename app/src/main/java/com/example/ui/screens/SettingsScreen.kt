package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PlayerServer
import com.example.ui.theme.CineTheme
import com.example.ui.theme.DarkThemePalette
import com.example.ui.theme.paletteColorsFor

@Composable
fun SettingsScreen(
    selectedPalette: DarkThemePalette,
    defaultServer: PlayerServer,
    watchlistCount: Int,
    continueWatchingCount: Int,
    searchHistoryCount: Int,
    appVersionName: String,
    onToggleHighReadability: () -> Unit,
    onSelectPalette: (DarkThemePalette) -> Unit,
    onSelectDefaultServer: (PlayerServer) -> Unit,
    onClearSearchHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(gradients.softBadge),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = stringResource(id = R.string.settings_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Appearance & Readability Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = colors.gold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_appearance_heading),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick User-Facing High-Readability Palette Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .clickable(role = Role.Switch) { onToggleHighReadability() }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("settings_readability_toggle_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contrast,
                                    contentDescription = null,
                                    tint = colors.softRose,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.settings_readability_toggle_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = stringResource(
                                        id = R.string.settings_readability_toggle_desc,
                                        selectedPalette.displayName
                                    ),
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = selectedPalette.isHighReadability,
                            onCheckedChange = { onToggleHighReadability() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.primary,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceHover
                            ),
                            modifier = Modifier.testTag("settings_readability_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(id = R.string.settings_palette_picker_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom Dark Theme Palette Options
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DarkThemePalette.entries.forEach { palette ->
                            PaletteOptionCard(
                                palette = palette,
                                isSelected = selectedPalette == palette,
                                onClick = { onSelectPalette(palette) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Readability Preview Sample
                    ReadabilityPreviewCard()
                }
            }
        }

        // Default Streaming Server Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = colors.softRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_playback_heading),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.settings_playback_subtitle),
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    PlayerServer.entries.forEachIndexed { index, server ->
                        val isSelected = server == defaultServer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) colors.primaryContainer.copy(alpha = 0.65f)
                                    else Color.Transparent
                                )
                                .clickable(role = Role.RadioButton) { onSelectDefaultServer(server) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("settings_server_${server.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectDefaultServer(server) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary,
                                    unselectedColor = colors.textMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = server.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = server.speed,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        if (index < PlayerServer.entries.lastIndex) {
                            HorizontalDivider(
                                color = colors.border.copy(alpha = 0.45f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Local Data & Privacy Card (real Room metrics only)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_library_heading),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LibraryStatRow(
                        icon = Icons.Default.Bookmark,
                        label = stringResource(id = R.string.settings_watchlist_count_label),
                        value = "$watchlistCount titles"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LibraryStatRow(
                        icon = Icons.Default.History,
                        label = stringResource(id = R.string.settings_continue_count_label),
                        value = "$continueWatchingCount items"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LibraryStatRow(
                        icon = Icons.Default.Search,
                        label = stringResource(id = R.string.settings_search_history_count_label),
                        value = "$searchHistoryCount / 5 saved"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onClearSearchHistory,
                        enabled = searchHistoryCount > 0,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_clear_search_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = if (searchHistoryCount > 0) colors.softRose else colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (searchHistoryCount > 0) {
                                stringResource(id = R.string.settings_clear_history_button)
                            } else {
                                stringResource(id = R.string.settings_history_already_empty)
                            },
                            color = if (searchHistoryCount > 0) colors.textPrimary else colors.textMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // About Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.gold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${stringResource(id = R.string.app_name)} v$appVersionName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = stringResource(id = R.string.settings_about_source),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaletteOptionCard(
    palette: DarkThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColors = CineTheme.colors
    val previewColors = paletteColorsFor(palette)
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColors.primary else activeColors.border,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "palette_card_border"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) activeColors.surfaceHover else activeColors.surfaceElevated,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "palette_card_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp)
            .testTag("settings_palette_${palette.storageKey}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Palette color swatch badge
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(previewColors.background)
                .border(1.dp, previewColors.border, RoundedCornerShape(10.dp))
                .padding(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(previewColors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aa",
                    color = previewColors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(previewColors.primary)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = palette.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = activeColors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(previewColors.primaryContainer)
                        .border(0.75.dp, previewColors.primary.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = palette.badge,
                        color = previewColors.onPrimaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = palette.subtitle,
                fontSize = 11.sp,
                color = activeColors.textSecondary,
                lineHeight = 15.sp
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = activeColors.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ReadabilityPreviewCard() {
    val colors = CineTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("settings_readability_preview")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = colors.softRose,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.settings_preview_label),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.softRose
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "AA+",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.gold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(id = R.string.settings_preview_sample_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = stringResource(id = R.string.settings_preview_sample_body),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun LibraryStatRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    val colors = CineTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.gold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
    }
}
