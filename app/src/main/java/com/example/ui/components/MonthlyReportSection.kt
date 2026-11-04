package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarDayCell
import com.example.model.MonthlyReportStats
import com.example.model.SavingPlanCalendar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DueCrimson
import com.example.ui.theme.DueCrimsonLight
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.NavySurfaceHighlight
import com.example.ui.theme.PaidEmerald
import com.example.ui.theme.PaidEmeraldLight
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondarySlate
import java.util.Locale

@Composable
fun MonthlyReportSection(
    report: MonthlyReportStats,
    allMonthlyReports: List<MonthlyReportStats>,
    selectedMonthIndex: Int,
    onSelectMonth: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedMonthlyProgress by animateFloatAsState(
        targetValue = report.progressFraction,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "monthly_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_report_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Month Report Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("active_monthly_report_card"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NavyBorder),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Monthly Report",
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "মাসিক রিপোর্ট • ${report.monthSpec.titleBn}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimaryWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Monthly Report (${report.monthSpec.titleEn})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondarySlate
                                )
                            )
                        }
                    }

                    val pct = String.format(Locale.US, "%.0f%%", report.progressFraction * 100f)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoyalBluePrimary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = pct,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Monthly Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(NavyBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedMonthlyProgress.coerceIn(0f, 1f))
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(RoyalBluePrimary, PaidEmerald)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Required Monthly Report Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonthlyReportStatTile(
                        labelBn = "মাসিক লক্ষ্যমাত্রা",
                        labelEn = "Monthly Target",
                        valueText = SavingPlanCalendar.formatRupee(report.monthlyTarget),
                        subValueBn = SavingPlanCalendar.formatRupeeBn(report.monthlyTarget),
                        valueColor = CyanAccent,
                        testTag = "monthly_target_value",
                        modifier = Modifier.weight(1f)
                    )
                    MonthlyReportStatTile(
                        labelBn = "জমা হয়েছে",
                        labelEn = "Amount Saved",
                        valueText = SavingPlanCalendar.formatRupee(report.amountSaved),
                        subValueBn = SavingPlanCalendar.formatRupeeBn(report.amountSaved),
                        valueColor = PaidEmeraldLight,
                        testTag = "monthly_saved_value",
                        modifier = Modifier.weight(1f)
                    )
                    MonthlyReportStatTile(
                        labelBn = "বাকি টাকা",
                        labelEn = "Remaining",
                        valueText = SavingPlanCalendar.formatRupee(report.remainingAmount),
                        subValueBn = SavingPlanCalendar.formatRupeeBn(report.remainingAmount),
                        valueColor = GoldHighlight,
                        testTag = "monthly_remaining_value",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonthlyReportStatTile(
                        labelBn = "সম্পন্ন জমার দিন",
                        labelEn = "Completed Payment Days",
                        valueText = "${report.completedPaymentDays} Days",
                        subValueBn = "${SavingPlanCalendar.toBengaliNumber(report.completedPaymentDays)} দিন সম্পন্ন",
                        valueColor = PaidEmeraldLight,
                        testTag = "monthly_completed_days_value",
                        modifier = Modifier.weight(1f)
                    )
                    MonthlyReportStatTile(
                        labelBn = "বাকি জমার দিন",
                        labelEn = "Pending Payment Days",
                        valueText = "${report.pendingPaymentDays} Days",
                        subValueBn = "${SavingPlanCalendar.toBengaliNumber(report.pendingPaymentDays)} দিন বাকি",
                        valueColor = DueCrimsonLight,
                        testTag = "monthly_pending_days_value",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // All 7 Months Complete Summary Breakdown
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("all_months_summary_card"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NavyBorder),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "৭ মাসের সম্পূর্ণ সঞ্চয় তালিকা (Dec 2026 – Jun 2027)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimaryWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "যেকোনো মাসে ট্যাপ করে সেই মাসের ক্যালেন্ডার দেখুন",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondarySlate
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                allMonthlyReports.forEachIndexed { idx, item ->
                    val isCurrent = idx == selectedMonthIndex
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrent) NavySurfaceHighlight else NavySurfaceElevated,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isCurrent) CyanAccent else NavyBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectMonth(idx) }
                            .testTag("summary_month_row_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = item.monthSpec.titleBn,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = TextPrimaryWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "${item.monthSpec.titleEn} • লক্ষ্য ${SavingPlanCalendar.formatRupee(item.monthlyTarget)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondarySlate
                                    )
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "জমা: ${SavingPlanCalendar.formatRupee(item.amountSaved)}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = PaidEmeraldLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "সম্পন্ন ${item.completedPaymentDays}/${item.monthSpec.evenSavingDaysCount} দিন • বাকি ${SavingPlanCalendar.formatRupee(item.remainingAmount)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (item.pendingPaymentDays == 0) PaidEmeraldLight else TextSecondarySlate
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyReportStatTile(
    labelBn: String,
    labelEn: String,
    valueText: String,
    subValueBn: String,
    valueColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NavySurfaceElevated,
        border = BorderStroke(1.dp, NavyBorder),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = labelBn,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextPrimaryWhite,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = labelEn,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondarySlate,
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = valueColor,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = subValueBn,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondarySlate,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun TransactionNoteDialog(
    cell: CalendarDayCell,
    initialNote: String,
    onSaveNote: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember(cell.dateIso, initialNote) { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurfaceElevated,
        titleContentColor = TextPrimaryWhite,
        textContentColor = TextSecondarySlate,
        icon = {
            Icon(
                imageVector = Icons.Default.EditNote,
                contentDescription = "Transaction Note",
                tint = GoldHighlight,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "লেনদেনের নোট (${cell.dateIso})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.testTag("note_dialog_title")
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "তারিখ: ${cell.dayOfMonthBn} (${cell.weekdayNameBn}) • পরিমাণ: ₹100 • স্ট্যাটাস: ${if (cell.isPaid) "জমা (PAID)" else "বাকি (DUE)"}",
                    style = MaterialTheme.typography.labelMedium.copy(color = CyanAccent)
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= 300) noteText = it },
                    label = { Text("নোট লিখুন (যেমন: UPI রেফারেন্স / নগদ জমা)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryWhite,
                        unfocusedTextColor = TextPrimaryWhite,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = NavyBorder,
                        focusedLabelColor = CyanAccent,
                        unfocusedLabelColor = TextSecondarySlate
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveNote(noteText) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaidEmerald,
                    contentColor = NavyBackground
                ),
                modifier = Modifier.testTag("save_note_button")
            ) {
                Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.testTag("cancel_note_button")
            ) {
                Text("বাতিল", color = TextPrimaryWhite)
            }
        }
    )
}

@Composable
fun ResetConfirmationDialog(
    onConfirmReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurfaceElevated,
        titleContentColor = TextPrimaryWhite,
        textContentColor = TextSecondarySlate,
        icon = {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Warning",
                tint = DueCrimsonLight,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "সব সঞ্চয় তথ্য রিসেট করবেন?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.testTag("reset_dialog_title")
            )
        },
        text = {
            Text(
                text = "আপনি কি নিশ্চিত যে আপনি সমস্ত জমা (PAID) তারিখের তথ্য মুছে ফেলতে চান? এটি পুনরায় সব ১০৪টি জোড় তারিখকে বাকি (DUE) অবস্থায় ফিরিয়ে নেবে।",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DueCrimson,
                    contentColor = TextPrimaryWhite
                ),
                modifier = Modifier.testTag("confirm_reset_button")
            ) {
                Text("হ্যাঁ, রিসেট করুন")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.testTag("cancel_reset_button")
            ) {
                Text("বাতিল করুন", color = TextPrimaryWhite)
            }
        }
    )
}

