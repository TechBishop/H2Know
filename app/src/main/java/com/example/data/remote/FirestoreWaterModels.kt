package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class WaterIssueDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val pwsid: String = "",
    val systemName: String = "",
    val zip: String = "",
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val severity: String = "medium",
    val status: String = "open",
    val affectedCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LocationHistoryDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val pwsid: String = "",
    val systemName: String = "",
    val zip: String = "",
    val city: String = "",
    val state: String = "",
    val qualityScore: Long = 0L,
    val fiveYearDelta: Long = 0L,
    val tenYearDelta: Long = 0L,
    val primaryWhySummary: String = "",
    val exceedsHealthGoalCount: Long = 0L,
    val violationCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
