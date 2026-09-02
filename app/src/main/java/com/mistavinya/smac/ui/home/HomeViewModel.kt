package com.mistavinya.smac.ui.home

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
import com.mistavinya.smac.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(
    private val callLogRepository: CallLogRepository,
    private val storageManager: LocalStorageManager,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    val todayCallCount = callLogRepository.getTodayCallCount(DateUtils.getCurrentDate())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val savedRecordingsCount = callLogRepository.getAllSavedRecordings()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalRecordingsCount = callLogRepository.getTotalRecordingsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentCalls: Flow<List<CallLogEntity>> = callLogRepository.getRecentCalls(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun deleteCall(call: CallLogEntity) {
        viewModelScope.launch {
            if (playbackInfo.callLogId == call.id) stopPlayback()
            storageManager.deleteRecording(call.recordingFilePath)
            callLogRepository.delete(call)
        }
    }

    private fun formatMs(ms: Int): String {
        val seconds = ms / 1000
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val db = CallSyncDatabase.getInstance(context)
            val callRepo = CallLogRepository(db.callLogDao())
            val storage = LocalStorageManager(context)
            val player = AudioPlayer(context)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(callRepo, storage, player) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
