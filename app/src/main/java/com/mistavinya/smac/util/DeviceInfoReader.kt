package com.mistavinya.smac.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

object DeviceInfoReader {

    private const val TAG = "DeviceInfoReader"

    data class DeviceInfo(
        val serialNumber: String,
        val imei1: String,
        val imei2: String,
        val phoneNumber1: String,
        val phoneNumber2: String,
        val deviceModel: String,
        val androidVersion: String
    )

    @SuppressLint("MissingPermission", "HardwareIds")
    fun readAll(context: Context): DeviceInfo {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "READING DEVICE INFO")
        Log.i(TAG, "═══════════════════════════════════")

        // Serial Number
        val serial = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.getSerial()
            } else {
                @Suppress("DEPRECATION")
                Build.SERIAL
            }
        } catch (e: Exception) {
            Log.e(TAG, "Serial error: ${e.message}")
            ""
        }
        Log.i(TAG, "  Serial: $serial")

        // IMEI
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val imei1 = try { 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tm.getImei(0) ?: "" 
            else @Suppress("DEPRECATION") tm.deviceId ?: ""
        } catch (e: Exception) { "" }
        
        val imei2 = try { 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tm.getImei(1) ?: "" 
            else ""
        } catch (e: Exception) { "" }
        
        Log.i(TAG, "  IMEI 1: $imei1")
        Log.i(TAG, "  IMEI 2: $imei2")

        // Phone Numbers
        var phone1 = ""
        var phone2 = ""

        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subs = sm.activeSubscriptionInfoList

            if (subs != null) {
                for (sub in subs) {
                    var number = ""

                    // Android 13+ API
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try { number = sm.getPhoneNumber(sub.subscriptionId) ?: "" } catch (_: Exception) {}
                    }

                    // Fallback: subInfo.number
                    if (number.isBlank()) {
                        @Suppress("DEPRECATION")
                        number = sub.number ?: ""
                    }

                    // Fallback: TelephonyManager.line1Number
                    if (number.isBlank()) {
                        try {
                            number = tm.createForSubscriptionId(sub.subscriptionId).line1Number ?: ""
                        } catch (_: Exception) {}
                    }

                    number = number.trim()

                    when (sub.simSlotIndex) {
                        0 -> phone1 = number
                        1 -> phone2 = number
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Phone number error: ${e.message}")
        }

        Log.i(TAG, "  Phone 1: ${phone1.ifBlank { "NOT AVAILABLE" }}")
        Log.i(TAG, "  Phone 2: ${phone2.ifBlank { "NOT AVAILABLE" }}")
        Log.i(TAG, "  Model: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.i(TAG, "  Android: ${Build.VERSION.RELEASE}")
        Log.i(TAG, "═══════════════════════════════════")

        return DeviceInfo(
            serialNumber = serial ?: "",
            imei1 = imei1,
            imei2 = imei2,
            phoneNumber1 = phone1,
            phoneNumber2 = phone2,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = Build.VERSION.RELEASE
        )
    }

    /**
     * Read all device info and save to DataStore.
     * Call this once in CallSyncApp.onCreate() after permissions are granted.
     */
    suspend fun readAndSave(context: Context): DeviceInfo {
        val info = readAll(context)
        val store = SettingsDataStore(context)

        if (info.serialNumber.isNotBlank()) store.setSerialNumber(info.serialNumber)
        if (info.imei1.isNotBlank()) store.setImei1(info.imei1)
        if (info.imei2.isNotBlank()) store.setImei2(info.imei2)
        if (info.phoneNumber1.isNotBlank()) store.setPhoneNumber1(info.phoneNumber1)
        if (info.phoneNumber2.isNotBlank()) store.setPhoneNumber2(info.phoneNumber2)
        store.setDeviceModel(info.deviceModel)
        store.setAndroidVersion(info.androidVersion)

        Log.i(TAG, "✅ All device info saved to DataStore")
        return info
    }
}
