package com.mistavinya.smac.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import java.util.UUID

object DeviceInfoUtil {

    private const val TAG = "DeviceInfoUtil"

    /**
     * Get a unique device identifier.
     * Priority:
     * 1. Build.getSerial() (Android 8-9 only)
     * 2. ANDROID_ID (unique per device + user combination)
     * 3. Fallback: Generate a UUID and store it in SharedPreferences
     */
    fun getSerialNumber(context: Context): String {
        return try {
            // Option 1: Try Build.getSerial() (works on Android 8-9, fails on 10+)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @SuppressLint("MissingPermission")
                val serial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try { Build.getSerial() } catch (e: SecurityException) { null }
                } else {
                    @Suppress("DEPRECATION")
                    Build.SERIAL
                }
                if (serial != null && serial != "unknown" && serial != Build.UNKNOWN) {
                    Log.d(TAG, "Got serial from Build: $serial")
                    return serial
                }
            }

            getSafeAndroidId(context)

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting serial: ${e.message}")
            getFallbackSerial(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting serial: ${e.message}")
            getFallbackSerial(context)
        }
    }

    private fun getFallbackSerial(context: Context): String {
        val prefs = context.getSharedPreferences("device_info", Context.MODE_PRIVATE)
        var storedId = prefs.getString("device_serial", null)
        if (storedId == null) {
            storedId = UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
            prefs.edit().putString("device_serial", storedId).apply()
            Log.d(TAG, "Generated new device serial: $storedId")
        }
        return storedId!!
    }

    /**
     * Get device IMEI or best alternative identifier.
     * 
     * On Android 10+, IMEI is not accessible to third-party apps.
     * We use a combination of identifiers to create a unique device ID.
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    fun getImei(context: Context): String {
        return try {
            // Android 8-9: Can get real IMEI
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try { telephonyManager.imei } catch (e: SecurityException) { null }
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.deviceId
                }
                if (imei != null && imei.isNotEmpty() && imei != "000000000000000") {
                    Log.d(TAG, "Got real IMEI: $imei")
                    return imei
                }
            }

            // Fallback: Use ANDROID_ID as IMEI substitute
            val androidId = getSafeAndroidId(context)
            // Convert ANDROID_ID to a 15-digit numeric string (IMEI-like format)
            val numericId = androidId.filter { it.isDigit() }.padEnd(15, '0').take(15)
            Log.d(TAG, "Using ANDROID_ID-based IMEI: $numericId")
            numericId

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting IMEI: ${e.message}")
            getFallbackImei(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IMEI: ${e.message}")
            getFallbackImei(context)
        }
    }

    private fun getSafeAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getFallbackImei(context: Context): String {
        val prefs = context.getSharedPreferences("device_info", Context.MODE_PRIVATE)
        var storedImei = prefs.getString("device_imei", null)
        if (storedImei == null) {
            val androidId = getSafeAndroidId(context)
            storedImei = if (androidId != "Unknown") {
                androidId.hashCode().toLong().let { 
                    Math.abs(it).toString().padEnd(15, '0').take(15) 
                }
            } else {
                generateRandomImei()
            }
            prefs.edit().putString("device_imei", storedImei).apply()
        }
        return storedImei!!
    }

    private fun generateRandomImei(): String {
        val random = java.util.Random()
        val sb = StringBuilder()
        repeat(15) { sb.append(random.nextInt(10)) }
        return sb.toString()
    }

    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    fun getAndroidVersion(): String {
        return Build.VERSION.RELEASE
    }
}
