package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreWaterRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveWaterIssue_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreWaterRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createWaterIssue(
                pwsid = "TX2270001",
                systemName = "Austin Water",
                zip = "78701",
                category = "Taste / Odor",
                title = "Earthy odor on cold tap",
                description = "Mild musty odor noticed in downtown Austin this morning.",
                severity = "medium"
            )
        }
        assertTrue(createResult.isSuccess)
        val issueId = createResult.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserWaterIssues(uid).first { list -> list.any { it.id == issueId } }
        }
        assertTrue(emitted.any { it.id == issueId && it.zip == "78701" })
    }

    @Test
    fun recordAndObserveLocationHistory_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreWaterRepository(firestore, auth)

        val recordResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordLocationHistory(
                pwsid = "TX2270001",
                systemName = "Austin Water",
                zip = "78701",
                city = "Austin",
                state = "TX",
                qualityScore = 86,
                fiveYearDelta = 4,
                tenYearDelta = 9,
                primaryWhySummary = "Colorado River surface water with seasonal chlorination byproducts.",
                exceedsHealthGoalCount = 3,
                violationCount = 1
            )
        }
        assertTrue(recordResult.isSuccess)
        val historyId = recordResult.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserLocationHistory(uid).first { list -> list.any { it.id == historyId } }
        }
        assertTrue(emitted.any { it.id == historyId && it.qualityScore == 86L })
    }

    @Test
    fun getWaterIssueById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreWaterRepository(firestore, auth)
        val issueId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createWaterIssue(
                pwsid = "MI0002310",
                systemName = "City of Flint Water System",
                zip = "48502",
                category = "Lead / Pipe Concern",
                title = "Checking corrosion control",
                description = "Testing cross-user isolation on reported issue.",
                severity = "high"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreWaterRepository(firestore, auth)
        val bobReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getWaterIssueById(issueId)
        }
        assertTrue(bobReadResult.isFailure)
        val exception = bobReadResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull(exception)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
    }

    @Test
    fun observeWaterIssues_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = FirestoreWaterRepository(firestore, auth)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserWaterIssues("unauth_uid").first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull(caughtException)
        val fsEx = caughtException as? FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@h2know.test"
        const val BOB_EMAIL = "bob@h2know.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
