package com.cadence.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Offline command queue (outbox) holding local user actions.
 * Synced atomically to Spring Boot backend via WorkManager once connectivity is restored.
 */
@Entity(tableName = "pending_commands")
data class PendingCommandEntity(
    @PrimaryKey
    val commandId: String = UUID.randomUUID().toString(),
    val commandType: String, // COMPLETE_BLOCK | EMERGENCY_FLOOR | SNOOZE | REPLAN_INTENT | GRADE_CARD
    val targetId: String? = null,
    val payloadJson: String = "{}",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)
