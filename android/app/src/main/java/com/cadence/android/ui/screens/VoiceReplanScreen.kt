package com.cadence.android.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadence.android.data.remote.ReplanProposalResponseDto
import com.cadence.android.data.remote.ScheduleDiffItemRemoteDto
import com.cadence.android.data.remote.TradeoffCardRemoteDto
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun VoiceReplanScreen(
    onProposeReplan: suspend (String) -> Result<ReplanProposalResponseDto>,
    onConfirmReplan: suspend (String) -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    var promptText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentProposal by remember { mutableStateOf<ReplanProposalResponseDto?>(null) }
    var confirmSuccessMessage by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) promptText = spoken
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Natural Language Replan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Speak or type constraints to re-optimize with CP-SAT.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        // Input row with Mic and Send
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = { Text("e.g. Dentist Tuesday 3pm, block 2h") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak schedule constraint...")
                    }
                    speechLauncher.launch(intent)
                }
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick suggestions
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SuggestionChip(onClick = { promptText = "Dentist Tuesday 3pm, block 2 hours" }, label = { Text("Dentist Tue 3pm", fontSize = 11.sp) })
            SuggestionChip(onClick = { promptText = "Exhausted today, lighten schedule to 3m floors" }, label = { Text("Exhausted today", fontSize = 11.sp) })
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                confirmSuccessMessage = null
                coroutineScope.launch {
                    val res = onProposeReplan(promptText)
                    isLoading = false
                    if (res.isSuccess) {
                        currentProposal = res.getOrNull()
                    } else {
                        errorMessage = res.exceptionOrNull()?.message ?: "Failed to generate proposal"
                    }
                }
            },
            enabled = promptText.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compiling Constraints & Solving...")
            } else {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Propose Schedule Diff")
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (confirmSuccessMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(confirmSuccessMessage!!, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Proposal details
        currentProposal?.let { prop ->
            ProposalView(
                proposal = prop,
                onConfirm = {
                    coroutineScope.launch {
                        isLoading = true
                        val res = onConfirmReplan(prop.proposalId)
                        isLoading = false
                        if (res.isSuccess) {
                            confirmSuccessMessage = "✓ Schedule confirmed & synced into local Room snapshot!"
                            currentProposal = null
                            promptText = ""
                        } else {
                            errorMessage = res.exceptionOrNull()?.message ?: "Confirmation failed"
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ProposalView(proposal: ReplanProposalResponseDto, onConfirm: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Schedule Diff & Constraint Explanation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!proposal.astSummary.isNullOrBlank()) {
                Text("AI Summary: ${proposal.astSummary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Text("Status: ${proposal.status} | Churn: ${proposal.churnScore}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(proposal.diffItems) { item ->
                    DiffItemRow(item)
                }
            }

            if (proposal.tradeoffCards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Capacity Tradeoff Cards:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                proposal.tradeoffCards.forEach { card ->
                    Text("• ${card.title}: ${card.impactSummary}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm New Schedule", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DiffItemRow(item: ScheduleDiffItemRemoteDto) {
    val actionColor = when (item.action) {
        "MOVED" -> Color(0xFFFFA000)
        "NEWLY_SCHEDULED" -> Color(0xFF43A047)
        "DOWNGRADED_TO_FLOOR" -> Color(0xFFE53935)
        else -> Color(0xFF757575)
    }
    Surface(color = actionColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(item.action, color = actionColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            if (item.causalReason.isNotBlank()) {
                Text("Reason: ${item.causalReason}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
