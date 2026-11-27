package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.DownloadPreferences
import com.example.data.local.NotificationPreferences
import com.example.data.model.AppLanguageOption
import com.example.data.model.PlayerServer
import com.example.data.model.PreferredDownloadQuality
import com.example.data.notifications.EpisodeNotificationHelper
import com.example.ui.components.CategoryPill
import com.example.ui.components.CineDialogEntryBox
import com.example.ui.components.CineEase
import com.example.ui.components.RedButton
import com.example.ui.components.cinePressScale
import com.example.ui.theme.AccentColorOption
import com.example.ui.theme.CinetraTheme
import com.example.ui.theme.DarkThemePalette
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.paletteColorsFor

@OptIn(ExperimentalLayoutApi::class)
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
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColorOption = AccentColorOption.VELVET_ROSE,
    appLanguage: AppLanguageOption = AppLanguageOption.ENGLISH,
    notificationPrefs: NotificationPreferences = NotificationPreferences(),
    cacheSizeLabel: String = "0 KB",
    feedbackMessage: String? = null,
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onSelectAccentColor: (AccentColorOption) -> Unit = {},
    onSelectAppLanguage: (AppLanguageOption) -> Unit = {},
    onUpdateNotifications: (enabled: Boolean, newEpisodes: Boolean, newMovies: Boolean) -> Unit = { _, _, _ -> },
    onCheckReleasesNow: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onReportProblemClick: () -> Unit = {},
    onLaunchMagnetStream: (String) -> Unit = {},
    onDismissFeedback: () -> Unit = {},
    downloadsCount: Int = 0,
    downloadPreferences: DownloadPreferences = DownloadPreferences(),
    onOpenDownloads: () -> Unit = {},
    onSaveDownloadPreferences: (movieBoxHostUrl: String, preferredQuality: PreferredDownloadQuality, wifiOnly: Boolean) -> Unit = { _, _, _ -> },
    onTestMovieBoxHost: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val colors = CinetraTheme.colors
    val bottomClearance = CinetraTheme.bottomBarClearance

    var hasNotifPermission by remember {
        mutableStateOf(EpisodeNotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            onCheckReleasesNow()
        }
    }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 28.dp,
            bottom = bottomClearance + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item(key = "settings_header", contentType = "header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(colors.surface)
                        .border(1.dp, colors.border, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = colors.primary,
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
                        color = colors.textMuted
                    )
                }
            }
        }

        // Settings Action Feedback Banner
        item(key = "settings_feedback_banner", contentType = "feedback_banner") {
            AnimatedVisibility(visible = feedbackMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(colors.success.copy(alpha = 0.14f))
                        .border(1.dp, colors.success.copy(alpha = 0.45f), MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = feedbackMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissFeedback, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Theme Mode, Accent Color & Readability Appearance Card
        item(key = "settings_appearance_card", contentType = "settings_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, MaterialTheme.shapes.medium)
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
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_appearance_heading),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val modeIcon = when (mode) {
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
                            }
                            val label = when (mode) {
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.SYSTEM -> "System"
                            }
                            CategoryPill(
                                label = label,
                                selected = themeMode == mode,
                                leadingIcon = modeIcon,
                                onClick = { onSelectThemeMode(mode) },
                                modifier = Modifier.testTag("settings_theme_mode_${mode.storageKey}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Accent Color (${accentColor.displayName})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccentColorOption.entries.forEach { option ->
                            val isSelected = accentColor == option
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .background(option.primaryColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) colors.textPrimary else colors.border,
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectAccentColor(option) }
                                    .testTag("settings_accent_${option.storageKey}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = option.displayName,
                                        tint = colors.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick User-Facing High-Readability Palette Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, MaterialTheme.shapes.small)
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
                                    tint = colors.primary,
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
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = selectedPalette.isHighReadability,
                            onCheckedChange = { onToggleHighReadability() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onPrimary,
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
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                    ReadabilityPreviewCard()
                }
            }
        }

        // Content Language Selection Card
        item(key = "settings_language_card", contentType = "settings_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Catalog & Metadata Language",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }
                    Text(
                        text = "Titles, descriptions & episode names refresh in your selected language.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguageOption.entries.forEach { lang ->
                            CategoryPill(
                                label = lang.nativeLabel,
                                selected = appLanguage == lang,
                                onClick = { onSelectAppLanguage(lang) },
                                modifier = Modifier.testTag("settings_lang_${lang.tmdbCode}")
                            )
                        }
                    }
                }
            }
        }

        // Notifications & Release Alerts Card
        item(key = "settings_notifications_card", contentType = "settings_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Episode & Movie Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }
                    Text(
                        text = "Get notified when followed Watchlist series air new episodes or new movies arrive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    NotificationToggleRow(
                        title = "Enable Release Notifications",
                        subtitle = "Master switch for all release alerts",
                        checked = notificationPrefs.enabled,
                        onToggle = { newVal ->
                            if (newVal && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            onUpdateNotifications(
                                newVal,
                                notificationPrefs.newEpisodesEnabled,
                                notificationPrefs.newMoviesEnabled
                            )
                        },
                        testTag = "settings_notif_master"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NotificationToggleRow(
                        title = "New TV Episodes",
                        subtitle = "Track new aired episodes for Watchlist shows",
                        checked = notificationPrefs.newEpisodesEnabled,
                        enabled = notificationPrefs.enabled,
                        onToggle = { newVal ->
                            onUpdateNotifications(
                                notificationPrefs.enabled,
                                newVal,
                                notificationPrefs.newMoviesEnabled
                            )
                        },
                        testTag = "settings_notif_episodes"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NotificationToggleRow(
                        title = "New Movies & Studio Picks",
                        subtitle = "Alert when fresh theatrical or Admin titles arrive",
                        checked = notificationPrefs.newMoviesEnabled,
                        enabled = notificationPrefs.enabled,
                        onToggle = { newVal ->
                            onUpdateNotifications(
                                notificationPrefs.enabled,
                                notificationPrefs.newEpisodesEnabled,
                                newVal
                            )
                        },
                        testTag = "settings_notif_movies"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                        RedButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            fillWidth = true,
                            minHeight = 48.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_request_notif_permission")
                        ) {
                            Text(
                                text = "Grant Notification Permission",
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onCheckReleasesNow()
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_check_releases_now")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Check for New Episodes Now",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Default Streaming Server Card
        item(key = "settings_server_card", contentType = "settings_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, MaterialTheme.shapes.medium)
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
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_playback_heading),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.settings_playback_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )

                    val visibleServers = remember {
                        PlayerServer.entries.filter { it != PlayerServer.WEBTORRENT }
                    }
                    visibleServers.forEachIndexed { index, server ->
                        val isSelected = server == defaultServer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) colors.surfaceElevated
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
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                        if (index < visibleServers.lastIndex) {
                            HorizontalDivider(
                                color = colors.border,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Offline Download Preferences Card
        item(key = "settings_downloads_card", contentType = "settings_card") {
            var qualitySelection by remember(downloadPreferences.preferredQuality) {
                mutableStateOf(downloadPreferences.preferredQuality)
            }
            var wifiOnlySelection by remember(downloadPreferences.wifiOnly) {
                mutableStateOf(downloadPreferences.wifiOnly)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline Downloads",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "$downloadsCount saved items • Manage download quality & network rules",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }

                    Text(
                        text = "Preferred Video Quality",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PreferredDownloadQuality.entries.forEach { q ->
                            CategoryPill(
                                label = q.displayName,
                                selected = qualitySelection == q,
                                onClick = {
                                    qualitySelection = q
                                    onSaveDownloadPreferences(
                                        downloadPreferences.movieBoxHostUrl,
                                        q,
                                        wifiOnlySelection
                                    )
                                }
                            )
                        }
                    }

                    NotificationToggleRow(
                        title = "Download Over Wi-Fi Only",
                        subtitle = "Pause large video downloads on cellular data",
                        checked = wifiOnlySelection,
                        onToggle = { checked ->
                            wifiOnlySelection = checked
                            onSaveDownloadPreferences(
                                downloadPreferences.movieBoxHostUrl,
                                qualitySelection,
                                checked
                            )
                        },
                        testTag = "settings_download_wifi_only"
                    )

                    OutlinedButton(
                        onClick = onOpenDownloads,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_open_downloads_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Downloads Manager ($downloadsCount)",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Local Data, Storage & Cache Card
        item(key = "settings_library_card", contentType = "settings_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_library_heading),
                        style = MaterialTheme.typography.titleMedium,
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
                    Spacer(modifier = Modifier.height(8.dp))
                    LibraryStatRow(
                        icon = Icons.Default.Storage,
                        label = "Poster & API Cache Size",
                        value = cacheSizeLabel
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onClearSearchHistory,
                        enabled = searchHistoryCount > 0,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.textPrimary,
                            disabledContainerColor = colors.surfaceElevated.copy(alpha = 0.5f),
                            disabledContentColor = colors.textMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_clear_search_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = if (searchHistoryCount > 0) colors.primary else colors.textMuted,
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

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onClearCache,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceElevated,
                            contentColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_clear_cache_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clear Image & API Cache ($cacheSizeLabel)",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Support, Legal & About Card
        item(key = "settings_support_card", contentType = "settings_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = "Support & Legal",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsActionRow(
                        icon = Icons.Default.BugReport,
                        iconTint = colors.primary,
                        title = "Report a Problem",
                        subtitle = "Broken stream, wrong info, subtitles, or app bug",
                        onClick = onReportProblemClick,
                        testTag = "settings_report_problem"
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 6.dp))
                    SettingsActionRow(
                        icon = Icons.Default.PrivacyTip,
                        iconTint = colors.textSecondary,
                        title = "Privacy Policy",
                        subtitle = "How watch history, watchlist & reports are stored",
                        onClick = { showPrivacyDialog = true },
                        testTag = "settings_privacy_policy"
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 6.dp))
                    SettingsActionRow(
                        icon = Icons.Default.Description,
                        iconTint = colors.textSecondary,
                        title = "Terms & Conditions",
                        subtitle = "Service terms, third-party embeds & metadata notice",
                        onClick = { showTermsDialog = true },
                        testTag = "settings_terms_conditions"
                    )
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.textSecondary,
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
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        Dialog(onDismissRequest = { showPrivacyDialog = false }) {
            CineDialogEntryBox {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colors.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, MaterialTheme.shapes.extraLarge)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Cinetra stores your Watchlist, Continue Watching progress, Search History, and Display Preferences locally on your device without requiring an account.\n\nIf you submit a Problem Report, only the selected issue category, description, media identifier, and optional screenshot image you choose are transmitted to the Cinetra Admin team for troubleshooting.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showPrivacyDialog = false }) {
                                Text("Close", color = colors.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTermsDialog) {
        Dialog(onDismissRequest = { showTermsDialog = false }) {
            CineDialogEntryBox {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colors.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, MaterialTheme.shapes.extraLarge)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Terms & Conditions",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Cinetra is an aggregator interface powered by The Movie Database (TMDb) metadata APIs. Third-party streaming providers operate independently of Cinetra.\n\nBy using this application, you agree to report broken links responsibly and adhere to all applicable local laws and content guidelines.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showTermsDialog = false }) {
                                Text("Accept", color = colors.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    testTag: String,
    enabled: Boolean = true
) {
    val colors = CinetraTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.small)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, MaterialTheme.shapes.small)
            .clickable(enabled = enabled, role = Role.Switch) { onToggle(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) colors.textPrimary else colors.textMuted
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) colors.textMuted else colors.textMuted.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surfaceHover
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = CinetraTheme.colors
    val rowInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .cinePressScale(rowInteraction)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = rowInteraction,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMuted
        )
    }
}

@Composable
private fun PaletteOptionCard(
    palette: DarkThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColors = CinetraTheme.colors
    val previewColors = paletteColorsFor(palette)
    val cardInteraction = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColors.primary else activeColors.border,
        animationSpec = tween(durationMillis = 180, easing = CineEase),
        label = "palette_card_border"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) activeColors.surfaceHover else activeColors.surfaceElevated,
        animationSpec = tween(durationMillis = 180, easing = CineEase),
        label = "palette_card_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .cinePressScale(cardInteraction)
            .clip(MaterialTheme.shapes.small)
            .background(containerColor)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = MaterialTheme.shapes.small)
            .clickable(
                interactionSource = cardInteraction,
                indication = ripple(bounded = true),
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(12.dp)
            .testTag("settings_palette_${palette.storageKey}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                        .border(1.dp, previewColors.primary.copy(alpha = 0.55f), RoundedCornerShape(50))
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
                style = MaterialTheme.typography.labelSmall,
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
    val colors = CinetraTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colors.background)
            .border(1.dp, colors.border, MaterialTheme.shapes.small)
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
                    tint = colors.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.settings_preview_label),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "AA+",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
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
    val colors = CinetraTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
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
