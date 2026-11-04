package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.SavingPlanCalendar
import com.example.ui.components.CsvExportModalDialog
import com.example.ui.components.DashboardStatisticsSection
import com.example.ui.components.HeroBannerHeader
import com.example.ui.components.LiveCalendarSection
import com.example.ui.components.MonthlyReportSection
import com.example.ui.components.ResetConfirmationDialog
import com.example.ui.components.TransactionNoteDialog
import com.example.ui.components.WebVersionScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.PaidEmeraldLight
import com.example.ui.theme.TextPrimaryWhite
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun SavingTrackerScreen(
    viewModel: SavingTrackerViewModel,
    onAuthActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showWebVersion by rememberSaveable { mutableStateOf(false) }

    // Auto-dismiss status banner after 3.5 seconds
    LaunchedEffect(uiState.statusMessageBn) {
        if (uiState.statusMessageBn != null) {
            delay(3500)
            viewModel.clearStatusMessage()
        }
    }

    // Android Storage Access Framework launcher for saving CSV file
    val createCsvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csvBytes = ("\uFEFF" + viewModel.generateCsvContent()).toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvBytes)
                    outputStream.flush()
                }
                viewModel.closeCsvExportDialog()
                viewModel.notifyCsvSaved("✓ CSV ফাইল সফলভাবে আপনার ডিভাইসে সংরক্ষিত হয়েছে!")
            } catch (e: Exception) {
                viewModel.notifyCsvSaved("CSV সংরক্ষণে সমস্যা হয়েছে: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    if (showWebVersion) {
        WebVersionScreen(
            paidDates = uiState.paidDates,
            transactionNotes = uiState.transactionNotes,
            onBackToAndroidUi = { showWebVersion = false },
            onExportCsvFromWeb = { csvFromWeb ->
                saveCsvToAppCache(context, csvFromWeb)
                createCsvDocumentLauncher.launch("my_saving_tracker_2026_2027.csv")
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NavyBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyBackground)
                .padding(innerPadding)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                val isWideTablet = maxWidth >= 740.dp
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1080.dp)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("main_scroll_container"),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. Hero Header with Title, Cloud Sync Status, Auth Action, CSV Export, Reset & Web Version Buttons
                    HeroBannerHeader(
                        isCloudConnected = uiState.isCloudConnected,
                        userDisplayName = uiState.userDisplayName,
                        userEmail = uiState.userEmail,
                        syncStatus = uiState.syncStatus,
                        onRetrySyncClick = { viewModel.retryCloudSync() },
                        onAuthActionClick = onAuthActionClick,
                        onExportCsvClick = { viewModel.openCsvExportDialog() },
                        onResetClick = { viewModel.requestResetConfirmation() },
                        onOpenWebVersionClick = { showWebVersion = true }
                    )

                    // Inline Status Feedback Banner
                    AnimatedVisibility(
                        visible = uiState.statusMessageBn != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NavySurfaceElevated,
                            border = BorderStroke(1.dp, CyanAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("status_feedback_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = uiState.statusMessageBn ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = PaidEmeraldLight,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearStatusMessage() }) {
                                    Text("ঠিক আছে", color = TextPrimaryWhite)
                                }
                            }
                        }
                    }

                    // 2. Dashboard Statistics (6 Metrics + Animated Progress Bar)
                    DashboardStatisticsSection(
                        stats = uiState.dashboardStats
                    )

                    // 3. Adaptive Layout for Live Calendar & Monthly Report
                    if (isWideTablet) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            LiveCalendarSection(
                                monthSpec = uiState.currentMonthSpec,
                                selectedMonthIndex = uiState.selectedMonthIndex,
                                totalMonthsCount = SavingPlanCalendar.PLAN_MONTHS.size,
                                calendarCells = uiState.calendarCells,
                                transactionNotes = uiState.transactionNotes,
                                monthlyReport = uiState.currentMonthlyReport,
                                onPreviousMonth = { viewModel.previousMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                onSelectMonthIndex = { viewModel.selectMonth(it) },
                                onDateClick = { viewModel.onCalendarDateClicked(it) },
                                onEditNoteClick = { viewModel.openNoteEditorForDate(it) },
                                modifier = Modifier.weight(1.1f)
                            )

                            MonthlyReportSection(
                                report = uiState.currentMonthlyReport,
                                allMonthlyReports = uiState.allMonthlyReports,
                                selectedMonthIndex = uiState.selectedMonthIndex,
                                onSelectMonth = { viewModel.selectMonth(it) },
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    } else {
                        LiveCalendarSection(
                            monthSpec = uiState.currentMonthSpec,
                            selectedMonthIndex = uiState.selectedMonthIndex,
                            totalMonthsCount = SavingPlanCalendar.PLAN_MONTHS.size,
                            calendarCells = uiState.calendarCells,
                            transactionNotes = uiState.transactionNotes,
                            monthlyReport = uiState.currentMonthlyReport,
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() },
                            onSelectMonthIndex = { viewModel.selectMonth(it) },
                            onDateClick = { viewModel.onCalendarDateClicked(it) },
                            onEditNoteClick = { viewModel.openNoteEditorForDate(it) }
                        )

                        MonthlyReportSection(
                            report = uiState.currentMonthlyReport,
                            allMonthlyReports = uiState.allMonthlyReports,
                            selectedMonthIndex = uiState.selectedMonthIndex,
                            onSelectMonth = { viewModel.selectMonth(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Transaction Note Dialog for any Saving Date
    val activeNoteCell = uiState.activeNoteCell
    if (activeNoteCell != null) {
        val currentNote = uiState.transactionNotes[activeNoteCell.dateIso] ?: ""
        TransactionNoteDialog(
            cell = activeNoteCell,
            initialNote = currentNote,
            onSaveNote = { updatedNote ->
                viewModel.saveTransactionNote(activeNoteCell.dateIso, updatedNote)
            },
            onDismiss = { viewModel.closeNoteEditor() }
        )
    }

    // Confirmation Dialog Before Resetting All Data
    if (uiState.showResetConfirmDialog) {
        ResetConfirmationDialog(
            onConfirmReset = { viewModel.confirmResetAllData() },
            onDismiss = { viewModel.dismissResetConfirmation() }
        )
    }

    // CSV Export & Share Modal Dialog
    if (uiState.showCsvDialog) {
        val csvContent = viewModel.generateCsvContent()
        CsvExportModalDialog(
            csvContent = csvContent,
            onSaveToDeviceFile = {
                saveCsvToAppCache(context, csvContent)
                createCsvDocumentLauncher.launch("my_saving_tracker_2026_2027.csv")
            },
            onShareCsv = {
                shareCsvText(context, csvContent)
                viewModel.closeCsvExportDialog()
            },
            onCopyCsv = {
                copyCsvToClipboard(context, csvContent)
                viewModel.closeCsvExportDialog()
                viewModel.notifyCsvSaved("✓ সম্পূর্ণ CSV রিপোর্ট ক্লিপবোর্ডে কপি করা হয়েছে!")
            },
            onDismiss = { viewModel.closeCsvExportDialog() }
        )
    }
}

private fun saveCsvToAppCache(context: Context, csvContent: String): File? {
    return try {
        val file = File(context.cacheDir, "my_saving_tracker_2026_2027.csv")
        file.writeText("\uFEFF" + csvContent, Charsets.UTF_8)
        file
    } catch (e: Exception) {
        null
    }
}

private fun copyCsvToClipboard(context: Context, csvContent: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("My Saving Tracker CSV", csvContent)
    clipboard?.setPrimaryClip(clip)
}

private fun shareCsvText(context: Context, csvContent: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "My Saving Tracker Report (Dec 2026 – Jun 2027)")
        putExtra(Intent.EXTRA_TEXT, csvContent)
    }
    val chooser = Intent.createChooser(sendIntent, "CSV এক্সপোর্ট শেয়ার করুন")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
