package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IssueReportCategory
import com.example.data.model.IssueReportEntity
import com.example.data.model.IssueReportStatus
import com.example.ui.theme.CineTheme
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminReportsTab(
    state: AdminUiState,
    reports: List<IssueReportEntity>,
    viewModel: AdminViewModel
) {
    val colors = CineTheme.colors
    var previewScreenshotBase64 by remember { mutableStateOf<String?>(null) }

    val filteredReports = remember(reports, state.reportFilterStatus) {
        if (state.reportFilterStatus == "ALL") reports
        else reports.filter { it.status.equals(state.reportFilterStatus, ignoreCase = true) }
    }

    val openCount = remember(reports) { reports.count { it.status == "OPEN" } }
    val inReviewCount = remember(reports) { reports.count { it.status == "IN_REVIEW" } }
    val resolvedCount = remember(reports) { reports.count { it.status == "RESOLVED" } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_reports_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "User Problem Reports (${reports.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "$openCount Open • $inReviewCount In Review • $resolvedCount Resolved",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All (${reports.size})",
                        "OPEN" to "Open ($openCount)",
                        "IN_REVIEW" to "In Review ($inReviewCount)",
                        "RESOLVED" to "Resolved ($resolvedCount)",
                        "DISMISSED" to "Dismissed"
                    )
                    filters.forEach { (key, label) ->
                        val isSelected = state.reportFilterStatus == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setReportFilterStatus(key) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = Color.White,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textPrimary
                            ),
                            modifier = Modifier.testTag("admin_report_filter_$key")
                        )
                    }
                }

                if (state.reportActionMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val bannerColor = if (state.isReportActionError) Color(0xFFEF4444) else colors.success
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bannerColor.copy(alpha = 0.14f))
                            .border(1.dp, bannerColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = state.reportActionMessage,
                            fontSize = 12.sp,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }

        if (filteredReports.isEmpty()) {
            item {
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No problem reports in this view.",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredReports, key = { it.id }) { report ->
                AdminReportItemCard(
                    report = report,
                    onSaveUpdate = { newStatus, notes ->
                        viewModel.updateIssueReportStatus(report.id, newStatus, notes)
                    },
                    onDelete = { viewModel.deleteIssueReport(report.id) },
                    onPreviewScreenshot = {
                        previewScreenshotBase64 = report.screenshotPreviewBase64
                    }
                )
            }
        }
    }

    if (!previewScreenshotBase64.isNullOrBlank()) {
        val bitmap = remember(previewScreenshotBase64) {
            runCatching {
                val bytes = Base64.decode(previewScreenshotBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
        AlertDialog(
            onDismissRequest = { previewScreenshotBase64 = null },
            containerColor = colors.surface,
            title = { Text("Attached Screenshot", color = colors.textPrimary) },
            text = {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "User uploaded screenshot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Text("Unable to decode image attachment.", color = colors.textSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { previewScreenshotBase64 = null }) {
                    Text("Close", color = colors.primary)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminReportItemCard(
    report: IssueReportEntity,
    onSaveUpdate: (String, String) -> Unit,
    onDelete: () -> Unit,
    onPreviewScreenshot: () -> Unit
) {
    val colors = CineTheme.colors
    var draftStatus by remember(report.id, report.status) { mutableStateOf(report.status) }
    var draftNotes by remember(report.id, report.adminNotes) { mutableStateOf(report.adminNotes) }
    val categoryEnum = IssueReportCategory.fromKey(report.category)
    val dateText = remember(report.createdAt) {
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.US).format(Date(report.createdAt))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .testTag("admin_report_card_${report.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryEnum.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = dateText,
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("admin_delete_report_${report.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete report",
                        tint = Color(0xFFEF4444)
                    )
                }
            }

            if (report.mediaTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(
                        report.mediaTitle,
                        report.episodeLabel.takeIf { it.isNotBlank() },
                        report.serverName.takeIf { it.isNotBlank() }?.let { "Server: $it" }
                    ).joinToString(" • "),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.gold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = report.description,
                fontSize = 13.sp,
                color = colors.textPrimary
            )

            if (report.screenshotPreviewBase64.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primaryContainer)
                        .clickable(onClick = onPreviewScreenshot)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = colors.softRose,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Attached Screenshot",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Status:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IssueReportStatus.entries.forEach { statusObj ->
                    val selected = draftStatus == statusObj.storageKey
                    FilterChip(
                        selected = selected,
                        onClick = { draftStatus = statusObj.storageKey },
                        label = { Text(statusObj.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surface,
                            labelColor = colors.textPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = draftNotes,
                onValueChange = { draftNotes = it },
                label = { Text("Admin Resolution Note", fontSize = 11.sp) },
                placeholder = { Text("Explain fix or resolution for user...", color = colors.textMuted, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onSaveUpdate(draftStatus, draftNotes) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("admin_save_report_${report.id}")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Update & Sync Report", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
