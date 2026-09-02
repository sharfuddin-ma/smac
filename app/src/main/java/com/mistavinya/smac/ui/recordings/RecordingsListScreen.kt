package com.mistavinya.smac.ui.recordings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.ui.components.AudioPlayerBar
import com.mistavinya.smac.util.DurationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsListScreen(
    navController: NavController,
    viewModel: RecordingsViewModel = viewModel(factory = RecordingsViewModelFactory(LocalContext.current))
) {
    val recordings by viewModel.recordings.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val playbackInfo = viewModel.playbackInfo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Recordings", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (playbackInfo.callLogId != -1L) {
                AudioPlayerBar(
                    playbackInfo = playbackInfo,
                    onPlayPause = {
                        if (playbackInfo.isPlaying) viewModel.pauseRecording()
                        else viewModel.playRecording(recordings.find { it.id == playbackInfo.callLogId }!!)
                    },
                    onClose = { viewModel.stopPlayback() }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            RecordingFilterSection(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.updateFilter(it) }
            )

            if (recordings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No recordings saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recordings) { recording ->
                        RecordingItemCard(
                            recording = recording,
                            isPlaying = playbackInfo.callLogId == recording.id && playbackInfo.isPlaying,
                            onPlay = { viewModel.playRecording(recording) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingFilterSection(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("All", "Client", "Team")
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { 
                    Text(
                        filter,
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
fun RecordingItemCard(
    recording: CallLogEntity,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val fileName = recording.recordingFilePath.substringAfterLast("/")
                Text(
                    text = if (recording.contactName != null) "Recording: ${recording.contactName}" else fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${recording.date} • ${DurationUtils.formatDuration(recording.durationSeconds)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                RecordingCategoryBadge(recording.category ?: "unclassified")
            }

            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RecordingCategoryBadge(category: String) {
    val color = when (category.lowercase()) {
        "client" -> Color(0xFF2E7D32)
        "team_member" -> Color(0xFF1976D2)
        else -> Color(0xFF757575)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = category.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
