package com.mistavinya.smac.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mistavinya.smac.storage.LocalStorageManager
import com.mistavinya.smac.ui.navigation.Screen
import com.mistavinya.smac.util.SettingsDataStore
import com.mistavinya.smac.util.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePreferences = remember { ThemePreferences(context) }
    val settingsDataStore = remember { SettingsDataStore(context) }
    val storageManager = remember { LocalStorageManager(context) }
    
    val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)
    val classificationTimeout by settingsDataStore.classificationTimeout.collectAsState(initial = 60)
    val recordingFolderUri by settingsDataStore.recordingFolderUri.collectAsState(initial = "")
    
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var storageUsed by remember { mutableStateOf("Calculating...") }
    
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { folderUri ->
            context.contentResolver.takePersistableUriPermission(
                folderUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            scope.launch {
                settingsDataStore.setRecordingFolderUri(folderUri.toString())
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            storageUsed = storageManager.getStorageUsedFormatted()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // DEVICE section
            item {
                SectionHeader("DEVICE")
                SettingsItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Device Info",
                    subtitle = "IMEI, serial number, and device details",
                    onClick = { navController.navigate(Screen.Profile.route) }
                )
            }
            
            // RECORDING section
            item {
                SectionHeader("RECORDING")
                
                // RECORDING FOLDER CARD — Professional Design
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Recording Folder",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (recordingFolderUri.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFF4CAF50)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Folder mapped",
                                            fontSize = 13.sp,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                } else {
                                    Text(
                                        "Not configured",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        if (recordingFolderUri.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = Uri.parse(recordingFolderUri).lastPathSegment ?: recordingFolderUri,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 36.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                if (recordingFolderUri.isBlank()) Icons.Default.FolderOpen else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (recordingFolderUri.isBlank()) "Set Recording Folder" else "Change Folder",
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Classification timeout",
                    subtitle = "${classificationTimeout} seconds",
                    onClick = { showTimeoutDialog = true }
                )
            }
            
            // STORAGE section
            item {
                SectionHeader("STORAGE")
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Space used",
                    subtitle = storageUsed,
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.FolderOpen,
                    title = "My Recordings",
                    subtitle = "View and manage recordings list",
                    onClick = { navController.navigate(Screen.RecordingsList.route) }
                )
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Clear old recordings",
                    subtitle = "Remove recordings older than 30 days",
                    onClick = { showClearDialog = true }
                )
            }
            
            // APPEARANCE section
            item {
                SectionHeader("APPEARANCE")
                SettingsItemWithSwitch(
                    icon = Icons.Default.DarkMode,
                    title = "Dark mode",
                    subtitle = if (isDarkMode) "On" else "Off",
                    checked = isDarkMode,
                    onCheckedChange = { enabled ->
                        scope.launch { themePreferences.setDarkMode(enabled) }
                    }
                )
            }
            
            // ABOUT section
            item {
                SectionHeader("ABOUT")
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About SalesEdgeAI",
                    subtitle = "Version 1.0.0",
                    onClick = { showAboutDialog = true }
                )
            }
            
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
    
    // Timeout dialog
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("Classification Timeout", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    listOf(30, 45, 60, 90).forEach { seconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        settingsDataStore.setClassificationTimeout(seconds)
                                    }
                                    showTimeoutDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = classificationTimeout == seconds,
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setClassificationTimeout(seconds)
                                    }
                                    showTimeoutDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("${seconds} seconds", fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Clear recordings dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Old Recordings", fontWeight = FontWeight.SemiBold) },
            text = { Text("Delete all recordings older than 30 days? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            storageManager.clearOldRecordings(30)
                            storageUsed = storageManager.getStorageUsedFormatted()
                        }
                        showClearDialog = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // About dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("SalesEdgeAI", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("Version 1.0.0", fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Mist Avinya Technologies",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "sharfuddin.m@mistavinya.com",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SettingsItemWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
