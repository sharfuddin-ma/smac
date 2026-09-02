package com.mistavinya.smac.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mistavinya.smac.data.CallSyncDatabase
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.data.repository.CallLogRepository
import com.mistavinya.smac.storage.LocalStorageManager
import com.mistavinya.smac.util.ContactUtils
import com.mistavinya.smac.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CallRecordingService : Service() {

    companion object {
        private const val TAG = "CallMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "callsync_monitoring"
        
        const val ACTION_START_MONITORING = "START_MONITORING"
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_CALL_TYPE = "call_type"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordingFile: File? = null
    private var isForegroundStarted = false
    
    private lateinit var storageManager: LocalStorageManager
    private lateinit var callLogRepository: CallLogRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var callStartTime: Long = 0
    private var currentPhoneNumber: String = "Unknown"
    private var currentContactName: String? = null
    private var currentCallType: String = "unknown"

    // Persistent state for call tracking
    private var lastState = TelephonyManager.EXTRA_STATE_IDLE
    private var isIncoming = false
    private var wasRinging = false
    private var wasAnswered = false
    private var savedNumber: String? = null

    private val internalCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d(TAG, "Internal Receiver onReceive: $action")

            if (action == Intent.ACTION_NEW_OUTGOING_CALL) {
                savedNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                isIncoming = false
                wasRinging = false
                wasAnswered = false
                Log.i(TAG, "Outgoing call detected to: $savedNumber")
                return
            }

            if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                if (incomingNumber != null && incomingNumber.isNotEmpty()) {
                    savedNumber = incomingNumber
                }

                Log.d(TAG, "State: $stateStr | Number: $savedNumber | wasRinging: $wasRinging")

                when (stateStr) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        isIncoming = true
                        wasRinging = true
                        wasAnswered = false
                        Log.i(TAG, "RINGING — Incoming call from: $savedNumber")
                    }

                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        wasAnswered = true
                        callStartTime = System.currentTimeMillis()

                        if (!wasRinging) {
                            isIncoming = false
                            Log.i(TAG, "OFFHOOK — OUTGOING call to: $savedNumber")
                        } else {
                            isIncoming = true
                            Log.i(TAG, "OFFHOOK — INCOMING call answered from: $savedNumber")
                        }

                        // Start recording directly
                        currentPhoneNumber = savedNumber ?: "Unknown"
                        currentCallType = if (isIncoming) "incoming" else "outgoing"
                        currentContactName = ContactUtils.getContactName(context, currentPhoneNumber)
                        startRecording()
                    }

                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        Log.i(TAG, "IDLE — call ended. wasAnswered=$wasAnswered")

                        if (lastState == TelephonyManager.EXTRA_STATE_OFFHOOK && wasAnswered) {
                            stopRecording()
                        } else if (wasRinging && !wasAnswered) {
                            Log.i(TAG, "Missed call from: $savedNumber")
                            saveMissedCall(savedNumber ?: "Unknown")
                        }

                        // Reset state
                        isIncoming = false
                        wasRinging = false
                        wasAnswered = false
                        savedNumber = null
                        callStartTime = 0
                    }
                }
                lastState = stateStr
                updateNotification()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "CallMonitorService created")
        storageManager = LocalStorageManager(this)
        val database = CallSyncDatabase.getInstance(this)
        callLogRepository = CallLogRepository(database.callLogDao())
        createNotificationChannel()
        
        // Register the receiver dynamically
        val filter = IntentFilter().apply {
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            addAction(Intent.ACTION_NEW_OUTGOING_CALL)
            priority = Int.MAX_VALUE
        }
        registerReceiver(internalCallReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand — action: ${intent?.action}")
        
        if (!isForegroundStarted) {
            startForegroundSafely(isRecordingNow = false)
        }

        return START_STICKY
    }

    private fun startForegroundSafely(isRecordingNow: Boolean) {
        val title = if (isRecordingNow) "Recording Call" else "CallSync"
        val text = if (isRecordingNow) "Recording — $currentPhoneNumber" else "Monitoring calls for recording"
        val notification = createNotification(title, text)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                // Type microphone only if actively recording
                if (isRecordingNow && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    }
                }
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundStarted = true
            Log.i(TAG, "Foreground started (isRecording=$isRecordingNow)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground: ${e.message}")
        }
    }

    private fun startRecording() {
        if (isRecording) return
        
        Log.i(TAG, "Starting recording for $currentPhoneNumber")

        try {
            recordingFile = storageManager.createRecordingFile(currentPhoneNumber)
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(applicationContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                // Audio source strategy
                val source = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    MediaRecorder.AudioSource.VOICE_CALL
                } else {
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION
                }
                
                setAudioSource(source)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(recordingFile?.absolutePath)
                
                prepare()
                start()
            }

            isRecording = true
            startForegroundSafely(isRecordingNow = true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording: ${e.message}")
            isRecording = false
            recordingFile = null
            // Try fallback with MIC source if failed
            if (e !is SecurityException) {
                tryMicFallback()
            }
        }
    }
    
    private fun tryMicFallback() {
        try {
            Log.i(TAG, "Attempting MIC fallback recording")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            startForegroundSafely(isRecordingNow = true)
        } catch (e: Exception) {
            Log.e(TAG, "MIC fallback also failed: ${e.message}")
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        Log.i(TAG, "Stopping recording")

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder: ${e.message}")
        } finally {
            mediaRecorder = null
            isRecording = false
        }

        val durationSeconds = (System.currentTimeMillis() - callStartTime) / 1000
        val finalFile = recordingFile
        
        if (finalFile != null && finalFile.exists() && durationSeconds > 0) {
            saveCallLog(durationSeconds, finalFile)
        }

        startForegroundSafely(isRecordingNow = false)
    }

    private fun saveCallLog(duration: Long, file: File) {
        serviceScope.launch {
            val callLog = CallLogEntity(
                phoneNumber = currentPhoneNumber,
                contactName = currentContactName,
                callType = currentCallType,
                date = DateUtils.getCurrentDate(),
                time = DateUtils.getCurrentTime(),
                durationSeconds = duration,
                recordingFilePath = file.absolutePath,
                category = null,
                storageStatus = "saved",
                createdAt = callStartTime
            )
            
            val insertedId = callLogRepository.insert(callLog)
            Log.i(TAG, "Call log saved: $insertedId")
            
            // Launch Classification Popup
            launchClassificationPopup(insertedId, duration)
        }
    }

    private fun launchClassificationPopup(logId: Long, duration: Long) {
        val intent = Intent(this, com.mistavinya.smac.ui.classification.ClassificationActivity::class.java).apply {
            putExtra("call_log_id", logId)
            putExtra("phone_number", currentPhoneNumber)
            putExtra("contact_name", currentContactName)
            putExtra("call_type", currentCallType)
            putExtra("duration_seconds", duration)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        if (Settings.canDrawOverlays(this)) {
            startActivity(intent)
        }
    }

    private fun saveMissedCall(phoneNumber: String) {
        serviceScope.launch {
            val missedLog = CallLogEntity(
                phoneNumber = phoneNumber,
                contactName = ContactUtils.getContactName(this@CallRecordingService, phoneNumber),
                callType = "missed",
                date = DateUtils.getCurrentDate(),
                time = DateUtils.getCurrentTime(),
                durationSeconds = 0,
                recordingFilePath = "",
                category = null,
                storageStatus = "none",
                createdAt = System.currentTimeMillis()
            )
            callLogRepository.insert(missedLog)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoring calls for recording"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, text: String): Notification {
        val mainIntent = Intent(this, com.mistavinya.smac.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification() {
        if (isForegroundStarted) {
            startForegroundSafely(isRecordingNow = isRecording)
        }
    }

    private fun canRecordCalls(): Boolean {
        // Simplified check
        return true
    }

    override fun onDestroy() {
        Log.i(TAG, "Service onDestroy")
        try {
            unregisterReceiver(internalCallReceiver)
        } catch (e: Exception) {}
        if (isRecording) stopRecording()
        super.onDestroy()
    }
}
