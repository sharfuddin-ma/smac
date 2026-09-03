package com.mistavinya.smac.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object PermissionSetup : Screen("permission_setup")
    object Home : Screen("home")
    object CallHistory : Screen("call_history")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object RecordingsList : Screen("recordings_list")
    object ClientForm : Screen("client_form/{callLogId}") {
        fun createRoute(callLogId: String) = "client_form/$callLogId"
    }
}
