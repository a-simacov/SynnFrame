package com.synngate.synnframe.presentation.navigation.graphs

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.synngate.synnframe.presentation.navigation.NavigationScopeManager
import com.synngate.synnframe.presentation.navigation.rememberEphemeralScreenContainer
import com.synngate.synnframe.presentation.navigation.rememberPersistentScreenContainer
import com.synngate.synnframe.presentation.navigation.routes.LogRoutes
import com.synngate.synnframe.presentation.ui.logs.LogDetailScreen
import com.synngate.synnframe.presentation.ui.logs.LogListScreen
import timber.log.Timber

/**
 * Создает навигационный граф для экранов логов.
 *
 * @param navController Контроллер навигации
 * @param navigationScopeManager Менеджер областей навигации для управления контейнерами экранов
 */
fun NavGraphBuilder.logsNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    navigation(
        startDestination = LogRoutes.LogList.route,
        route = LogRoutes.LogsGraph.route
    ) {
        // Экран списка логов - используем ПОСТОЯННЫЙ контейнер
        composable(LogRoutes.LogList.route) { entry ->
            val screenContainer = rememberPersistentScreenContainer(
                navController = navController,
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember { screenContainer.createLogListViewModel() }

            // Логгируем для отладки, чтобы видеть, когда создается ViewModel
            Timber.d("Using LogListViewModel from persistent container")

            LogListScreen(
                viewModel = viewModel,
                navigateToLogDetail = { logId ->
                    navController.navigate(LogRoutes.LogDetail.createRoute(logId))
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран детальной информации о логе - используем ВРЕМЕННЫЙ контейнер
        composable(
            route = LogRoutes.LogDetail.route,
            arguments = listOf(
                navArgument("logId") {
                    type = NavType.IntType
                }
            )
        ) { entry ->
            val logId = entry.arguments?.getInt("logId") ?: 0
            val screenContainer = rememberEphemeralScreenContainer(
                navController = navController,
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
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