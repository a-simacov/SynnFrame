package com.synngate.synnframe.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.ui.server.ServerListScreen
import timber.log.Timber

/**
 * Основной навигационный граф приложения
 */
@Composable
fun AppNavigation(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.ServerList.route
) {
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Timber.d("Current navigation route: $currentRoute")

    // Основная навигация приложения
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Экран списка серверов
        composable(Screen.ServerList.route) {
            ServerListScreen(
                appContainer = appContainer,
                navigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ServerList.route) { inclusive = false }
                    }
                }
            )
        }

        // Экран аутентификации
        composable(Screen.Login.route) {
            // Временный заполнитель для экрана логина
            // LoginScreen(
            //     appContainer = appContainer,
            //     navigateToMainMenu = {
            //         navController.navigate(Screen.MainMenu.route) {
            //             popUpTo(Screen.Login.route) { inclusive = true }
            //         }
            //     },
            //     navigateToServers = {
            //         navController.navigate(Screen.ServerList.route) {
            //             popUpTo(Screen.Login.route) { inclusive = true }
            //         }
            //     }
            // )
        }

        // Экран главного меню
        composable(Screen.MainMenu.route) {
            // Временный заполнитель для главного меню
            // MainMenuScreen(
            //     appContainer = appContainer,
            //     navigateToTasks = { navController.navigate(Screen.TaskList.route) },
            //     navigateToProducts = { navController.navigate(Screen.ProductList.route) },
            //     navigateToLogs = { navController.navigate(Screen.LogList.route) },
            //     navigateToSettings = { navController.navigate(Screen.Settings.route) },
            //     navigateToLogin = {
            //         navController.navigate(Screen.Login.route) {
            //             popUpTo(Screen.MainMenu.route) { inclusive = true }
            //         }
            //     }
            // )
        }

        // Остальные экраны будут добавлены в следующих этапах реализации
    }
}

/**
 * Класс, представляющий экраны в навигационном графе
 */
sealed class Screen(val route: String) {
    object ServerList : Screen("server_list")
    object ServerDetail : Screen("server_detail/{serverId}") {
        fun createRoute(serverId: Int? = null) = serverId?.let { "server_detail/$it" } ?: "server_detail/new"
    }
    object Login : Screen("login")
    object MainMenu : Screen("main_menu")
    object TaskList : Screen("task_list")
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
    object ProductList : Screen("product_list")
    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    object LogList : Screen("log_list")
    object LogDetail : Screen("log_detail/{logId}") {
        fun createRoute(logId: Int) = "log_detail/$logId"
    }
    object Settings : Screen("settings")
}