package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.WaterSystemReport
import com.example.data.remote.LocationHistoryDocument
import com.example.data.remote.OperationType
import com.example.data.remote.WaterIssueDocument
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreWaterRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserWaterIssues(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<WaterIssueDocument>> = callbackFlow {
        val path = "water_issues"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.toObjects(
                        WaterIssueDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeUserLocationHistory(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<LocationHistoryDocument>> = callbackFlow {
        val path = "location_history"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.toObjects(
                        LocationHistoryDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun createWaterIssue(
        pwsid: String,
        systemName: String,
        zip: String,
        category: String,
        title: String,
        description: String,
        severity: String
    ): Result<String> {
        val uid = requireUserId()
        val docId = "issue_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("water_issues").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "pwsid" to pwsid.take(64).ifBlank { "UNKNOWN" },
            "systemName" to systemName.take(200).ifBlank { "Community Water System" },
            "zip" to zip.take(10).padEnd(5, '0'),
            "category" to category.take(60).ifBlank { "Taste / Odor" },
            "title" to title.take(160).ifBlank { "Water quality observation" },
            "description" to description.take(1000).ifBlank { "Resident logged a water quality report." },
            "severity" to if (severity in listOf("low", "medium", "high")) severity else "medium",
            "status" to "open",
            "affectedCount" to 1L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getWaterIssueById(issueId: String): Result<WaterIssueDocument?> {
        val docRef = db.collection("water_issues").document(issueId)
        return try {
            val snap = docRef.get().await()
            val item = snap.toObject(
                WaterIssueDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserWaterIssues(): Result<List<WaterIssueDocument>> {
        val uid = requireUserId()
        val path = "water_issues"
        return try {
            val snap = db.collection(path).whereEqualTo("userId", uid).get().await()
            val list = snap.toObjects(
                WaterIssueDocument::class.java,
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

    suspend fun incrementIssueAffectedCount(issue: WaterIssueDocument): Result<Unit> {
        requireUserId()
        require(issue.id.isNotBlank()) { "Issue ID must not be blank" }
        val docRef = db.collection("water_issues").document(issue.id)
        return try {
            docRef.update(
                mapOf(
                    "affectedCount" to (issue.affectedCount + 1L).coerceAtMost(100000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun resolveWaterIssue(issue: WaterIssueDocument): Result<Unit> {
        requireUserId()
        require(issue.id.isNotBlank()) { "Issue ID must not be blank" }
        val docRef = db.collection("water_issues").document(issue.id)
        return try {
            docRef.update(
                mapOf(
                    "status" to "resolved",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteWaterIssue(issueId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("water_issues").document(issueId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordLocationHistory(
        pwsid: String,
        systemName: String,
        zip: String,
        city: String,
        state: String,
        qualityScore: Int,
        fiveYearDelta: Int,
        tenYearDelta: Int,
        primaryWhySummary: String,
        exceedsHealthGoalCount: Int,
        violationCount: Int
    ): Result<String> {
        val uid = requireUserId()
        val docId = "hist_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("location_history").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "pwsid" to pwsid.take(64).ifBlank { "UNKNOWN" },
            "systemName" to systemName.take(200).ifBlank { "Water Utility" },
            "zip" to zip.take(10).padEnd(5, '0'),
            "city" to city.take(100).ifBlank { "Local" },
            "state" to state.take(40).ifBlank { "US" },
            "qualityScore" to qualityScore.coerceIn(0, 100).toLong(),
            "fiveYearDelta" to fiveYearDelta.coerceIn(-100, 100).toLong(),
            "tenYearDelta" to tenYearDelta.coerceIn(-100, 100).toLong(),
            "primaryWhySummary" to primaryWhySummary.take(500).ifBlank { "Public water record." },
            "exceedsHealthGoalCount" to exceedsHealthGoalCount.coerceIn(0, 100).toLong(),
            "violationCount" to violationCount.coerceIn(0, 1000).toLong(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordLocationHistoryFromReport(report: WaterSystemReport): Result<String> {
        val topAlert = report.alerts.firstOrNull()?.supplyExplanation
            ?: report.sourceDescription.take(220)
        return recordLocationHistory(
            pwsid = report.pwsid,
            systemName = report.name,
            zip = report.zip,
            city = report.city,
            state = report.state,
            qualityScore = report.qualityScore,
            fiveYearDelta = report.fiveYearScoreDelta,
            tenYearDelta = report.tenYearScoreDelta,
            primaryWhySummary = topAlert,
            exceedsHealthGoalCount = report.contaminants.count { it.exceedsHealthGoal },
            violationCount = report.violations.size
        )
    }

    suspend fun getLocationHistoryById(historyId: String): Result<LocationHistoryDocument?> {
        val docRef = db.collection("location_history").document(historyId)
        return try {
            val snap = docRef.get().await()
            val item = snap.toObject(
                LocationHistoryDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteLocationHistory(historyId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("location_history").document(historyId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
