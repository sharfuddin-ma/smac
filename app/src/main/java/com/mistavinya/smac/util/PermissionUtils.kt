package com.mistavinya.smac.util

import android.Manifest
import android.app.admin.DevicePolicyManager
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
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_PHONE_NUMBERS)
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * Checks if all runtime permissions are granted.
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return areRuntimePermissionsGranted(context)
    }

    /**
     * Checks if only runtime permissions are granted.
     */
    fun areRuntimePermissionsGranted(context: Context): Boolean {
        return getRequiredRuntimePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Returns true if overlay permission is granted.
     */
    fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Checks if app can proceed to main screen.
     * Logic: Device Owner, MDM Config, or Runtime permissions granted.
     * Never block completely.
     */
    fun canProceed(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isDeviceOwnerApp(context.packageName)) return true
        if (MdmConfigReader.hasConfig(context)) return true
        if (areRuntimePermissionsGranted(context)) return true
        
        // Fallback: let them proceed even if not all granted, to avoid blocking
        return true 
    }

    /**
     * Returns list of required runtime permissions that are NOT yet granted.
     */
    fun getMissingPermissions(context: Context): List<String> {
        return getRequiredRuntimePermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Opens overlay settings page.
     */
    fun requestOverlayPermission(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PermissionUtils", "Overlay settings error: ${e.message}")
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
