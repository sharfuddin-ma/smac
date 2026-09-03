package com.mistavinya.smac.ui.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.ui.components.AudioPlayerBar
import com.mistavinya.smac.ui.history.CallDetailsContent
import com.mistavinya.smac.ui.navigation.Screen
import com.mistavinya.smac.util.DateUtils
import com.mistavinya.smac.util.DurationUtils
import com.mistavinya.smac.util.SettingsDataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val todayCount by viewModel.todayCallCount.collectAsState()
    val savedCount by viewModel.savedRecordingsCount.collectAsState()
    val totalCount by viewModel.totalRecordingsCount.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    val playbackInfo = viewModel.playbackInfo

    val settingsDataStore = remember { SettingsDataStore(context) }
    val devicePhone1 by settingsDataStore.phoneNumber1.collectAsState(initial = "")
    val devicePhone2 by settingsDataStore.phoneNumber2.collectAsState(initial = "")

    var selectedCallForDetails by remember { mutableStateOf<CallLogEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<CallLogEntity?>(null) }
    val sheetState = rememberModalBottomSheetState()

    BackHandler {
        (context as? Activity)?.finish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SalesEdgeAI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "MA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (playbackInfo.callLogIdString.isNotEmpty()) {
                AudioPlayerBar(
                    playbackInfo = playbackInfo,
                    onPlayPause = {
                        if (playbackInfo.isPlaying) viewModel.pauseRecording()
                        else {
                            val call = recentCalls.find { it.id == playbackInfo.callLogIdString }
                            if (call != null) viewModel.playRecording(call)
                        }
                    },
                    onClose = { viewModel.stopPlayback() }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            
            // Status Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B5E20).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = Color(0xFF4CAF50)
                        ) {}
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Active",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                "All calls are being recorded",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50).copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }
            
            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = todayCount.toString(),
                        label = "Today"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = savedCount.toString(),
                        label = "Saved"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = totalCount.toString(),
                        label = "Total"
                    )
                }
            }
            
            // Recent Calls Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Calls",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { navController.navigate(Screen.CallHistory.route) }) {
                        Text("View All", fontSize = 13.sp)
                    }
                }
            }
            
            // Call List
            if (recentCalls.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No calls recorded yet",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Make a call to get started",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(recentCalls) { call ->
                    CallItemCard(
                        callLog = call,
                        devicePhone1 = devicePhone1,
                        devicePhone2 = devicePhone2,
                        onClick = { selectedCallForDetails = call }
                    )
                }
            }
            
            item { Spacer(Modifier.height(16.dp)) }
        }

        if (selectedCallForDetails != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedCallForDetails = null },
                sheetState = sheetState
            ) {
                CallDetailsContent(
                    call = selectedCallForDetails!!,
                    getFormData = { viewModel.getFormData(it) },
                    onPlay = { 
                        viewModel.playRecording(selectedCallForDetails!!)
                        selectedCallForDetails = null 
                    },
                    onDelete = {
                        showDeleteConfirmation = selectedCallForDetails
                        selectedCallForDetails = null
                    }
                )
            }
        }

        if (showDeleteConfirmation != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                title = { Text("Delete Log?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                text = { Text("Are you sure you want to delete this call log and recording?", fontSize = 14.sp) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteCall(showDeleteConfirmation!!)
                        showDeleteConfirmation = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = null }) {
                        Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CallItemCard(
    callLog: CallLogEntity, 
    devicePhone1: String,
    devicePhone2: String,
    onClick: () -> Unit
) {
    val displayNumber = getDisplayNumber(callLog, devicePhone1, devicePhone2)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = when (callLog.callDirection) {
                    "OUTGOING" -> Color(0xFF1976D2).copy(alpha = 0.1f)
                    "INCOMING" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    "MISSED" -> Color(0xFFEF5350).copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (callLog.callDirection) {
                            "OUTGOING" -> Icons.AutoMirrored.Filled.CallMade
                            "INCOMING" -> Icons.AutoMirrored.Filled.CallReceived
                            "MISSED" -> Icons.Default.PhoneMissed
                            else -> Icons.Default.Phone
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = when (callLog.callDirection) {
                            "OUTGOING" -> Color(0xFF1976D2)
                            "INCOMING" -> Color(0xFF4CAF50)
                            "MISSED" -> Color(0xFFEF5350)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    callLog.contactName ?: displayNumber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(when (callLog.callDirection) {
                            "OUTGOING" -> "↗ Outgoing"
                            "INCOMING" -> "↙ Incoming"
                            "MISSED"   -> "✕ Missed"
                            else -> callLog.callDirection.lowercase().replaceFirstChar { it.uppercase() }
                        })
                        if (callLog.durationSeconds > 0) {
                            append(" · ${DurationUtils.formatDuration(callLog.durationSeconds)}")
                        }
                        if (displayNumber.isNotEmpty()) {
                            append(" · $displayNumber")
                        }
                    },
                    fontSize = 13.sp,
                    color = when (callLog.callDirection) {
                        "OUTGOING" -> MaterialTheme.colorScheme.primary
                        "INCOMING" -> Color(0xFF4CAF50)
                        "MISSED"   -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    DateUtils.formatRelativeTime(callLog.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (callLog.callCategory != "PENDING") {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (callLog.callCategory) {
                            "CLIENT" -> Color(0xFF1976D2).copy(alpha = 0.1f)
                            "TEAM_MEMBER" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            when (callLog.callCategory) {
                                "CLIENT" -> "Client"
                                "TEAM_MEMBER" -> "Team"
                                "PERSONAL" -> "Personal"
                                "MISSED" -> "Missed"
                                else -> ""
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (callLog.callCategory) {
                                "CLIENT" -> Color(0xFF1976D2)
                                "TEAM_MEMBER" -> Color(0xFF4CAF50)
                                "MISSED" -> Color(0xFFD32F2F)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

fun getDisplayNumber(callLog: CallLogEntity, devicePhone1: String, devicePhone2: String): String {
    return when (callLog.callDirection) {
        "OUTGOING" -> callLog.calleeNumber
        "INCOMING", "MISSED" -> callLog.callerNumber
        else -> {
            when {
                callLog.callerNumber == devicePhone1 || callLog.callerNumber == devicePhone2 -> callLog.calleeNumber
                callLog.calleeNumber == devicePhone1 || callLog.calleeNumber == devicePhone2 -> callLog.callerNumber
                else -> callLog.callerNumber
            }
        }
    }
}
