package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SavingPaymentDoc
import com.example.model.SavingPlanCalendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.Locale

class SavingRepository(
    context: Context,
    private val userId: String = DEFAULT_LOCAL_USER_ID,
    private val dao: SavingPaymentDao = SavingDatabase.getInstance(context).savingPaymentDao()
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateLock = Any()
    private val daoMutex = Mutex()

    private val _paidDatesFlow = MutableStateFlow<Set<String>>(loadPaidDatesFromPrefs())
    val paidDatesFlow: StateFlow<Set<String>> = _paidDatesFlow.asStateFlow()

    private val _transactionNotesFlow = MutableStateFlow<Map<String, String>>(loadNotesFromPrefs())
    val transactionNotesFlow: StateFlow<Map<String, String>> = _transactionNotesFlow.asStateFlow()

    init {
        repoScope.launch {
            daoMutex.withLock {
                val roomEntities = dao.getUserPaymentsOnce(userId)
                val roomPaid = roomEntities.filter { it.isPaid }.map { it.dateIso }.toSet()
                val roomNotes = roomEntities
                    .filter { it.transactionNote.isNotBlank() }
                    .associate { it.dateIso to it.transactionNote }

                val toSyncToRoom: Pair<Set<String>, Map<String, String>> = synchronized(stateLock) {
                    val currentPaid = _paidDatesFlow.value
                    val currentNotes = _transactionNotesFlow.value
                    val mergedPaid = currentPaid union roomPaid
                    val mergedNotes = roomNotes + currentNotes

                    if (mergedPaid != currentPaid) {
                        savePaidDatesToPrefs(mergedPaid)
                        _paidDatesFlow.value = mergedPaid
                    }
                    if (mergedNotes != currentNotes) {
                        saveNotesToPrefs(mergedNotes)
                        _transactionNotesFlow.value = mergedNotes
                    }
                    mergedPaid to mergedNotes
                }

                for (dateIso in toSyncToRoom.first) {
                    dao.upsertPayment(
                        SavingPaymentEntity(
                            userId = userId,
                            dateIso = dateIso,
                            isPaid = true,
                            transactionNote = toSyncToRoom.second[dateIso] ?: ""
                        )
                    )
                }
            }
        }
    }

    private fun paidDatesKey(): String = "${KEY_PAID_DATES_PREFIX}_$userId"
    private fun notesKey(): String = "${KEY_NOTES_PREFIX}_$userId"

    private fun loadPaidDatesFromPrefs(): Set<String> {
        val raw = prefs.getStringSet(paidDatesKey(), null)
            ?: if (userId == DEFAULT_LOCAL_USER_ID) prefs.getStringSet(LEGACY_KEY_PAID_DATES, emptySet()) else emptySet()
        return (raw ?: emptySet()).toSet()
    }

    private fun savePaidDatesToPrefs(dates: Set<String>) {
        val editor = prefs.edit().putStringSet(paidDatesKey(), dates.toSet())
        if (userId == DEFAULT_LOCAL_USER_ID) {
            editor.putStringSet(LEGACY_KEY_PAID_DATES, dates.toSet())
        }
        editor.commit()
    }

    private fun loadNotesFromPrefs(): Map<String, String> {
        val rawJson = prefs.getString(notesKey(), null) ?: return emptyMap()
        return try {
            val obj = JSONObject(rawJson)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k, "")
                if (v.isNotBlank()) map[k] = v
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveNotesToPrefs(notes: Map<String, String>) {
        val obj = JSONObject()
        notes.forEach { (k, v) ->
            if (v.isNotBlank()) obj.put(k, v)
        }
        prefs.edit().putString(notesKey(), obj.toString()).commit()
    }

    suspend fun toggleDateStatus(dateIso: String): Boolean {
        val (nowPaid, currentNote) = synchronized(stateLock) {
            val current = _paidDatesFlow.value.toMutableSet()
            val paid = if (current.contains(dateIso)) {
                current.remove(dateIso)
                false
            } else {
                current.add(dateIso)
                true
            }
            val note = _transactionNotesFlow.value[dateIso] ?: ""
            savePaidDatesToPrefs(current)
            _paidDatesFlow.value = current.toSet()
            paid to note
        }

        daoMutex.withLock {
            dao.upsertPayment(
                SavingPaymentEntity(
                    userId = userId,
                    dateIso = dateIso,
                    isPaid = nowPaid,
                    transactionNote = currentNote
                )
            )
        }
        return nowPaid
    }

    suspend fun updateTransactionNote(dateIso: String, note: String) {
        val (trimmed, isPaid) = synchronized(stateLock) {
            val clean = note.trim().take(300)
            val updatedNotes = _transactionNotesFlow.value.toMutableMap()
            if (clean.isEmpty()) {
                updatedNotes.remove(dateIso)
            } else {
                updatedNotes[dateIso] = clean
            }
            saveNotesToPrefs(updatedNotes)
            _transactionNotesFlow.value = updatedNotes.toMap()
            clean to _paidDatesFlow.value.contains(dateIso)
        }

        daoMutex.withLock {
            dao.upsertPayment(
                SavingPaymentEntity(
                    userId = userId,
                    dateIso = dateIso,
                    isPaid = isPaid,
                    transactionNote = trimmed
                )
            )
        }
    }

    /**
     * Merges cloud records into local backup without ever overwriting cloud records
     * with empty local data. Returns the set of local-only paid dates that should be
     * pushed up to the cloud.
     */
    suspend fun mergeFromCloud(
        cloudPayments: List<SavingPaymentDoc>,
        cloudSummaryPaidDates: Set<String>
    ): Set<String> {
        val (mergedPaid, updatedNotes, localOnlyToPush) = synchronized(stateLock) {
            val cloudPaidFromDocs = cloudPayments
                .filter { it.status == "PAID" }
                .map { it.dateIso }
                .toSet()
            val cloudDueFromDocs = cloudPayments
                .filter { it.status == "DUE" }
                .map { it.dateIso }
                .toSet()

            val allCloudPaid = (cloudPaidFromDocs union cloudSummaryPaidDates) - cloudDueFromDocs
            val localPaid = _paidDatesFlow.value

            // Never overwrite cloud data with empty local data: union cloud + local
            val merged = (allCloudPaid union localPaid) - cloudDueFromDocs
            val localOnly = localPaid - allCloudPaid - cloudDueFromDocs

            val notesMap = _transactionNotesFlow.value.toMutableMap()
            for (doc in cloudPayments) {
                if (doc.transactionNote.isNotBlank()) {
                    notesMap[doc.dateIso] = doc.transactionNote
                }
            }

            savePaidDatesToPrefs(merged)
            saveNotesToPrefs(notesMap)
            _paidDatesFlow.value = merged
            _transactionNotesFlow.value = notesMap.toMap()
            Triple(merged, notesMap.toMap(), localOnly)
        }

        daoMutex.withLock {
            for (dateIso in mergedPaid) {
                dao.upsertPayment(
                    SavingPaymentEntity(
                        userId = userId,
                        dateIso = dateIso,
                        isPaid = true,
                        transactionNote = updatedNotes[dateIso] ?: ""
                    )
                )
            }
        }
        return localOnlyToPush
    }

    suspend fun resetAllData() {
        synchronized(stateLock) {
            savePaidDatesToPrefs(emptySet())
            saveNotesToPrefs(emptyMap())
            _paidDatesFlow.value = emptySet()
            _transactionNotesFlow.value = emptyMap()
        }
        daoMutex.withLock {
            dao.clearUserPayments(userId)
        }
    }

    fun generateCsvContent(
        paidDates: Set<String> = _paidDatesFlow.value,
        notes: Map<String, String> = _transactionNotesFlow.value
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Month,Weekday (BN),Weekday (EN),Target Amount (INR),Status,Status (BN),Saved Amount (INR),Transaction Note")
        val allDays = SavingPlanCalendar.buildAllSavingDays(paidDates)
        for (cell in allDays) {
            val monthSpec = SavingPlanCalendar.PLAN_MONTHS.first {
                it.year == cell.year && it.month == cell.month
            }
            val statusEn = if (cell.isPaid) "PAID" else "DUE"
            val statusBn = if (cell.isPaid) "জমা হয়েছে" else "বাকি"
            val savedAmt = if (cell.isPaid) cell.amount else 0
            val cleanNote = (notes[cell.dateIso] ?: "").replace(",", " ").replace("\n", " ")
            sb.appendLine(
                String.format(
                    Locale.US,
                    "%s,%s %d,%s,%s,%d,%s,%s,%d,%s",
                    cell.dateIso,
                    monthSpec.nameEn,
                    monthSpec.year,
                    cell.weekdayNameBn,
                    cell.weekdayNameEn,
                    cell.amount,
                    statusEn,
                    statusBn,
                    savedAmt,
                    cleanNote
                )
            )
        }
        val stats = SavingPlanCalendar.computeDashboardStats(paidDates)
        sb.appendLine()
        sb.appendLine("SUMMARY,Total Target,Total Saved,Remaining Amount,Paid Days,Due Days,Progress (%)")
        sb.appendLine(
            String.format(
                Locale.US,
                "TOTAL,%d,%d,%d,%d,%d,%.2f%%",
                stats.totalTarget,
                stats.totalSaved,
                stats.remainingAmount,
                stats.totalPaidDays,
                stats.totalDueDays,
                stats.progressPercentage
            )
        )
        return sb.toString()
    }

    companion object {
        const val DEFAULT_LOCAL_USER_ID = "local_device_user"
        private const val PREFS_NAME = "my_saving_tracker_local_storage"
        private const val LEGACY_KEY_PAID_DATES = "paid_even_dates_set"
        private const val KEY_PAID_DATES_PREFIX = "paid_dates"
        private const val KEY_NOTES_PREFIX = "tx_notes"
    }
}
