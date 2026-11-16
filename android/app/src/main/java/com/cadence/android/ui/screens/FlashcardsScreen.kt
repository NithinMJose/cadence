package com.cadence.android.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
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
import com.cadence.android.data.remote.FeynmanGradeResponseDto
import com.cadence.android.ui.components.FeynmanReviewDialog
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun FlashcardsScreen(
    dueCards: List<LocalTopicCard>,
    onGradeCard: (cardId: String, grade: Int) -> Unit,
    onFeynmanGrade: suspend (cardId: String, transcript: String) -> Result<FeynmanGradeResponseDto>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isRevealed by remember { mutableStateOf(false) }
    var isFloorMode by remember { mutableStateOf(false) }
    var showFeynmanDialog by remember { mutableStateOf(false) }
    var isFeynmanGrading by remember { mutableStateOf(false) }
    var feynmanResult by remember { mutableStateOf<FeynmanGradeResponseDto?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(dueCards.size) {
        if (currentIndex >= dueCards.size) {
            currentIndex = 0
            isRevealed = false
            isFloorMode = false
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
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
                        "FSRS-4.5 interval scheduling has placed all concepts into optimal memory retention.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            val card = dueCards.getOrNull(currentIndex) ?: return
            val checkpoints = remember(card.rubricJson) { parseCheckpoints(card.rubricJson) }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth().weight(1f).clickable { isRevealed = !isRevealed }.animateContentSize(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.topicTag.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        FilterChip(
                            selected = isFloorMode,
                            onClick = { isFloorMode = !isFloorMode },
                            label = { Text("⚡ 3m Floor", fontSize = 11.sp) },
                            leadingIcon = if (isFloorMode) { { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp)) } } else null
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = card.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isFloorMode) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Emergency Floor Micro-Quiz:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(card.emergencyFloorPrompt, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            // Feynman Voice Mode Button
            OutlinedButton(
                onClick = { feynmanResult = null; showFeynmanDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("🎙️ Explain Like Feynman (60s Voice Grade)", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4-Button FSRS Rating Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Again", "1", Color(0xFFEF5350)) to 1,
                    Triple("Hard", "2", Color(0xFFFFA726)) to 2,
                    Triple("Good", "3", Color(0xFF26A69A)) to 3,
                    Triple("Easy", "4", Color(0xFF66BB6A)) to 4
                ).forEach { (meta, grade) ->
                    FsrsButton(meta.first, meta.second, meta.third, Modifier.weight(1f)) {
                        onGradeCard(card.id, grade); isRevealed = false; isFloorMode = false
                    }
                }
            }

            if (showFeynmanDialog) {
                FeynmanReviewDialog(
                    card = card,
                    checkpoints = checkpoints,
                    isGrading = isFeynmanGrading,
                    gradeResult = feynmanResult,
                    onDismiss = {
                        showFeynmanDialog = false
                        if (feynmanResult != null) {
                            isRevealed = false
                            isFloorMode = false
                            currentIndex = if (currentIndex + 1 < dueCards.size) currentIndex + 1 else 0
                        }
                    },
                    onSubmit = { transcript ->
                        scope.launch {
                            isFeynmanGrading = true
                            val res = onFeynmanGrade(card.id, transcript)
                            isFeynmanGrading = false
                            feynmanResult = res.getOrNull()
                        }
                    }
                )
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

private fun parseCheckpoints(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = if (json.trim().startsWith("[")) org.json.JSONArray(json)
        else JSONObject(json).optJSONArray("checkpoints") ?: return emptyList()
        List(arr.length()) { arr.getString(it) }
    } catch (_: Exception) { emptyList() }
}
