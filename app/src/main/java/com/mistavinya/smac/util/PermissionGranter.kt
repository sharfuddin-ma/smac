package com.mistavinya.smac.util

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

object PermissionGranter {

    private const val TAG = "PermissionGranter"

    private fun getRuntimePermissions(): List<Pair<String, String>> {
        return buildList {
            add("Phone" to Manifest.permission.READ_PHONE_STATE)
            add("Call Log" to Manifest.permission.READ_CALL_LOG)
            add("Contacts" to Manifest.permission.READ_CONTACTS)
            add("Microphone" to Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add("Phone Numbers" to Manifest.permission.READ_PHONE_NUMBERS)
                add("Notifications" to Manifest.permission.POST_NOTIFICATIONS)
                add("Storage Audio" to Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add("Storage" to Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    fun grantAllPermissions(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(
            context,
            com.mistavinya.smac.receiver.DeviceAdminReceiver::class.java
        )

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.e(TAG, "❌ NOT Device Owner")
            return false
        }

        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "FORCE GRANTING ALL PERMISSIONS")
        Log.i(TAG, "═══════════════════════════════════")

        var allGranted = true

        // Runtime permissions via DPM
        for ((label, permission) in getRuntimePermissions()) {
            try {
                val result = dpm.setPermissionGrantState(
                    adminComponent, context.packageName, permission,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                )
                val verified = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                Log.i(TAG, "  ${if (result && verified) "✅" else "❌"} $label")
                if (!result || !verified) allGranted = false
            } catch (e: Exception) {
                Log.e(TAG, "  ❌ $label — ${e.message}")
                allGranted = false
            }
        }

        // Overlay via AppOpsManager (special permission — DPM cannot grant it)
        try {
            if (!Settings.canDrawOverlays(context)) {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                try {
                    val setModeMethod = appOps.javaClass.getMethod(
                        "setMode",
                        Int::class.java,
                        Int::class.java,
                        String::class.java,
                        Int::class.java
                    )
                    // 24 is AppOpsManager.OP_SYSTEM_ALERT_WINDOW
                    setModeMethod.invoke(
                        appOps,
                        24,
                        Process.myUid(),
                        context.packageName,
                        AppOpsManager.MODE_ALLOWED
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "  ❌ Failed to invoke setMode via reflection: ${e.message}")
                }
            }
            Log.i(TAG, "  ${if (Settings.canDrawOverlays(context)) "✅" else "❌"} Overlay")
            if (!Settings.canDrawOverlays(context)) allGranted = false
        } catch (e: Exception) {
            Log.e(TAG, "  ❌ Overlay — ${e.message}")
            allGranted = false
        }

        // Battery optimization
        try {
            PermissionUtils.requestBatteryOptimizationExemption(context)
            Log.i(TAG, "  ✅ Battery optimization")
        } catch (e: Exception) {
            Log.w(TAG, "  ⚠️ Battery — ${e.message}")
        }

        Log.i(TAG, if (allGranted) "✅ ALL GRANTED" else "⚠️ SOME FAILED")
        Log.i(TAG, "═══════════════════════════════════")
        return allGranted
    }

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }
}
