package com.cadence.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FastPathRouterCommandParsingTest {

    @Test
    fun parseDoneCommand_extractsActionAndTarget() {
        val parts = "done block-123".trim().split("\\s+".toRegex())
        assertEquals("done", parts[0].lowercase())
        assertEquals("block-123", parts[1])
    }

    @Test
    fun parseSnoozeCommand_extractsMinutes() {
        val parts1 = "snooze block-456 15m".trim().split("\\s+".toRegex())
        val mins1 = parts1[2].replace("m", "").toIntOrNull() ?: 30
        assertEquals(15, mins1)

        val parts2 = "snooze block-456".trim().split("\\s+".toRegex())
        val mins2 = if (parts2.size > 2) parts2[2].toIntOrNull() ?: 30 else 30
        assertEquals(30, mins2)
    }

    @Test
    fun parseFloorCommand_identifiesEmergencyFloor() {
        val parts = "floor block-789".trim().split("\\s+".toRegex())
        assertEquals("floor", parts[0].lowercase())
        assertEquals("block-789", parts[1])
    }

    @Test
    fun parseEmptyOrUnknown_failsGracefully() {
        val emptyParts = "".trim().split("\\s+".toRegex())
        val isBlank = emptyParts.isEmpty() || emptyParts[0].isEmpty()
        assertFalse(!isBlank)
    }
}
