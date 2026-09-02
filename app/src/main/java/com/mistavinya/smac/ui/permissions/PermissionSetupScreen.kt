package com.mistavinya.smac.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.mistavinya.smac.ui.navigation.Screen
import com.mistavinya.smac.util.PermissionUtils

@Composable
fun PermissionSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // All runtime permissions we need
    val permissions = mutableListOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS
    )
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.READ_PHONE_NUMBERS)
    }
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allRuntimeGranted = results.values.all { it }
        if (allRuntimeGranted) {
            PermissionUtils.requestBatteryOptimizationExemption(context)
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        }
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (PermissionUtils.areAllPermissionsGranted(context)) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PermissionSetup.route) { inclusive = true }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    LaunchedEffect(Unit) {
        if (PermissionUtils.areAllPermissionsGranted(context)) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.PermissionSetup.route) { inclusive = true }
            }
        } else {
            multiplePermissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            "Permissions Required",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            "CallSync needs the following permissions to record and manage your business calls. These are mandatory for the app to function.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        
        Spacer(Modifier.height(32.dp))
        
        val permissionItems = listOf(
            Icons.Default.Phone to "Phone — Detect incoming & outgoing calls",
            Icons.Default.Phone to "Phone Numbers — Read SIM phone numbers",
            Icons.AutoMirrored.Filled.ListAlt to "Call Log — Read call history details",
            Icons.Default.Mic to "Microphone — Record call audio",
            Icons.Default.Person to "Contacts — Identify caller names",
            Icons.Default.Notifications to "Notifications — Show recording status",
            Icons.Default.Layers to "Overlay — Show post-call popup"
        )
        
        permissionItems.forEach { (icon, text) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = {
                val ungrantedPermissions = permissions.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                
                if (ungrantedPermissions.isNotEmpty()) {
                    multiplePermissionLauncher.launch(ungrantedPermissions.toTypedArray())
                } else if (!Settings.canDrawOverlays(context)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
                
                if (PermissionUtils.areAllPermissionsGranted(context)) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PermissionSetup.route) { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant All Permissions", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        
        Spacer(Modifier.height(16.dp))
        
        TextButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        ) {
            Text("Open App Settings", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}
