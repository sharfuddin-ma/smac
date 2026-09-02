package com.mistavinya.smac.ui.classification

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mistavinya.smac.data.CallSyncDatabase
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.data.repository.CallLogRepository
import com.mistavinya.smac.storage.LocalStorageManager
import com.mistavinya.smac.util.SettingsDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

sealed class ClassificationEvent {
    object Finished : ClassificationEvent()
    data class NavigateToForm(val id: Long) : ClassificationEvent()
}

class ClassificationViewModel(
    private val callLogId: Long,
    private val phoneNumber: String,
    private val repository: CallLogRepository,
    private val storageManager: LocalStorageManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    var countdown by mutableIntStateOf(60)
    var knownContact by mutableStateOf<CallLogEntity?>(null)
    var currentCallLog by mutableStateOf<CallLogEntity?>(null)
    var isLoading by mutableStateOf(true)

    private val _events = MutableSharedFlow<ClassificationEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            countdown = settingsDataStore.classificationTimeout.first()
            currentCallLog = repository.getById(callLogId)
            knownContact = repository.getLastClientCallByNumber(phoneNumber)
            isLoading = false
            startTimer()
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            onClassifyClient()
        }
    }

    fun onClassifyPersonal() {
        viewModelScope.launch {
            currentCallLog?.let { log ->
                storageManager.deleteRecording(log.recordingFilePath)
                repository.deleteById(log.id)
            }
            _events.emit(ClassificationEvent.Finished)
        }
    }

    fun onClassifyTeamMember() {
        viewModelScope.launch {
            currentCallLog?.let { log ->
                val updatedLog = log.copy(category = "team_member")
                repository.update(updatedLog)
                
                val oldFile = File(log.recordingFilePath)
                val newFile = storageManager.moveRecordingToCategory(oldFile, "team_member", log.phoneNumber)
                repository.updateFilePath(log.id, newFile.absolutePath)
                
                storageManager.saveMetadataJson(updatedLog.copy(recordingFilePath = newFile.absolutePath), "team_member")
            }
            _events.emit(ClassificationEvent.Finished)
        }
    }

    fun onClassifyClient() {
        viewModelScope.launch {
            currentCallLog?.let { log ->
                repository.updateCategory(log.id, "client")
                _events.emit(ClassificationEvent.NavigateToForm(log.id))
            } ?: run {
                _events.emit(ClassificationEvent.Finished)
            }
        }
    }

    fun onConfirmKnownContact() {
        viewModelScope.launch {
            val prev = knownContact ?: return@launch
            currentCallLog?.let { log ->
                val updatedLog = log.copy(
                    category = "client",
                    companyName = prev.companyName,
                    contactPersonName = prev.contactPersonName,
                    contactDesignation = prev.contactDesignation,
                    dealName = prev.dealName
                )
                repository.update(updatedLog)
                
                val oldFile = File(log.recordingFilePath)
                val newFile = storageManager.moveRecordingToCategory(oldFile, "client", log.phoneNumber)
                repository.updateFilePath(log.id, newFile.absolutePath)
                
                storageManager.saveMetadataJson(updatedLog.copy(recordingFilePath = newFile.absolutePath), "client")
            }
            _events.emit(ClassificationEvent.Finished)
        }
    }
}

class ClassificationViewModelFactory(
    private val context: Context,
    private val callLogId: Long,
    private val phoneNumber: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = CallSyncDatabase.getInstance(context)
        val repo = CallLogRepository(db.callLogDao())
        val storage = LocalStorageManager(context)
        val settings = SettingsDataStore(context)
        @Suppress("UNCHECKED_CAST")
        return ClassificationViewModel(callLogId, phoneNumber, repo, storage, settings) as T
    }
}
