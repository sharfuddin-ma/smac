package com.mistavinya.smac.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mistavinya.smac.service.CallRecordingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, CallRecordingService::class.java).apply {
                action = CallRecordingService.ACTION_START_MONITORING
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
