package com.mistavinya.smac

import android.app.Application
import android.util.Log
import com.mistavinya.smac.util.DeviceInfoReader
import com.mistavinya.smac.util.PermissionGranter
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date
import kotlinx.coroutines.runBlocking

class CallSyncApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Global crash handler — catches ALL uncaught exceptions
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()

                // Log to Logcat
                Log.e("CALLSYNC_CRASH", "====== APP CRASHED ======")
                Log.e("CALLSYNC_CRASH", "Thread: ${thread.name}")
                Log.e("CALLSYNC_CRASH", "Exception: ${throwable.javaClass.simpleName}")
                Log.e("CALLSYNC_CRASH", "Message: ${throwable.message}")
                Log.e("CALLSYNC_CRASH", "Stack: $stackTrace")
                Log.e("CALLSYNC_CRASH", "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                Log.e("CALLSYNC_CRASH", "Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                Log.e("CALLSYNC_CRASH", "=========================")

                // Save to file (readable even after crash)
                val crashFile = File(getExternalFilesDir(null), "crash_log.txt")
                crashFile.appendText("""
                    |====== CRASH at ${Date()} ======
                    |Thread: ${thread.name}
                    |Exception: ${throwable.javaClass.simpleName}
                    |Message: ${throwable.message}
                    |Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                    |Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})
                    |Stack Trace:
                    |$stackTrace
                    |===============================
                    |
                """.trimMargin())

            } catch (e: Exception) {
                Log.e("CALLSYNC_CRASH", "Failed to log crash: ${e.message}")
            }

            // Call default handler (shows system crash dialog)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        Log.i("CALLSYNC", "CallSyncApp initialized — crash handler registered")

        // ═══════════════════════════════════════════════════
        // STEP 1: Force grant ALL permissions (Device Owner)
        // ═══════════════════════════════════════════════════
        try {
            val granted = PermissionGranter.grantAllPermissions(this)
            Log.i("CALLSYNC", if (granted) "All permissions granted ✅" else "Some permissions failed ⚠️")
        } catch (e: Exception) {
            Log.e("CALLSYNC", "Permission error: ${e.message}")
        }

        // ═══════════════════════════════════════════════════
        // STEP 2: Read ALL device info and save to DataStore
        // ═══════════════════════════════════════════════════
        try {
            runBlocking {
                val info = DeviceInfoReader.readAndSave(this@CallSyncApp)
                Log.i("CALLSYNC", "Device: serial=${info.serialNumber}, imei1=${info.imei1}, phone1=${info.phoneNumber1}")
            }
        } catch (e: Exception) {
            Log.e("CALLSYNC", "Device info error: ${e.message}")
        }
    }
}
