package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SavingCloudRepository
import com.example.data.SavingRepository
import com.example.model.CalendarDayCell
import com.example.model.CloudSyncStatus
import com.example.model.DashboardStats
import com.example.model.MonthlyReportStats
import com.example.model.SavingMonthSpec
import com.example.model.SavingPlanCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavingTrackerUiState(
    val currentUserId: String? = null,
    val userDisplayName: String = "",
    val userEmail: String = "",
    val isCloudConnected: Boolean = false,
    val syncStatus: CloudSyncStatus = CloudSyncStatus.Synced(System.currentTimeMillis()),
    val selectedMonthIndex: Int = 0, // Starts automatically at 0 = December 2026
    val currentMonthSpec: SavingMonthSpec = SavingPlanCalendar.PLAN_MONTHS[0],
    val calendarCells: List<CalendarDayCell> = SavingPlanCalendar.buildMonthCells(
        SavingPlanCalendar.PLAN_MONTHS[0],
        emptySet()
    ),
    val dashboardStats: DashboardStats = SavingPlanCalendar.computeDashboardStats(emptySet()),
    val currentMonthlyReport: MonthlyReportStats = SavingPlanCalendar.computeMonthlyReport(
        SavingPlanCalendar.PLAN_MONTHS[0],
        emptySet()
    ),
    val allMonthlyReports: List<MonthlyReportStats> = SavingPlanCalendar.PLAN_MONTHS.map {
        SavingPlanCalendar.computeMonthlyReport(it, emptySet())
    },
    val paidDates: Set<String> = emptySet(),
    val transactionNotes: Map<String, String> = emptyMap(),
    val activeNoteCell: CalendarDayCell? = null,
    val showResetConfirmDialog: Boolean = false,
    val showCsvDialog: Boolean = false,
    val statusMessageBn: String? = null
)

