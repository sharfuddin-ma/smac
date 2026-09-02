package com.mistavinya.smac.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsDataStore(private val context: Context) {
    companion object {
        private val KEY_AUTO_RECORD = booleanPreferencesKey("auto_record")
        private val KEY_CLASSIFICATION_TIMEOUT = intPreferencesKey("classification_timeout")
        private val KEY_DEFAULT_CATEGORY = stringPreferencesKey("default_category")
    }

    val autoRecordEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_RECORD] ?: true }
    val classificationTimeout: Flow<Int> = context.dataStore.data.map { it[KEY_CLASSIFICATION_TIMEOUT] ?: 60 }
    val defaultCategory: Flow<String> = context.dataStore.data.map { it[KEY_DEFAULT_CATEGORY] ?: "client" }

    suspend fun setAutoRecord(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_RECORD] = enabled }
    }

    suspend fun setClassificationTimeout(seconds: Int) {
        context.dataStore.edit { it[KEY_CLASSIFICATION_TIMEOUT] = seconds }
    }

    suspend fun setDefaultCategory(category: String) {
        context.dataStore.edit { it[KEY_DEFAULT_CATEGORY] = category }
    }
}
