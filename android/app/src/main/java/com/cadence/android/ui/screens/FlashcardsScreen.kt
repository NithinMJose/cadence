package com.cadence.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadence.android.data.local.entity.LocalTopicCard
import org.json.JSONObject

@Composable
fun FlashcardsScreen(
    dueCards: List<LocalTopicCard>,
    onGradeCard: (cardId: String, grade: Int) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isRevealed by remember { mutableStateOf(false) }
    var isFloorMode by remember { mutableStateOf(false) }

    LaunchedEffect(dueCards.size) {
        if (currentIndex >= dueCards.size) {
            currentIndex = 0
            isRevealed = false
            isFloorMode = false
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Commute Flashcards", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (dueCards.isEmpty()) "All caught up! 🎉" else "Card ${currentIndex + 1} of ${dueCards.size} due",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh due queue")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (dueCards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Zero Cards Due", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "FSRS-4.5 interval scheduling has placed all your concepts into optimal memory retention.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            val card = dueCards.getOrNull(currentIndex) ?: return
            val checkpoints = remember(card.rubricJson) { parseCheckpoints(card.rubricJson) }

            // Flashcard container
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { isRevealed = !isRevealed }
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    // Tag & Floor toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.topicTag.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        FilterChip(
                            selected = isFloorMode,
                            onClick = { isFloorMode = !isFloorMode },
                            label = { Text("⚡ 3m Floor", fontSize = 11.sp) },
                            leadingIcon = if (isFloorMode) {
                                { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isFloorMode) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Emergency Floor Micro-Quiz:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(card.emergencyFloorPrompt, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Front hint or Revealed answer
                    if (!isRevealed) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap card to reveal rubric & answer", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                Text("Concept Summary:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(card.conceptSummary, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (checkpoints.isNotEmpty()) {
                                item {
                                    Text("Rubric Checkpoints:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                items(checkpoints.size) { i ->
                                    Text("• ${checkpoints[i]}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4-Button FSRS Rating Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FsrsButton(label = "Again", sublabel = "1", color = Color(0xFFEF5350), modifier = Modifier.weight(1f)) {
                    onGradeCard(card.id, 1)
                    isRevealed = false
                    isFloorMode = false
                }
                FsrsButton(label = "Hard", sublabel = "2", color = Color(0xFFFFA726), modifier = Modifier.weight(1f)) {
                    onGradeCard(card.id, 2)
                    isRevealed = false
                    isFloorMode = false
                }
                FsrsButton(label = "Good", sublabel = "3", color = Color(0xFF26A69A), modifier = Modifier.weight(1f)) {
                    onGradeCard(card.id, 3)
                    isRevealed = false
                    isFloorMode = false
                }
                FsrsButton(label = "Easy", sublabel = "4", color = Color(0xFF66BB6A), modifier = Modifier.weight(1f)) {
                    onGradeCard(card.id, 4)
                    isRevealed = false
                    isFloorMode = false
                }
            }
        }
    }
}

@Composable
fun FsrsButton(label: String, sublabel: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Text(sublabel, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

private fun parseCheckpoints(rubricJson: String?): List<String> {
    if (rubricJson.isNullOrBlank()) return emptyList()
    return try {
        val obj = JSONObject(rubricJson)
        val arr = obj.optJSONArray("checkpoints") ?: return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        list
    } catch (_: Exception) {
        emptyList()
    }
}
