package com.mistavinya.smac

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

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
    }
}
