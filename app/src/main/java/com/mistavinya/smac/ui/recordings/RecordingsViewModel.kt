package com.mistavinya.smac.ui.recordings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mistavinya.smac.data.CallSyncDatabase
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.data.repository.CallLogRepository
import com.mistavinya.smac.storage.LocalStorageManager
import com.mistavinya.smac.util.AudioPlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackInfo(
    val callLogId: Long = -1,
    val filePath: String = "",
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentPosition: String = "0:00",
    val duration: String = "0:00"
)

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingsViewModel(
    private val repository: CallLogRepository,
    private val storageManager: LocalStorageManager,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val recordings: StateFlow<List<CallLogEntity>> = _selectedFilter.flatMapLatest { filter ->
        repository.getAllSavedRecordings().map { list ->
            when (filter) {
                "Client" -> list.filter { it.category == "client" }
                "Team" -> list.filter { it.category == "team_member" }
                else -> list
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var playbackInfo by mutableStateOf(PlaybackInfo())
        private set

    init {
        startProgressTracker()
    }

    private fun startProgressTracker() {
        viewModelScope.launch {
            while (true) {
                if (playbackInfo.isPlaying) {
                    val pos = audioPlayer.getCurrentPosition()
                    val dur = audioPlayer.getDuration()
                    if (dur > 0) {
                        playbackInfo = playbackInfo.copy(
                            progress = pos.toFloat() / dur.toFloat(),
                            currentPosition = formatMs(pos),
                            duration = formatMs(dur)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    fun updateFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun playRecording(call: CallLogEntity) {
        if (playbackInfo.callLogId == call.id && !playbackInfo.isPlaying) {
            audioPlayer.resume()
            playbackInfo = playbackInfo.copy(isPlaying = true)
        } else {
            audioPlayer.play(call.recordingFilePath) {
                playbackInfo = playbackInfo.copy(isPlaying = false, progress = 0f)
            }
            playbackInfo = PlaybackInfo(
                callLogId = call.id,
                filePath = call.recordingFilePath,
                isPlaying = true,
                duration = formatMs(audioPlayer.getDuration())
            )
        }
    }

    fun pauseRecording() {
        audioPlayer.pause()
        playbackInfo = playbackInfo.copy(isPlaying = false)
    }

    fun stopPlayback() {
        audioPlayer.stop()
        playbackInfo = PlaybackInfo()
    }

    fun deleteRecording(call: CallLogEntity) {
        viewModelScope.launch {
            if (playbackInfo.callLogId == call.id) stopPlayback()
            storageManager.deleteRecording(call.recordingFilePath)
            repository.delete(call)
        }
    }

    private fun formatMs(ms: Int): String {
        val seconds = ms / 1000
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%d:%02d", m, s)
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}

class RecordingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = CallSyncDatabase.getInstance(context)
        val repo = CallLogRepository(db.callLogDao())
        val storage = LocalStorageManager(context)
        val player = AudioPlayer(context)
        @Suppress("UNCHECKED_CAST")
        return RecordingsViewModel(repo, storage, player) as T
    }
}
