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
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.NavHostContainer
import com.synngate.synnframe.presentation.di.ServerListGraphContainer
import com.synngate.synnframe.presentation.ui.login.LoginScreen
import com.synngate.synnframe.presentation.ui.main.MainMenuScreen
import com.synngate.synnframe.presentation.ui.server.ServerDetailScreen
import com.synngate.synnframe.presentation.ui.server.ServerListScreen
import com.synngate.synnframe.presentation.ui.settings.SettingsScreen
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Timber.d("Current navigation route: $currentRoute")

    // Создаем контейнер для NavHost
    val navHostContainer = remember { (appContainer as AppContainer).createNavHostContainer() }

    // Отслеживаем жизненный цикл навигационного хоста для очистки ресурсов
    val lifecycleOwner = LocalLifecycleOwner.current
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

    // Основная навигация приложения
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Экран списка серверов
        composable(Screen.ServerList.route) { entry ->
            // Получаем контейнер для подграфа серверов
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

        // Экран деталей сервера
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
            // Получаем контейнер для подграфа серверов
            val serverListGraphContainer = rememberServerListGraphContainer(navHostContainer, entry)

            // Получаем serverId из аргументов
            val serverIdArg = entry.arguments?.getString("serverId")
            val serverId = serverIdArg?.toIntOrNull()

            ServerDetailScreen(
                viewModel = serverListGraphContainer.createServerDetailViewModel(serverId),
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран аутентификации
        composable(Screen.Login.route) { entry ->
            // Получаем контейнер для экрана логина
            val loginScreenContainer = remember {
                navHostContainer.createLoginScreenContainer()
            }

            // Отслеживаем жизненный цикл для очистки ресурсов
            //val lifecycleOwner = LocalLifecycleOwner.current
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

        // Экран главного меню
        composable(Screen.MainMenu.route) { entry ->
            // Получаем контейнер для экрана главного меню
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

        // Экран настроек
        composable(Screen.Settings.route) { entry ->
            // Получаем контейнер для экрана настроек
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
    // Создаем контейнер для подграфа
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

/**
 * Класс, представляющий экраны в навигационном графе
 */
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