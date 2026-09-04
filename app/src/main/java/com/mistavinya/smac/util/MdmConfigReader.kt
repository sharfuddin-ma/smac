package com.mistavinya.smac.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionsManager
import android.os.Bundle
import android.util.Log

object MdmConfigReader {

    private const val TAG = "MdmConfigReader"

    fun readConfig(context: Context): DeviceInfoReader.DeviceInfo {
        val manager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictions: Bundle = manager.applicationRestrictions

        val imei = restrictions.getString("device_imei", "")
        val imei2 = restrictions.getString("device_imei2", "")
        val serial = restrictions.getString("device_serial", "")
        val phone1 = restrictions.getString("device_phone1", "")
        val phone2 = restrictions.getString("device_phone2", "")

        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "READING MDM MANAGED CONFIG")
        Log.i(TAG, "  IMEI 1: $imei")
        Log.i(TAG, "  IMEI 2: $imei2")
        Log.i(TAG, "  Serial: $serial")
        Log.i(TAG, "  Phone 1: $phone1")
        Log.i(TAG, "  Phone 2: $phone2")
        Log.i(TAG, "═══════════════════════════════════")

        return DeviceInfoReader.DeviceInfo(
            serialNumber = serial,
            imei1 = imei,
            imei2 = imei2,
            phoneNumber1 = phone1,
            phoneNumber2 = phone2,
            deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            androidVersion = android.os.Build.VERSION.RELEASE,
            source = "mdm_config"
        )
    }

    fun hasConfig(context: Context): Boolean {
        val manager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictions = manager.applicationRestrictions ?: return false
        return !restrictions.getString("device_imei").isNullOrBlank() || 
               !restrictions.getString("device_serial").isNullOrBlank()
    }

    class ConfigChangeReceiver(private val onUpdate: (DeviceInfoReader.DeviceInfo) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED) {
                Log.i(TAG, "Managed restrictions changed, re-reading...")
                onUpdate(readConfig(context))
            }
        }

        companion object {
            fun intentFilter() = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
        }
    }
}
