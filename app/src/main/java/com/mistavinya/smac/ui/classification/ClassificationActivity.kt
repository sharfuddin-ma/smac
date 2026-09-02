package com.mistavinya.smac.ui.classification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mistavinya.smac.MainActivity
import com.mistavinya.smac.data.CallSyncDatabase
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.storage.LocalStorageManager
import com.mistavinya.smac.ui.theme.CallSyncTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ClassificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val phoneNumber = intent.getStringExtra("phone_number") ?: "Unknown"
        val contactName = intent.getStringExtra("contact_name")
        val callType = intent.getStringExtra("call_type") ?: "unknown"
        val duration = intent.getLongExtra("duration_seconds", 0L)
        
        // Find the most recent call log for this number to get the ID
        // Note: In a production app, we'd pass the exact ID from the Service.
        // For this rewrite, we'll fetch the latest unclassified log for this number.
        
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing — user must select an option or wait for timeout
            }
        })
        
        setContent {
            CallSyncTheme {
                // Fetch ID inside a launched effect
                var callLogId by remember { mutableLongStateOf(-1L) }
                val context = LocalContext.current
                
                LaunchedEffect(Unit) {
                    val db = CallSyncDatabase.getInstance(context)
                    // Simple heuristic: get the latest log for this number
                    val latestLogs = db.callLogDao().getRecentCalls(1).collect { list ->
                        if (list.isNotEmpty()) {
                            callLogId = list.first().id
                        }
                    }
                }

                if (callLogId != -1L) {
                    ClassificationScreen(
                        callLogId = callLogId,
                        phoneNumber = phoneNumber,
                        contactName = contactName,
                        isOutgoing = callType == "outgoing",
                        durationSeconds = duration,
                        onDismiss = { finish() },
                        onNavigateToClientForm = { id ->
                            val intent = Intent(this, MainActivity::class.java).apply {
                                putExtra("navigate_to", "client_form")
                                putExtra("call_log_id", id)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ClassificationScreen(
    callLogId: Long,
    phoneNumber: String,
    contactName: String?,
    isOutgoing: Boolean,
    durationSeconds: Long,
    onDismiss: () -> Unit,
    onNavigateToClientForm: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var countdown by remember { mutableIntStateOf(60) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            if (!isProcessing) countdown--
        }
        if (!isProcessing) {
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                val db = CallSyncDatabase.getInstance(context)
                db.callLogDao().updateCategory(callLogId, "client")
                withContext(Dispatchers.Main) { onNavigateToClientForm(callLogId) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000).copy(alpha = 0.65f))
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                
                // Header row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOutgoing) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isOutgoing) Color(0xFF1976D2) else Color(0xFF388E3C)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Call Ended",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Contact info
                Text(
                    contactName ?: phoneNumber,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (contactName != null && contactName != "Unknown") {
                    Text(
                        phoneNumber,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${if (isOutgoing) "Outgoing" else "Incoming"} · ${formatDuration(durationSeconds)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(20.dp))
                
                // Timer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = if (countdown <= 10) Color(0xFFD32F2F) 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Auto-classifies as Client in ${countdown}s",
                            fontSize = 12.sp,
                            color = if (countdown <= 10) Color(0xFFD32F2F) 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Section label
                Text(
                    "CLASSIFY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(12.dp))
                
                // Client button (primary action)
                Button(
                    onClick = {
                        if (!isProcessing) {
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val db = CallSyncDatabase.getInstance(context)
                                db.callLogDao().updateCategory(callLogId, "client")
                                withContext(Dispatchers.Main) { onNavigateToClientForm(callLogId) }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.Business, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Client", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Team button
                OutlinedButton(
                    onClick = {
                        if (!isProcessing) {
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val db = CallSyncDatabase.getInstance(context)
                                db.callLogDao().updateCategory(callLogId, "team_member")
                                val storageManager = LocalStorageManager(context)
                                val callLog = db.callLogDao().getById(callLogId)
                                callLog?.let {
                                    if (it.recordingFilePath.isNotEmpty() && File(it.recordingFilePath).exists()) {
                                        val newFile = storageManager.moveRecordingToCategory(
                                            File(it.recordingFilePath), "team_member", it.phoneNumber
                                        )
                                        db.callLogDao().updateFilePath(callLogId, newFile.absolutePath)
                                    }
                                }
                                withContext(Dispatchers.Main) { onDismiss() }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.Group, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Team Member", fontSize = 14.sp)
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Personal button (de-emphasized)
                TextButton(
                    onClick = {
                        if (!isProcessing) {
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val db = CallSyncDatabase.getInstance(context)
                                val callLog = db.callLogDao().getById(callLogId)
                                callLog?.let {
                                    if (it.recordingFilePath.isNotEmpty()) {
                                        File(it.recordingFilePath).delete()
                                    }
                                    db.callLogDao().deleteById(callLogId)
                                }
                                withContext(Dispatchers.Main) { onDismiss() }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    enabled = !isProcessing
                ) {
                    Icon(
                        Icons.Default.DeleteOutline, null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Personal — Delete recording",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isProcessing) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val min = seconds / 60
    val sec = seconds % 60
    return if (min > 0) "${min}m ${sec}s" else "${sec}s"
}
