package com.mistavinya.smac.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mistavinya.smac.storage.LocalStorageManager
import com.mistavinya.smac.util.SettingsDataStore
import com.mistavinya.smac.util.ThemePreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val themePreferences: ThemePreferences,
    private val storageManager: LocalStorageManager,
    private val storagePath: String
) : ViewModel() {

    val isDarkMode = themePreferences.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val classificationTimeout = settingsDataStore.classificationTimeout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    val defaultCategory = settingsDataStore.defaultCategory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "client")

    private val _storageUsed = MutableStateFlow(storageManager.getStorageUsedFormatted())
    val storageUsed = _storageUsed.asStateFlow()

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDarkMode(enabled) }
    }

    fun updateClassificationTimeout(seconds: Int) {
        viewModelScope.launch { settingsDataStore.setClassificationTimeout(seconds) }
    }

    fun updateDefaultCategory(category: String) {
        viewModelScope.launch { settingsDataStore.setDefaultCategory(category) }
    }

    fun clearOldRecordings(days: Int) {
        viewModelScope.launch {
            storageManager.clearOldRecordings(days)
            _storageUsed.value = storageManager.getStorageUsedFormatted()
        }
    }
    
    fun getStoragePath(): String = storagePath
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val settings = SettingsDataStore(context)
            val theme = ThemePreferences(context)
            val storage = LocalStorageManager(context)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                settings, 
                theme, 
                storage, 
                context.filesDir.absolutePath + "/CallSync"
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