@Composable
fun CsvExportModalDialog(
    csvContent: String,
    onSaveToDeviceFile: () -> Unit,
    onShareCsv: () -> Unit,
    onCopyCsv: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        titleContentColor = TextPrimaryWhite,
        textContentColor = TextSecondarySlate,
        title = {
            Column {
                Text(
                    text = "CSV আর্থিক রিপোর্ট এক্সপোর্ট",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryWhite
                    ),
                    modifier = Modifier.testTag("csv_dialog_title")
                )
                Text(
                    text = "My Saving Tracker (Dec 2026 – Jun 2027)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanAccent
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "আপনার সঞ্চয় পরিকল্পনার সম্পূর্ণ ১০৪ দিনের তথ্য নিচে CSV ফরম্যাটে প্রস্তুত করা হয়েছে। আপনি ফাইল হিসেবে সেভ, শেয়ার বা কপি করতে পারেন:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondarySlate)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NavyBackground,
                    border = BorderStroke(1.dp, NavyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 220.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = csvContent,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = PaidEmeraldLight,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.testTag("csv_preview_content")
                        )
                    }
                }

                HorizontalDivider(color = NavyBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveToDeviceFile,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_csv_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save CSV File",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ফাইল সেভ", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onShareCsv,
                        colors = ButtonDefaults.buttonColors(containerColor = PaidEmerald),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share CSV",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("শেয়ার", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onCopyCsv,
                    border = BorderStroke(1.dp, CyanAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("copy_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy CSV",
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ক্লিপবোর্ডে CSV কপি করুন", color = CyanAccent)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_csv_dialog_button")
            ) {
                Text("বন্ধ করুন", color = TextPrimaryWhite)
            }
        }
    )
}
