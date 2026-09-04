package com.mistavinya.smac

import android.app.Application
import android.os.Build
import android.util.Log
import com.mistavinya.smac.util.DeviceInfoReader
import com.mistavinya.smac.util.MdmConfigReader
import com.mistavinya.smac.util.PermissionGranter
import com.mistavinya.smac.util.SettingsDataStore
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CallSyncApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Global crash handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()
                Log.e("CALLSYNC_CRASH", "====== APP CRASHED ======")
                Log.e("CALLSYNC_CRASH", "Stack: $stackTrace")
                val crashFile = File(getExternalFilesDir(null), "crash_log.txt")
                crashFile.appendText("CRASH at ${Date()}\n$stackTrace\n")
            } catch (e: Exception) {
                Log.e("CALLSYNC_CRASH", "Failed to log crash: ${e.message}")
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        Log.i("CALLSYNC", "SalesEdgeAI initialized")

        // ═══════════════════════════════════════════════════
        // STEP 1: Handle Permissions (Silently if possible)
        // ═══════════════════════════════════════════════════
        try {
            val result = PermissionGranter.grantAllPermissions(this)
            Log.i("CALLSYNC", "Grant result: method=${result.method}, runtime=${result.runtimeGranted}")
        } catch (e: Exception) {
            Log.e("CALLSYNC", "Permission error: ${e.message}")
        }

        // ═══════════════════════════════════════════════════
        // STEP 2: Read and Save Device Info
        // ═══════════════════════════════════════════════════
        try {
            runBlocking {
                val info = DeviceInfoReader.readAndSave(this@CallSyncApp)
                Log.i("CALLSYNC", "Device Info Saved (Source: ${info.source})")
            }
        } catch (e: Exception) {
            Log.e("CALLSYNC", "Device info error: ${e.message}")
        }

        // ═══════════════════════════════════════════════════
        // STEP 3: MDM Config Change Listener
        // ═══════════════════════════════════════════════════
        try {
            val mdmReceiver = MdmConfigReader.ConfigChangeReceiver { updatedInfo ->
                Log.i("CALLSYNC", "MDM config updated")
                CoroutineScope(Dispatchers.IO).launch {
                    val store = SettingsDataStore(this@CallSyncApp)
                    if (updatedInfo.imei1.isNotBlank()) store.setImei1(updatedInfo.imei1)
                    if (updatedInfo.serialNumber.isNotBlank()) store.setSerialNumber(updatedInfo.serialNumber)
                    if (updatedInfo.phoneNumber1.isNotBlank()) store.setPhoneNumber1(updatedInfo.phoneNumber1)
                    store.setDeviceInfoSource("mdm_config_updated")
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    mdmReceiver,
                    MdmConfigReader.ConfigChangeReceiver.intentFilter(),
                    RECEIVER_NOT_EXPORTED
                )
            } else {
                registerReceiver(
                    mdmReceiver,
                    MdmConfigReader.ConfigChangeReceiver.intentFilter()
                )
            }
        } catch (e: Exception) {
            Log.e("CALLSYNC", "MDM listener error: ${e.message}")
        }
    }
}
