package com.cadence.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity representing a scheduled learning block on phone or laptop.
 */
@Entity(tableName = "local_time_blocks")
data class LocalTimeBlock(
    @PrimaryKey
    val id: String,
    val title: String,
    val topicTag: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val status: String = "SCHEDULED", // SCHEDULED | COMPLETED | SKIPPED
    val deviceType: String = "LAPTOP_DEEP", // LAPTOP_DEEP | PHONE_COMMUTE
    val durationMin: Int = 45,
    val isEmergencyFloor: Boolean = false,
    val warmupBriefJson: String? = null // 3-bullet context refresher
) {
    val isCompleted: Boolean get() = status == "COMPLETED"
}
