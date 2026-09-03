package com.mistavinya.smac.ui.permissions

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
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
import com.mistavinya.smac.util.PermissionGranter
import com.mistavinya.smac.util.PermissionUtils

@Composable
fun PermissionSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ═══ DEVICE OWNER BYPASS ═══
    // If app is Device Owner, permissions are already granted — skip this screen immediately
    LaunchedEffect(Unit) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isDeviceOwnerApp(context.packageName)) {
            Log.i("PermissionSetup", "Device Owner detected — bypassing permission screen")

            // Force re-grant just in case
            PermissionGranter.grantAllPermissions(context)

            // Navigate away immediately
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.PermissionSetup.route) { inclusive = true }
            }
            return@LaunchedEffect
        }
    }
    // ═══ END BYPASS ═══

    // Track current permission state
    var runtimeGranted by remember {
        mutableStateOf(PermissionUtils.areRuntimePermissionsGranted(context))
    }
    var overlayGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    // Status text to show user what's needed
    var statusText by remember { mutableStateOf("Tap below to grant permissions") }

    val requiredPermissions = remember { PermissionUtils.getRequiredRuntimePermissions() }

    // Runtime permission launcher
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        runtimeGranted = PermissionUtils.areRuntimePermissionsGranted(context)

        if (runtimeGranted) {
            // Runtime permissions done — now handle overlay
            PermissionUtils.requestBatteryOptimizationExemption(context)

            if (!Settings.canDrawOverlays(context)) {
                statusText = "Now grant Overlay permission to show post-call popup"
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            } else {
                // Everything granted — navigate
                overlayGranted = true
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.PermissionSetup.route) { inclusive = true }
                }
            }
        } else {
            statusText = "Some permissions were denied. Tap to try again or open App Settings."
        }
    }

    // Re-check permissions when user returns from Settings/Overlay screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                runtimeGranted = PermissionUtils.areRuntimePermissionsGranted(context)
                overlayGranted = Settings.canDrawOverlays(context)

                if (runtimeGranted && overlayGranted) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PermissionSetup.route) { inclusive = true }
                    }
                } else if (runtimeGranted && !overlayGranted) {
                    statusText = "Overlay permission still needed. Tap below to grant."
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-check on first composition
    LaunchedEffect(Unit) {
        if (runtimeGranted && overlayGranted) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.PermissionSetup.route) { inclusive = true }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════
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

        @Suppress("DEPRECATION")
        Text(
            "Permissions Required",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "SalesEdgeAI needs the following permissions to manage your business calls. These are mandatory for the app to function.",
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
            Icons.Default.Person to "Contacts — Identify caller names",
            Icons.Default.Folder to "Storage — Access call recordings",
            Icons.Default.Notifications to "Notifications — Show status",
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
                    tint = if (text.startsWith("Overlay") && !overlayGranted) {
                        MaterialTheme.colorScheme.error
                    } else if (runtimeGranted || (text.startsWith("Overlay") && overlayGranted)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Status text
        Text(
            statusText,
            fontSize = 13.sp,
            color = if (statusText.contains("denied") || statusText.contains("still needed")) 
                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Main action button — changes based on state
        Button(
            onClick = {
                if (!runtimeGranted) {
                    // Step 1: Request runtime permissions
                    val ungrantedPermissions = requiredPermissions.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }.toTypedArray()

                    if (ungrantedPermissions.isNotEmpty()) {
                        multiplePermissionLauncher.launch(ungrantedPermissions)
                    } else {
                        // All runtime granted but state not updated
                        runtimeGranted = true
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                } else if (!overlayGranted) {
                    // Step 2: Request overlay permission
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                } else {
                    // All done — navigate
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
            Text(
                text = when {
                    !runtimeGranted -> "Grant Permissions"
                    !overlayGranted -> "Grant Overlay Permission"
                    else -> "Continue"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(16.dp))

        @Suppress("DEPRECATION")
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
