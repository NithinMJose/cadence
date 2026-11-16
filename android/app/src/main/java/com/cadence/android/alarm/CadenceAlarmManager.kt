package com.cadence.android.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.cadence.android.data.local.entity.LocalTimeBlock

class CadenceAlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarmsForBlocks(blocks: List<LocalTimeBlock>) {
        if (!ExactAlarmPermissionHelper.canScheduleExactAlarms(context)) return

        val nowMs = System.currentTimeMillis()
        for (block in blocks) {
            // Alarm fires 5 minutes before scheduled session
            val reminderTimeMs = block.startTimeEpochMs - (5 * 60 * 1000L)
            if (reminderTimeMs > nowMs && !block.isCompleted) {
                scheduleExactAlarm(block, reminderTimeMs)
            }
        }
    }

    private fun scheduleExactAlarm(block: LocalTimeBlock, triggerAtMs: Long) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_SESSION_REMINDER
            putExtra(NotificationReceiver.EXTRA_BLOCK_ID, block.id)
            putExtra(NotificationReceiver.EXTRA_TITLE, block.title)
            putExtra(NotificationReceiver.EXTRA_TOPIC_TAG, block.topicTag)
            putExtra(NotificationReceiver.EXTRA_WARMUP_BRIEF, block.warmupBriefJson)
            putExtra(NotificationReceiver.EXTRA_DURATION_MIN, block.durationMin)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            block.id.hashCode(),
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Handled via permission helper
        }
    }
}
