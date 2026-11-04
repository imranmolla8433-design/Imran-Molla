package com.example.data

import android.content.Context
import com.example.R
import com.example.model.SavingPaymentDoc
import com.example.model.SavingPlanCalendar
import com.example.model.SavingPlanSummaryDoc
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SavingCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before modifying Firestore records.")
    }

    fun observeUserPayments(userId: String): Flow<List<SavingPaymentDoc>> {
        val path = "users/$userId/payments"
        return db.collection("users")
            .document(userId)
            .collection("payments")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SavingPaymentDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observePlanSummary(userId: String): Flow<SavingPlanSummaryDoc?> {
        val path = "users/$userId/saving_plans/$PLAN_DOC_ID"
        return db.collection("users")
            .document(userId)
            .collection("saving_plans")
            .document(PLAN_DOC_ID)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        SavingPlanSummaryDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun getUserPaymentByDate(userId: String, dateIso: String): Result<SavingPaymentDoc?> {
        val path = "users/$userId/payments/$dateIso"
        return try {
            val snap = db.collection("users")
                .document(userId)
                .collection("payments")
                .document(dateIso)
                .get()
                .await()
            val doc = if (snap.exists()) {
                snap.toObject(
                    SavingPaymentDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun getUserPaymentsOnce(userId: String): Result<List<SavingPaymentDoc>> {
        val path = "users/$userId/payments"
        return try {
            val snap = db.collection("users")
                .document(userId)
                .collection("payments")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val list = snap.toObjects(
                SavingPaymentDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun upsertPaymentRecord(
        dateIso: String,
        status: String,
        transactionNote: String
    ): Result<String> {
        val uid = requireUserId()
        val safeNote = transactionNote.take(300)
        val docRef = db.collection("users")
            .document(uid)
            .collection("payments")
            .document(dateIso)

        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "status" to status,
                        "transactionNote" to safeNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "dateIso" to dateIso,
                    "status" to status,
                    "amount" to SavingPlanCalendar.SAVING_AMOUNT_PER_DAY.toLong(),
                    "transactionNote" to safeNote,
                    "planStartDate" to PLAN_START_LABEL,
                    "planEndDate" to PLAN_END_LABEL,
                    "totalTarget" to SavingPlanCalendar.TOTAL_TARGET_AMOUNT.toLong(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(dateIso)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncPlanSummary(paidDates: Set<String>): Result<String> {
        val uid = requireUserId()
        val sortedPaid = paidDates.sorted().take(SavingPlanCalendar.TOTAL_SAVING_DAYS)
        val savedAmount = (sortedPaid.size * SavingPlanCalendar.SAVING_AMOUNT_PER_DAY).toLong()
        val remainingAmount = (SavingPlanCalendar.TOTAL_TARGET_AMOUNT.toLong() - savedAmount).coerceAtLeast(0L)

        val summaryRef = db.collection("users")
            .document(uid)
            .collection("saving_plans")
            .document(PLAN_DOC_ID)

        return try {
            val existingSnap = summaryRef.get().await()
            if (existingSnap.exists()) {
                summaryRef.update(
                    mapOf(
                        "paidDates" to sortedPaid,
                        "totalSavedAmount" to savedAmount,
                        "remainingAmount" to remainingAmount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "planStartDate" to PLAN_START_LABEL,
                    "planEndDate" to PLAN_END_LABEL,
                    "totalTarget" to SavingPlanCalendar.TOTAL_TARGET_AMOUNT.toLong(),
                    "paidDates" to sortedPaid,
                    "totalSavedAmount" to savedAmount,
                    "remainingAmount" to remainingAmount,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                summaryRef.set(createPayload).await()
            }
            Result.success(PLAN_DOC_ID)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, summaryRef.path)
            Result.failure(e)
        }
    }

    suspend fun resetAllCloudRecords(existingDates: Set<String>): Result<Unit> {
        val uid = requireUserId()
        return try {
            for (dateIso in existingDates) {
                val docRef = db.collection("users")
                    .document(uid)
                    .collection("payments")
                    .document(dateIso)
                docRef.delete().await()
            }
            syncPlanSummary(emptySet()).getOrThrow()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/payments")
            Result.failure(e)
        }
    }

    companion object {
        const val PLAN_DOC_ID = "plan_2026_2027"
        const val PLAN_START_LABEL = "December 2026"
        const val PLAN_END_LABEL = "June 2027"
    }
}
