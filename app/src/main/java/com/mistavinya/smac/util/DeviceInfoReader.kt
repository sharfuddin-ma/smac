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
        val androidVersion: String,
        val source: String = "unknown"
    )

    fun read(context: Context): DeviceInfo {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        
        return when {
            dpm.isDeviceOwnerApp(context.packageName) -> {
                Log.i(TAG, "Path 1: Device Owner")
                readDirectly(context).copy(source = "device_owner")
            }
            MdmConfigReader.hasConfig(context) -> {
                Log.i(TAG, "Path 2: MDM Managed Config")
                val mdmInfo = MdmConfigReader.readConfig(context)
                enrichFromRuntime(context, mdmInfo)
            }
            else -> {
                Log.i(TAG, "Path 3: Runtime Fallback")
                readDirectly(context).copy(source = "runtime_fallback")
            }
        }.also { logResult(it) }
    }

    private fun enrichFromRuntime(context: Context, mdmInfo: DeviceInfo): DeviceInfo {
        if (!PermissionUtils.areRuntimePermissionsGranted(context)) return mdmInfo
        
        val directInfo = readDirectly(context)
        return mdmInfo.copy(
            phoneNumber1 = mdmInfo.phoneNumber1.ifBlank { directInfo.phoneNumber1 },
            phoneNumber2 = mdmInfo.phoneNumber2.ifBlank { directInfo.phoneNumber2 }
        )
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun readDirectly(context: Context): DeviceInfo {
        // Serial Number
        val serial = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.getSerial()
            } else {
                @Suppress("DEPRECATION")
                Build.SERIAL
            }
        } catch (e: Exception) { "" }

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

        // Phone Numbers
        var phone1 = ""
        var phone2 = ""

        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subs = sm.activeSubscriptionInfoList

            if (subs != null) {
                for (sub in subs) {
                    var number = ""
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try { number = sm.getPhoneNumber(sub.subscriptionId) ?: "" } catch (_: Exception) {}
                    }
                    if (number.isBlank()) {
                        @Suppress("DEPRECATION")
                        number = sub.number ?: ""
                    }
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

    private fun logResult(info: DeviceInfo) {
        Log.i(TAG, "Final Device Info (Source: ${info.source})")
        Log.i(TAG, "  Serial: ${info.serialNumber}")
        Log.i(TAG, "  IMEI 1: ${info.imei1}")
        Log.i(TAG, "  Phone 1: ${info.phoneNumber1}")
    }

    suspend fun readAndSave(context: Context): DeviceInfo {
        val info = read(context)
        val store = SettingsDataStore(context)

        if (info.serialNumber.isNotBlank()) store.setSerialNumber(info.serialNumber)
        if (info.imei1.isNotBlank()) store.setImei1(info.imei1)
        if (info.imei2.isNotBlank()) store.setImei2(info.imei2)
        if (info.phoneNumber1.isNotBlank()) store.setPhoneNumber1(info.phoneNumber1)
        if (info.phoneNumber2.isNotBlank()) store.setPhoneNumber2(info.phoneNumber2)
        store.setDeviceModel(info.deviceModel)
        store.setAndroidVersion(info.androidVersion)
        store.setDeviceInfoSource(info.source)

        return info
    }
}
