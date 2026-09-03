package com.mistavinya.smac.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionUtils {

    /**
     * Returns the list of required RUNTIME permissions.
     */
    fun getRequiredRuntimePermissions(): List<String> {
        return buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_PHONE_NUMBERS)
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }
    }

    /**
     * Checks if ALL permissions (Runtime + Overlay) are granted.
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        val runtimeGranted = areRuntimePermissionsGranted(context)
        val overlayGranted = Settings.canDrawOverlays(context)
        return runtimeGranted && overlayGranted
    }

    /**
     * Checks if only runtime permissions are granted (without overlay).
     */
    fun areRuntimePermissionsGranted(context: Context): Boolean {
        return getRequiredRuntimePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request battery optimization exemption (non-blocking).
     */
    fun requestBatteryOptimizationExemption(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("PermissionUtils", "Error requesting battery exemption: ${e.message}")
        }
    }
}
