package com.synngate.synnframe.presentation.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.presentation.di.ScreenContainer
import com.synngate.synnframe.presentation.ui.login.LoginScreen
import com.synngate.synnframe.presentation.ui.logs.LogDetailScreen
import com.synngate.synnframe.presentation.ui.logs.LogListScreen
import com.synngate.synnframe.presentation.ui.main.MainMenuScreen
import com.synngate.synnframe.presentation.ui.products.ProductDetailScreen
import com.synngate.synnframe.presentation.ui.products.ProductListScreen
import com.synngate.synnframe.presentation.ui.server.ServerDetailScreen
import com.synngate.synnframe.presentation.ui.server.ServerListScreen
import com.synngate.synnframe.presentation.ui.settings.SettingsScreen
import com.synngate.synnframe.presentation.ui.sync.SyncHistoryScreen
import com.synngate.synnframe.presentation.ui.tasks.TaskDetailScreen
import com.synngate.synnframe.presentation.ui.tasks.TaskListScreen
import timber.log.Timber

/**
 * Основной навигационный компонент приложения
 */
@Composable
fun AppNavigation(
    startDestination: String = Screen.ServerList.route,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Получаем приложение и создаем контейнер навигации
    val app = context.applicationContext as SynnFrameApplication
    val navigationContainer = remember { app.appContainer.createNavigationContainer() }

    // Создаем контейнер экрана
    val screenContainer = remember { navigationContainer.createScreenContainer() }

    // Отслеживаем жизненный цикл для освобождения ресурсов
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                Timber.d("Navigation destroyed, disposing navigation container")
                navigationContainer.dispose()
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
        // Экран списка серверов
        composable(Screen.ServerList.route) {
            val viewModel = remember { screenContainer.createServerListViewModel() }

            ServerListScreen(
                viewModel = viewModel,
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

        // Экран детальной информации о сервере
        composable(
            route = Screen.ServerDetail.route,
            arguments = listOf(
                navArgument("serverId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            val serverIdArg = it.arguments?.getString("serverId")
            val serverId = serverIdArg?.toIntOrNull()

            val viewModel = remember { screenContainer.createServerDetailViewModel(serverId) }

            ServerDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран логина
        composable(Screen.Login.route) {
            val viewModel = remember { screenContainer.createLoginViewModel() }

            LoginScreen(
                viewModel = viewModel,
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

        // Главное меню
        composable(Screen.MainMenu.route) {
            val viewModel = remember { screenContainer.createMainMenuViewModel() }

            MainMenuScreen(
                viewModel = viewModel,
                navigateToTasks = {
                    navController.navigate(Screen.TaskList.route) {
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
                        popUpTo(Screen.MainMenu.route) { inclusive = true }
                    }
                },
                exitApp = {
                    (context as? Activity)?.finish()
                }
            )
        }

        // Экран настроек
        composable(Screen.Settings.route) {
            val viewModel = remember { screenContainer.createSettingsViewModel() }

            SettingsScreen(
                viewModel = viewModel,
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

        // Граф навигации для задач
        tasksNavGraph(
            navController = navController,
            screenContainer = screenContainer
        )

        // Граф навигации для продуктов
        productsNavGraph(
            navController = navController,
            screenContainer = screenContainer
        )

        // Граф навигации для логов
        logsNavGraph(
            navController = navController,
            screenContainer = screenContainer
        )

        // Экран истории синхронизации
        composable(Screen.SyncHistory.route) {
            val viewModel = remember { screenContainer.createSyncHistoryViewModel() }

            SyncHistoryScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Функция добавления графа навигации для задач
 */
fun NavGraphBuilder.tasksNavGraph(
    navController: NavController,
    screenContainer: ScreenContainer
) {
    navigation(
        startDestination = Screen.TaskList.route,
        route = "tasks_graph"
    ) {
        // Экран списка задач
        composable(Screen.TaskList.route) {
            val viewModel = remember { screenContainer.createTaskListViewModel() }

            TaskListScreen(
                viewModel = viewModel,
                navigateToTaskDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран детальной информации о задаче
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                }
            )
        ) {
            val taskId = it.arguments?.getString("taskId") ?: ""
            val viewModel = remember { screenContainer.createTaskDetailViewModel(taskId) }

            TaskDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                },
                navigateToProductsList = {
                    navController.navigate(Screen.ProductList.createRoute(true))
                },
                navController = navController
            )
        }
    }
}

/**
 * Функция добавления графа навигации для продуктов
 */
fun NavGraphBuilder.productsNavGraph(
    navController: NavController,
    screenContainer: ScreenContainer
) {
    navigation(
        startDestination = Screen.ProductList.route,
        route = "products_graph"
    ) {
        // Экран списка продуктов
        composable(
            route = Screen.ProductList.route,
            arguments = listOf(
                navArgument("isSelectionMode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            val isSelectionMode = it.arguments?.getBoolean("isSelectionMode") ?: false
            val viewModel = remember { screenContainer.createProductListViewModel(isSelectionMode) }

            ProductListScreen(
                viewModel = viewModel,
                navigateToProductDetail = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                navigateBack = {
                    navController.popBackStack()
                },
                returnProductToTask = { product ->
                    // Возвращаемся к предыдущему экрану (экрану задания)
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "selected_product", product
                    )
                    navController.popBackStack()
                }
            )
        }

        // Экран детальной информации о продукте
        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) {
            val productId = it.arguments?.getString("productId") ?: ""
            val viewModel = remember { screenContainer.createProductDetailViewModel(productId) }

            ProductDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                },
                navigateToProduct = { newProductId ->
                    navController.navigate(Screen.ProductDetail.createRoute(newProductId)) {
                        popUpTo(Screen.ProductDetail.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Функция добавления графа навигации для логов
 */
fun NavGraphBuilder.logsNavGraph(
    navController: NavController,
    screenContainer: ScreenContainer
) {
    navigation(
        startDestination = Screen.LogList.route,
        route = "logs_graph"
    ) {
        // Экран списка логов
        composable(Screen.LogList.route) {
            val viewModel = remember { screenContainer.createLogListViewModel() }

            LogListScreen(
                viewModel = viewModel,
                navigateToLogDetail = { logId ->
                    navController.navigate(Screen.LogDetail.createRoute(logId))
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран детальной информации о логе
        composable(
            route = Screen.LogDetail.route,
            arguments = listOf(
                navArgument("logId") {
                    type = NavType.IntType
                }
            )
        ) {
            val logId = it.arguments?.getInt("logId") ?: 0
            val viewModel = remember { screenContainer.createLogDetailViewModel(logId) }

            LogDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Запечатанный класс для определения экранов навигации
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