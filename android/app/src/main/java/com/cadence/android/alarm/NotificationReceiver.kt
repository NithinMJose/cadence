package com.cadence.android.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.cadence.android.CadenceApplication
import com.cadence.android.data.repository.CadenceRepository
import com.cadence.android.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SESSION_REMINDER = "com.cadence.android.ACTION_SESSION_REMINDER"
        const val ACTION_DOWNGRADE_FLOOR = "com.cadence.android.ACTION_DOWNGRADE_FLOOR"

        const val EXTRA_BLOCK_ID = "extra_block_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TOPIC_TAG = "extra_topic_tag"
        const val EXTRA_WARMUP_BRIEF = "extra_warmup_brief"
        const val EXTRA_DURATION_MIN = "extra_duration_min"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val blockId = intent.getStringExtra(EXTRA_BLOCK_ID) ?: return

        when (intent.action) {
            ACTION_DOWNGRADE_FLOOR -> {
                // One-tap downgrade directly from notification shade
                CoroutineScope(Dispatchers.IO).launch {
                    val repo = CadenceRepository(context)
                    repo.downgradeToEmergencyFloor(blockId)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(blockId.hashCode())
            }
            ACTION_SESSION_REMINDER -> {
                showSessionReminderNotification(context, intent)
            }
        }
    }

    private fun showSessionReminderNotification(context: Context, intent: Intent) {
        val blockId = intent.getStringExtra(EXTRA_BLOCK_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Upcoming Learning Session"
        val topicTag = intent.getStringExtra(EXTRA_TOPIC_TAG) ?: "Learning"
        val warmupBrief = intent.getStringExtra(EXTRA_WARMUP_BRIEF) ?: "Session starts in 5 minutes."

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val floorIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DOWNGRADE_FLOOR
            putExtra(EXTRA_BLOCK_ID, blockId)
        }
        val floorPendingIntent = PendingIntent.getBroadcast(
            context,
            blockId.hashCode() + 1,
            floorIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CadenceApplication.SESSION_REMINDERS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("5m Alert: $title [$topicTag]")
            .setContentText(warmupBrief)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Warm-Up Brief:\n$warmupBrief"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Start Session", openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "3m Emergency Floor", floorPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(blockId.hashCode(), notification)
    }
}
