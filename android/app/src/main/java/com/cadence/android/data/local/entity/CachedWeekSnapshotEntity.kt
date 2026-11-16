package com.cadence.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 100% offline snapshot of the confirmed 7-day schedule, flex credits, and timezone.
 * Enables autonomous operation when the laptop is off or unreachable.
 */
@Entity(tableName = "cached_week_snapshot")
data class CachedWeekSnapshotEntity(
    @PrimaryKey
    val id: Int = 1,
    val windowStartEpochMs: Long,
    val windowEndEpochMs: Long,
    val timezone: String = "Asia/Kolkata",
    val flexCreditsMinutes: Int = 0,
    val recoveryModeActive: Boolean = false,
    val totalBlocksCount: Int = 0,
    val lastSyncedEpochMs: Long = System.currentTimeMillis()
)
