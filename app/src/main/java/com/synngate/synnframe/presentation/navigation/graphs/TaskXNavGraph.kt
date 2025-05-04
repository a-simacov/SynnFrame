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
import com.synngate.synnframe.presentation.navigation.routes.TaskXRoutes
import com.synngate.synnframe.presentation.ui.taskx.TaskXDetailScreen
import com.synngate.synnframe.presentation.ui.taskx.TaskXListScreen

/**
 * Создает навигационный граф для экранов заданий X.
 *
 * @param navController Контроллер навигации
 * @param navigationScopeManager Менеджер областей навигации для управления контейнерами экранов
 */
fun NavGraphBuilder.taskXNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    navigation(
        startDestination = TaskXRoutes.TaskXList.route,
        route = TaskXRoutes.TaskXGraph.route
    ) {
        // Экран списка заданий X - используем ПОСТОЯННЫЙ контейнер
        composable(TaskXRoutes.TaskXList.route) { entry ->
            val screenContainer = rememberPersistentScreenContainer(
                navController = navController,
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember { screenContainer.createTaskXListViewModel() }

            TaskXListScreen(
                viewModel = viewModel,
                navigateToTaskDetail = { taskId ->
                    navController.navigate(TaskXRoutes.TaskXDetail.createRoute(taskId))
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран детальной информации о задании X - используем ВРЕМЕННЫЙ контейнер
        composable(
            route = TaskXRoutes.TaskXDetail.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId") ?: ""
            val screenContainer = rememberEphemeralScreenContainer(
                navController = navController,
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
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