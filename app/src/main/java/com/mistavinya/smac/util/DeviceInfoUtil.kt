package com.mistavinya.smac.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

/**
 * DeviceInfoUtil — Device Owner MDM Version
 *
 * This app runs as DEVICE OWNER via MDM, which grants elevated privileges:
 * - Build.getSerial()              → Works on Android 10+ (unlike normal apps)
 * - TelephonyManager.getImei(slot) → Works on Android 10+ (unlike normal apps)
 * - SubscriptionManager            → Full access to SIM info
 *
 * NO FALLBACKS TO ANDROID_ID OR RANDOM UUIDs.
 */
object DeviceInfoUtil {

    private const val TAG = "DeviceInfoUtil"

    data class DeviceInfo(
        val serialNumber: String,
        val imei1: String,
        val imei2: String?,
        val phoneNumber1: String,
        val phoneNumber2: String?,
        val deviceModel: String,
        val androidVersion: String
    )

    fun getAllDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            serialNumber = getSerialNumber(),
            imei1 = getImei(context, 0),
            imei2 = getImei(context, 1),
            phoneNumber1 = getPhoneNumber(context, 0),
            phoneNumber2 = getPhoneNumber(context, 1),
            deviceModel = getDeviceModel(),
            androidVersion = getAndroidVersion()
        )
    }

    @SuppressLint("MissingPermission")
    fun getSerialNumber(): String {
        return try {
            val serial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.getSerial()
            } else {
                @Suppress("DEPRECATION")
                Build.SERIAL
            }
            if (serial.isNullOrBlank() || serial == Build.UNKNOWN || serial == "unknown") {
                Log.e(TAG, "Serial returned unknown — is app set as Device Owner?")
                "UNKNOWN"
            } else {
                Log.d(TAG, "Serial Number: $serial")
                serial
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting serial. Device Owner NOT set! ${e.message}")
            "PERMISSION_DENIED"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting serial: ${e.message}")
            "ERROR"
        }
    }

    @SuppressLint("MissingPermission")
    fun getImei(context: Context, slotIndex: Int = 0): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tm.getImei(slotIndex)
            } else {
                @Suppress("DEPRECATION")
                if (slotIndex == 0) tm.deviceId else null
            }
            if (imei.isNullOrBlank() || imei == "000000000000000") {
                if (slotIndex == 0) {
                    Log.e(TAG, "IMEI slot $slotIndex returned null — is app set as Device Owner?")
                    "UNKNOWN"
                } else {
                    Log.d(TAG, "IMEI slot $slotIndex is null — likely single SIM device")
                    ""
                }
            } else {
                Log.d(TAG, "IMEI slot $slotIndex: $imei")
                imei
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting IMEI slot $slotIndex. Device Owner NOT set! ${e.message}")
            if (slotIndex == 0) "PERMISSION_DENIED" else ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IMEI slot $slotIndex: ${e.message}")
            if (slotIndex == 0) "ERROR" else ""
        }
    }

    @SuppressLint("MissingPermission")
    fun getPhoneNumber(context: Context, slotIndex: Int = 0): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ — Use SubscriptionManager.getPhoneNumber()
                // This is the EXACT approach that works in the dummy project
                val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val subInfoList = sm.activeSubscriptionInfoList

                if (subInfoList != null && slotIndex < subInfoList.size) {
                    val subscriptionId = subInfoList[slotIndex].subscriptionId
                    val number = sm.getPhoneNumber(subscriptionId)
                    if (!number.isNullOrBlank()) {
                        Log.d(TAG, "Phone number slot $slotIndex (getPhoneNumber API): $number")
                        return number
                    }
                } else if (slotIndex == 0) {
                    // Fallback for slot 0: use DEFAULT_SUBSCRIPTION_ID (same as dummy project)
                    val number = sm.getPhoneNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)
                    if (!number.isNullOrBlank()) {
                        Log.d(TAG, "Phone number slot $slotIndex (DEFAULT_SUBSCRIPTION_ID): $number")
                        return number
                    }
                }
            } else {
                // Android < 13 — Use TelephonyManager.line1Number
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                @Suppress("DEPRECATION")
                val line1 = tm.line1Number
                if (!line1.isNullOrBlank()) {
                    Log.d(TAG, "Phone number slot $slotIndex (line1Number): $line1")
                    return line1
                }
            }
            Log.w(TAG, "Phone number slot $slotIndex not available")
            ""
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException slot $slotIndex — missing READ_PHONE_NUMBERS? ${e.message}")
            ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting phone number slot $slotIndex: ${e.message}")
            ""
        }
    }

    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    fun getAndroidVersion(): String = Build.VERSION.RELEASE

    fun logAllDeviceInfo(context: Context) {
        val info = getAllDeviceInfo(context)
        Log.i(TAG, "╔══════════════════════════════════════")
        Log.i(TAG, "║ DEVICE INFO DIAGNOSTIC")
        Log.i(TAG, "╠══════════════════════════════════════")
        Log.i(TAG, "║ Serial Number : ${info.serialNumber}")
        Log.i(TAG, "║ IMEI 1        : ${info.imei1}")
        Log.i(TAG, "║ IMEI 2        : ${info.imei2 ?: "N/A (single SIM)"}")
        Log.i(TAG, "║ Phone Number 1: ${info.phoneNumber1.ifBlank { "Not on SIM" }}")
        Log.i(TAG, "║ Phone Number 2: ${info.phoneNumber2?.ifBlank { "Not on SIM" } ?: "N/A"}")
        Log.i(TAG, "║ Device Model  : ${info.deviceModel}")
        Log.i(TAG, "║ Android       : ${info.androidVersion}")
        Log.i(TAG, "╚══════════════════════════════════════")
        if (info.serialNumber == "PERMISSION_DENIED" || info.imei1 == "PERMISSION_DENIED") {
            Log.e(TAG, "⚠️ DEVICE OWNER IS NOT SET! Run:")
            Log.e(TAG, "   adb shell dpm set-device-owner com.mistavinya.smac/.receiver.DeviceAdminReceiver")
        }
    }
}
