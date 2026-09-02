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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(LocalContext.current))
) {
    val todayCount by viewModel.todayCallCount.collectAsState()
    val savedCount by viewModel.savedRecordingsCount.collectAsState()
    val totalCount by viewModel.totalRecordingsCount.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState(initial = emptyList())
    val playbackInfo = viewModel.playbackInfo

    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }
    var selectedCallForDetails by remember { mutableStateOf<CallLogEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<CallLogEntity?>(null) }
    val sheetState = rememberModalBottomSheetState()

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
            text = { Text("Are you sure you want to close CallSync?", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { (context as? Activity)?.finish() }) {
                    Text("Exit", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CallSync",
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
            if (playbackInfo.callLogId != -1L) {
                AudioPlayerBar(
                    playbackInfo = playbackInfo,
                    onPlayPause = {
                        if (playbackInfo.isPlaying) viewModel.pauseRecording()
                        else viewModel.playRecording(recentCalls.find { it.id == playbackInfo.callLogId }!!)
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
                        // Green dot indicator
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

            // Recording Tip
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "Tip: For best recording quality, use speaker mode during calls.",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
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
fun CallItemCard(callLog: CallLogEntity, onClick: () -> Unit) {
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
            // Call type icon
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = when (callLog.callType) {
                    "outgoing" -> Color(0xFF1976D2).copy(alpha = 0.1f)
                    "incoming" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    "missed" -> Color(0xFFEF5350).copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (callLog.callType) {
                            "outgoing" -> Icons.AutoMirrored.Filled.CallMade
                            "incoming" -> Icons.AutoMirrored.Filled.CallReceived
                            "missed" -> Icons.Default.PhoneMissed
                            else -> Icons.Default.Phone
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = when (callLog.callType) {
                            "outgoing" -> Color(0xFF1976D2)
                            "incoming" -> Color(0xFF4CAF50)
                            "missed" -> Color(0xFFEF5350)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            // Name and number
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    callLog.contactName ?: callLog.phoneNumber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(callLog.callType.replaceFirstChar { it.uppercase() })
                        if (callLog.durationSeconds > 0) {
                            append(" · ${DurationUtils.formatDuration(callLog.durationSeconds)}")
                        }
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Time and category
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    DateUtils.formatRelativeTime(callLog.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (callLog.category != null) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (callLog.category) {
                            "client" -> Color(0xFF1976D2).copy(alpha = 0.1f)
                            "team_member" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            when (callLog.category) {
                                "client" -> "Client"
                                "team_member" -> "Team"
                                "personal" -> "Personal"
                                else -> ""
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (callLog.category) {
                                "client" -> Color(0xFF1976D2)
                                "team_member" -> Color(0xFF4CAF50)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
