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
import java.util.Base64

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
                },
                navArgument("endpoint") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId") ?: ""
            val encodedEndpoint = entry.arguments?.getString("endpoint") ?: ""

            // Декодируем endpoint из Base64
            val endpoint = try {
                String(Base64.getDecoder().decode(encodedEndpoint))
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode endpoint from Base64")
                ""
            }

            Timber.d("Opening TaskXDetailScreen with taskId=$taskId, endpoint=$endpoint")

            val screenContainer = rememberEphemeralScreenContainer(
                navBackStackEntry = entry,
                navigationScopeManager = navigationScopeManager
            )

            // Создаем ViewModel через контейнер с новыми параметрами
            val viewModel = remember(taskId, endpoint) {
                screenContainer.createTaskXDetailViewModel(taskId, endpoint)
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