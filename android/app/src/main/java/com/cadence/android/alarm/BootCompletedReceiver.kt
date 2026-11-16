package com.cadence.android.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cadence.android.data.local.CadenceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restores exact alarm reminders upon phone reboot.
 * Operates 100% autonomously using local Room snapshot without requiring backend connectivity.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = CadenceDatabase.getInstance(context)
                    val futureBlocks = db.scheduleDao().getFutureBlocks(System.currentTimeMillis())
                    val alarmManager = CadenceAlarmManager(context)
                    alarmManager.scheduleAlarmsForBlocks(futureBlocks)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
