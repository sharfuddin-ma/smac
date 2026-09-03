package com.mistavinya.smac

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mistavinya.smac.service.CallRecordingService
import com.mistavinya.smac.storage.LocalStorageManager
import com.mistavinya.smac.ui.navigation.MainScreen
import com.mistavinya.smac.ui.navigation.Screen
import com.mistavinya.smac.ui.theme.SalesEdgeAITheme
import com.mistavinya.smac.util.DeviceInfoUtil
import com.mistavinya.smac.util.PermissionUtils
import com.mistavinya.smac.util.SettingsDataStore
import com.mistavinya.smac.util.ThemePreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure window adjusts for keyboard (needed for form screens)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        try {
            Log.i("CALLSYNC_DEBUG", "MainActivity.onCreate() started")
            Log.i("CALLSYNC_DEBUG", "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            Log.i("CALLSYNC_DEBUG", "Android: ${Build.VERSION.RELEASE} API ${Build.VERSION.SDK_INT}")

            enableEdgeToEdge()
            Log.i("CALLSYNC_DEBUG", "enableEdgeToEdge() done")

            DeviceInfoUtil.logAllDeviceInfo(this)
            
            // Initialize storage structure
            LocalStorageManager(this)
            settingsDataStore = SettingsDataStore(this)
            val themePreferences = ThemePreferences(this)
            
            val navigateTo = intent.getStringExtra("navigate_to") ?: "splash"
            val callLogId = intent.getStringExtra("call_log_id")
            
            setContent {
                Log.i("CALLSYNC_DEBUG", "setContent starting")
                val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)
                
                SalesEdgeAITheme(darkTheme = isDarkMode) {
                    MainScreen(
                        startDestination = if (navigateTo == "client_form") Screen.ClientForm.route else Screen.Splash.route,
                        startCallLogId = callLogId
                    )
                }
            }

            Log.i("CALLSYNC_DEBUG", "MainActivity.onCreate() completed successfully")

            // Start call monitoring service immediately
            checkAndStartService()

        } catch (e: Exception) {
            Log.e("CALLSYNC_CRASH", "CRASH in onCreate: ${e.javaClass.simpleName}: ${e.message}")
            Log.e("CALLSYNC_CRASH", "Stack: ${e.stackTraceToString()}")
            
            // Show a basic error screen instead of crashing
            setContent {
                SalesEdgeAITheme {
                    Surface {
                        Text(
                            text = "App Error: ${e.message}\n\nPlease report this to the developer.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndStartService()
    }

    private fun checkAndStartService() {
        try {
            val dpm = getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager

            if (dpm.isDeviceOwnerApp(packageName)) {
                // ═══ MDM DEVICE OWNER ═══
                // All permissions are force-granted. No checks needed. Start service directly.
                Log.i("CALLSYNC_DEBUG", "MDM Device Owner ✅ — starting CallRecordingService directly")
                startCallService()
            } else {
                // ═══ NON-DEVICE OWNER (Development/Testing only) ═══
                lifecycleScope.launch {
                    if (PermissionUtils.areAllPermissionsGranted(this@MainActivity)) {
                        Log.i("CALLSYNC_DEBUG", "Permissions granted ✅ — starting CallRecordingService")
                        startCallService()
                    } else {
                        Log.w("CALLSYNC_DEBUG", "⚠️ Permissions NOT granted — service NOT started")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CALLSYNC_DEBUG", "❌ checkAndStartService error: ${e.message}", e)
        }
    }

    private fun startCallService() {
        try {
            val serviceIntent = Intent(this, CallRecordingService::class.java).apply {
                action = CallRecordingService.ACTION_START_MONITORING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.i("CALLSYNC_DEBUG", "✅ CallRecordingService started successfully")
        } catch (e: Exception) {
            Log.e("CALLSYNC_DEBUG", "❌ Failed to start CallRecordingService: ${e.message}", e)
        }
    }
}
