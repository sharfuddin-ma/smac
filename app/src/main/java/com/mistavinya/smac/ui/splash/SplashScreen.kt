package com.mistavinya.smac.ui.splash

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mistavinya.smac.ui.navigation.Screen
import com.mistavinya.smac.util.PermissionUtils
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(1500) // Splash delay

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (dpm.isDeviceOwnerApp(context.packageName)) {
            // ═══ DEVICE OWNER — Go directly to Home ═══
            // Permissions are already forced in CallSyncApp.onCreate()
            // Device info is already read and saved in CallSyncApp.onCreate()
            // Nothing to setup — app is ready
            Log.i("Navigation", "Device Owner → Home ✅")
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            // ═══ NOT DEVICE OWNER — Development fallback ═══
            val allGranted = PermissionUtils.areAllPermissionsGranted(context)
            if (!allGranted) {
                navController.navigate(Screen.PermissionSetup.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF283593))
                )
            )
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Logo",
                        tint = Color(0xFF1A237E),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            @Suppress("DEPRECATION")
            Text(
                text = "CallSync",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            @Suppress("DEPRECATION")
            Text(
                text = "Professional Call Management",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        @Suppress("DEPRECATION")
        Text(
            text = "Mist Avinya Technologies",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )
    }
}
