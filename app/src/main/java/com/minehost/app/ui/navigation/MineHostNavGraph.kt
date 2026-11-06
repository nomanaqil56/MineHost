package com.minehost.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minehost.app.features.about.DiagnosticsScreen
import com.minehost.app.features.about.DiagnosticsViewModel
import com.minehost.app.features.console.ConsoleScreen
import com.minehost.app.features.console.ConsoleViewModel
import com.minehost.app.features.dashboard.DashboardScreen
import com.minehost.app.features.dashboard.DashboardViewModel
import com.minehost.app.features.players.PlayersScreen
import com.minehost.app.features.players.PlayersViewModel
import com.minehost.app.features.servers.CreateServerScreen
import com.minehost.app.features.servers.CreateServerViewModel
import com.minehost.app.features.servers.ServerDetailScreen
import com.minehost.app.features.servers.ServerDetailViewModel
import com.minehost.app.features.worlds.BackupsScreen
import com.minehost.app.features.worlds.BackupsViewModel

@Composable
fun MineHostNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onCreateServerClick = { navController.navigate(Screen.CreateServer.route) },
                onServerClick = { serverId -> navController.navigate(Screen.ServerDetail.createRoute(serverId)) },
                onDiagnosticsClick = { navController.navigate(Screen.Diagnostics.route) }
            )
        }

        composable(Screen.CreateServer.route) {
            val viewModel: CreateServerViewModel = hiltViewModel()
            CreateServerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onServerCreated = { serverId ->
                    navController.popBackStack()
                    navController.navigate(Screen.ServerDetail.createRoute(serverId))
                }
            )
        }

        composable(
            route = Screen.ServerDetail.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) {
            val viewModel: ServerDetailViewModel = hiltViewModel()
            ServerDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateConsole = { serverId -> navController.navigate(Screen.Console.createRoute(serverId)) },
                onNavigatePlayers = { serverId -> navController.navigate(Screen.Players.createRoute(serverId)) },
                onNavigateBackups = { serverId -> navController.navigate(Screen.Backups.createRoute(serverId)) },
                onNavigateSettings = { serverId -> navController.navigate(Screen.ServerDetail.createRoute(serverId)) }
            )
        }

        composable(
            route = Screen.Console.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) {
            val viewModel: ConsoleViewModel = hiltViewModel()
            ConsoleScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Players.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) {
            val viewModel: PlayersViewModel = hiltViewModel()
            PlayersScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Backups.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) {
            val viewModel: BackupsViewModel = hiltViewModel()
            BackupsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Diagnostics.route) {
            val viewModel: DiagnosticsViewModel = hiltViewModel()
            DiagnosticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
