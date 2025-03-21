package com.synngate.synnframe.presentation.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.NavHostContainer
import com.synngate.synnframe.presentation.di.ServerListGraphContainer
import com.synngate.synnframe.presentation.ui.login.LoginScreen
import com.synngate.synnframe.presentation.ui.logs.LogDetailScreen
import com.synngate.synnframe.presentation.ui.logs.LogListScreen
import com.synngate.synnframe.presentation.ui.main.MainMenuScreen
import com.synngate.synnframe.presentation.ui.server.ServerDetailScreen
import com.synngate.synnframe.presentation.ui.server.ServerListScreen
import com.synngate.synnframe.presentation.ui.settings.SettingsScreen
import com.synngate.synnframe.presentation.ui.sync.SyncHistoryScreen
import timber.log.Timber

/**
 * Основной навигационный граф приложения с интеграцией DI
 */
@Composable
fun AppNavigation(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.ServerList.route
) {
    val context = LocalContext.current
    // Отслеживаем жизненный цикл навигационного хоста для очистки ресурсов
    val lifecycleOwner = LocalLifecycleOwner.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Timber.d("Current navigation route: $currentRoute")

    val navHostContainer = remember { appContainer.createNavHostContainer() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                Timber.d("NavHost destroyed, clearing NavHostContainer")
                navHostContainer.clear()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.ServerList.route) { entry ->
            val serverListGraphContainer = rememberServerListGraphContainer(navHostContainer, entry)

            ServerListScreen(
                viewModel = serverListGraphContainer.createServerListViewModel(),
                navigateToServerDetail = { serverId ->
                    navController.navigate(Screen.ServerDetail.createRoute(serverId))
                },
                navigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ServerList.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.ServerDetail.route,
            arguments = listOf(
                navArgument("serverId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val serverListGraphContainer = rememberServerListGraphContainer(navHostContainer, entry)

            val serverIdArg = entry.arguments?.getString("serverId")
            val serverId = serverIdArg?.toIntOrNull()

            ServerDetailScreen(
                viewModel = serverListGraphContainer.createServerDetailViewModel(serverId),
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Login.route) { entry ->
            val loginScreenContainer = remember {
                navHostContainer.createLoginScreenContainer()
            }

            // Отслеживаем жизненный цикл для очистки ресурсов
            DisposableEffect(lifecycleOwner, entry) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            Timber.d("Login screen destroyed, clearing container")
                            loginScreenContainer.clear()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LoginScreen(
                viewModel = loginScreenContainer.createLoginViewModel(),
                navigateToMainMenu = {
                    navController.navigate(Screen.MainMenu.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                navigateToServersList = {
                    navController.navigate(Screen.ServerList.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                    }
                },
                exitApp = {
                    (context as? Activity)?.finish()
                }
            )
        }

        composable(Screen.MainMenu.route) { entry ->
            val mainMenuScreenContainer = remember {
                navHostContainer.createMainMenuScreenContainer()
            }

            // Отслеживаем жизненный цикл для очистки ресурсов
            DisposableEffect(lifecycleOwner, entry) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            Timber.d("Main Menu screen destroyed, clearing container")
                            mainMenuScreenContainer.clear()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MainMenuScreen(
                viewModel = mainMenuScreenContainer.createMainMenuViewModel(),
                navigateToTasks = {
                    navController.navigate(Screen.TaskList.route) {
                        // Сохраняем главное меню в бэкстеке для возврата
                        popUpTo(Screen.MainMenu.route) { inclusive = false }
                    }
                },
                navigateToProducts = {
                    navController.navigate(Screen.ProductList.route) {
                        popUpTo(Screen.MainMenu.route) { inclusive = false }
                    }
                },
                navigateToLogs = {
                    navController.navigate(Screen.LogList.route) {
                        popUpTo(Screen.MainMenu.route) { inclusive = false }
                    }
                },
                navigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.MainMenu.route) { inclusive = false }
                    }
                },
                navigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        // При смене пользователя удаляем главное меню из бэкстека
                        popUpTo(Screen.MainMenu.route) { inclusive = true }
                    }
                },
                exitApp = {
                    (context as? Activity)?.finish()
                }
            )
        }

        composable(Screen.Settings.route) { entry ->
            val settingsScreenContainer = remember {
                navHostContainer.createSettingsScreenContainer()
            }

            // Отслеживаем жизненный цикл для очистки ресурсов
            DisposableEffect(lifecycleOwner, entry) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            Timber.d("Settings screen destroyed, clearing container")
                            settingsScreenContainer.clear()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            SettingsScreen(
                viewModel = settingsScreenContainer.createSettingsViewModel(),
                navigateToServerList = {
                    navController.navigate(Screen.ServerList.route) {
                        popUpTo(Screen.Settings.route) { inclusive = false }
                    }
                },
                navigateToSyncHistory = {
                    navController.navigate(Screen.SyncHistory.route)
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.LogList.route) { entry ->
            val logsGraphContainer = remember {
                navHostContainer.createLogsGraphContainer()
            }

            // Отслеживаем жизненный цикл для очистки ресурсов
            DisposableEffect(lifecycleOwner, entry) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            Timber.d("Logs graph destroyed, clearing container")
                            logsGraphContainer.clear()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LogListScreen(
                viewModel = logsGraphContainer.createLogListViewModel(),
                navigateToLogDetail = { logId ->
                    navController.navigate(Screen.LogDetail.createRoute(logId))
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.LogDetail.route,
            arguments = listOf(
                navArgument("logId") {
                    type = NavType.IntType
                }
            )
        ) { entry ->
            val logsGraphContainer = remember {
                navHostContainer.createLogsGraphContainer()
            }

            val logId = entry.arguments?.getInt("logId") ?: 0

            // Отслеживаем жизненный цикл для очистки ресурсов
            DisposableEffect(lifecycleOwner, entry) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            Timber.d("LogDetail screen destroyed, clearing container")
                            logsGraphContainer.clear()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LogDetailScreen(
                viewModel = logsGraphContainer.createLogDetailViewModel(logId),
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Подключаем граф навигации для заданий
        tasksNavGraph(
            navController = navController,
            tasksGraphContainer = navHostContainer.createTasksGraphContainer(),
            lifecycleOwner = lifecycleOwner,
            navigateToProductsList = { isSelectionMode ->
                navController.navigate(Screen.ProductList.createRoute(isSelectionMode))
            }
        )

        productsNavGraph(
            navController = navController,
            productsGraphContainer = navHostContainer.createProductsGraphContainer(),
            lifecycleOwner = lifecycleOwner,
            returnProductToTask = { product ->
                // Возвращаемся к предыдущему экрану (экрану задания)
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "selected_product", product
                )
                navController.popBackStack()
            }
        )

        composable(Screen.SyncHistory.route) { entry ->
            val settingsScreenContainer = remember {
                navHostContainer.createSettingsScreenContainer()
            }

            // Отслеживаем жизненный цикл для очистки ресурсов
            DisposableEffect(lifecycleOwner, entry) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            Timber.d("SyncHistory screen destroyed, clearing container")
                            settingsScreenContainer.clear()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            SyncHistoryScreen(
                viewModel = settingsScreenContainer.createSyncHistoryViewModel(),
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Остальные экраны будут добавлены в следующих этапах реализации
    }
}

/**
 * Получение контейнера для подграфа серверов с отслеживанием жизненного цикла
 */
@Composable
private fun rememberServerListGraphContainer(
    navHostContainer: NavHostContainer,
    entry: NavBackStackEntry
): ServerListGraphContainer {
    val container = remember(navHostContainer) {
        navHostContainer.createServerListGraphContainer()
    }

    // Отслеживаем жизненный цикл для очистки ресурсов
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, entry) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    Timber.d("ServerList graph destroyed, clearing container")
                    container.clear()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return container
}

sealed class Screen(val route: String) {
    object ServerList : Screen("server_list")
    object ServerDetail : Screen("server_detail/{serverId}") {
        fun createRoute(serverId: Int? = null) =
            serverId?.let { "server_detail/$it" } ?: "server_detail/new"
    }

    object Login : Screen("login")
    object MainMenu : Screen("main_menu")
    object TaskList : Screen("task_list")
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }

    object ProductList : Screen("product_list?isSelectionMode={isSelectionMode}") {
        fun createRoute(isSelectionMode: Boolean = false) = "product_list?isSelectionMode=$isSelectionMode"
    }
    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }

    object LogList : Screen("log_list")
    object LogDetail : Screen("log_detail/{logId}") {
        fun createRoute(logId: Int) = "log_detail/$logId"
    }

    object Settings : Screen("settings")

    object SyncHistory : Screen("sync_history")
}