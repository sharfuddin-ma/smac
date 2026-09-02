package com.mistavinya.smac.ui.history

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
import com.mistavinya.smac.ui.recordings.PlaybackInfo
import com.mistavinya.smac.util.AudioPlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class HistoryFilter {
    ALL, CLIENT, TEAM, PERSONAL, MISSED, SAVED
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CallHistoryViewModel(
    private val repository: CallLogRepository,
    private val storageManager: LocalStorageManager,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    val calls: StateFlow<List<CallLogEntity>> = combine(
        _selectedFilter,
        _searchQuery.debounce(300)
    ) { filter, query ->
        filter to query
    }.flatMapLatest { (filter, query) ->
        if (query.isNotEmpty()) {
            repository.searchByNameOrNumber(query)
        } else {
            when (filter) {
                HistoryFilter.ALL -> repository.getAll()
                HistoryFilter.CLIENT -> repository.getByCategory("client")
                HistoryFilter.TEAM -> repository.getByCategory("team_member")
                HistoryFilter.PERSONAL -> repository.getByCategory("personal")
                HistoryFilter.MISSED -> repository.getByCallType("missed")
                HistoryFilter.SAVED -> repository.getAllSavedRecordings()
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

    private fun formatMs(ms: Int): String {
        val seconds = ms / 1000
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
        if (filter != HistoryFilter.ALL) {
            _searchQuery.value = "" // Clear search when specific filter applied
        }
    }

    fun deleteCall(call: CallLogEntity) {
        viewModelScope.launch {
            if (playbackInfo.callLogId == call.id) stopPlayback()
            storageManager.deleteRecording(call.recordingFilePath)
            repository.delete(call)
        }
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}

class CallHistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallHistoryViewModel::class.java)) {
            val db = CallSyncDatabase.getInstance(context)
            val repo = CallLogRepository(db.callLogDao())
            val storage = LocalStorageManager(context)
            val player = AudioPlayer(context)
            @Suppress("UNCHECKED_CAST")
            return CallHistoryViewModel(repo, storage, player) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
