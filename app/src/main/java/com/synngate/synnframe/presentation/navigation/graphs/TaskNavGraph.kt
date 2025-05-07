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
import com.synngate.synnframe.presentation.navigation.routes.ProductRoutes
import com.synngate.synnframe.presentation.navigation.routes.TaskRoutes
import com.synngate.synnframe.presentation.ui.tasks.TaskDetailScreen
import com.synngate.synnframe.presentation.ui.tasks.TaskListScreen

/**
 * Создает навигационный граф для экранов задач.
 *
 * @param navController Контроллер навигации
 * @param navigationScopeManager Менеджер областей навигации для управления контейнерами экранов
 */
fun NavGraphBuilder.tasksNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    navigation(
        startDestination = TaskRoutes.TaskList.route,
        route = TaskRoutes.TasksGraph.route
    ) {
        // Экран списка задач - используем ПОСТОЯННЫЙ контейнер
        composable(TaskRoutes.TaskList.route) { entry ->
            val screenContainer = rememberPersistentScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember { screenContainer.createTaskListViewModel() }

            TaskListScreen(
                viewModel = viewModel,
                navigateToTaskDetail = { taskId ->
                    navController.navigate(TaskRoutes.TaskDetail.createRoute(taskId))
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Экран детальной информации о задаче - используем ВРЕМЕННЫЙ контейнер
        composable(
            route = TaskRoutes.TaskDetail.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId") ?: ""
            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
            val viewModel = remember(taskId) { screenContainer.createTaskDetailViewModel(taskId) }

            TaskDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                },
                navigateToProductsList = {
                    navController.navigate(ProductRoutes.ProductList.createRoute(true))
                },
                navController = navController
            )
        }
    }
}