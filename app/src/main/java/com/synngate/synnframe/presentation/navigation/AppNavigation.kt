package com.synngate.synnframe.presentation.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.presentation.common.LocalCurrentUser
import com.synngate.synnframe.presentation.ui.dynamicmenu.DynamicMenuScreen
import com.synngate.synnframe.presentation.ui.dynamicmenu.DynamicTaskDetailScreen
import com.synngate.synnframe.presentation.ui.dynamicmenu.DynamicTasksScreen
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
import com.synngate.synnframe.presentation.ui.taskx.TaskXDetailScreen
import com.synngate.synnframe.presentation.ui.taskx.TaskXListScreen
import timber.log.Timber
import java.net.URLEncoder.encode

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

    // Получаем приложение
    val app = context.applicationContext as SynnFrameApplication

    // Получаем данные о текущем пользователе
    val userRepository = app.appContainer.userRepository
    val currentUser by userRepository.getCurrentUser().collectAsState(initial = null)

    // Создаем менеджер областей навигации
    val navigationScopeManager = remember {
        NavigationScopeManager(navController) {
            app.appContainer.createNavigationContainer().createScreenContainer()
        }
    }

    // Отслеживаем жизненный цикл для освобождения ресурсов
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                Timber.d("Navigation destroyed, disposing all containers")
                navigationScopeManager.dispose()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(
        LocalCurrentUser provides currentUser
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // Экран списка серверов
            composable(Screen.ServerList.route) { entry ->
                val screenContainer =
                    rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
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
            ) { entry ->
                val serverIdArg = entry.arguments?.getString("serverId")
                val serverId = serverIdArg?.toIntOrNull()

                val screenContainer =
                    rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
                val viewModel =
                    remember(serverId) { screenContainer.createServerDetailViewModel(serverId) }

                ServerDetailScreen(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Экран логина
            composable(Screen.Login.route) { entry ->
                val screenContainer =
                    rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
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
            composable(Screen.MainMenu.route) { entry ->
                val screenContainer =
                    rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
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
                    navigateToTasksX = { // Добавляем обработчик для навигации к заданиям X
                        navController.navigate(Screen.TaskXList.route) {
                            popUpTo(Screen.MainMenu.route) { inclusive = false }
                        }
                    },
                    navigateToDynamicMenu = {
                        navController.navigate(Screen.DynamicMenu.route) {
                            popUpTo(Screen.MainMenu.route) { inclusive = false }
                        }
                    },
                    exitApp = {
                        (context as? Activity)?.finish()
                    }
                )
            }

            // Экран настроек
            composable(Screen.Settings.route) { entry ->
                val screenContainer =
                    rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
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

            composable(Screen.DynamicMenu.route) { entry ->
                val screenContainer = rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
                val viewModel = remember { screenContainer.createDynamicMenuViewModel() }

                DynamicMenuScreen(
                    viewModel = viewModel,
                    navigateToDynamicTasks = { menuItemId, menuItemName, menuItemType ->
                        navController.navigate(Screen.DynamicTasks.createRoute(
                            menuItemId = menuItemId,
                            menuItemName = menuItemName,
                            menuItemType = menuItemType
                        ))
                    },
                    navigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.DynamicTasks.route,
                arguments = listOf(
                    navArgument("menuItemId") {
                        type = NavType.StringType
                    },
                    navArgument("menuItemName") {
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument("menuItemType") {
                        type = NavType.StringType
                        defaultValue = DynamicMenuItemType.SHOW_LIST.name
                    }
                )
            ) { entry ->
                val menuItemId = entry.arguments?.getString("menuItemId") ?: ""
                val encodedMenuItemName = entry.arguments?.getString("menuItemName") ?: ""
                val menuItemName = java.net.URLDecoder.decode(encodedMenuItemName, "UTF-8")
                val menuItemTypeStr = entry.arguments?.getString("menuItemType") ?: DynamicMenuItemType.SHOW_LIST.name
                val menuItemType = DynamicMenuItemType.fromString(menuItemTypeStr)

                val screenContainer = rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
                val viewModel = remember(menuItemId, menuItemName, menuItemType) {
                    screenContainer.createDynamicTasksViewModel(menuItemId, menuItemName, menuItemType)
                }

                DynamicTasksScreen(
                    viewModel = viewModel,
                    navigateToTaskDetail = { task ->
                        navController.navigate(Screen.DynamicTaskDetail.createRoute(task))
                    },
                    navigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.DynamicTaskDetail.route,
                arguments = listOf(
                    navArgument("taskId") {
                        type = NavType.StringType
                    },
                    navArgument("taskName") {
                        type = NavType.StringType
                        nullable = true
                    }
                )
            ) { entry ->
                val taskId = entry.arguments?.getString("taskId") ?: ""
                val encodedTaskName = entry.arguments?.getString("taskName") ?: ""
                val taskName = java.net.URLDecoder.decode(encodedTaskName, "UTF-8")

                val screenContainer = rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
                val viewModel = remember(taskId, taskName) {
                    screenContainer.createDynamicTaskDetailViewModel(
                        DynamicTask(id = taskId, name = taskName)
                    )
                }

                DynamicTaskDetailScreen(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Граф навигации для задач
            tasksNavGraph(
                navController = navController,
                navigationScopeManager = navigationScopeManager
            )

            // Граф навигации для продуктов
            productsNavGraph(
                navController = navController,
                navigationScopeManager = navigationScopeManager
            )

            // Граф навигации для логов
            logsNavGraph(
                navController = navController,
                navigationScopeManager = navigationScopeManager
            )

            taskXNavGraph(
                navController = navController,
                navigationScopeManager = navigationScopeManager
            )

            // Экран истории синхронизации
            composable(Screen.SyncHistory.route) { entry ->
                val screenContainer =
                    rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
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
}

/**
 * Функция добавления графа навигации для задач
 */
fun NavGraphBuilder.tasksNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    // Получаем контейнер для графа напрямую
    val graphContainer = navigationScopeManager.getGraphContainer("tasks_graph")

    navigation(
        startDestination = Screen.TaskList.route,
        route = "tasks_graph"
    ) {
        // Экран списка задач - используем ПОСТОЯННЫЙ контейнер
        composable(Screen.TaskList.route) { entry ->
            // Для списка задач используем постоянный контейнер, чтобы сохранять состояние
            val screenContainer =
                rememberPersistentScreenContainer(navController, entry, navigationScopeManager)
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

        // Экран детальной информации о задаче - используем ВРЕМЕННЫЙ контейнер
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId") ?: ""
            // Для деталей задачи используем временный контейнер, который будет уничтожен при возврате
            val screenContainer =
                rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
            val viewModel = remember(taskId) { screenContainer.createTaskDetailViewModel(taskId) }

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
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    // Получаем контейнер для графа напрямую
    val graphContainer = navigationScopeManager.getGraphContainer("products_graph")

    navigation(
        startDestination = Screen.ProductList.route,
        route = "products_graph"
    ) {
        // Экран списка продуктов - используем ПОСТОЯННЫЙ контейнер
        composable(
            route = Screen.ProductList.route,
            arguments = listOf(
                navArgument("isSelectionMode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            val isSelectionMode = entry.arguments?.getBoolean("isSelectionMode") ?: false
            // Для списка продуктов используем постоянный контейнер
            val screenContainer =
                rememberPersistentScreenContainer(navController, entry, navigationScopeManager)
            val viewModel = remember(isSelectionMode) {
                screenContainer.createProductListViewModel(isSelectionMode)
            }

            ProductListScreen(
                viewModel = viewModel,
                navigateToProductDetail = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId)) {
                        // Добавляем эти параметры для правильной работы кнопки "Назад"
                        launchSingleTop = true  // Предотвращает создание множественных экземпляров
                        popUpTo(Screen.ProductList.route) // Определяем, до какого экрана "сворачивать" при нажатии "Назад"
                    }
                },
                navigateBack = {
                    navController.popBackStack()
                },
                navController = navController, // Передаем NavController в экран
            )
        }

        // Экран детальной информации о продукте - используем ВРЕМЕННЫЙ контейнер
        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val productId = entry.arguments?.getString("productId") ?: ""
            // Для детальной информации используем временный контейнер
            val screenContainer =
                rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
            val viewModel = remember(productId) {
                screenContainer.createProductDetailViewModel(productId)
            }

            ProductDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Функция добавления графа навигации для логов
 */
fun NavGraphBuilder.logsNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    // Получаем контейнер для графа напрямую
    val graphContainer = navigationScopeManager.getGraphContainer("logs_graph")

    navigation(
        startDestination = Screen.LogList.route,
        route = "logs_graph"
    ) {
        // Экран списка логов - используем ПОСТОЯННЫЙ контейнер
        composable(Screen.LogList.route) { entry ->
            // Используем постоянный контейнер для списка логов, который будет сохранен
            // до выхода из графа навигации
            val screenContainer =
                rememberPersistentScreenContainer(navController, entry, navigationScopeManager)
            val viewModel = remember { screenContainer.createLogListViewModel() }

            // Логгируем для отладки, чтобы видеть, когда создается ViewModel
            Timber.d("Using LogListViewModel from persistent container")

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

        // Экран детальной информации о логе - используем ВРЕМЕННЫЙ контейнер
        composable(
            route = Screen.LogDetail.route,
            arguments = listOf(
                navArgument("logId") {
                    type = NavType.IntType
                }
            )
        ) { entry ->
            val logId = entry.arguments?.getInt("logId") ?: 0
            // Используем временный контейнер для детального экрана, который будет уничтожен
            // при возврате к списку логов
            val screenContainer =
                rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
            val viewModel = remember(logId) { screenContainer.createLogDetailViewModel(logId) }

            // Логгируем для отладки, чтобы видеть, когда создается ViewModel
            Timber.d("Using LogDetailViewModel from ephemeral container for logId: $logId")

            LogDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

// Добавим функцию расширения для NavGraphBuilder
fun NavGraphBuilder.taskXNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    // Получаем контейнер для графа
    val graphContainer = navigationScopeManager.getGraphContainer("taskx_graph")

    navigation(
        startDestination = Screen.TaskXList.route,
        route = "taskx_graph"
    ) {
        // Экран списка заданий X - используем ПОСТОЯННЫЙ контейнер
        composable(Screen.TaskXList.route) { entry ->
            val screenContainer =
                rememberPersistentScreenContainer(navController, entry, navigationScopeManager)
            val viewModel = remember { screenContainer.createTaskXListViewModel() }

            TaskXListScreen(
                viewModel = viewModel,
                navigateToTaskDetail = { taskId ->
                    navController.navigate(Screen.TaskXDetail.createRoute(taskId))
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран детальной информации о задании X - используем ВРЕМЕННЫЙ контейнер
        composable(
            route = Screen.TaskXDetail.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId") ?: ""
            val screenContainer =
                rememberEphemeralScreenContainer(navController, entry, navigationScopeManager)
            val viewModel = remember(taskId) { screenContainer.createTaskXDetailViewModel(taskId) }

            TaskXDetailScreen(
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
        fun createRoute(isSelectionMode: Boolean = false) =
            "product_list?isSelectionMode=$isSelectionMode"
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

    object TaskXList : Screen("taskx_list") // Список заданий X
    object TaskXDetail : Screen("taskx_detail/{taskId}") { // Детальная информация о задании X
        fun createRoute(taskId: String) = "taskx_detail/$taskId"
    }

    object DynamicMenu : Screen("dynamic_menu")
    object DynamicTasks : Screen("dynamic_tasks/{menuItemId}/{menuItemName}?menuItemType={menuItemType}") {
        fun createRoute(menuItemId: String, menuItemName: String, menuItemType: DynamicMenuItemType = DynamicMenuItemType.SHOW_LIST): String {
            val encodedName = encode(menuItemName, "UTF-8")
            return "dynamic_tasks/$menuItemId/$encodedName?operationType=${menuItemType.name}"
        }
    }

    object DynamicTaskDetail : Screen("dynamic_task_detail?taskId={taskId}&taskName={taskName}") {
        fun createRoute(task: DynamicTask): String {
            val encodedName = encode(task.name, "UTF-8")
            return "dynamic_task_detail?taskId=${task.id}&taskName=$encodedName"
        }
    }
}