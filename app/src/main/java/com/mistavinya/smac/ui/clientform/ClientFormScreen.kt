package com.mistavinya.smac.ui.clientform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mistavinya.smac.data.CallSyncDatabase
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.storage.LocalStorageManager
import com.mistavinya.smac.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormScreen(callLogId: Long, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var companyName by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var reasonForCall by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var callLog by remember { mutableStateOf<CallLogEntity?>(null) }
    
    LaunchedEffect(callLogId) {
        val db = CallSyncDatabase.getInstance(context)
        callLog = db.callLogDao().getById(callLogId)
        callLog?.let {
            companyName = it.companyName ?: ""
            customerName = it.contactPersonName ?: it.contactName ?: ""
        }
    }
    
    val isFormValid = companyName.isNotBlank() && customerName.isNotBlank() && reasonForCall.isNotBlank()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Details", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            
            // Call summary
            callLog?.let { log ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (log.callType) {
                                "outgoing" -> Icons.AutoMirrored.Filled.CallMade
                                "incoming" -> Icons.AutoMirrored.Filled.CallReceived
                                else -> Icons.AutoMirrored.Filled.PhoneMissed
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${log.contactName ?: log.phoneNumber} · ${log.callType.replaceFirstChar { it.uppercase() }} · ${formatDuration(log.durationSeconds)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(28.dp))
            
            // Form fields
            Text(
                "CLIENT INFORMATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name") },
                placeholder = { Text("e.g., Acme Corp") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Customer Name") },
                placeholder = { Text("e.g., Rahul Sharma") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "CALL DETAILS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = reasonForCall,
                onValueChange = { reasonForCall = it },
                label = { Text("Reason for Call") },
                placeholder = { Text("e.g., Product demo follow-up") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Brief summary of the conversation...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Save button
            Button(
                onClick = {
                    if (isFormValid && !isSaving) {
                        isSaving = true
                        scope.launch(Dispatchers.IO) {
                            val db = CallSyncDatabase.getInstance(context)
                            db.callLogDao().updateClientDetails(
                                id = callLogId,
                                companyName = companyName.trim(),
                                contactPersonName = customerName.trim(),
                                callPurpose = reasonForCall.trim(),
                                notes = description.trim()
                            )
                            val storageManager = LocalStorageManager(context)
                            val log = db.callLogDao().getById(callLogId)
                            log?.let {
                                if (it.recordingFilePath.isNotEmpty() && File(it.recordingFilePath).exists()) {
                                    val newFile = storageManager.moveRecordingToCategory(
                                        File(it.recordingFilePath), "client", it.phoneNumber
                                    )
                                    db.callLogDao().updateFilePath(callLogId, newFile.absolutePath)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                isSaving = false
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = isFormValid && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Skip option
            TextButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val db = CallSyncDatabase.getInstance(context)
                        val storageManager = LocalStorageManager(context)
                        val log = db.callLogDao().getById(callLogId)
                        log?.let {
                            if (it.recordingFilePath.isNotEmpty() && File(it.recordingFilePath).exists()) {
                                val newFile = storageManager.moveRecordingToCategory(
                                    File(it.recordingFilePath), "client", it.phoneNumber
                                )
                                db.callLogDao().updateFilePath(callLogId, newFile.absolutePath)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Skip — save without details",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val min = seconds / 60
    val sec = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", min, sec)
}
