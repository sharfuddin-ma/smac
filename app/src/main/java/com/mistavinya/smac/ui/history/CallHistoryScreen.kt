package com.mistavinya.smac.ui.history

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.data.entity.CallFormDataEntity
import com.mistavinya.smac.ui.components.AudioPlayerBar
import com.mistavinya.smac.ui.recordings.PlaybackInfo
import com.mistavinya.smac.util.DateUtils
import com.mistavinya.smac.util.DurationUtils
import com.mistavinya.smac.util.SettingsDataStore
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    navController: NavController,
    viewModel: CallHistoryViewModel = viewModel(factory = CallHistoryViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val calls by viewModel.calls.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val playbackInfo = viewModel.playbackInfo
    
    val settingsDataStore = remember { SettingsDataStore(context) }
    val devicePhone1 by settingsDataStore.phoneNumber1.collectAsState(initial = "")
    val devicePhone2 by settingsDataStore.phoneNumber2.collectAsState(initial = "")
    
    var isLoading by remember { mutableStateOf(true) }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(calls) {
        delay(300)
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun playRecording(uriString: String, callId: String) {
        try {
            if (currentlyPlayingId == callId && mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.pause()
                    isPlaying = false
                } else {
                    mediaPlayer!!.start()
                    isPlaying = true
                }
                return
            }

            mediaPlayer?.release()
            val uri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                start()
                setOnCompletionListener {
                    currentlyPlayingId = null
                    isPlaying = false
                }
            }
            currentlyPlayingId = callId
            isPlaying = true
        } catch (e: Exception) {
            currentlyPlayingId = null
            isPlaying = false
        }
    }

    fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingId = null
        isPlaying = false
    }

    var selectedCallForDetails by remember { mutableStateOf<CallLogEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<CallLogEntity?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("SalesEdgeAI", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
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
            if (playbackInfo.callLogIdString.isNotEmpty()) {
                AudioPlayerBar(
                    playbackInfo = playbackInfo,
                    onPlayPause = {
                        if (playbackInfo.isPlaying) viewModel.pauseRecording()
                        else {
                            val call = calls.find { it.id == playbackInfo.callLogIdString }
                            if (call != null) viewModel.playRecording(call)
                        }
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
                    Text("No call history available.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(calls) { call ->
                        CallHistoryItemCard(
                            call = call,
                            devicePhone1 = devicePhone1,
                            devicePhone2 = devicePhone2,
                            currentlyPlayingId = currentlyPlayingId,
                            isPlaying = isPlaying,
                            onPlayPause = { playRecording(call.localRecordingPath!!, call.id) },
                            onStop = { stopPlayback() },
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
                    getFormData = viewModel::getFormData,
                    onPlay = { 
                        if (!selectedCallForDetails!!.localRecordingPath.isNullOrBlank()) {
                            playRecording(selectedCallForDetails!!.localRecordingPath!!, selectedCallForDetails!!.id)
                        }
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
                        filter.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
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
fun CallHistoryItemCard(
    call: CallLogEntity,
    devicePhone1: String,
    devicePhone2: String,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onClick: () -> Unit
) {
    val displayNumber = getDisplayNumber(call, devicePhone1, devicePhone2)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = call.contactName ?: displayNumber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = "$displayNumber • ${DurationUtils.formatDuration(call.durationSeconds)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = when (call.callDirection) {
                            "OUTGOING" -> "↗ Outgoing"
                            "INCOMING" -> "↙ Incoming"
                            "MISSED"   -> "✕ Missed"
                            else -> call.callDirection.lowercase().replaceFirstChar { it.uppercase() }
                        },
                        fontSize = 13.sp,
                        color = when (call.callDirection) {
                            "OUTGOING" -> MaterialTheme.colorScheme.primary
                            "INCOMING" -> Color(0xFF4CAF50)
                            "MISSED"   -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DateUtils.formatRelativeTime(call.createdAt),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    HistoryCategoryBadge(call.callCategory)
                }
            }

            if (call.hasRecording && !call.localRecordingPath.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (currentlyPlayingId == call.id && isPlaying)
                                Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (currentlyPlayingId == call.id && isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (currentlyPlayingId == call.id) {
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = call.recordingFileName ?: "Recording",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (call.recordingFileSizeBytes > 0L) {
                            Text(
                                text = formatFileSize(call.recordingFileSizeBytes),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCategoryBadge(category: String) {
    val isDark = isSystemInDarkTheme()
    val (bgColor, textColor, text) = when (category.uppercase()) {
        "CLIENT" -> Triple(
            if (isDark) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE8F5E9),
            if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
            "Client"
        )
        "TEAM_MEMBER" -> Triple(
            if (isDark) Color(0xFF0D47A1).copy(alpha = 0.2f) else Color(0xFFE3F2FD),
            if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
            "Team"
        )
        "PERSONAL" -> Triple(
            if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5),
            if (isDark) Color(0xFFB0B0B0) else Color(0xFF757575),
            "Personal"
        )
        "MISSED" -> Triple(
            if (isDark) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFFFEBEE),
            if (isDark) Color(0xFFEF9A9A) else Color(0xFFD32F2F),
            "Missed"
        )
        else -> Triple(
            if (isDark) Color(0xFFE65100).copy(alpha = 0.2f) else Color(0xFFFFF3E0),
            if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100),
            "Pending"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 13.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CallDetailsContent(
    call: CallLogEntity,
    getFormData: suspend (String) -> CallFormDataEntity?,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var formData by remember { mutableStateOf<CallFormDataEntity?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    LaunchedEffect(call.id) {
        formData = getFormData(call.id)
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // ═══ SCROLLABLE CONTENT ═══
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(text = "Call Details", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))

            DetailRow("NAME", call.contactName ?: "Unknown")
            DetailRow("CALLER", call.callerNumber)
            DetailRow("CALLEE", call.calleeNumber)
            DetailRow("TYPE", call.callDirection.replaceFirstChar { it.uppercase() })
            DetailRow("DURATION", DurationUtils.formatDuration(call.durationSeconds))
            DetailRow("CATEGORY", call.callCategory.uppercase())

            formData?.let { form ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("FORM DATA", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("COMPANY", form.companyName)
                DetailRow("CUSTOMER", form.customerName)
                form.reasonForCall?.let { if (it.isNotBlank()) DetailRow("REASON", it) }
                form.notes?.let { if (it.isNotBlank()) DetailRow("NOTES", it) }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ═══ FIXED BOTTOM BUTTONS ═══
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play Recording Button
            if (call.hasRecording && !call.localRecordingPath.isNullOrBlank()) {
                Button(
                    onClick = {
                        if (isPlaying) {
                            mediaPlayer?.pause()
                            isPlaying = false
                        } else {
                            try {
                                if (mediaPlayer == null) {
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(context, Uri.parse(call.localRecordingPath!!))
                                        prepare()
                                    }
                                }
                                mediaPlayer?.start()
                                isPlaying = true
                                mediaPlayer?.setOnCompletionListener {
                                    isPlaying = false
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CallDetails", "Playback error: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) "Pause" else "Play Recording",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Delete Button
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Delete",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
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

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
