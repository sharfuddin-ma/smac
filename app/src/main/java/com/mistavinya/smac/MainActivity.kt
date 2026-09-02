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
import com.mistavinya.smac.ui.theme.CallSyncTheme
import com.mistavinya.smac.util.DeviceInfoUtil
import com.mistavinya.smac.util.PermissionUtils
import com.mistavinya.smac.util.SettingsDataStore
import com.mistavinya.smac.util.ThemePreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            val callLogId = intent.getLongExtra("call_log_id", -1L).takeIf { it != -1L }
            
            setContent {
                Log.i("CALLSYNC_DEBUG", "setContent starting")
                val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)
                
                CallSyncTheme(darkTheme = isDarkMode) {
                    MainScreen(
                        startDestination = if (navigateTo == "client_form") Screen.ClientForm.route else Screen.Splash.route,
                        startCallLogId = callLogId
                    )
                }
            }

            Log.i("CALLSYNC_DEBUG", "MainActivity.onCreate() completed successfully")

        } catch (e: Exception) {
            Log.e("CALLSYNC_CRASH", "CRASH in onCreate: ${e.javaClass.simpleName}: ${e.message}")
            Log.e("CALLSYNC_CRASH", "Stack: ${e.stackTraceToString()}")
            
            // Show a basic error screen instead of crashing
            setContent {
                CallSyncTheme {
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
        lifecycleScope.launch {
            try {
                if (PermissionUtils.areAllPermissionsGranted(this@MainActivity)) {
                    val intent = Intent(this@MainActivity, CallRecordingService::class.java).apply {
                        action = CallRecordingService.ACTION_START_MONITORING
                    }
                    startForegroundService(intent)
                    Log.i("CALLSYNC_DEBUG", "startForegroundService(ACTION_START_MONITORING) called")
                }
            } catch (e: Exception) {
                Log.e("CALLSYNC_CRASH", "Error starting service in onResume: ${e.message}")
            }
        }
    }
}
