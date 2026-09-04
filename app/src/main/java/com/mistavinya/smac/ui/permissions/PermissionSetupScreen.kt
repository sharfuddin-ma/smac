package com.mistavinya.smac.ui.permissions

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.mistavinya.smac.util.MdmConfigReader
import com.mistavinya.smac.util.PermissionGranter
import com.mistavinya.smac.util.PermissionUtils
import kotlinx.coroutines.delay

@Composable
fun PermissionSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Track current permission state
    var runtimeGranted by remember {
        mutableStateOf(PermissionUtils.areRuntimePermissionsGranted(context))
    }
    var overlayGranted by remember {
        mutableStateOf(PermissionUtils.isOverlayGranted(context))
    }

    // Path bypass logic
    LaunchedEffect(Unit) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val isDeviceOwner = dpm.isDeviceOwnerApp(context.packageName)
        val hasMdmConfig = MdmConfigReader.hasConfig(context)
        
        // If DO or MDM, we can skip the manual setup screen entirely
        if (isDeviceOwner || hasMdmConfig) {
            Log.i("PermissionSetup", "Bypassing screen: DO=$isDeviceOwner, MDM=$hasMdmConfig")
            if (isDeviceOwner) PermissionGranter.grantAllPermissions(context)
            
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
            return@LaunchedEffect
        }
        
        // If runtime already granted, we might still need overlay
        if (runtimeGranted && overlayGranted) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    var requestCount by remember { mutableIntStateOf(0) }
    var showOverlayStep by remember { mutableStateOf(false) }

    // Logic to decide if we should show overlay step
    // Show overlay step if runtime is granted (or we are skipping it) and overlay is missing
    val currentStepIsOverlay = (runtimeGranted || requestCount > 0) && !overlayGranted && showOverlayStep

    // Re-check permissions when user returns from Settings/Overlay screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                runtimeGranted = PermissionUtils.areRuntimePermissionsGranted(context)
                overlayGranted = PermissionUtils.isOverlayGranted(context)

                if (runtimeGranted && overlayGranted) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        requestCount++
        runtimeGranted = PermissionUtils.areRuntimePermissionsGranted(context)
        if (runtimeGranted) {
            if (!overlayGranted) {
                showOverlayStep = true
            } else {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        } else {
            // Show skip option even if denied
            showOverlayStep = true
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
        if (!currentStepIsOverlay) {
            // ═══════════════════════════════════════════════════
            // STEP 1: RUNTIME PERMISSIONS
            // ═══════════════════════════════════════════════════
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
                "SalesEdgeAI needs permissions to manage your business calls. This screen is for manual configuration if MDM auto-grant is not available.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(32.dp))

            // Permission list with state icons
            val items = listOf(
                Triple(Manifest.permission.READ_PHONE_STATE, "Phone", Icons.Default.Phone),
                Triple(Manifest.permission.READ_CALL_LOG, "Call Log", Icons.AutoMirrored.Filled.ListAlt),
                Triple(Manifest.permission.READ_CONTACTS, "Contacts", Icons.Default.Person),
                Triple(Manifest.permission.RECORD_AUDIO, "Microphone", Icons.Default.Mic)
            )

            items.forEach { (perm, label, icon) ->
                val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (granted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    if (granted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val missing = PermissionUtils.getMissingPermissions(context).toTypedArray()
                    if (missing.isNotEmpty()) {
                        launcher.launch(missing)
                    } else {
                        if (!overlayGranted) {
                            showOverlayStep = true
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (runtimeGranted) "Continue" else "Grant Permissions")
            }

            if (requestCount > 0 || runtimeGranted) {
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        if (!overlayGranted) {
                            showOverlayStep = true
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text(
                        if (runtimeGranted) "Next" else "Skip for now (Limited Mode)",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // ═══════════════════════════════════════════════════
            // STEP 2: OVERLAY PERMISSION
            // ═══════════════════════════════════════════════════
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Display Over Other Apps",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "SalesEdgeAI needs 'Display over other apps' permission to show the post-call classification popup. Without this, you'll need to open the app manually after each call.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    PermissionUtils.requestOverlayPermission(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Grant Overlay Permission")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            ) {
                Text(
                    "Skip for now",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
