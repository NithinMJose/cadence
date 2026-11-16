package com.cadence.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cadence.android.data.local.entity.LocalTopicCard
import com.cadence.android.data.remote.FeynmanGradeResponseDto

@Composable
fun FeynmanReviewDialog(
    card: LocalTopicCard,
    checkpoints: List<String>,
    isGrading: Boolean,
    gradeResult: FeynmanGradeResponseDto?,
    onDismiss: () -> Unit,
    onSubmit: (transcript: String) -> Unit
) {
    var transcript by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "🎙️ Feynman Voice Dojo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Explain '${card.title}' in plain terms:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (gradeResult == null) {
                    OutlinedTextField(
                        value = transcript,
                        onValueChange = { transcript = it },
                        label = { Text("Spoken or Typed Explanation") },
                        placeholder = { Text("Explain key mechanisms, edge cases, invariants...") },
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isGrading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grading with local LLM...", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { if (transcript.isNotBlank()) onSubmit(transcript) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = transcript.isNotBlank()
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit for Feynman Grading")
                        }
                    }
                } else {
                    val gradeColor = when (gradeResult.fsrsGrade) {
                        4 -> Color(0xFF66BB6A)
                        3 -> Color(0xFF26A69A)
                        2 -> Color(0xFFFFA726)
                        else -> Color(0xFFEF5350)
                    }
                    val gradeLabel = when (gradeResult.fsrsGrade) {
                        4 -> "Easy (Grade 4)"
                        3 -> "Good (Grade 3)"
                        2 -> "Hard (Grade 2)"
                        else -> "Again (Grade 1)"
                    }

                    Surface(
                        color = gradeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "FSRS Grade: $gradeLabel",
                            color = gradeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = gradeResult.feedback,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Checkpoints Breakdown:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    gradeResult.matchedCheckpoints.forEach { cp ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(cp, fontSize = 11.sp, color = Color(0xFF4CAF50))
                        }
                    }
                    gradeResult.missingCheckpoints.forEach { cp ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(cp, fontSize = 11.sp, color = Color(0xFFEF5350))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Continue Review")
                    }
                }
            }
        }
    }
}
