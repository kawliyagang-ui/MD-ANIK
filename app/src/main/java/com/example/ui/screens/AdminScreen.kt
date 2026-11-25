package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.CloudSyncStatus
import com.example.data.model.CustomMediaEntity
import com.example.data.model.EpisodeDirectLinksCodec
import com.example.data.model.MediaItem
import com.example.data.model.toMediaItem
import com.example.ui.components.CatalogCountChip
import com.example.ui.components.CineDialogEntryBox
import com.example.ui.components.DetailedMovieView
import com.example.ui.components.TmdbSearchRowSkeleton
import com.example.ui.components.rememberCineImageRequest
import com.example.ui.components.rememberDarkImagePainter
import com.example.ui.theme.CineTheme
import com.example.ui.viewmodel.AdminTab
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    adminState: AdminUiState,
    customMediaList: List<CustomMediaEntity>,
    announcementsList: List<AdminAnnouncementEntity>,
    totalCatalogCount: Int,
    watchlistCount: Int = 0,
    continueWatchingCount: Int = 0,
    onPreviewMedia: (MediaItem) -> Unit,
    onExitAdmin: () -> Unit,
    onOpenMainApp: () -> Unit = onExitAdmin,
    cloudSyncStatus: CloudSyncStatus = CloudSyncStatus(),
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val issueReports by viewModel.issueReports.collectAsState()
    val openReportCount = remember(issueReports) { issueReports.count { it.status == "OPEN" } }

    BackHandler {
        onExitAdmin()
    }

    if (!adminState.isAuthenticated) {
        if (!adminState.isSecurityLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
            return
        }
        AdminLoginGate(
            authError = adminState.authError,
            isUsingDefaultPasscode = adminState.isUsingDefaultPasscode,
            onLogin = { pin -> viewModel.login(pin) },
            onCancel = onExitAdmin
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Admin Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExitAdmin,
                modifier = Modifier.testTag("admin_exit_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Exit to User App",
                    tint = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_cinetra_logo),
                contentDescription = "Cinetra Logo",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ADMIN PANEL",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cinetra Studio",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }
                Text(
                    text = "Catalog & Stream Server Management",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            IconButton(
                onClick = onOpenMainApp,
                modifier = Modifier.testTag("admin_open_main_app_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = "Open User App",
                    tint = colors.gold
                )
            }

            IconButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.testTag("admin_logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = colors.textSecondary
                )
            }
        }

        // Tab Navigation
        ScrollableTabRow(
            selectedTabIndex = adminState.currentTab.ordinal,
            edgePadding = 8.dp,
            containerColor = colors.surfaceElevated,
            contentColor = colors.textPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[adminState.currentTab.ordinal]),
                    color = colors.primary
                )
            }
        ) {
            AdminTab.values().forEach { tab ->
                val isSelected = adminState.currentTab == tab
                val tabLabel = if (tab == AdminTab.REPORTS && openReportCount > 0) {
                    "${tab.title} ($openReportCount)"
                } else {
                    tab.title
                }
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.switchTab(tab) },
                    text = {
                        Text(
                            text = tabLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_${tab.name}")
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (adminState.currentTab) {
                AdminTab.ANALYTICS -> AdminAnalyticsTab(
                    totalCatalogCount = totalCatalogCount,
                    customMediaCount = customMediaList.size,
                    announcementsCount = announcementsList.count { it.active },
                    watchlistCount = watchlistCount,
                    continueWatchingCount = continueWatchingCount,
                    liveTmdbCount = adminState.liveTmdbCount,
                    isTmdbConnected = adminState.isTmdbConnected,
                    isCheckingTmdb = adminState.isCheckingTmdb,
                    cloudSyncStatus = cloudSyncStatus,
                    adGateEnabled = adminState.adIsEnabled &&
                        (adminState.adUrl.isNotBlank() || adminState.adPopUnderScript.isNotBlank()),
                    isUsingDefaultPasscode = adminState.isUsingDefaultPasscode,
                    onRefreshHealth = { viewModel.refreshSystemHealth() },
                    onNavigateToAdd = { viewModel.switchTab(AdminTab.ADD_CONTENT) },
                    onNavigateToManage = { viewModel.switchTab(AdminTab.MANAGE_CONTENT) },
                    onNavigateToAds = { viewModel.switchTab(AdminTab.AD_CONFIG) },
                    onNavigateToSecurity = { viewModel.switchTab(AdminTab.SECURITY) }
                )
                AdminTab.ADD_CONTENT -> AdminAddContentTab(
                    state = adminState,
                    viewModel = viewModel,
                    onPreviewMedia = onPreviewMedia,
                    onPreviewStream = {
                        val dummy = MediaItem(
                            id = adminState.formId.ifBlank { "test_stream" },
                            tmdbId = adminState.formTmdbId ?: adminState.formVidlyId.trim().toLongOrNull(),
                            title = adminState.formTitle.ifBlank { "Stream Preview" },
                            overview = adminState.formOverview,
                            posterPath = adminState.formPosterUrl,
                            backdropPath = adminState.formBackdropUrl,
                            mediaType = adminState.formMediaType,
                            certification = adminState.formCertification.trim(),
                            vidlyId = adminState.formVidlyId,
                            directVideoUrl = adminState.formDirectVideoUrl.trim(),
                            downloadUrl = adminState.formDownloadUrl.trim(),
                            episodeLinksJson = if (adminState.formMediaType == "tv") {
                                EpisodeDirectLinksCodec.parseMultilineInput(adminState.formEpisodeLinksText)
                            } else "",
                            numberOfSeasons = adminState.formSeasons.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            episodesPerSeason = adminState.formEpisodes.toIntOrNull()?.coerceAtLeast(1) ?: 10
                        )
                        onPreviewMedia(dummy)
                    }
                )
                AdminTab.MANAGE_CONTENT -> AdminManageContentTab(
                    items = customMediaList,
                    onEdit = { viewModel.startEdit(it) },
                    onDelete = { viewModel.deleteContent(it) },
                    onPreview = { onPreviewMedia(it.toMediaItem()) },
                    onAddNew = {
                        viewModel.clearForm()
                        viewModel.switchTab(AdminTab.ADD_CONTENT)
                    }
                )
                AdminTab.ANNOUNCEMENTS -> AdminAnnouncementsTab(
                    state = adminState,
                    announcements = announcementsList,
                    viewModel = viewModel
                )
                AdminTab.REPORTS -> AdminReportsTab(
                    state = adminState,
                    reports = issueReports,
                    viewModel = viewModel
                )
                AdminTab.AD_CONFIG -> AdminAdConfigTab(
                    state = adminState,
                    viewModel = viewModel
                )
                AdminTab.SECURITY -> AdminSecurityTab(
                    state = adminState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun adminTextFieldColors() = CineTheme.colors.let { colors ->
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.border,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.textSecondary,
        cursorColor = colors.primary
    )
}

@Composable
private fun AdminLoginGate(
    authError: String?,
    isUsingDefaultPasscode: Boolean,
    onLogin: (String) -> Unit,
    onCancel: () -> Unit
) {
    val colors = CineTheme.colors
    var pinText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(colors.primary.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Admin Authentication",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                Text(
                    text = "Enter the administrator passcode to manage content and streaming settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = { pinText = it },
                    label = { Text("Admin Password") },
                    placeholder = {
                        Text(
                            text = if (isUsingDefaultPasscode) "admin123" else "Enter password",
                            color = colors.textMuted
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onLogin(pinText) }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = colors.textSecondary
                            )
                        }
                    },
                    colors = adminTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )

                if (authError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = authError,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isUsingDefaultPasscode) {
                            "Default passcode is: admin123"
                        } else {
                            "Protected with your custom admin password"
                        },
                        color = colors.gold,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("admin_login_cancel_button")
                    ) {
                        Text("Back to App", color = colors.textPrimary)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { onLogin(pinText) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("admin_login_submit_button")
                    ) {
                        Text("Unlock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAnalyticsTab(
    totalCatalogCount: Int,
    customMediaCount: Int,
    announcementsCount: Int,
    watchlistCount: Int,
    continueWatchingCount: Int,
    liveTmdbCount: Int,
    isTmdbConnected: Boolean?,
    isCheckingTmdb: Boolean,
    cloudSyncStatus: CloudSyncStatus,
    adGateEnabled: Boolean,
    isUsingDefaultPasscode: Boolean,
    onRefreshHealth: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToManage: () -> Unit,
    onNavigateToAds: () -> Unit,
    onNavigateToSecurity: () -> Unit
) {
    val colors = CineTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Platform Analytics",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Text(
            text = "Live catalog metrics and verified cloud/API connectivity.",
            fontSize = 12.sp,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Real Metric Cards Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalyticsStatCard(
                title = "Total Catalog",
                value = "$totalCatalogCount",
                icon = Icons.Default.Movie,
                accentColor = colors.primary,
                modifier = Modifier.weight(1f)
            )
            AnalyticsStatCard(
                title = "Admin Added",
                value = "$customMediaCount",
                icon = Icons.Default.Add,
                accentColor = colors.gold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalyticsStatCard(
                title = "Active Notices",
                value = "$announcementsCount",
                icon = Icons.Default.Campaign,
                accentColor = colors.success,
                modifier = Modifier.weight(1f)
            )
            AnalyticsStatCard(
                title = "Saved Library",
                value = "$watchlistCount",
                icon = Icons.Default.Bookmark,
                accentColor = colors.info,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Real System Status Box
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System & Service Status",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    IconButton(
                        onClick = onRefreshHealth,
                        enabled = !isCheckingTmdb,
                        modifier = Modifier.testTag("admin_refresh_health_button")
                    ) {
                        if (isCheckingTmdb) {
                            CircularProgressIndicator(
                                color = colors.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Cloud & TMDb Status",
                                tint = colors.gold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val cloudColor = when {
                    !cloudSyncStatus.isAvailable || cloudSyncStatus.lastError != null -> Color(0xFFEF4444)
                    cloudSyncStatus.stateLabel.startsWith("CONNECTED") -> colors.success
                    else -> colors.warning
                }
                ServiceStatusRow(
                    service = "Cloud Sync (Firestore)",
                    status = cloudSyncStatus.stateLabel,
                    statusColor = cloudColor
                )
                if (!cloudSyncStatus.lastError.isNullOrBlank()) {
                    Text(
                        text = cloudSyncStatus.lastError,
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val (tmdbStatusText, tmdbStatusColor) = when {
                    isCheckingTmdb -> "CHECKING..." to colors.warning
                    isTmdbConnected == true -> "ONLINE ($liveTmdbCount live)" to colors.success
                    isTmdbConnected == false -> "OFFLINE / UNREACHABLE" to Color(0xFFEF4444)
                    else -> "PENDING CHECK" to colors.textSecondary
                }
                ServiceStatusRow(
                    service = "TMDb API v3 Gateway",
                    status = tmdbStatusText,
                    statusColor = tmdbStatusColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                ServiceStatusRow(
                    service = "Room Local Cache",
                    status = "ACTIVE ($customMediaCount custom • $continueWatchingCount watching)",
                    statusColor = colors.success
                )

                Spacer(modifier = Modifier.height(10.dp))

                ServiceStatusRow(
                    service = "Adsterra Pop-under Gate",
                    status = if (adGateEnabled) "ENABLED" else "DISABLED",
                    statusColor = if (adGateEnabled) colors.success else colors.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                ServiceStatusRow(
                    service = "Stream Providers (Vidly / WebTorrent)",
                    status = "5 CONFIGURED",
                    statusColor = colors.gold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onNavigateToAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quick_add_content_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Content", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateToManage,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quick_manage_button")
            ) {
                Text("Manage Items", color = colors.textPrimary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onNavigateToAds,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("quick_ad_gate_button")
        ) {
            Text(
                text = "Configure Adsterra Pop-under Ad Gate",
                color = colors.gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onNavigateToSecurity,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("quick_change_password_button")
        ) {
            Text(
                text = if (isUsingDefaultPasscode) {
                    "Change Admin Password (Default Active)"
                } else {
                    "Change Admin Password"
                },
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, colors.border, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = colors.textSecondary)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun ServiceStatusRow(service: String, status: String, statusColor: Color) {
    val colors = CineTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = service,
                fontSize = 12.sp,
                color = colors.textPrimary.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

@Composable
private fun AdminAddContentTab(
    state: AdminUiState,
    viewModel: AdminViewModel,
    onPreviewMedia: (MediaItem) -> Unit,
    onPreviewStream: () -> Unit
) {
    val colors = CineTheme.colors
    val searchHistory by viewModel.tmdbSearchHistory.collectAsState()

    // Detailed Movie View modal when a movie is selected from TMDb search results
    state.selectedTmdbPreview?.let { selectedMovie ->
        Dialog(
            onDismissRequest = { viewModel.selectTmdbPreview(null) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CineDialogEntryBox {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.background),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .testTag("tmdb_selected_detail_dialog")
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 18.dp)
                ) {
                    DetailedMovieView(
                        media = selectedMovie,
                        backdropHeight = 200.dp,
                        showBackButton = true,
                        useCloseIcon = true,
                        onDismissOrBack = { viewModel.selectTmdbPreview(null) },
                        actionsContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.autoFillFromTmdb(selectedMovie) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(48.dp)
                                        .testTag("tmdb_detail_autofill_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auto-Fill Form", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.selectTmdbPreview(null)
                                        onPreviewMedia(selectedMovie)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("tmdb_detail_preview_stream_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = colors.gold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Stream", color = colors.gold, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    )
                }
            }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Section 1: TMDb Search & Auto-Fill
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = colors.gold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TMDb Auto-Fill Search",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }
                Text(
                    text = "Type any movie or show title to fetch metadata, posters, and auto-populate the form.",
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.tmdbSearchQuery,
                        onValueChange = { viewModel.onTmdbSearchQueryChange(it) },
                        placeholder = {
                            Text(
                                text = "Search TMDb (e.g., Inception, Arcane)...",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        },
                        singleLine = true,
                        trailingIcon = {
                            if (state.tmdbSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onTmdbSearchQueryChange("") },
                                    modifier = Modifier.testTag("admin_clear_tmdb_input")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.searchTmdb() }),
                        colors = adminTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_tmdb_search_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.searchTmdb() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("admin_tmdb_search_button")
                    ) {
                        if (state.isTmdbSearching) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Search", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // TMDb Search History (Last 5 searched movie titles saved in Room DB)
                if (searchHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Search History",
                                tint = colors.gold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Recent Searches (${searchHistory.size}/5):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.gold
                            )
                        }
                        Text(
                            text = "Clear All",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { viewModel.clearSearchHistory() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("clear_tmdb_search_history")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = searchHistory,
                            key = { it.query },
                            contentType = { "tmdb_history_chip" }
                        ) { item ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.searchTmdb(item.query)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("recent_tmdb_chip_${item.query}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = item.query,
                                        fontSize = 12.sp,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable {
                                                viewModel.deleteSearchHistoryItem(item.query)
                                            }
                                            .testTag("delete_recent_tmdb_${item.query}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove ${item.query}",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TMDb loading skeleton, error, or results list
                if (state.isTmdbSearching) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Searching TMDb catalog...",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TmdbSearchRowSkeleton()
                } else if (state.tmdbSearchError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.14f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .testTag("admin_tmdb_error_banner")
                    ) {
                        Text(
                            text = state.tmdbSearchError,
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (state.tmdbSearchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap a title to view details & auto-fill:",
                        fontSize = 11.sp,
                        color = colors.gold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = state.tmdbSearchResults,
                            key = { it.id },
                            contentType = { "tmdb_result_card" }
                        ) { item ->
                            val darkPlaceholder = rememberDarkImagePainter(colors.surfaceElevated)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable { viewModel.selectTmdbPreview(item) }
                                    .testTag("tmdb_result_${item.id}")
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(90.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.surfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item.fullPosterUrl.isNotBlank()) {
                                            val thumbRequest = rememberCineImageRequest(
                                                url = item.fullPosterUrl,
                                                widthDp = 128.dp,
                                                heightDp = 90.dp
                                            )
                                            AsyncImage(
                                                model = thumbRequest,
                                                placeholder = darkPlaceholder,
                                                error = darkPlaceholder,
                                                contentDescription = item.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Movie,
                                                contentDescription = item.title,
                                                tint = colors.textMuted,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.mediaType.uppercase()} • ${item.releaseDate.take(4).ifBlank { "N/A" }}",
                                        fontSize = 9.sp,
                                        color = colors.gold
                                    )
                                }
                            }
                        }
                    }
                } else if (state.hasSearchedTmdb && !state.isTmdbSearching) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No titles found for \"${state.tmdbSearchQuery}\". Try another keyword or fill the form manually below.",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Movie / Series Form
        Text(
            text = if (state.isEditing) "Edit Catalog Item" else "Manual Addition Form",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )

        if (state.formStatusMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (state.isFormSuccess) colors.success.copy(alpha = 0.2f) else colors.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = state.formStatusMessage,
                    color = if (state.isFormSuccess) colors.success else colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title
        OutlinedTextField(
            value = state.formTitle,
            onValueChange = { viewModel.updateTitle(it) },
            label = { Text("Title *") },
            placeholder = { Text("e.g. Inception", color = colors.textMuted) },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_form_title")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category (Movie / TV)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Category:", fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(12.dp))
            FilterChip(
                selected = state.formMediaType == "movie",
                onClick = { viewModel.updateMediaType("movie") },
                label = { Text("Movie") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = Color.White,
                    labelColor = colors.textSecondary
                ),
                modifier = Modifier.testTag("admin_cat_movie")
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = state.formMediaType == "tv",
                onClick = { viewModel.updateMediaType("tv") },
                label = { Text("TV Series") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = Color.White,
                    labelColor = colors.textSecondary
                ),
                modifier = Modifier.testTag("admin_cat_tv")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stream ID, URL, or WebTorrent Magnet / .torrent
        val isWtInput = com.example.ui.components.WebTorrentPolicy.isWebTorrentSource(state.formVidlyId)
        val isCustomHttpInput = state.formVidlyId.trim().startsWith("http://", ignoreCase = true) ||
            state.formVidlyId.trim().startsWith("https://", ignoreCase = true)
        OutlinedTextField(
            value = state.formVidlyId,
            onValueChange = { viewModel.updateVidlyId(it) },
            label = { Text("Stream ID, URL, or Magnet / .torrent *") },
            placeholder = { Text("e.g. 27205, https://..., or magnet:?xt=urn:btih:...", color = colors.textMuted) },
            supportingText = {
                Text(
                    text = when {
                        isWtInput -> "✓ WebTorrent P2P stream detected (Magnet / .torrent / InfoHash)"
                        isCustomHttpInput -> "Direct custom stream URL: ${state.formVidlyId.trim()}"
                        else -> "Mapped to: https://vidly.pro/${if (state.formMediaType == "tv") "tv" else "movie"}/${state.formVidlyId.ifBlank { "{id}" }}"
                    },
                    color = if (isWtInput) colors.success else colors.gold,
                    fontSize = 11.sp
                )
            },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_form_vidly_id")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Direct Video Stream URL (optional; also used for download if downloadUrl is blank)
        OutlinedTextField(
            value = state.formDirectVideoUrl,
            onValueChange = { viewModel.updateDirectVideoUrl(it) },
            label = { Text("Direct Stream URL (.mp4 / .m3u8)") },
            placeholder = { Text("https://cdn.example.com/movie.mp4 or .../master.m3u8", color = colors.textMuted) },
            supportingText = {
                Text(
                    text = if (state.formDirectVideoUrl.isNotBlank() && state.formDownloadUrl.isBlank()) {
                        "Used for both Media3 direct streaming and offline download."
                    } else {
                        "Optional: streams natively in Media3 ExoPlayer when set."
                    },
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_form_direct_video_url")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Direct Download URL (optional; falls back to directVideoUrl when blank)
        OutlinedTextField(
            value = state.formDownloadUrl,
            onValueChange = { viewModel.updateDownloadUrl(it) },
            label = { Text("Direct Download URL (Optional)") },
            placeholder = { Text("https://cdn.example.com/movie-download.mp4", color = colors.textMuted) },
            supportingText = {
                Text(
                    text = if (state.formDownloadUrl.isNotBlank() && state.formDirectVideoUrl.isBlank()) {
                        "Used for both offline download and Media3 direct streaming."
                    } else {
                        "Leave blank to reuse the Direct Stream URL for downloading."
                    },
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_form_download_url")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Year & Rating
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.formReleaseYear,
                onValueChange = { viewModel.updateReleaseYear(it) },
                label = { Text("Release Year") },
                placeholder = { Text("e.g. 2024", color = colors.textMuted) },
                singleLine = true,
                colors = adminTextFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_form_year")
            )
            OutlinedTextField(
                value = state.formVoteAverage,
                onValueChange = { viewModel.updateVoteAverage(it) },
                label = { Text("Rating (0 - 10)") },
                placeholder = { Text("e.g. 8.4", color = colors.textMuted) },
                singleLine = true,
                colors = adminTextFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_form_rating")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Certification / Content Rating (e.g. PG-13, R, TV-MA, TV-14)
        OutlinedTextField(
            value = state.formCertification,
            onValueChange = { viewModel.updateCertification(it) },
            label = { Text("Certification / Content Rating") },
            placeholder = {
                Text(
                    text = if (state.formMediaType == "tv") "e.g. TV-14, TV-MA, TV-PG" else "e.g. PG-13, R, PG, G",
                    color = colors.textMuted
                )
            },
            supportingText = if (state.isFetchingCertification) {
                { Text("Fetching TMDb certification...", fontSize = 11.sp, color = colors.gold) }
            } else {
                null
            },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_form_certification")
        )

        Spacer(modifier = Modifier.height(6.dp))

        val certPresets = remember(state.formMediaType) {
            if (state.formMediaType == "tv") {
                listOf("TV-Y", "TV-G", "TV-PG", "TV-14", "TV-MA", "NR")
            } else {
                listOf("G", "PG", "PG-13", "R", "NC-17", "NR")
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = certPresets,
                key = { it },
                contentType = { "cert_preset_chip" }
            ) { preset ->
                val isSelected = state.formCertification.equals(preset, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.updateCertification(if (isSelected) "" else preset)
                    },
                    label = { Text(preset, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = Color.White,
                        labelColor = colors.textSecondary
                    ),
                    modifier = Modifier.testTag("admin_cert_chip_$preset")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Genres
        OutlinedTextField(
            value = state.formGenres,
            onValueChange = { viewModel.updateGenres(it) },
            label = { Text("Genres (Comma separated)") },
            placeholder = { Text("e.g. Action, Sci-Fi, Thriller", color = colors.textMuted) },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_form_genres")
        )

        if (state.formMediaType == "tv") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.formSeasons,
                    onValueChange = { viewModel.updateSeasons(it) },
                    label = { Text("Number of Seasons") },
                    singleLine = true,
                    colors = adminTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_form_seasons")
                )
                OutlinedTextField(
                    value = state.formEpisodes,
                    onValueChange = { viewModel.updateEpisodes(it) },
                    label = { Text("Episodes per Season") },
                    singleLine = true,
                    colors = adminTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_form_episodes")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.formEpisodeLinksText,
                onValueChange = { viewModel.updateEpisodeLinksText(it) },
                label = { Text("Episode Direct / Download Links (Optional)") },
                placeholder = {
                    Text(
                        "S1E1 | https://cdn.example.com/s1e1.mp4\nS1E2 | https://cdn.example.com/s1e2.mp4 | https://cdn.example.com/s1e2_dl.mp4",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                },
                supportingText = {
                    Text(
                        text = "One per line: S1E1 | streamUrl or S1E1 | streamUrl | downloadUrl. Used for season & episode downloads.",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                },
                minLines = 3,
                maxLines = 6,
                colors = adminTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_form_episode_links")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Poster URL
        OutlinedTextField(
            value = state.formPosterUrl,
            onValueChange = { viewModel.updatePosterUrl(it) },
            label = { Text("Poster Image URL") },
            placeholder = { Text("https://image.tmdb.org/t/p/w500/...", color = colors.textMuted) },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_form_poster_url")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Backdrop URL
        OutlinedTextField(
            value = state.formBackdropUrl,
            onValueChange = { viewModel.updateBackdropUrl(it) },
            label = { Text("Backdrop Image URL (Optional)") },
            placeholder = { Text("https://image.tmdb.org/t/p/w1280/...", color = colors.textMuted) },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_form_backdrop_url")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Overview
        OutlinedTextField(
            value = state.formOverview,
            onValueChange = { viewModel.updateOverview(it) },
            label = { Text("Plot Overview / Description") },
            minLines = 3,
            colors = adminTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_form_overview")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Actions
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { viewModel.clearForm() },
                modifier = Modifier
                    .weight(0.7f)
                    .height(48.dp)
                    .testTag("admin_clear_form_button")
            ) {
                Text("Reset", color = colors.textPrimary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (state.formVidlyId.isNotBlank() ||
                state.formDirectVideoUrl.isNotBlank() ||
                state.formDownloadUrl.isNotBlank()
            ) {
                OutlinedButton(
                    onClick = onPreviewStream,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("admin_test_stream_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.gold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Stream", color = colors.gold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Button(
                onClick = { viewModel.saveContent() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("admin_save_content_button")
            ) {
                Text(
                    text = if (state.isEditing) "Update Title" else "Save to Catalog",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun AdminManageContentTab(
    items: List<CustomMediaEntity>,
    onEdit: (CustomMediaEntity) -> Unit,
    onDelete: (String) -> Unit,
    onPreview: (CustomMediaEntity) -> Unit,
    onAddNew: () -> Unit
) {
    val colors = CineTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    val filtered = items.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
            it.vidlyId.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Custom Media Database",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                CatalogCountChip(
                    totalAvailable = filtered.size,
                    isFiltered = searchQuery.isNotBlank(),
                    customLabel = if (searchQuery.isNotBlank()) {
                        "${filtered.size} / ${items.size} matching"
                    } else {
                        "${items.size} in database"
                    },
                    testTag = "admin_manage_count_chip"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("admin_manage_add_new_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Title", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter custom titles...", fontSize = 12.sp, color = colors.textMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary) },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manage_search_filter")
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (items.isEmpty()) "No custom movies or series added yet.\nUse 'Add Content' tab to add items!" else "No matching items found.",
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.id },
                    contentType = { "manage_media_item" }
                ) { item ->
                    val posterUrl = remember(item.posterPath) { item.toMediaItem().fullPosterUrl }
                    val posterPlaceholder = rememberDarkImagePainter(colors.surface)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .testTag("manage_item_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 50.dp, height = 75.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (posterUrl.isNotBlank()) {
                                    val posterRequest = rememberCineImageRequest(
                                        url = posterUrl,
                                        widthDp = 50.dp,
                                        heightDp = 75.dp
                                    )
                                    AsyncImage(
                                        model = posterRequest,
                                        placeholder = posterPlaceholder,
                                        error = posterPlaceholder,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = item.title,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (item.certification.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .border(1.dp, colors.gold.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = item.certification,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.gold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.mediaType.uppercase()} • Year: ${item.releaseDate.ifBlank { "N/A" }} • Rating: ${item.voteAverage}",
                                    fontSize = 11.sp,
                                    color = colors.gold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val streamLabel = if (com.example.ui.components.WebTorrentPolicy.isWebTorrentSource(item.vidlyId)) {
                                    "Stream: WebTorrent P2P (Magnet / .torrent)"
                                } else {
                                    "Vidly ID: ${item.vidlyId}"
                                }
                                Text(
                                    text = streamLabel,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Actions
                            IconButton(
                                onClick = { onPreview(item) },
                                modifier = Modifier.testTag("preview_manage_${item.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = colors.gold)
                            }

                            IconButton(
                                onClick = { onEdit(item) },
                                modifier = Modifier.testTag("edit_manage_${item.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = colors.textPrimary)
                            }

                            IconButton(
                                onClick = { onDelete(item.id) },
                                modifier = Modifier.testTag("delete_manage_${item.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAnnouncementsTab(
    state: AdminUiState,
    announcements: List<AdminAnnouncementEntity>,
    viewModel: AdminViewModel
) {
    val colors = CineTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Announcement & Notice Banner Management",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Text(
            text = "Broadcast alerts, server updates, and notices directly to users on the app home screen.",
            fontSize = 12.sp,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Create Announcement Form
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = if (state.editingAnnouncementId != null) {
                        "Edit Announcement"
                    } else {
                        "Publish New Announcement"
                    },
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 14.sp
                )

                if (state.announcementStatusMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.announcementStatusMessage,
                        color = if (state.isAnnouncementError) Color(0xFFEF4444) else colors.success,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.announcementTitle,
                    onValueChange = { viewModel.updateAnnouncementTitle(it) },
                    label = { Text("Banner Title") },
                    placeholder = { Text("e.g. 4K Streaming Servers Online!", color = colors.textMuted) },
                    singleLine = true,
                    colors = adminTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("announcement_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.announcementMessage,
                    onValueChange = { viewModel.updateAnnouncementMessage(it) },
                    label = { Text("Notice Message") },
                    placeholder = { Text("e.g. We added backup players for seamless movie playback.", color = colors.textMuted) },
                    minLines = 2,
                    colors = adminTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("announcement_message_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Banner Type:", fontSize = 12.sp, color = colors.textPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf("INFO", "UPDATE", "WARNING").forEach { type ->
                        FilterChip(
                            selected = state.announcementType == type,
                            onClick = { viewModel.updateAnnouncementType(type) },
                            label = { Text(type, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = Color.White,
                                labelColor = colors.textSecondary
                            ),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("announcement_type_$type")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.editingAnnouncementId != null) {
                        OutlinedButton(
                            onClick = { viewModel.cancelEditAnnouncement() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(0.7f)
                                .height(48.dp)
                                .testTag("cancel_edit_announcement_button")
                        ) {
                            Text("Cancel", color = colors.textPrimary)
                        }
                    }

                    Button(
                        onClick = { viewModel.saveAnnouncement() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("publish_announcement_button")
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.editingAnnouncementId != null) "Update Notice" else "Publish to Users",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Existing Announcements List
        Text(
            text = "Active & Past Announcements (${announcements.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        announcements.forEach { ann ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .testTag("announcement_item_${ann.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when (ann.type.uppercase()) {
                                            "WARNING" -> colors.warning
                                            "UPDATE" -> colors.success
                                            else -> colors.info
                                        }
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(text = ann.type, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ann.title,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ann.message,
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Active Toggle
                    Switch(
                        checked = ann.active,
                        onCheckedChange = { viewModel.toggleAnnouncementActive(ann.id, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.success,
                            uncheckedTrackColor = colors.surface,
                            uncheckedThumbColor = colors.textSecondary
                        ),
                        modifier = Modifier.testTag("toggle_announcement_${ann.id}")
                    )

                    IconButton(
                        onClick = { viewModel.startEditAnnouncement(ann) },
                        modifier = Modifier.testTag("edit_announcement_${ann.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = colors.textPrimary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.deleteAnnouncement(ann.id) },
                        modifier = Modifier.testTag("delete_announcement_${ann.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
