package com.mistavinya.smac.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.ui.components.AudioPlayerBar
import com.mistavinya.smac.util.DateUtils
import com.mistavinya.smac.util.DurationUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    navController: NavController,
    viewModel: CallHistoryViewModel = viewModel(factory = CallHistoryViewModelFactory(LocalContext.current))
) {
    val calls by viewModel.calls.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val playbackInfo = viewModel.playbackInfo
    
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(calls) {
        delay(300)
        isLoading = false
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCallForDetails by remember { mutableStateOf<CallLogEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<CallLogEntity?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Call History", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                    actions = {
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                AnimatedVisibility(visible = isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        placeholder = { Text("Search by name or number...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                }
            }
        },
        bottomBar = {
            if (playbackInfo.callLogId != -1L) {
                AudioPlayerBar(
                    playbackInfo = playbackInfo,
                    onPlayPause = {
                        if (playbackInfo.isPlaying) viewModel.pauseRecording()
                        else viewModel.playRecording(calls.find { it.id == playbackInfo.callLogId }!!)
                    },
                    onClose = { viewModel.stopPlayback() }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            FilterChipsSection(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.updateFilter(it) }
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                }
            } else if (calls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.Assignment, 
                            null, 
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No call history available.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(calls) { call ->
                        CallHistoryItemCard(
                            call = call,
                            onClick = { selectedCallForDetails = call }
                        )
                    }
                }
            }
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
fun FilterChipsSection(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HistoryFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { 
                    Text(
                        filter.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun CallHistoryItemCard(call: CallLogEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initials = call.contactName?.split(" ")
                ?.filter { it.isNotEmpty() }
                ?.take(2)
                ?.map { it[0] }
                ?.joinToString("") ?: "?"

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials.uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = call.contactName ?: call.phoneNumber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (call.storageStatus == "saved") {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Saved",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${call.phoneNumber} • ${DurationUtils.formatDuration(call.durationSeconds)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = DateUtils.formatRelativeTime(call.createdAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, color) = when (call.callType) {
                        "outgoing" -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF1976D2)
                        "incoming" -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF4CAF50)
                        "missed" -> Icons.AutoMirrored.Filled.PhoneMissed to Color(0xFFEF5350)
                        else -> Icons.Default.Phone to Color.Gray
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = color
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    HistoryCategoryBadge(call.category ?: "unclassified")
                }
            }
        }
    }
}

@Composable
fun HistoryCategoryBadge(category: String) {
    val isDark = isSystemInDarkTheme()
    val (bgColor, textColor, text) = when (category.lowercase()) {
        "client" -> Triple(
            if (isDark) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE8F5E9),
            if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
            "Client"
        )
        "team_member" -> Triple(
            if (isDark) Color(0xFF0D47A1).copy(alpha = 0.2f) else Color(0xFFE3F2FD),
            if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
            "Team"
        )
        else -> Triple(
            if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5),
            if (isDark) Color(0xFFB0B0B0) else Color(0xFF757575),
            "Personal"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CallDetailsContent(
    call: CallLogEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(text = "Call Details", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(24.dp))

        DetailRow("NAME", call.contactName ?: call.phoneNumber)
        DetailRow("NUMBER", call.phoneNumber)
        DetailRow("TYPE", call.callType.replaceFirstChar { it.uppercase() })
        DetailRow("DATE", call.date)
        DetailRow("TIME", call.time)
        DetailRow("DURATION", DurationUtils.formatDuration(call.durationSeconds))
        DetailRow("CATEGORY", (call.category ?: "Unclassified").uppercase())
        if (call.notes != null) DetailRow("NOTES", call.notes)

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (call.recordingFilePath.isNotEmpty()) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Recording", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = label, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value, 
            fontWeight = FontWeight.Medium, 
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