class SavingTrackerViewModel(
    private val localRepository: SavingRepository,
    private val cloudRepository: SavingCloudRepository? = null,
    private val currentUserId: String? = null,
    private val userDisplayName: String = "",
    private val userEmail: String = ""
) : ViewModel() {

    private val _selectedMonthIndex = MutableStateFlow(0) // 0 = December 2026
    private val _syncStatus = MutableStateFlow<CloudSyncStatus>(
        if (currentUserId != null && cloudRepository != null) {
            CloudSyncStatus.Syncing
        } else {
            CloudSyncStatus.Synced(System.currentTimeMillis())
        }
    )
    private val _activeNoteDateIso = MutableStateFlow<String?>(null)
    private val _showResetConfirmDialog = MutableStateFlow(false)
    private val _showCsvDialog = MutableStateFlow(false)
    private val _statusMessageBn = MutableStateFlow<String?>(null)

    private val dialogStateFlow = combine(
        _activeNoteDateIso,
        _showResetConfirmDialog,
        _showCsvDialog,
        _statusMessageBn
    ) { noteDateIso, showReset, showCsv, statusMsg ->
        DialogOverlayState(noteDateIso, showReset, showCsv, statusMsg)
    }

    val uiState: StateFlow<SavingTrackerUiState> = combine(
        _selectedMonthIndex,
        localRepository.paidDatesFlow,
        localRepository.transactionNotesFlow,
        _syncStatus,
        dialogStateFlow
    ) { monthIdx, paidDates, notes, syncStatus, overlays ->
        val safeIdx = monthIdx.coerceIn(0, SavingPlanCalendar.PLAN_MONTHS.lastIndex)
        val monthSpec = SavingPlanCalendar.PLAN_MONTHS[safeIdx]
        val cells = SavingPlanCalendar.buildMonthCells(monthSpec, paidDates)
        val dashboard = SavingPlanCalendar.computeDashboardStats(paidDates)
        val monthlyReport = SavingPlanCalendar.computeMonthlyReport(monthSpec, paidDates)
        val allReports = SavingPlanCalendar.PLAN_MONTHS.map {
            SavingPlanCalendar.computeMonthlyReport(it, paidDates)
        }
        val activeCell = overlays.activeNoteDateIso?.let { iso ->
            cells.firstOrNull { it.dateIso == iso }
                ?: SavingPlanCalendar.buildAllSavingDays(paidDates).firstOrNull { it.dateIso == iso }
        }
        SavingTrackerUiState(
            currentUserId = currentUserId,
            userDisplayName = userDisplayName,
            userEmail = userEmail,
            isCloudConnected = currentUserId != null && cloudRepository != null,
            syncStatus = syncStatus,
            selectedMonthIndex = safeIdx,
            currentMonthSpec = monthSpec,
            calendarCells = cells,
            dashboardStats = dashboard,
            currentMonthlyReport = monthlyReport,
            allMonthlyReports = allReports,
            paidDates = paidDates,
            transactionNotes = notes,
            activeNoteCell = activeCell,
            showResetConfirmDialog = overlays.showResetConfirmDialog,
            showCsvDialog = overlays.showCsvDialog,
            statusMessageBn = overlays.statusMessageBn
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildInitialState(
            paidDates = localRepository.paidDatesFlow.value,
            notes = localRepository.transactionNotesFlow.value
        )
    )

    init {
        if (currentUserId != null && cloudRepository != null) {
            startCloudRealtimeSync(currentUserId, cloudRepository)
        }
    }

    private fun startCloudRealtimeSync(userId: String, cloudRepo: SavingCloudRepository) {
        viewModelScope.launch {
            _syncStatus.value = CloudSyncStatus.Syncing
            combine(
                cloudRepo.observeUserPayments(userId),
                cloudRepo.observePlanSummary(userId)
            ) { payments, summary ->
                payments to summary
            }
                .catch { error ->
                    Log.w(TAG, "Error observing Firestore records for user $userId", error)
                    _syncStatus.value = CloudSyncStatus.SyncFailed(
                        "অফলাইন / ক্লাউড সিঙ্ক ব্যর্থ — লোকাল ব্যাকআপ সক্রিয়"
                    )
                }
                .collect { (cloudPayments, cloudSummary) ->
                    val summaryPaid = cloudSummary?.paidDates?.toSet() ?: emptySet()
                    val localOnlyDates = localRepository.mergeFromCloud(
                        cloudPayments = cloudPayments,
                        cloudSummaryPaidDates = summaryPaid
                    )

                    // If local backup has dates that are not yet in the cloud, push them without overwriting cloud data
                    if (localOnlyDates.isNotEmpty()) {
                        val currentNotes = localRepository.transactionNotesFlow.value
                        for (dateIso in localOnlyDates) {
                            cloudRepo.upsertPaymentRecord(
                                dateIso = dateIso,
                                status = "PAID",
                                transactionNote = currentNotes[dateIso] ?: ""
                            )
                        }
                        cloudRepo.syncPlanSummary(localRepository.paidDatesFlow.value)
                    } else if (cloudSummary == null) {
                        // Initialize plan summary in Firestore on first login
                        cloudRepo.syncPlanSummary(localRepository.paidDatesFlow.value)
                    }

                    val latestTs = cloudSummary?.updatedAt?.toDate()?.time
                        ?: cloudPayments.mapNotNull { it.updatedAt?.toDate()?.time }.maxOrNull()
                        ?: System.currentTimeMillis()
                    _syncStatus.value = CloudSyncStatus.Synced(latestTs)
                }
        }
    }

    private fun buildInitialState(
        paidDates: Set<String>,
        notes: Map<String, String>
    ): SavingTrackerUiState {
        val monthSpec = SavingPlanCalendar.PLAN_MONTHS[0]
        return SavingTrackerUiState(
            currentUserId = currentUserId,
            userDisplayName = userDisplayName,
            userEmail = userEmail,
            isCloudConnected = currentUserId != null && cloudRepository != null,
            syncStatus = _syncStatus.value,
            selectedMonthIndex = 0,
            currentMonthSpec = monthSpec,
            calendarCells = SavingPlanCalendar.buildMonthCells(monthSpec, paidDates),
            dashboardStats = SavingPlanCalendar.computeDashboardStats(paidDates),
            currentMonthlyReport = SavingPlanCalendar.computeMonthlyReport(monthSpec, paidDates),
            allMonthlyReports = SavingPlanCalendar.PLAN_MONTHS.map {
                SavingPlanCalendar.computeMonthlyReport(it, paidDates)
            },
            paidDates = paidDates,
            transactionNotes = notes
        )
    }

    fun previousMonth() {
        val current = _selectedMonthIndex.value
        if (current > 0) {
            _selectedMonthIndex.value = current - 1
        }
    }

    fun nextMonth() {
        val current = _selectedMonthIndex.value
        if (current < SavingPlanCalendar.PLAN_MONTHS.lastIndex) {
            _selectedMonthIndex.value = current + 1
        }
    }

    fun selectMonth(index: Int) {
        if (index in SavingPlanCalendar.PLAN_MONTHS.indices) {
            _selectedMonthIndex.value = index
        }
    }

    fun onCalendarDateClicked(cell: CalendarDayCell) {
        if (!cell.isSavingDay) {
            _statusMessageBn.value =
                "${cell.dayOfMonthBn} তারিখ বিজোড় দিন — শুধুমাত্র জোড় তারিখে ₹100 সঞ্চয় প্রযোজ্য।"
            return
        }
        viewModelScope.launch {
            val nowPaid = localRepository.toggleDateStatus(cell.dateIso)
            val monthSpec = SavingPlanCalendar.PLAN_MONTHS.firstOrNull {
                it.year == cell.year && it.month == cell.month
            }
            val monthName = monthSpec?.nameBn ?: ""
            _statusMessageBn.value = if (nowPaid) {
                "✓ ${cell.dayOfMonthBn} $monthName: ₹100 জমা (PAID) হিসেবে সংরক্ষিত!"
            } else {
                "↺ ${cell.dayOfMonthBn} $monthName: পুনরায় বাকি (DUE) হিসেবে চিহ্নিত।"
            }

            if (currentUserId != null && cloudRepository != null) {
                _syncStatus.value = CloudSyncStatus.Syncing
                val note = localRepository.transactionNotesFlow.value[cell.dateIso] ?: ""
                val recordRes = cloudRepository.upsertPaymentRecord(
                    dateIso = cell.dateIso,
                    status = if (nowPaid) "PAID" else "DUE",
                    transactionNote = note
                )
                val summaryRes = cloudRepository.syncPlanSummary(localRepository.paidDatesFlow.value)
                if (recordRes.isSuccess && summaryRes.isSuccess) {
                    _syncStatus.value = CloudSyncStatus.Synced(System.currentTimeMillis())
                } else {
                    _syncStatus.value = CloudSyncStatus.SyncFailed(
                        "অফলাইন / ক্লাউড সিঙ্ক ব্যর্থ — লোকাল ব্যাকআপে সুরক্ষিত"
                    )
                }
            }
        }
    }

    fun openNoteEditorForDate(cell: CalendarDayCell) {
        if (cell.isSavingDay) {
            _activeNoteDateIso.value = cell.dateIso
        }
    }

    fun closeNoteEditor() {
        _activeNoteDateIso.value = null
    }

    fun saveTransactionNote(dateIso: String, note: String) {
        viewModelScope.launch {
            localRepository.updateTransactionNote(dateIso, note)
            _activeNoteDateIso.value = null
            _statusMessageBn.value = "✓ লেনদেনের নোট সংরক্ষিত হয়েছে।"

            if (currentUserId != null && cloudRepository != null) {
                _syncStatus.value = CloudSyncStatus.Syncing
                val isPaid = localRepository.paidDatesFlow.value.contains(dateIso)
                val res = cloudRepository.upsertPaymentRecord(
                    dateIso = dateIso,
                    status = if (isPaid) "PAID" else "DUE",
                    transactionNote = note.trim()
                )
                if (res.isSuccess) {
                    _syncStatus.value = CloudSyncStatus.Synced(System.currentTimeMillis())
                } else {
                    _syncStatus.value = CloudSyncStatus.SyncFailed(
                        "নোট লোকাল ব্যাকআপে সেভ হয়েছে (ক্লাউড সিঙ্ক ব্যর্থ)"
                    )
                }
            }
        }
    }

    fun retryCloudSync() {
        val cloudRepo = cloudRepository ?: return
        if (currentUserId == null) return
        viewModelScope.launch {
            _syncStatus.value = CloudSyncStatus.Syncing
            val paid = localRepository.paidDatesFlow.value
            val notes = localRepository.transactionNotesFlow.value
            var allSucceeded = true
            for (dateIso in paid) {
                val r = cloudRepo.upsertPaymentRecord(
                    dateIso = dateIso,
                    status = "PAID",
                    transactionNote = notes[dateIso] ?: ""
                )
                if (r.isFailure) allSucceeded = false
            }
            val summaryRes = cloudRepo.syncPlanSummary(paid)
            if (allSucceeded && summaryRes.isSuccess) {
                _syncStatus.value = CloudSyncStatus.Synced(System.currentTimeMillis())
                _statusMessageBn.value = "✓ ক্লাউড সিঙ্ক সফলভাবে সম্পন্ন হয়েছে!"
            } else {
                _syncStatus.value = CloudSyncStatus.SyncFailed(
                    "সিঙ্ক ব্যর্থ হয়েছে — ইন্টারনেট সংযোগ পরীক্ষা করুন"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessageBn.value = null
    }

    fun requestResetConfirmation() {
        _showResetConfirmDialog.value = true
    }

    fun dismissResetConfirmation() {
        _showResetConfirmDialog.value = false
    }

    fun confirmResetAllData() {
        viewModelScope.launch {
            val previousDates = localRepository.paidDatesFlow.value
            localRepository.resetAllData()
            _showResetConfirmDialog.value = false
            _statusMessageBn.value = "সব সঞ্চয় তথ্য সফলভাবে রিসেট করা হয়েছে।"

            if (currentUserId != null && cloudRepository != null) {
                _syncStatus.value = CloudSyncStatus.Syncing
                val res = cloudRepository.resetAllCloudRecords(previousDates)
                if (res.isSuccess) {
                    _syncStatus.value = CloudSyncStatus.Synced(System.currentTimeMillis())
                } else {
                    _syncStatus.value = CloudSyncStatus.SyncFailed("ক্লাউড রিসেট ব্যর্থ হয়েছে")
                }
            }
        }
    }

    fun openCsvExportDialog() {
        _showCsvDialog.value = true
    }

    fun closeCsvExportDialog() {
        _showCsvDialog.value = false
    }

    fun generateCsvContent(): String {
        return localRepository.generateCsvContent(
            paidDates = uiState.value.paidDates,
            notes = uiState.value.transactionNotes
        )
    }

    fun notifyCsvSaved(messageBn: String) {
        _statusMessageBn.value = messageBn
    }

    private data class DialogOverlayState(
        val activeNoteDateIso: String?,
        val showResetConfirmDialog: Boolean,
        val showCsvDialog: Boolean,
        val statusMessageBn: String?
    )

    private companion object {
        const val TAG = "SavingTrackerVM"
    }
}
