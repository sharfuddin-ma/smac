package com.mistavinya.smac.util

import android.util.Log
import java.io.File

object SamsungRecordingFinder {
    private const val TAG = "SamsungRecordingFinder"

    private val RECORDING_DIRS = listOf(
        "/storage/emulated/0/Call/",
        "/storage/emulated/0/Recordings/Call/",
        "/storage/emulated/0/Samsung/Call/",
        "/storage/emulated/0/DCIM/.call_recording/"
    )

    /**
     * Find the most recent Samsung auto-recording file that matches the call.
     * Match by: file modified time close to callEndTime (within toleranceMs)
     * and optionally phone number in filename.
     */
    fun findRecording(phoneNumber: String, callEndTime: Long, toleranceMs: Long = 30_000): File? {
        for (dirPath in RECORDING_DIRS) {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) continue

            Log.d(TAG, "Searching in: $dirPath")

            val candidates = dir.listFiles()
                ?.filter { it.isFile && (it.extension in listOf("m4a", "mp3", "amr", "3gp", "ogg")) }
                ?.filter { Math.abs(it.lastModified() - callEndTime) < toleranceMs }
                ?.sortedByDescending { it.lastModified() }
                ?: continue

            Log.d(TAG, "Found ${candidates.size} candidates in $dirPath")

            // Prefer file with phone number in name (last 7 digits to handle country code variations)
            val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "").takeLast(7)
            if (cleanNumber.length >= 4) {
                val withNumber = candidates.firstOrNull { it.name.contains(cleanNumber) }
                if (withNumber != null) {
                    Log.i(TAG, "Found recording by number match: ${withNumber.name}")
                    return withNumber
                }
            }

            // Otherwise return most recent match by timestamp
            if (candidates.isNotEmpty()) {
                Log.i(TAG, "Found recording by timestamp match: ${candidates.first().name}")
                return candidates.first()
            }
        }

        Log.w(TAG, "No Samsung recording found for $phoneNumber")
        return null
    }
}
