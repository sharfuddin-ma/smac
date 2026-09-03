package com.mistavinya.smac.ui.classification

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.mistavinya.smac.MainActivity
import com.mistavinya.smac.data.CallSyncDatabase
import com.mistavinya.smac.data.entity.CallFormDataEntity
import com.mistavinya.smac.data.entity.CallRecordingEntity
import com.mistavinya.smac.data.entity.UploadQueueEntity
import com.mistavinya.smac.ui.theme.SalesEdgeAITheme
import com.mistavinya.smac.util.DeviceInfoUtil
import com.mistavinya.smac.util.SamsungRecordingFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ClassificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // This makes the window resize when keyboard appears
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val callLogId = intent.getStringExtra("call_log_id") ?: ""
        val phoneNumber = intent.getStringExtra("phone_number") ?: "Unknown"
        val contactName = intent.getStringExtra("contact_name")
        val callDirection = intent.getStringExtra("call_direction") ?: "INCOMING"
        val duration = intent.getLongExtra("duration_seconds", 0L)
        
        if (callLogId.isEmpty()) {
            finish()
            return
        }
        
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing — user must select an option or wait for timeout
            }
        })
        
        setContent {
            SalesEdgeAITheme {
                ClassificationScreen(
                    callLogId = callLogId,
                    phoneNumber = phoneNumber,
                    contactName = contactName,
                    callDirection = callDirection,
                    durationSeconds = duration,
                    onDismiss = { finish() },
                    onNavigateToHome = {
                        val intent = Intent(this, MainActivity::class.java).apply {
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

@Composable
fun ClassificationScreen(
    callLogId: String,
    phoneNumber: String,
    contactName: String?,
    callDirection: String,
    durationSeconds: Long,
    onDismiss: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    var countdown by remember { mutableIntStateOf(60) }
    var isProcessing by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }

    // Form states
    var companyName by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf(contactName ?: "") }
    var reasonForCall by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Auto-scroll to focused field when keyboard appears
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0 && showForm) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(Unit) {
        while (countdown > 0 && !showForm) {
            delay(1000)
            if (!isProcessing) countdown--
        }
        if (!isProcessing && !showForm) {
            isProcessing = true
            handleSelection(context, scope, callLogId, "CLIENT", phoneNumber, true) {
                showForm = true
                isProcessing = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000).copy(alpha = 0.65f))
            .systemBarsPadding()
            .imePadding(), // Ensure whole container pushes up for keyboard
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState)
            ) {
                if (!showForm) {
                    // SELECTION UI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (callDirection == "OUTGOING") Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (callDirection == "OUTGOING") Color(0xFF1976D2) else Color(0xFF388E3C)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Call Ended", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(contactName ?: phoneNumber, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    if (contactName != null && contactName != "Unknown") {
                        Text(phoneNumber, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${callDirection.lowercase().replaceFirstChar { it.uppercase() }} · ${formatDuration(durationSeconds)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(Modifier.height(20.dp))
                    
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(14.dp), tint = if (countdown <= 10) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(6.dp))
                            Text("Auto-classifies in ${countdown}s", fontSize = 12.sp, color = if (countdown <= 10) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Text("CLASSIFY", fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    
                    Button(onClick = { isProcessing = true; handleSelection(context, scope, callLogId, "CLIENT", phoneNumber, true) { showForm = true; isProcessing = false } }, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(8.dp), enabled = !isProcessing) {
                        Icon(Icons.Default.Business, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Client", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { isProcessing = true; handleSelection(context, scope, callLogId, "TEAM_MEMBER", phoneNumber, true) { onDismiss() } }, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(8.dp), enabled = !isProcessing) {
                        Icon(Icons.Default.Group, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Team Member", fontSize = 14.sp)
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { isProcessing = true; handleSelection(context, scope, callLogId, "PERSONAL", phoneNumber, false) { onDismiss() } }, modifier = Modifier.fillMaxWidth().height(40.dp), enabled = !isProcessing) {
                        Icon(Icons.Default.DeleteOutline, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("Personal — Delete recording", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    // CLIENT FORM UI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Client Details", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                if (companyName.isNotBlank() && customerName.isNotBlank()) {
                                    isProcessing = true
                                    scope.launch(Dispatchers.IO) {
                                        val db = CallSyncDatabase.getInstance(context)
                                        db.callFormDataDao().insert(CallFormDataEntity(callLogId = callLogId, companyName = companyName.trim(), customerName = customerName.trim(), reasonForCall = reasonForCall.trim(), notes = notes.trim()))
                                        db.callLogDao().markFormSubmitted(callLogId)
                                        db.uploadQueueDao().insert(UploadQueueEntity(callLogLocalId = callLogId, uploadType = "FORM_DATA", payload = "{}"))
                                        withContext(Dispatchers.Main) { onNavigateToHome() }
                                    }
                                } else {
                                    formError = "Please fill required fields"
                                }
                            }
                        ) {
                            Text("Done", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reasonForCall,
                        onValueChange = { reasonForCall = it },
                        label = { Text("Reason for Call") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                    
                    if (formError != null) {
                        Text(formError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (companyName.isBlank() || customerName.isBlank()) {
                                formError = "Please fill required fields"
                            } else {
                                focusManager.clearFocus()
                                isProcessing = true
                                scope.launch(Dispatchers.IO) {
                                    val db = CallSyncDatabase.getInstance(context)
                                    db.callFormDataDao().insert(CallFormDataEntity(callLogId = callLogId, companyName = companyName.trim(), customerName = customerName.trim(), reasonForCall = reasonForCall.trim(), notes = notes.trim()))
                                    db.callLogDao().markFormSubmitted(callLogId)
                                    db.uploadQueueDao().insert(UploadQueueEntity(callLogLocalId = callLogId, uploadType = "FORM_DATA", payload = "{}"))
                                    withContext(Dispatchers.Main) { onNavigateToHome() }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isProcessing
                    ) {
                        Text("Submit")
                    }
                    
                    // Extra spacer at bottom to ensure button is scrollable above keyboard
                    Spacer(Modifier.height(32.dp))
                }
                
                if (isProcessing) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                }
            }
        }
    }
}

private fun handleSelection(context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope, callLogId: String, category: String, phoneNumber: String, shouldFindRecording: Boolean, onComplete: () -> Unit) {
    scope.launch(Dispatchers.IO) {
        val db = CallSyncDatabase.getInstance(context)
        db.callLogDao().updateCategory(callLogId, category, formRequired = category == "CLIENT", hasRecording = shouldFindRecording)
        db.uploadQueueDao().insert(UploadQueueEntity(callLogLocalId = callLogId, uploadType = "CALL_LOG", payload = "{}"))
        
        if (category == "PERSONAL") {
            // ═══ PERSONAL — Remove any recording reference ═══
            try {
                db.callRecordingDao().deleteByCallLogId(callLogId)
                db.callLogDao().updateRecording(
                    callId = callLogId,
                    hasRecording = false,
                    localRecordingPath = null,
                    recordingFileName = null,
                    recordingFileSizeBytes = 0
                )
                db.uploadQueueDao().deleteByCallLogIdAndType(callLogId, "RECORDING")
                Log.i("Classification", "🗑️ PERSONAL — recording reference removed for $callLogId")
            } catch (e: Exception) {
                Log.w("Classification", "No recording to remove: ${e.message}")
            }
        } else if (shouldFindRecording) {
            val recordingFile = SamsungRecordingFinder.findRecording(phoneNumber, System.currentTimeMillis())
            if (recordingFile != null) {
                db.callRecordingDao().insert(CallRecordingEntity(callLogId = callLogId, deviceSerial = DeviceInfoUtil.getSerialNumber(), localFilePath = recordingFile.absolutePath, fileSizeBytes = recordingFile.length()))
                db.uploadQueueDao().insert(UploadQueueEntity(callLogLocalId = callLogId, uploadType = "RECORDING", payload = "{}", filePath = recordingFile.absolutePath, fileSizeBytes = recordingFile.length()))
            }
        }
        withContext(Dispatchers.Main) { onComplete() }
    }
}

private fun formatDuration(seconds: Long): String {
    val min = seconds / 60
    val sec = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", min, sec)
}
