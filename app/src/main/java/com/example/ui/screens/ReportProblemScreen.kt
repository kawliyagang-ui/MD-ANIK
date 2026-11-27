package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IssueReportCategory
import com.example.data.model.IssueReportEntity
import com.example.data.model.IssueReportStatus
import com.example.data.model.MediaItem
import com.example.ui.components.CategoryPill
import com.example.ui.components.RedButton
import com.example.ui.theme.CinetraTheme
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportProblemScreen(
    prefilledMedia: MediaItem?,
    serverName: String,
    episodeLabel: String,
    reportHistory: List<IssueReportEntity>,
    isSubmitting: Boolean,
    submitMessage: String?,
    isError: Boolean,
    onSubmitReport: (
        category: IssueReportCategory,
        description: String,
        media: MediaItem?,
        serverName: String,
        episodeLabel: String,
        screenshotBase64: String
    ) -> Unit,
    onClearFeedback: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CinetraTheme.colors

    var selectedCategory by remember(prefilledMedia?.id, serverName) {
        mutableStateOf(
            if (serverName.isNotBlank()) IssueReportCategory.VIDEO_NOT_WORKING
            else IssueReportCategory.APP_BUG
        )
    }
    var description by remember { mutableStateOf("") }
    var screenshotBase64 by remember { mutableStateOf("") }
    var screenshotBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encodedPair = compressImageUriToBase64(context, uri)
            if (encodedPair != null) {
                screenshotBitmap = encodedPair.first
                screenshotBase64 = encodedPair.second
            }
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("report_problem_screen")
    ) {
        // Top Header Bar
        Surface(
            color = colors.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("report_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Report a Problem",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Synced directly to Cinetra Admin team",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = colors.border)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Context card if opened from a specific movie/episode/server
            if (prefilledMedia != null) {
                item(key = "report_context_card", contentType = "context_card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Reported Title Context",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = prefilledMedia.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textPrimary
                            )
                            val contextParts = listOfNotNull(
                                prefilledMedia.mediaType.uppercase(Locale.US),
                                episodeLabel.takeIf { it.isNotBlank() },
                                serverName.takeIf { it.isNotBlank() }?.let { "Server: $it" }
                            )
                            if (contextParts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = contextParts.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Issue Category Selector & Form Card
            item(key = "report_form_card", contentType = "form_card") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Problem Category",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IssueReportCategory.entries.forEach { category ->
                                CategoryPill(
                                    label = category.displayName,
                                    selected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory = category
                                        onClearFeedback()
                                    },
                                    modifier = Modifier.testTag("report_category_${category.storageKey}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Describe What Happened",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                onClearFeedback()
                            },
                            placeholder = {
                                Text(
                                    text = "Include steps to reproduce, server name, buffering error, or broken link details...",
                                    color = colors.textMuted,
                                    fontSize = 13.sp
                                )
                            },
                            minLines = 3,
                            maxLines = 6,
                            shape = MaterialTheme.shapes.small,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.border,
                                focusedContainerColor = colors.surfaceElevated,
                                unfocusedContainerColor = colors.surfaceElevated,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                cursorColor = colors.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_description_field")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Zero-permission Photo Picker optional screenshot attachment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Screenshot (Optional)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, colors.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = colors.surfaceElevated,
                                    contentColor = colors.textPrimary
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("report_attach_screenshot_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (screenshotBitmap == null) "Attach Image" else "Replace Image",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        if (screenshotBitmap != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .border(1.dp, colors.border, MaterialTheme.shapes.small)
                            ) {
                                Image(
                                    bitmap = screenshotBitmap!!.asImageBitmap(),
                                    contentDescription = "Selected screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        screenshotBitmap = null
                                        screenshotBase64 = ""
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .minimumInteractiveComponentSize()
                                        .clip(CircleShape)
                                        .background(colors.background.copy(alpha = 0.78f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove screenshot",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = submitMessage != null) {
                            val bannerColor = if (isError) colors.primary else colors.success
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(bannerColor.copy(alpha = 0.14f))
                                    .border(1.dp, bannerColor.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                                    .padding(12.dp)
                                    .testTag("report_feedback_banner"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = bannerColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = submitMessage.orEmpty(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        RedButton(
                            onClick = {
                                onSubmitReport(
                                    selectedCategory,
                                    description,
                                    prefilledMedia,
                                    serverName,
                                    episodeLabel,
                                    screenshotBase64
                                )
                                if (description.trim().length >= 5) {
                                    description = ""
                                    screenshotBitmap = null
                                    screenshotBase64 = ""
                                }
                            },
                            enabled = !isSubmitting,
                            fillWidth = true,
                            minHeight = 48.dp,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_submit_button")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = colors.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSubmitting) "Submitting..." else "Submit Report",
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimary
                            )
                        }
                    }
                }
            }

            // Live Report History & Status Section
            item(key = "report_history_header", contentType = "section_header") {
                Text(
                    text = "Submitted Reports (${reportHistory.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (reportHistory.isEmpty()) {
                item(key = "report_history_empty", contentType = "empty_card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                    ) {
                        Text(
                            text = "No problem reports submitted yet. Any issue you submit will appear here with real-time admin review status.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(
                    items = reportHistory,
                    key = { it.id },
                    contentType = { "report_history_item" }
                ) { report ->
                    val statusEnum = IssueReportStatus.fromKey(report.status)
                    val categoryEnum = IssueReportCategory.fromKey(report.category)
                    val statusColor = when (statusEnum) {
                        IssueReportStatus.OPEN -> colors.warning
                        IssueReportStatus.IN_REVIEW -> colors.info
                        IssueReportStatus.RESOLVED -> colors.success
                        IssueReportStatus.DISMISSED -> colors.textMuted
                    }
                    val dateLabel = remember(report.createdAt) {
                        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(report.createdAt))
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                            .testTag("report_history_item_${report.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = categoryEnum.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(statusColor.copy(alpha = 0.16f))
                                        .border(1.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = statusEnum.displayName,
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (report.mediaTitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = listOfNotNull(
                                        report.mediaTitle,
                                        report.episodeLabel.takeIf { it.isNotBlank() },
                                        report.serverName.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            if (report.adminNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.border, MaterialTheme.shapes.extraSmall)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Admin Note: ${report.adminNotes}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Submitted $dateLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            item(key = "report_bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun compressImageUriToBase64(context: Context, uri: Uri): Pair<Bitmap, String>? {
    return runCatching {
        val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null

        val maxDim = 600
        val scale = if (decoded.width > maxDim || decoded.height > maxDim) {
            minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height)
        } else {
            1f
        }
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }
        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val encoded = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        resized to encoded
    }.getOrNull()
}
