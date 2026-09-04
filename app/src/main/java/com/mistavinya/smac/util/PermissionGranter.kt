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

    data class GrantResult(
        val method: String,           // "device_owner", "mdm_managed", "manual"
        val runtimeGranted: Boolean,
        val overlayGranted: Boolean,
        val missingPermissions: List<String> = emptyList()
    )

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

    fun grantAllPermissions(context: Context): GrantResult {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(
            context,
            com.mistavinya.smac.receiver.DeviceAdminReceiver::class.java
        )

        val isDeviceOwner = dpm.isDeviceOwnerApp(context.packageName)

        if (isDeviceOwner) {
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "FORCE GRANTING ALL PERMISSIONS (Device Owner)")
            Log.i(TAG, "═══════════════════════════════════")

            var allGranted = true

            // Runtime permissions via DPM
            for ((label, permission) in getRuntimePermissions()) {
                try {
                    val result = dpm.setPermissionGrantState(
                        adminComponent,
                        context.packageName,
                        permission,
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                    )

                    val actuallyGranted = ContextCompat.checkSelfPermission(
                        context, permission
                    ) == PackageManager.PERMISSION_GRANTED

                    if (result && actuallyGranted) {
                        Log.i(TAG, "  ✅ $label ($permission)")
                    } else {
                        Log.e(TAG, "  ❌ $label ($permission) — Failed")
                        allGranted = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "  ❌ $label ($permission) — Exception: ${e.message}")
                    allGranted = false
                }
            }

            // Overlay via AppOpsManager
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
                        setModeMethod.invoke(appOps, 24, Process.myUid(), context.packageName, AppOpsManager.MODE_ALLOWED)
                    } catch (e: Exception) {
                        Log.e(TAG, "  ❌ Failed to set overlay mode: ${e.message}")
                    }
                }
                Log.i(TAG, "  ${if (Settings.canDrawOverlays(context)) "✅" else "❌"} Overlay")
            } catch (e: Exception) {
                Log.e(TAG, "  ❌ Overlay — ${e.message}")
            }

            PermissionUtils.requestBatteryOptimizationExemption(context)

            return GrantResult(
                method = "device_owner",
                runtimeGranted = allGranted,
                overlayGranted = Settings.canDrawOverlays(context)
            )
        } else {
            // Path 2: Non-Device-Owner (MDM auto-grant or manual)
            Log.i(TAG, "Checking existing permissions (MDM Managed/Manual)...")
            
            val missing = mutableListOf<String>()
            for ((label, permission) in getRuntimePermissions()) {
                val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    Log.i(TAG, "  ✅ $label (already granted)")
                } else {
                    Log.e(TAG, "  ❌ $label (NOT granted)")
                    missing.add(permission)
                }
            }

            val overlay = Settings.canDrawOverlays(context)
            Log.i(TAG, "  ${if (overlay) "✅" else "❌"} Overlay")

            return GrantResult(
                method = if (MdmConfigReader.hasConfig(context)) "mdm_managed" else "manual",
                runtimeGranted = missing.isEmpty(),
                overlayGranted = overlay,
                missingPermissions = missing
            )
        }
    }

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }
}
