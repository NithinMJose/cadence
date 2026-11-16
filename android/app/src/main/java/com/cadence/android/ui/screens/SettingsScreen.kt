package com.cadence.android.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadence.android.alarm.ExactAlarmPermissionHelper
import com.cadence.android.data.remote.NetworkClient

@Composable
fun SettingsScreen(
    onSyncNow: () -> Unit,
    onSeedDemo: () -> Unit,
    syncStatusMessage: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var baseUrl by remember { mutableStateOf(NetworkClient.getBaseUrl(context)) }
    var hasExactAlarmPermission by remember { mutableStateOf(ExactAlarmPermissionHelper.canScheduleExactAlarms(context)) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings & Diagnostics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        // Backend Connectivity Card
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Backend Host & Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        NetworkClient.setBaseUrl(context, it)
                    },
                    label = { Text("Base URL (e.g. http://127.0.0.1:8080)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSyncNow) {
                        Text("Sync Now")
                    }
                    OutlinedButton(onClick = onSeedDemo) {
                        Text("Seed Demo")
                    }
                }
                if (!syncStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(syncStatusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Exact Alarm Permission Card
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Android 12+ Exact Alarms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                if (hasExactAlarmPermission) {
                    Text("✓ Exact Alarm Permission is GRANTED. 5-minute pre-session reminders will trigger reliably while idle.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("⚠️ Exact Alarm permission is not granted. Alarms may be delayed by system doze mode.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        val intent = ExactAlarmPermissionHelper.getRequestPermissionIntent(context)
                        if (intent != null) context.startActivity(intent)
                        hasExactAlarmPermission = ExactAlarmPermissionHelper.canScheduleExactAlarms(context)
                    }) {
                        Text("Enable in System Settings")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Offline Resilience Diagnostics
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Laptop-Off Autonomous Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Cadence caches the entire week's schedule and cards inside local Room SQLite. When your laptop is off or unreachable, all reminders, warm-up briefs, and 1-tap emergency floors work 100% on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
