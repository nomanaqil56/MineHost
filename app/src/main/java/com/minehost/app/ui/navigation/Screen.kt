package com.minehost.app.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object CreateServer : Screen("create_server")
    data object ServerDetail : Screen("server_detail/{serverId}") {
        fun createRoute(serverId: String) = "server_detail/$serverId"
    }
    data object Console : Screen("console/{serverId}") {
        fun createRoute(serverId: String) = "console/$serverId"
    }
    data object Players : Screen("players/{serverId}") {
        fun createRoute(serverId: String) = "players/$serverId"
    }
    data object Settings : Screen("settings/{serverId}") {
        fun createRoute(serverId: String) = "settings/$serverId"
    }
    data object Backups : Screen("backups/{serverId}") {
        fun createRoute(serverId: String) = "backups/$serverId"
    }
    data object Diagnostics : Screen("diagnostics")
}
