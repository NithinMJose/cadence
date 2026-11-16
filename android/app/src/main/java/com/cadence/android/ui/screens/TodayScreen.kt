package com.cadence.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadence.android.data.local.entity.CachedWeekSnapshotEntity
import com.cadence.android.data.local.entity.LocalTimeBlock
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodayScreen(
    blocks: List<LocalTimeBlock>,
    snapshot: CachedWeekSnapshotEntity?,
    pendingCount: Int,
    onCompleteBlock: (String) -> Unit,
    onDowngradeFloor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val todayStr = remember { dateFormatter.format(Date()) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Date & Status Header
        Text(text = "Today's Schedule", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = todayStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        // Status Row (Flex credits, Offline snapshot status, Outbox)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val flexCredits = snapshot?.flexCreditsMinutes ?: 0
            AssistChip(
                onClick = {},
                label = { Text("⚡ ${flexCredits}m Flex", fontSize = 12.sp) }
            )
            AssistChip(
                onClick = {},
                label = {
                    val statusText = if (snapshot != null) "📁 Offline Ready" else "⚠️ Not Synced"
                    Text(statusText, fontSize = 12.sp)
                }
            )
            if (pendingCount > 0) {
                AssistChip(
                    onClick = {},
                    label = { Text("⏳ $pendingCount Outbox", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (blocks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No study blocks scheduled for today.\nSync with your laptop in Settings to pull the week's plan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(blocks, key = { it.id }) { block ->
                    TimeBlockCard(
                        block = block,
                        timeFormatter = timeFormatter,
                        onComplete = { onCompleteBlock(block.id) },
                        onDowngrade = { onDowngradeFloor(block.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TimeBlockCard(
    block: LocalTimeBlock,
    timeFormatter: SimpleDateFormat,
    onComplete: () -> Unit,
    onDowngrade: () -> Unit
) {
    val startStr = timeFormatter.format(Date(block.startTimeEpochMs))
    val endStr = timeFormatter.format(Date(block.endTimeEpochMs))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "$startStr - $endStr (${block.durationMin}m)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val badgeColor = if (block.deviceType == "LAPTOP_DEEP") Color(0xFF3F51B5) else Color(0xFF009688)
                Text(
                    text = if (block.deviceType == "LAPTOP_DEEP") "💻 Laptop Deep" else "📱 Phone Commute",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.background(badgeColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = block.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = "Topic: ${block.topicTag}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (block.isEmergencyFloor) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("3-min Emergency Floor Active", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }

            if (!block.warmupBriefJson.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Warm-Up: ${block.warmupBriefJson}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (!block.isCompleted) {
                    if (!block.isEmergencyFloor && block.deviceType == "LAPTOP_DEEP") {
                        OutlinedButton(
                            onClick = onDowngrade,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("3m Floor")
                        }
                    }
                    Button(onClick = onComplete) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Done")
                    }
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("✓ Completed", color = Color(0xFF4CAF50)) }
                    )
                }
            }
        }
    }
}
