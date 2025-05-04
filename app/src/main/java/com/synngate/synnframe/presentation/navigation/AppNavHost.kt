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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.presentation.common.LocalCurrentUser
import com.synngate.synnframe.presentation.navigation.graphs.dynamicNavGraph
import com.synngate.synnframe.presentation.navigation.graphs.serverNavGraph
import com.synngate.synnframe.presentation.navigation.graphs.settingsNavGraph
import com.synngate.synnframe.presentation.navigation.routes.AuthRoutes
import com.synngate.synnframe.presentation.navigation.routes.MainRoutes
import com.synngate.synnframe.presentation.navigation.routes.ServerRoutes
import com.synngate.synnframe.presentation.ui.login.LoginScreen
import com.synngate.synnframe.presentation.ui.main.MainMenuScreen
import timber.log.Timber

/**
 * Основной навигационный компонент приложения, координирующий все навигационные графы.
 *
 * @param startDestination Начальный маршрут навигации
 * @param navController Контроллер навигации
 */
@Composable
fun AppNavHost(
    startDestination: String = ServerRoutes.ServerList.route,
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

    // Предоставляем текущего пользователя через CompositionLocal
    CompositionLocalProvider(
        LocalCurrentUser provides currentUser
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // Граф навигации для серверов
            serverNavGraph(
                navController = navController,
                navigationScopeManager = navigationScopeManager,
                navigateToLogin = {
                    navController.navigate(AuthRoutes.Login.route) {
                        popUpTo(ServerRoutes.ServerList.route) { inclusive = false }
                    }
                }
            )

            // Экран логина
            composable(AuthRoutes.Login.route) { entry ->
                val screenContainer = rememberEphemeralScreenContainer(
                    navController = navController,
                    navBackStackEntry = entry,
                    navigationScopeManager = navigationScopeManager
                )
                val viewModel = remember { screenContainer.createLoginViewModel() }

                LoginScreen(
                    viewModel = viewModel,
                    navigateToMainMenu = {
                        navController.navigate(MainRoutes.MainMenu.route) {
                            popUpTo(AuthRoutes.Login.route) { inclusive = true }
                        }
                    },
                    navigateToServersList = {
                        navController.navigate(ServerRoutes.ServerList.route) {
                            popUpTo(AuthRoutes.Login.route) { inclusive = false }
                        }
                    },
                    exitApp = {
                        (context as? Activity)?.finish()
                    }
                )
            }

            // Главное меню
            composable(MainRoutes.MainMenu.route) { entry ->
                val screenContainer = rememberEphemeralScreenContainer(
                    navController = navController,
                    navBackStackEntry = entry,
                    navigationScopeManager = navigationScopeManager
                )
                val viewModel = remember { screenContainer.createMainMenuViewModel() }

                MainMenuScreen(
                    viewModel = viewModel,
                    // Функции навигации вынесены в отдельные лямбды для краткости
                    navigateToTasks = {
                        navController.navigateToTaskList()
                    },
                    navigateToProducts = {
                        navController.navigateToProductList()
                    },
                    navigateToLogs = {
                        navController.navigateToLogList()
                    },
                    navigateToSettings = {
                        navController.navigateToSettings()
                    },
                    navigateToLogin = {
                        navController.navigateToLogin()
                    },
                    navigateToTasksX = {
                        navController.navigateToTaskXList()
                    },
                    navigateToDynamicMenu = {
                        navController.navigateToDynamicMenu()
                    },
                    exitApp = {
                        (context as? Activity)?.finish()
                    }
                )
            }

            // Подключаем остальные графы навигации
            tasksNavGraph(navController, navigationScopeManager)
            productsNavGraph(navController, navigationScopeManager)
            logsNavGraph(navController, navigationScopeManager)
            taskXNavGraph(navController, navigationScopeManager)
            settingsNavGraph(navController, navigationScopeManager)
            dynamicNavGraph(navController, navigationScopeManager)
        }
    }
}