package com.mistavinya.smac.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mistavinya.smac.data.CallSyncDatabase
import com.mistavinya.smac.data.dao.CallFormDataDao
import com.mistavinya.smac.data.dao.CallRecordingDao
import com.mistavinya.smac.data.dao.UploadQueueDao
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.data.entity.UploadQueueEntity
import com.mistavinya.smac.data.repository.CallLogRepository
import com.mistavinya.smac.storage.LocalStorageManager
import com.mistavinya.smac.util.ContactUtils
import com.mistavinya.smac.util.DateUtils
import com.mistavinya.smac.util.DeviceInfoUtil
import com.mistavinya.smac.util.RecordingMatcher
import com.mistavinya.smac.util.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class CallRecordingService : Service() {

    companion object {
        private const val TAG = "CallMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "callsync_monitoring"
        
        const val ACTION_START_MONITORING = "START_MONITORING"
    }

    private var isForegroundStarted = false
    
    private lateinit var storageManager: LocalStorageManager
    private lateinit var callLogRepository: CallLogRepository
    private lateinit var callFormDataDao: CallFormDataDao
    private lateinit var callRecordingDao: CallRecordingDao
    private lateinit var uploadQueueDao: UploadQueueDao
    private lateinit var settingsDataStore: SettingsDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    
    private var deviceSerial: String = ""
    private var devicePhoneNumber1: String = ""
    
    // Class-level variables for call tracking
    private var savedRemoteNumber: String = ""
    private var isOutgoingCall: Boolean = false
    private var wasRinging: Boolean = false
    private var wasAnswered: Boolean = false
    private var callStartTime: Long = 0L
    private var lastState = TelephonyManager.EXTRA_STATE_IDLE

    private val internalCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                // OUTGOING — fires BEFORE phone state changes
                Intent.ACTION_NEW_OUTGOING_CALL, "android.intent.action.NEW_OUTGOING_CALL" -> {
                    val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: ""
                    savedRemoteNumber = outgoingNumber
                    isOutgoingCall = true
                    wasRinging = false
                    wasAnswered = false
                    Log.i(TAG, "📞 OUTGOING call to: $savedRemoteNumber")
                }

                TelephonyManager.ACTION_PHONE_STATE_CHANGED, "android.intent.action.PHONE_STATE" -> {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
                    val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                        ?: intent.getStringExtra("incoming_number") ?: ""

                    if (incomingNumber.isNotBlank()) {
                        savedRemoteNumber = incomingNumber
                    }

                    Log.d(TAG, "State: $state | Remote: $savedRemoteNumber | Outgoing: $isOutgoingCall")

                    when (state) {
                        TelephonyManager.EXTRA_STATE_RINGING -> {
                            isOutgoingCall = false
                            wasRinging = true
                            wasAnswered = false
                            Log.i(TAG, "📞 INCOMING/RINGING from: $savedRemoteNumber")
                        }

                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            wasAnswered = true
                            callStartTime = System.currentTimeMillis()
                            
                            if (!wasRinging) {
                                isOutgoingCall = true
                                Log.i(TAG, "📞 OFFHOOK — OUTGOING call to: $savedRemoteNumber")
                            } else {
                                isOutgoingCall = false
                                Log.i(TAG, "📞 OFFHOOK — INCOMING call answered from: $savedRemoteNumber")
                            }
                            
                            startForegroundSafely(isCallActive = true)
                        }

                        TelephonyManager.EXTRA_STATE_IDLE -> {
                            Log.i(TAG, "📞 IDLE — ended: remote=$savedRemoteNumber, outgoing=$isOutgoingCall, wasRinging=$wasRinging, wasAnswered=$wasAnswered")

                            if (savedRemoteNumber.isNotBlank()) {
                                if (wasRinging && !wasAnswered && !isOutgoingCall) {
                                    saveMissedCall(savedRemoteNumber)
                                } else if (wasAnswered || isOutgoingCall) {
                                    handleCallEnded(savedRemoteNumber, isOutgoingCall)
                                }
                            }

                            // Reset state for next call
                            savedRemoteNumber = ""
                            isOutgoingCall = false
                            wasRinging = false
                            wasAnswered = false
                            callStartTime = 0
                            startForegroundSafely(isCallActive = false)
                        }
                    }
                    lastState = state
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "CallMonitorService created")
        storageManager = LocalStorageManager(this)
        settingsDataStore = SettingsDataStore(this)
        
        val database = CallSyncDatabase.getInstance(this)
        callLogRepository = CallLogRepository(database.callLogDao())
        callFormDataDao = database.callFormDataDao()
        callRecordingDao = database.callRecordingDao()
        uploadQueueDao = database.uploadQueueDao()

        val deviceInfo = DeviceInfoUtil.getAllDeviceInfo(this)
        deviceSerial = deviceInfo.serialNumber
        devicePhoneNumber1 = deviceInfo.phoneNumber1
        DeviceInfoUtil.logAllDeviceInfo(this)

        createNotificationChannel()
        
        // Register broadcast receiver for call events
        val filter = IntentFilter().apply {
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            addAction(Intent.ACTION_NEW_OUTGOING_CALL)
            priority = Int.MAX_VALUE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(internalCallReceiver, filter, RECEIVER_EXPORTED)
            Log.i(TAG, "BroadcastReceiver registered with RECEIVER_EXPORTED (Android 13+) ✅")
        } else {
            registerReceiver(internalCallReceiver, filter)
            Log.i(TAG, "BroadcastReceiver registered ✅")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand — action: ${intent?.action}")
        
        if (!isForegroundStarted) {
            startForegroundSafely(isCallActive = false)
        }

        return START_STICKY
    }

    private fun startForegroundSafely(isCallActive: Boolean) {
        val title = if (isCallActive) "Call Active" else "SalesEdgeAI"
        val text = if (isCallActive) "Active call — $savedRemoteNumber" else "Monitoring calls for recording"
        val notification = createNotification(title, text)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundStarted = true
            Log.i(TAG, "Foreground started (isCallActive=$isCallActive)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground: ${e.message}")
        }
    }

    private fun handleCallEnded(remoteNumber: String, isOutgoing: Boolean) {
        val durationSeconds = if (callStartTime > 0) (System.currentTimeMillis() - callStartTime) / 1000 else 0
        val callEndTime = System.currentTimeMillis()

        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "CALL ENDED — Processing...")
        Log.i(TAG, "  Remote: $remoteNumber")
        Log.i(TAG, "  Outgoing: $isOutgoing")
        Log.i(TAG, "  Duration: ${durationSeconds}s")
        Log.i(TAG, "═══════════════════════════════════")

        serviceScope.launch {
            try {
                val devicePhoneNumber = settingsDataStore.getPhoneNumber1()
                Log.i(TAG, "  Device phone: $devicePhoneNumber")

                val callerNumber: String
                val calleeNumber: String
                val callDirection: String

                if (isOutgoing) {
                    callerNumber = devicePhoneNumber   // I am the caller
                    calleeNumber = remoteNumber        // They are the callee
                    callDirection = "OUTGOING"
                } else {
                    callerNumber = remoteNumber        // They are the caller
                    calleeNumber = devicePhoneNumber   // I am the callee
                    callDirection = "INCOMING"
                }

                Log.i(TAG, "  Direction: $callDirection")
                Log.i(TAG, "  Caller: $callerNumber")
                Log.i(TAG, "  Callee: $calleeNumber")

                val contactName = ContactUtils.getContactName(applicationContext, remoteNumber)
                Log.i(TAG, "  Contact: ${contactName ?: "Unknown"}")

                val callLog = CallLogEntity(
                    deviceSerial = deviceSerial,
                    callDirection = callDirection,
                    callerNumber = callerNumber,
                    calleeNumber = calleeNumber,
                    durationSeconds = durationSeconds,
                    callCategory = "PENDING",
                    contactName = contactName
                )
                val callLogId = callLog.id
                callLogRepository.insert(callLog)
                Log.i(TAG, "✅ Call saved to Room: id=$callLogId")

                // Auto-link recording after 5 seconds
                handler.postDelayed({
                    serviceScope.launch {
                        try {
                            // Check if call was already classified as PERSONAL — skip recording link
                            val currentCallLog = CallSyncDatabase.getInstance(applicationContext).callLogDao().getById(callLogId)
                            if (currentCallLog?.callCategory == "PERSONAL") {
                                Log.i(TAG, "⏭️ Skipping recording link — call classified as PERSONAL")
                                return@launch
                            }

                            val folderUri = settingsDataStore.getRecordingFolderUri()
                            Log.i(TAG, "Recording folder URI: ${folderUri.ifBlank { "NOT SET" }}")
                            if (folderUri.isNotBlank()) {
                                val match = RecordingMatcher.findRecording(
                                    context = applicationContext,
                                    folderUriString = folderUri,
                                    phoneNumber = remoteNumber,
                                    callEndTimeMillis = callEndTime
                                )
                                if (match != null) {
                                    val db = CallSyncDatabase.getInstance(applicationContext)
                                    db.callLogDao().updateRecording(
                                        callId = callLogId,
                                        hasRecording = true,
                                        localRecordingPath = match.uri.toString(),
                                        recordingFileName = match.fileName,
                                        recordingFileSizeBytes = match.fileSizeBytes
                                    )
                                    Log.i(TAG, "✅ Recording auto-linked: ${match.fileName}")
                                } else {
                                    Log.w(TAG, "⚠️ No recording match found")
                                }
                            } else {
                                Log.w(TAG, "⚠️ Recording folder not mapped — skipping")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Recording match error: ${e.message}")
                        }
                    }
                }, 5000)

                Log.i(TAG, "Launching classification popup...")
                launchClassificationPopup(callLogId, durationSeconds, remoteNumber, callDirection)
            } catch (e: Exception) {
                Log.e(TAG, "❌ CRITICAL — handleCallEnded failed: ${e.message}", e)
            }
        }
    }

    private fun launchClassificationPopup(logId: String, duration: Long, remoteNumber: String, direction: String) {
        val intent = Intent(this, com.mistavinya.smac.ui.classification.ClassificationActivity::class.java).apply {
            putExtra("call_log_id", logId)
            putExtra("phone_number", remoteNumber)
            putExtra("contact_name", ContactUtils.getContactName(applicationContext, remoteNumber))
            putExtra("call_direction", direction)
            putExtra("duration_seconds", duration)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        try {
            if (Settings.canDrawOverlays(this)) {
                Log.i(TAG, "✅ Overlay permission granted — launching popup")
            } else {
                Log.w(TAG, "⚠️ Overlay permission NOT granted — attempting launch anyway")
            }
            // Launch regardless — as Device Owner with FLAG_ACTIVITY_NEW_TASK, it should work
            startActivity(intent)
            Log.i(TAG, "✅ Classification popup launched for $remoteNumber (callLogId=$logId)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to launch classification popup: ${e.message}", e)
        }
    }

    private fun saveMissedCall(remoteNumber: String) {
        serviceScope.launch {
            val devicePhoneNumber = settingsDataStore.getPhoneNumber1()
            
            val callLog = CallLogEntity(
                deviceSerial = deviceSerial,
                callDirection = "MISSED",
                callerNumber = remoteNumber,
                calleeNumber = devicePhoneNumber,
                durationSeconds = 0,
                callCategory = "MISSED",
                hasRecording = false,
                isFormRequired = false,
                contactName = ContactUtils.getContactName(this@CallRecordingService, remoteNumber)
            )
            callLogRepository.insert(callLog)
            Log.i(TAG, "Missed call saved: ${callLog.id}")

            uploadQueueDao.insert(UploadQueueEntity(
                callLogLocalId = callLog.id,
                uploadType = "CALL_LOG",
                payload = "{}"
            ))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SalesEdgeAI Call Monitoring",
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

    override fun onDestroy() {
        Log.i(TAG, "Service onDestroy")
        try {
            unregisterReceiver(internalCallReceiver)
        } catch (e: Exception) { /* already unregistered */ }
        super.onDestroy()
    }
}
