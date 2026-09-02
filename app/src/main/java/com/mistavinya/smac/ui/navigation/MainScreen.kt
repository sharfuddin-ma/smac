package com.mistavinya.smac.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.mistavinya.smac.ui.clientform.ClientFormScreen
import com.mistavinya.smac.ui.history.CallHistoryScreen
import com.mistavinya.smac.ui.home.HomeScreen
import com.mistavinya.smac.ui.permissions.PermissionSetupScreen
import com.mistavinya.smac.ui.profile.DeviceInfoScreen
import com.mistavinya.smac.ui.recordings.RecordingsListScreen
import com.mistavinya.smac.ui.settings.SettingsScreen
import com.mistavinya.smac.ui.splash.SplashScreen

@Composable
fun MainScreen(startDestination: String = Screen.Splash.route, startCallLogId: Long? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.CallHistory.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startDestination == Screen.ClientForm.route && startCallLogId != null) 
                Screen.ClientForm.createRoute(startCallLogId) 
            else startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) { SplashScreen(navController) }
            composable(Screen.PermissionSetup.route) { PermissionSetupScreen(navController) }
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.CallHistory.route) { CallHistoryScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable(Screen.Profile.route) { DeviceInfoScreen(navController) }
            composable(Screen.RecordingsList.route) { RecordingsListScreen(navController) }
            composable(
                route = Screen.ClientForm.route,
                arguments = listOf(navArgument("callLogId") { type = NavType.LongType })
            ) { backStackEntry ->
                val callLogId = backStackEntry.arguments?.getLong("callLogId") ?: -1L
                ClientFormScreen(callLogId, navController)
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name)
    }
}
