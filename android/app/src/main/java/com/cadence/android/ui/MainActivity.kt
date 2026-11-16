package com.cadence.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.cadence.android.data.remote.NetworkClient
import com.cadence.android.data.repository.CadenceRepository
import com.cadence.android.ui.screens.FlashcardsScreen
import com.cadence.android.ui.screens.SettingsScreen
import com.cadence.android.ui.screens.TodayScreen
import com.cadence.android.ui.screens.VoiceReplanScreen
import com.cadence.android.ui.theme.CadenceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: CadenceRepository

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications disabled. Reminders won't show.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = CadenceRepository(this)
        checkNotificationPermission()

        setContent {
            CadenceTheme {
                CadenceMainApp(repository)
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun CadenceMainApp(repository: CadenceRepository) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    val blocks by repository.blocksFlow.collectAsState(initial = emptyList())
    val snapshot by repository.snapshotFlow.collectAsState(initial = null)
    val pendingCount by repository.pendingCountFlow.collectAsState(initial = 0)
    val dueCards by repository.dueCardsFlow.collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Today") },
                    label = { Text("Today") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Cards") },
                    label = { Text("Cards (${dueCards.size})") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Replan") },
                    label = { Text("Replan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> TodayScreen(
                blocks = blocks,
                snapshot = snapshot,
                pendingCount = pendingCount,
                onCompleteBlock = { blockId ->
                    scope.launch {
                        val res = repository.fastPathRouter.markDone(blockId)
                        actionFeedback = "⚡ ${res.message}"
                    }
                },
                onDowngradeFloor = { blockId ->
                    scope.launch {
                        val res = repository.fastPathRouter.emergencyFloor(blockId)
                        actionFeedback = "⚡ ${res.message}"
                    }
                },
                onSnoozeBlock = { blockId, mins ->
                    scope.launch {
                        val res = repository.fastPathRouter.snooze(blockId, mins)
                        actionFeedback = "⚡ ${res.message}"
                    }
                },
                statusFeedback = actionFeedback,
                modifier = Modifier.padding(innerPadding)
            )
            1 -> FlashcardsScreen(
                dueCards = dueCards,
                onGradeCard = { cardId, grade ->
                    scope.launch {
                        val res = repository.gradeCardOptimistic(cardId, grade)
                        actionFeedback = "⚡ ${res.message}"
                    }
                },
                onFeynmanGrade = { cardId, text ->
                    repository.gradeFeynmanExplanation(cardId, text)
                },
                onRefresh = {
                    scope.launch { repository.syncWithBackend() }
                },
                modifier = Modifier.padding(innerPadding)
            )
            2 -> VoiceReplanScreen(
                onProposeReplan = { prompt -> repository.proposeReplan(prompt) },
                onConfirmReplan = { proposalId -> repository.confirmReplan(proposalId).map { } },
                modifier = Modifier.padding(innerPadding)
            )
            3 -> SettingsScreen(
                onSyncNow = {
                    scope.launch {
                        syncMessage = "Syncing with backend..."
                        val result = repository.syncWithBackend()
                        syncMessage = if (result.isSuccess) {
                            "Synced successfully: ${result.getOrNull()} blocks cached."
                        } else {
                            "Sync failed: ${result.exceptionOrNull()?.message}"
                        }
                    }
                },
                onSeedDemo = {
                    scope.launch {
                        syncMessage = "Seeding demo on backend..."
                        try {
                            NetworkClient.getApiService(context).seedDemo()
                            repository.syncWithBackend()
                            syncMessage = "Demo curriculum seeded & synced!"
                        } catch (ex: Exception) {
                            syncMessage = "Seed failed: ${ex.message}"
                        }
                    }
                },
                syncStatusMessage = syncMessage,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
