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
import timber.log.Timber

/**
 * Создает навигационный граф для экранов заданий X.
 */
fun NavGraphBuilder.taskXNavGraph(
    navController: NavHostController,
    navigationScopeManager: NavigationScopeManager
) {
    navigation(
        startDestination = TaskXRoutes.TaskXList.route,
        route = TaskXRoutes.TaskXGraph.route
    ) {
        // Экран списка заданий X
        composable(TaskXRoutes.TaskXList.route) { entry ->
            val screenContainer = rememberPersistentScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
        }

        // Экран детальной информации о задании X
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
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )

            // Создаем ViewModel через контейнер
            val viewModel = remember(taskId) {
                screenContainer.createTaskXDetailViewModel()
            }

            TaskXDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                },
                navigateToActionWizard = { taskId, actionId ->
                    navController.navigate(
                        TaskXRoutes.ActionWizardScreen.createRoute(taskId, actionId)
                    )
                }
            )
        }

        // Новый экран визарда действий
        composable(
            route = TaskXRoutes.ActionWizardScreen.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                },
                navArgument("actionId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId") ?: ""
            val actionId = entry.arguments?.getString("actionId") ?: ""

            // Добавляем логирование
            Timber.d("Создание экрана визарда с taskId=$taskId, actionId=$actionId")

            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )
        }
    }
}