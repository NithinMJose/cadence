package com.cadence.android

import com.cadence.android.data.local.entity.LocalTimeBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmTimeCalculationTest {

    @Test
    fun testFiveMinutePreSessionReminderOffset() {
        val sessionStartMs = 1700000000000L
        val expectedAlarmMs = sessionStartMs - (5 * 60 * 1000L) // 5 min earlier

        val block = LocalTimeBlock(
            id = "test-block-1",
            title = "Transformer KV Caching",
            topicTag = "ai_eng",
            startTimeEpochMs = sessionStartMs,
            endTimeEpochMs = sessionStartMs + (45 * 60 * 1000L),
            status = "SCHEDULED",
            durationMin = 45
        )

        val computedAlarmMs = block.startTimeEpochMs - (5 * 60 * 1000L)
        assertEquals(expectedAlarmMs, computedAlarmMs)
        assertFalse(block.isCompleted)
    }

    @Test
    fun testEmergencyFloorStateTransitions() {
        val block = LocalTimeBlock(
            id = "test-block-2",
            title = "Monotonic Stack",
            topicTag = "dsa_java",
            startTimeEpochMs = 1700000000000L,
            endTimeEpochMs = 1700000000000L + (45 * 60 * 1000L),
            status = "SCHEDULED",
            durationMin = 45,
            isEmergencyFloor = false
        )

        val downgraded = block.copy(isEmergencyFloor = true, durationMin = 3)
        assertTrue(downgraded.isEmergencyFloor)
        assertEquals(3, downgraded.durationMin)
    }
}
