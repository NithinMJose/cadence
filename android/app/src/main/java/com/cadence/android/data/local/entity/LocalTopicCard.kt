package com.cadence.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity caching FSRS flashcards for 100% offline commute review.
 */
@Entity(tableName = "local_topic_cards")
data class LocalTopicCard(
    @PrimaryKey
    val id: String,
    val goalId: String? = null,
    val title: String,
    val conceptSummary: String,
    val topicTag: String,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val state: String = "NEW",
    val emergencyFloorPrompt: String,
    val rubricJson: String? = null,
    val nextReviewEpochMs: Long = 0L,
    val lastReviewEpochMs: Long? = null,
    val isLocallyGraded: Boolean = false
)
