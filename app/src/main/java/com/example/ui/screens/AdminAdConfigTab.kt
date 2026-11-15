package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdConfigEntity
import com.example.data.model.AdGatePolicy
import com.example.ui.theme.CineTheme
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminAdConfigTab(
    state: AdminUiState,
    viewModel: AdminViewModel
) {
    val colors = CineTheme.colors
    val previewEntity = remember(
        state.adIsEnabled,
        state.adUrl,
        state.adPopUnderScript,
        state.adCooldownHours,
        state.adLastCompletedAtMillis
    ) {
        AdConfigEntity(
            isEnabled = state.adIsEnabled,
            adUrl = state.adUrl,
            popUnderScript = state.adPopUnderScript,
            cooldownHours = state.adCooldownHours.toIntOrNull() ?: AdGatePolicy.DEFAULT_COOLDOWN_HOURS,
            lastCompletedAtMillis = state.adLastCompletedAtMillis
        )
    }

    val remainingMillis = remember(previewEntity) {
        AdGatePolicy.remainingCooldownMillis(previewEntity)
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.border,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("admin_ad_config_tab")
    ) {
        Text(
            text = "Adsterra Pop-under Ad Gate",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Text(
            text = "Configure the pre-app pop-up ad, 30-second ad page timer, and 6-hour repeat interval.",
            fontSize = 12.sp,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Live Status & Enable Switch Card
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdsClick,
                                contentDescription = null,
                                tint = colors.gold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Enable Pre-App Ad Gate",
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (state.adIsEnabled) {
                                    "Users must view Adsterra ad before opening Cinetra"
                                } else {
                                    "Currently disabled — users open Cinetra directly"
                                },
                                fontSize = 11.sp,
                                color = if (state.adIsEnabled) colors.success else colors.textSecondary
                            )
                        }
                    }

                    Switch(
                        checked = state.adIsEnabled,
                        onCheckedChange = { viewModel.updateAdEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.success
                        ),
                        modifier = Modifier.testTag("admin_ad_enable_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cooldown & Frequency Summary Pill Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = colors.gold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.adLastCompletedAtMillis <= 0L) {
                                "Next pop-up: Ready immediately"
                            } else {
                                "Cooldown: ${AdGatePolicy.formatRemainingCooldown(remainingMillis)}"
                            },
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "${state.adRequiredStaySeconds.ifBlank { "30" }}s / ${state.adCooldownHours.ifBlank { "6" }}h",
                        color = colors.gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (state.adStatusMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (state.isAdStatusSuccess) colors.success.copy(alpha = 0.18f)
                        else colors.primary.copy(alpha = 0.2f)
                    )
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (state.isAdStatusSuccess) colors.success else colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.adStatusMessage,
                    color = if (state.isAdStatusSuccess) colors.success else colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("admin_ad_status_message")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Adsterra Integration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "Adsterra Configuration",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Select format and paste your Adsterra Pop-under direct URL and/or <script> code.",
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ad Format:", fontSize = 12.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(10.dp))
                    listOf("Pop-under", "Direct Link").forEach { format ->
                        FilterChip(
                            selected = state.adFormat == format,
                            onClick = { viewModel.updateAdFormat(format) },
                            label = { Text(format, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .testTag("admin_ad_format_${format.lowercase().replace("-", "_")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.adUrl,
                    onValueChange = { viewModel.updateAdUrl(it) },
                    label = { Text("Adsterra Pop-under / Direct Link URL") },
                    placeholder = { Text("https://www.profitablecpmrate.com/...", fontSize = 12.sp, color = colors.textMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = colors.gold)
                    },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_ad_url_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.adPopUnderScript,
                    onValueChange = { viewModel.updateAdPopUnderScript(it) },
                    label = { Text("Adsterra Pop-under <script> Code (Optional)") },
                    placeholder = {
                        Text(
                            "<script type=\"text/javascript\" src=\"//plXXXX.effectivegatecpm.com/.../invoke.js\"></script>",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Code, contentDescription = null, tint = colors.gold)
                    },
                    minLines = 3,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_ad_script_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pop-up Copy & Timing Card
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "Pop-Up Prompt & Timing Rules",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.adHeadline,
                    onValueChange = { viewModel.updateAdHeadline(it) },
                    label = { Text("Pop-up Headline") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_ad_headline_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.adDescription,
                    onValueChange = { viewModel.updateAdDescription(it) },
                    label = { Text("Pop-up Message") },
                    minLines = 2,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_ad_description_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.adCtaText,
                    onValueChange = { viewModel.updateAdCtaText(it) },
                    label = { Text("Ad Button Text") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_ad_cta_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = state.adRequiredStaySeconds,
                        onValueChange = { viewModel.updateAdRequiredStaySeconds(it) },
                        label = { Text("Stay Time (sec)") },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = colors.gold, modifier = Modifier.size(18.dp))
                        },
                        supportingText = { Text("Default: 30s", fontSize = 10.sp, color = colors.textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_ad_stay_seconds_input")
                    )

                    OutlinedTextField(
                        value = state.adCooldownHours,
                        onValueChange = { viewModel.updateAdCooldownHours(it) },
                        label = { Text("Repeat (hours)") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = colors.gold, modifier = Modifier.size(18.dp))
                        },
                        supportingText = { Text("Default: 6h", fontSize = 10.sp, color = colors.textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_ad_cooldown_hours_input")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.resetAdCooldown() },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .height(48.dp)
                    .testTag("admin_ad_reset_cooldown_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset 6h Timer", color = colors.gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { viewModel.saveAdConfig() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(48.dp)
                    .testTag("admin_ad_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Ad Gate", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
