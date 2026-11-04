package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CloudSyncStatus
import com.example.model.DashboardStats
import com.example.model.SavingPlanCalendar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DueCrimson
import com.example.ui.theme.DueCrimsonLight
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.PaidEmerald
import com.example.ui.theme.PaidEmeraldLight
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondarySlate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HeroBannerHeader(
    isCloudConnected: Boolean,
    userDisplayName: String,
    userEmail: String,
    syncStatus: CloudSyncStatus,
    onRetrySyncClick: () -> Unit,
    onAuthActionClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onResetClick: () -> Unit,
    onOpenWebVersionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_header_card"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NavyBorder),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ic_saving_vault_hero),
                contentDescription = "Saving Growth Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = RoyalBlueDark.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = "App Logo",
                                    tint = PaidEmeraldLight,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "My Saving Tracker",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryWhite
                                ),
                                modifier = Modifier.testTag("app_title_text")
                            )
                            Text(
                                text = "মাই সেভিং ট্র্যাকার • ড্যাশবোর্ড",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = CyanAccent
                                )
                            )
                        }
                    }

                    // Logout / Sign-In Action Button
                    OutlinedButton(
                        onClick = onAuthActionClick,
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("auth_header_action_button")
                    ) {
                        Icon(
                            imageVector = if (isCloudConnected) Icons.AutoMirrored.Filled.Logout else Icons.Default.AccountCircle,
                            contentDescription = if (isCloudConnected) "Logout" else "Sign In",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCloudConnected) "লগআউট" else "ক্লাউড লগইন",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextPrimaryWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User Session & Cloud Sync Status Bar
                CloudSyncStatusBanner(
                    isCloudConnected = isCloudConnected,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    syncStatus = syncStatus,
                    onRetrySyncClick = onRetrySyncClick
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF071022).copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, NavyBorder.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📅 ১ ডিসেম্বর ২০২৬ – ৩০ জুন ২০২৭",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextPrimaryWhite
                            )
                        )
                        Text(
                            text = "মোট ১০৪ দিন • ₹10,400",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GoldHighlight,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportCsvClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalBluePrimary,
                            contentColor = TextPrimaryWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export CSV",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CSV এক্সপোর্ট",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onResetClick,
                        border = BorderStroke(1.dp, DueCrimson.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DueCrimsonLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("reset_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset All Data",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "রিসেট করুন",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenWebVersionClick,
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NavySurfaceElevated.copy(alpha = 0.75f),
                        contentColor = CyanAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("open_web_version_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Open Web Version",
                        tint = PaidEmeraldLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ওয়েব ভার্সন খুলুন ও index.html ডাউনলোড করুন (Web App)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextPrimaryWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudSyncStatusBanner(
    isCloudConnected: Boolean,
    userDisplayName: String,
    userEmail: String,
    syncStatus: CloudSyncStatus,
    onRetrySyncClick: () -> Unit
) {
    val (statusLabelBn, statusColor, statusIcon) = when {
        !isCloudConnected -> Triple(
            "লোকাল ব্যাকআপ সক্রিয় (ক্লাউড সিঙ্ক করতে লগইন করুন)",
            GoldHighlight,
            Icons.Default.CloudOff
        )
        syncStatus is CloudSyncStatus.Syncing -> Triple(
            "ক্লাউডে সিঙ্ক হচ্ছে... (Syncing...)",
            CyanAccent,
            Icons.Default.CloudSync
        )
        syncStatus is CloudSyncStatus.Synced -> {
            val formattedTime = rememberFormattedTime(syncStatus.lastUpdatedEpochMs)
            Triple(
                "ক্লাউডে সংরক্ষিত ✓ (Synced • $formattedTime)",
                PaidEmeraldLight,
                Icons.Default.CloudDone
            )
        }
        syncStatus is CloudSyncStatus.SyncFailed -> Triple(
            "${syncStatus.messageBn} (ট্যাপ করে পুনরায় চেষ্টা করুন)",
            DueCrimsonLight,
            Icons.Default.CloudOff
        )
        else -> Triple("Synced", PaidEmeraldLight, Icons.Default.CloudDone)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF071022).copy(alpha = 0.82f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = syncStatus is CloudSyncStatus.SyncFailed) {
                onRetrySyncClick()
            }
            .testTag("cloud_sync_status_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isCloudConnected && syncStatus is CloudSyncStatus.Syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CyanAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Cloud Sync Status",
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = statusLabelBn,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (isCloudConnected && (userDisplayName.isNotBlank() || userEmail.isNotBlank())) {
                        val accountLabel = listOf(userDisplayName, userEmail)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                        Text(
                            text = "অ্যাকাউন্ট: $accountLabel",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondarySlate
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun rememberFormattedTime(epochMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        sdf.format(Date(epochMs))
    } catch (e: Exception) {
        "Just now"
    }
}

@Composable
fun DashboardStatisticsSection(
    stats: DashboardStats,
    modifier: Modifier = Modifier
) {
    val animatedSaved by animateIntAsState(
        targetValue = stats.totalSaved,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "animated_saved"
    )
    val animatedRemaining by animateIntAsState(
        targetValue = stats.remainingAmount,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "animated_remaining"
    )
    val animatedDueDays by animateIntAsState(
        targetValue = stats.totalDueDays,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "animated_due_days"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = stats.progressFraction,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "animated_progress"
    )

    val formattedPercentage = String.format(Locale.US, "%.1f%%", stats.progressPercentage)
    val formattedPercentageBn = SavingPlanCalendar.toBengaliDigits(formattedPercentage)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_stats_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Total Target & Total Saved
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMetricCard(
                titleBn = "মোট লক্ষ্যমাত্রা",
                subtitleEn = "Total Target",
                primaryValue = SavingPlanCalendar.formatRupee(stats.totalTarget),
                secondaryValueBn = "${SavingPlanCalendar.formatRupeeBn(stats.totalTarget)} (১০৪ দিন)",
                icon = Icons.Default.Flag,
                accentColor = CyanAccent,
                testTag = "stat_total_target",
                modifier = Modifier.weight(1f)
            )

            StatMetricCard(
                titleBn = "মোট জমা",
                subtitleEn = "Total Saved",
                primaryValue = SavingPlanCalendar.formatRupee(animatedSaved),
                secondaryValueBn = "${SavingPlanCalendar.formatRupeeBn(stats.totalSaved)} (${SavingPlanCalendar.toBengaliNumber(stats.totalPaidDays)} দিন)",
                icon = Icons.Default.CheckCircle,
                accentColor = PaidEmerald,
                testTag = "stat_total_saved",
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Remaining Amount & Total Due Days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMetricCard(
                titleBn = "বাকি টাকা",
                subtitleEn = "Remaining Amount",
                primaryValue = SavingPlanCalendar.formatRupee(animatedRemaining),
                secondaryValueBn = SavingPlanCalendar.formatRupeeBn(stats.remainingAmount),
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = GoldHighlight,
                testTag = "stat_remaining_amount",
                modifier = Modifier.weight(1f)
            )

            StatMetricCard(
                titleBn = "মোট বাকি দিন",
                subtitleEn = "Total Due Days",
                primaryValue = "$animatedDueDays Days",
                secondaryValueBn = "${SavingPlanCalendar.toBengaliNumber(stats.totalDueDays)} দিন বাকি • ${SavingPlanCalendar.toBengaliNumber(stats.totalPaidDays)} সম্পন্ন",
                icon = Icons.Default.PendingActions,
                accentColor = DueCrimsonLight,
                testTag = "stat_total_due_days",
                modifier = Modifier.weight(1f)
            )
        }

        // Progress Bar Card (5. Saving Progress Percentage & 6. Animated Progress Bar)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stat_progress_card"),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, NavyBorder),
            colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EventRepeat,
                            contentDescription = "Saving Progress",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "সঞ্চয়ের অগ্রগতি (Saving Progress)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimaryWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "প্রতি জোড় তারিখে ₹100 জমা • মোট লক্ষ্য ₹10,400",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextSecondarySlate
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PaidEmerald.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, PaidEmerald.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "$formattedPercentage ($formattedPercentageBn)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = PaidEmeraldLight,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("stat_progress_percentage")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Animated Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF091326))
                        .testTag("animated_progress_bar")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(RoyalBluePrimary, CyanAccent, PaidEmerald)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "জমা হয়েছে: ${SavingPlanCalendar.formatRupee(stats.totalSaved)} (${stats.totalPaidDays}/104 দিন)",
                        style = MaterialTheme.typography.labelMedium.copy(color = PaidEmeraldLight)
                    )
                    Text(
                        text = "বাকি: ${SavingPlanCalendar.formatRupee(stats.remainingAmount)} (${stats.totalDueDays} দিন)",
                        style = MaterialTheme.typography.labelMedium.copy(color = DueCrimsonLight)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    titleBn: String,
    subtitleEn: String,
    primaryValue: String,
    secondaryValueBn: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NavyBorder),
        colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleBn,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimaryWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = subtitleEn,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondarySlate
                        )
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.16f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = subtitleEn,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = secondaryValueBn,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextSecondarySlate
                )
            )
        }
    }
}
