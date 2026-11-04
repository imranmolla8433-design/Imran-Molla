package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarDayCell
import com.example.model.MonthlyReportStats
import com.example.model.SavingMonthSpec
import com.example.model.SavingPlanCalendar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DueCrimson
import com.example.ui.theme.DueCrimsonBorder
import com.example.ui.theme.DueCrimsonContainer
import com.example.ui.theme.DueCrimsonLight
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.NavySurfaceHighlight
import com.example.ui.theme.PaidEmerald
import com.example.ui.theme.PaidEmeraldBorder
import com.example.ui.theme.PaidEmeraldContainer
import com.example.ui.theme.PaidEmeraldLight
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.TextMutedSlate
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondarySlate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveCalendarSection(
    monthSpec: SavingMonthSpec,
    selectedMonthIndex: Int,
    totalMonthsCount: Int,
    calendarCells: List<CalendarDayCell>,
    transactionNotes: Map<String, String>,
    monthlyReport: MonthlyReportStats,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonthIndex: (Int) -> Unit,
    onDateClick: (CalendarDayCell) -> Unit,
    onEditNoteClick: (CalendarDayCell) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_calendar_card"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NavyBorder),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Title + Live Monthly Saved Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Live Calendar",
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "লাইভ সঞ্চয় ক্যালেন্ডার",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimaryWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "ট্যাপ করে জমা (PAID) / বাকি (DUE) করুন • লং-প্রেসে নোট যোগ করুন",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondarySlate
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavySurfaceElevated,
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${SavingPlanCalendar.formatRupee(monthlyReport.amountSaved)} / ${SavingPlanCalendar.formatRupee(monthlyReport.monthlyTarget)}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("calendar_month_saved_badge")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Month Selector Pills (Dec 2026 - Jun 2027)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SavingPlanCalendar.PLAN_MONTHS.forEach { spec ->
                    val isSelected = spec.index == selectedMonthIndex
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectMonthIndex(spec.index) },
                        label = {
                            Text(
                                text = "${spec.nameBn} '${spec.yearBn.takeLast(2)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalBluePrimary,
                            selectedLabelColor = TextPrimaryWhite,
                            containerColor = NavySurfaceElevated,
                            labelColor = TextSecondarySlate
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = NavyBorder,
                            selectedBorderColor = CyanAccent
                        ),
                        modifier = Modifier.testTag("month_chip_${spec.index}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Previous / Next Month Navigation Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NavySurfaceElevated,
                border = BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val canGoPrev = selectedMonthIndex > 0
                    IconButton(
                        onClick = onPreviousMonth,
                        enabled = canGoPrev,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (canGoPrev) NavySurfaceHighlight else Color.Transparent,
                            contentColor = if (canGoPrev) TextPrimaryWhite else TextMutedSlate,
                            disabledContentColor = TextMutedSlate.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("prev_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "পূর্ববর্তী মাস (Previous Month)"
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.testTag("current_month_header")
                    ) {
                        Text(
                            text = monthSpec.titleBn,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = TextPrimaryWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${monthSpec.titleEn} • ${SavingPlanCalendar.toBengaliNumber(monthSpec.evenSavingDaysCount)}টি সঞ্চয় দিন",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyanAccent
                            )
                        )
                    }

                    val canGoNext = selectedMonthIndex < totalMonthsCount - 1
                    IconButton(
                        onClick = onNextMonth,
                        enabled = canGoNext,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (canGoNext) NavySurfaceHighlight else Color.Transparent,
                            contentColor = if (canGoNext) TextPrimaryWhite else TextMutedSlate,
                            disabledContentColor = TextMutedSlate.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("next_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "পরবর্তী মাস (Next Month)"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend Row: PAID (Green) vs DUE (Red) vs Odd Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(
                    color = PaidEmerald,
                    label = "জমা / PAID (সবুজ)"
                )
                LegendIndicator(
                    color = DueCrimson,
                    label = "বাকি / DUE (লাল)"
                )
                LegendIndicator(
                    color = NavySurfaceHighlight,
                    label = "বিজোড় দিন"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7 Weekday Headers (Sun - Sat / রবি - শনি)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until 7) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyBackground)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = SavingPlanCalendar.WEEKDAYS_BN[i],
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = SavingPlanCalendar.WEEKDAYS_EN[i],
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondarySlate,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real Monthly Calendar Grid
            val leadingEmptyCount = monthSpec.startDayOfWeekIndex
            val totalSlots = leadingEmptyCount + calendarCells.size
            val rowCount = (totalSlots + 6) / 7

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_grid"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (rowIdx in 0 until rowCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (colIdx in 0 until 7) {
                            val slotIndex = rowIdx * 7 + colIdx
                            val dayIndex = slotIndex - leadingEmptyCount
                            if (dayIndex in calendarCells.indices) {
                                val cell = calendarCells[dayIndex]
                                val hasNote = !transactionNotes[cell.dateIso].isNullOrBlank()
                                CalendarDateCellView(
                                    cell = cell,
                                    hasNote = hasNote,
                                    onClick = { onDateClick(cell) },
                                    onLongClick = { onEditNoteClick(cell) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(68.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Recent / Active Month Transaction Notes List & Quick Note Editor Bar
            val monthSavingCells = calendarCells.filter { it.isSavingDay }
            val monthNotes = monthSavingCells.mapNotNull { cell ->
                val noteText = transactionNotes[cell.dateIso]
                if (!noteText.isNullOrBlank()) cell to noteText else null
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NavySurfaceElevated,
                border = BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Transaction Notes",
                                tint = GoldHighlight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "লেনদেনের নোট (Transaction Notes)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = TextPrimaryWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        val firstSavingCell = monthSavingCells.firstOrNull()
                        if (firstSavingCell != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RoyalBluePrimary.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onClick = { onEditNoteClick(firstSavingCell) }
                                    )
                                    .testTag("add_note_quick_button")
                            ) {
                                Text(
                                    text = "+ নোট যোগ করুন",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyanAccent,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (monthNotes.isEmpty()) {
                        Text(
                            text = "যেকোনো জোড় তারিখে লং-প্রেস (চাপ দিয়ে ধরে রাখুন) করে লেনদেনের নোট যোগ করুন।",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondarySlate
                            )
                        )
                    } else {
                        monthNotes.forEach { (cell, note) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NavyBackground.copy(alpha = 0.6f))
                                    .combinedClickable(onClick = { onEditNoteClick(cell) })
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${cell.dayOfMonthBn} ${monthSpec.nameBn}: $note",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextPrimaryWhite
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (cell.isPaid) "PAID" else "DUE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (cell.isPaid) PaidEmeraldLight else DueCrimsonLight,
                                        fontWeight = FontWeight.Bold
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarDateCellView(
    cell: CalendarDayCell,
    hasNote: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            !cell.isSavingDay -> NavyBackground.copy(alpha = 0.65f)
            cell.isPaid -> PaidEmeraldContainer
            else -> DueCrimsonContainer
        },
        animationSpec = tween(durationMillis = 220),
        label = "cell_bg_color"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !cell.isSavingDay -> NavyBorder.copy(alpha = 0.45f)
            cell.isPaid -> PaidEmeraldBorder
            else -> DueCrimsonBorder
        },
        animationSpec = tween(durationMillis = 220),
        label = "cell_border_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (cell.isPaid) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cell_scale"
    )

    val statusText = when {
        !cell.isSavingDay -> "Odd Day"
        cell.isPaid -> "PAID"
        else -> "DUE"
    }

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minWidth = 48.dp, minHeight = 68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (cell.isSavingDay) 1.5.dp else 0.8.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                contentDescription =
                    "Date ${cell.dateIso}, ${if (cell.isSavingDay) "Saving Day ₹100, Status $statusText" else "Non-saving odd day"}"
            }
            .testTag("calendar_day_${cell.dateIso}")
            .padding(vertical = 5.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${cell.dayOfMonth}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (cell.isSavingDay) TextPrimaryWhite else TextMutedSlate,
                        fontWeight = if (cell.isSavingDay) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                )
                if (cell.isSavingDay && cell.isPaid) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Paid Check",
                        tint = PaidEmeraldLight,
                        modifier = Modifier.size(12.dp)
                    )
                }
                if (hasNote) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(GoldHighlight)
                    )
                }
            }

            if (cell.isSavingDay) {
                Text(
                    text = "₹100",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (cell.isPaid) PaidEmeraldLight else TextPrimaryWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (cell.isPaid) PaidEmerald.copy(alpha = 0.28f)
                            else DueCrimson.copy(alpha = 0.28f)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (cell.isPaid) "PAID" else "DUE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (cell.isPaid) PaidEmeraldLight else DueCrimsonLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = cell.dayOfMonthBn,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMutedSlate.copy(alpha = 0.65f),
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "—",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMutedSlate.copy(alpha = 0.45f),
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun LegendIndicator(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondarySlate
            )
        )
    }
}
