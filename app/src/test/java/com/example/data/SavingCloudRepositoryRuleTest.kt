package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SavingCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertPaymentRecord_andSyncPlanSummary_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SavingCloudRepository(firestore, auth)

        val writeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertPaymentRecord(
                dateIso = "2026-12-02",
                status = "PAID",
                transactionNote = "প্রথম কিস্তি জমা"
            )
        }
        assertTrue("Expected upsertPaymentRecord to succeed: ${writeRes.exceptionOrNull()}", writeRes.isSuccess)

        val summaryRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncPlanSummary(setOf("2026-12-02"))
        }
        assertTrue("Expected syncPlanSummary to succeed: ${summaryRes.exceptionOrNull()}", summaryRes.isSuccess)

        val listRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserPaymentsOnce(uid)
        }
        assertTrue(listRes.isSuccess)
        val payments = listRes.getOrThrow()
        assertTrue(payments.any { it.dateIso == "2026-12-02" && it.status == "PAID" && it.transactionNote == "প্রথম কিস্তি জমা" })
    }

    @Test
    fun observeUserPayments_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SavingCloudRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertPaymentRecord("2026-12-04", "PAID", "অনলাইন জমা").getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserPayments(uid).first { list ->
                list.any { it.dateIso == "2026-12-04" && it.status == "PAID" }
            }
        }
        assertTrue(emitted.any { it.dateIso == "2026-12-04" })
    }

    @Test
    fun getUserPaymentByDate_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SavingCloudRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertPaymentRecord("2026-12-06", "PAID", "Alice Note").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = SavingCloudRepository(firestore, auth)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getUserPaymentByDate(aliceUid, "2026-12-06")
        }
        assertTrue("Expected cross-user access to fail", crossResult.isFailure)
        val fsEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeUserPayments_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SavingCloudRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserPayments("unauth_user_123").first()
            }
            fail("Expected observeUserPayments to fail for unauthenticated user")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_saving@test.com"
        const val BOB_EMAIL = "bob_saving@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
