package com.mistavinya.smac.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsDataStore(private val context: Context) {
    companion object {
        private val KEY_AUTO_RECORD = booleanPreferencesKey("auto_record")
        private val KEY_CLASSIFICATION_TIMEOUT = intPreferencesKey("classification_timeout")
        private val KEY_DEFAULT_CATEGORY = stringPreferencesKey("default_category")
        private val RECORDING_FOLDER_URI = stringPreferencesKey("recording_folder_uri")
        private val KEY_PHONE_NUMBER_1 = stringPreferencesKey("phone_number_1")
        private val KEY_PHONE_NUMBER_2 = stringPreferencesKey("phone_number_2")
        
        private val SERIAL_NUMBER = stringPreferencesKey("serial_number")
        private val IMEI_1 = stringPreferencesKey("imei_1")
        private val IMEI_2 = stringPreferencesKey("imei_2")
        private val DEVICE_MODEL = stringPreferencesKey("device_model")
        private val ANDROID_VERSION = stringPreferencesKey("android_version")
        private val DEVICE_INFO_SOURCE = stringPreferencesKey("device_info_source")
    }

    val autoRecordEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_RECORD] ?: true }
    val classificationTimeout: Flow<Int> = context.dataStore.data.map { it[KEY_CLASSIFICATION_TIMEOUT] ?: 60 }
    val defaultCategory: Flow<String> = context.dataStore.data.map { it[KEY_DEFAULT_CATEGORY] ?: "client" }
    val recordingFolderUri: Flow<String> = context.dataStore.data.map { it[RECORDING_FOLDER_URI] ?: "" }
    val phoneNumber1: Flow<String> = context.dataStore.data.map { it[KEY_PHONE_NUMBER_1] ?: "" }
    val phoneNumber2: Flow<String> = context.dataStore.data.map { it[KEY_PHONE_NUMBER_2] ?: "" }
    
    val serialNumber: Flow<String> = context.dataStore.data.map { it[SERIAL_NUMBER] ?: "" }
    val imei1: Flow<String> = context.dataStore.data.map { it[IMEI_1] ?: "" }
    val imei2: Flow<String> = context.dataStore.data.map { it[IMEI_2] ?: "" }
    val deviceModel: Flow<String> = context.dataStore.data.map { it[DEVICE_MODEL] ?: "" }
    val androidVersion: Flow<String> = context.dataStore.data.map { it[ANDROID_VERSION] ?: "" }
    val deviceInfoSource: Flow<String> = context.dataStore.data.map { it[DEVICE_INFO_SOURCE] ?: "unknown" }

    suspend fun setAutoRecord(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_RECORD] = enabled }
    }

    suspend fun setClassificationTimeout(seconds: Int) {
        context.dataStore.edit { it[KEY_CLASSIFICATION_TIMEOUT] = seconds }
    }

    suspend fun setDefaultCategory(category: String) {
        context.dataStore.edit { it[KEY_DEFAULT_CATEGORY] = category }
    }

    suspend fun setRecordingFolderUri(uri: String) {
        context.dataStore.edit { it[RECORDING_FOLDER_URI] = uri }
    }

    suspend fun getRecordingFolderUri(): String {
        return context.dataStore.data.first()[RECORDING_FOLDER_URI] ?: ""
    }

    suspend fun getPhoneNumber1(): String {
        return context.dataStore.data.first()[KEY_PHONE_NUMBER_1] ?: ""
    }
    
    suspend fun getPhoneNumber2(): String {
        return context.dataStore.data.first()[KEY_PHONE_NUMBER_2] ?: ""
    }
    
    suspend fun getSerialNumber(): String = context.dataStore.data.first()[SERIAL_NUMBER] ?: ""
    suspend fun getImei1(): String = context.dataStore.data.first()[IMEI_1] ?: ""
    suspend fun getImei2(): String = context.dataStore.data.first()[IMEI_2] ?: ""
    suspend fun getDeviceModel(): String = context.dataStore.data.first()[DEVICE_MODEL] ?: ""
    suspend fun getAndroidVersion(): String = context.dataStore.data.first()[ANDROID_VERSION] ?: ""

    suspend fun setPhoneNumber1(value: String) { context.dataStore.edit { it[KEY_PHONE_NUMBER_1] = value } }
    suspend fun setPhoneNumber2(value: String) { context.dataStore.edit { it[KEY_PHONE_NUMBER_2] = value } }
    suspend fun setSerialNumber(value: String) { context.dataStore.edit { it[SERIAL_NUMBER] = value } }
    suspend fun setImei1(value: String) { context.dataStore.edit { it[IMEI_1] = value } }
    suspend fun setImei2(value: String) { context.dataStore.edit { it[IMEI_2] = value } }
    suspend fun setDeviceModel(value: String) { context.dataStore.edit { it[DEVICE_MODEL] = value } }
    suspend fun setAndroidVersion(value: String) { context.dataStore.edit { it[ANDROID_VERSION] = value } }
    suspend fun setDeviceInfoSource(value: String) { context.dataStore.edit { it[DEVICE_INFO_SOURCE] = value } }
}
