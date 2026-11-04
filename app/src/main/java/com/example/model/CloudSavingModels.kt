package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SavingPlanSummaryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planStartDate: String = "December 2026",
    val planEndDate: String = "June 2027",
    val totalTarget: Long = 10400L,
    val paidDates: List<String> = emptyList(),
    val totalSavedAmount: Long = 0L,
    val remainingAmount: Long = 10400L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavingPaymentDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateIso: String = "",
    val status: String = "DUE",
    val amount: Long = 100L,
    val transactionNote: String = "",
    val planStartDate: String = "December 2026",
    val planEndDate: String = "June 2027",
    val totalTarget: Long = 10400L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface CloudSyncStatus {
    data object Syncing : CloudSyncStatus
    data class Synced(val lastUpdatedEpochMs: Long) : CloudSyncStatus
    data class SyncFailed(val messageBn: String) : CloudSyncStatus
}
