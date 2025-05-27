package com.synngate.synnframe.presentation.navigation.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.synngate.synnframe.presentation.navigation.NavigationScopeManager
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.navigation.rememberEphemeralScreenContainer
import com.synngate.synnframe.presentation.navigation.rememberPersistentScreenContainer
import com.synngate.synnframe.presentation.navigation.routes.TaskXRoutes
import com.synngate.synnframe.presentation.ui.taskx.TaskXDetailScreen
import com.synngate.synnframe.presentation.ui.taskx.wizard.ActionWizardScreen
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

            // Проверяем наличие данных в синглтоне перед созданием ViewModel
            val hasDataInHolder = TaskXDataHolderSingleton.hasData()
            Timber.d("Данные в TaskXDataHolderSingleton перед созданием ViewModel: $hasDataInHolder")

            // Если данных нет, показываем сообщение об ошибке
            if (!hasDataInHolder) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Ошибка: данные задания недоступны",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { navController.popBackStack() }) {
                            Text("Вернуться")
                        }
                    }
                }
            } else {
                // Создаем ViewModel только если данные доступны
                val viewModel = remember(taskId, actionId) {
                    screenContainer.createActionWizardViewModel(taskId, actionId)
                }

                ActionWizardScreen(
                    viewModel = viewModel,
                    navigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}