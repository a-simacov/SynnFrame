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
import com.synngate.synnframe.presentation.navigation.graphs.authRoutes
import com.synngate.synnframe.presentation.navigation.graphs.dynamicNavGraph
import com.synngate.synnframe.presentation.navigation.graphs.logsNavGraph
import com.synngate.synnframe.presentation.navigation.graphs.productsNavGraph
import com.synngate.synnframe.presentation.navigation.graphs.serverNavGraph
import com.synngate.synnframe.presentation.navigation.graphs.settingsNavGraph
import com.synngate.synnframe.presentation.navigation.graphs.taskXNavGraph
import com.synngate.synnframe.presentation.navigation.routes.MainRoutes
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
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity

    // Получаем приложение
    val app = context.applicationContext as SynnFrameApplication

    // Получаем данные о текущем пользователе
    val userRepository = app.appContainer.userRepository
    val currentUser by userRepository.getCurrentUser().collectAsState(initial = null)

    // Получаем менеджер областей навигации из AppContainer
    val navigationScopeManager = app.appContainer.getOrCreateNavigationScopeManager(navController)

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
                    navController.navigateToLogin()
                }
            )

            // Маршруты аутентификации
            authRoutes(
                navController = navController,
                navigationScopeManager = navigationScopeManager,
                exitApp = { activity?.finish() }
            )

            // Главное меню
            composable(MainRoutes.MainMenu.route) { entry ->
                val screenContainer = rememberEphemeralScreenContainer(
                    navBackStackEntry = entry,
                    navigationScopeManager = navigationScopeManager
                )
                val viewModel = screenContainer.createMainMenuViewModel()

                MainMenuScreen(
                    viewModel = viewModel,
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
                    exitApp = { activity?.finish() }
                )
            }

            // Подключаем остальные графы навигации
            productsNavGraph(navController, navigationScopeManager)
            logsNavGraph(navController, navigationScopeManager)
            taskXNavGraph(navController, navigationScopeManager)
            settingsNavGraph(navController, navigationScopeManager)
            dynamicNavGraph(navController, navigationScopeManager)
        }
    }
}