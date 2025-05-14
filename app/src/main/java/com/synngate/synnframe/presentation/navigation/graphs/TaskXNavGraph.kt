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
import com.synngate.synnframe.presentation.ui.wizard.ActionWizardScreen
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
            val viewModel = remember(taskId) { screenContainer.createTaskXDetailViewModel(taskId) }

            // Проверяем наличие результата от экрана визарда
            val completedActionId = entry.savedStateHandle.get<String>("completedActionId")
            if (completedActionId != null) {
                // Если есть результат, обрабатываем его и очищаем
                Timber.d("TaskXDetail: Получен результат completedActionId=$completedActionId")
                viewModel.onActionCompleted(completedActionId)
                entry.savedStateHandle.remove<String>("completedActionId")
            }

            TaskXDetailScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                },
                // Добавляем функцию навигации к экрану визарда
                navigateToActionWizard = { taskIdLambda, actionId ->
                    navController.navigate(TaskXRoutes.ActionWizardScreen.createRoute(taskIdLambda, actionId)) {
                        // Явно указываем, что при возврате мы должны вернуться к этому экрану
                        // Без закрытия самого экрана задания
                        popUpTo(TaskXRoutes.TaskXDetail.route) { inclusive = false }
                        launchSingleTop = true
                    }
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

            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )

            val viewModel = remember(taskId, actionId) {
                screenContainer.createActionWizardViewModel(taskId, actionId)
            }

            ActionWizardScreen(
                viewModel = viewModel,
                navigateBack = {
                    // Явно указываем, что возвращаемся к экрану задания
                    navController.popBackStack(TaskXRoutes.TaskXDetail.route, false)
                },
                navigateBackWithSuccess = { completedActionId ->
                    // Сохраняем результат и возвращаемся к экрану задания
                    navController.previousBackStackEntry?.let { previousEntry ->
                        // Сохраняем результат в savedStateHandle предыдущего экрана
                        Timber.d("ActionWizard: Сохраняем результат completedActionId=$completedActionId для предыдущего экрана")
                        previousEntry.savedStateHandle["completedActionId"] = completedActionId
                    }
                    // Явно указываем, что возвращаемся к экрану задания
                    navController.popBackStack(TaskXRoutes.TaskXDetail.route, false)
                }
            )
        }
    }
}