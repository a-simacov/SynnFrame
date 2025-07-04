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
            Timber.d("TaskXNavGraph: composable вызван для ActionWizardScreen с taskId=$taskId, actionId=$actionId")

            val currentRoute = navController.currentBackStackEntry?.destination?.route
            val previousRoute = navController.previousBackStackEntry?.destination?.route

            Timber.d("TaskXNavGraph: текущий маршрут=$currentRoute, предыдущий маршрут=$previousRoute")

            val navigateBackToTaskDetail: () -> Unit = {
                // Получаем текущий endpoint из синглтона
                val endpoint = TaskXDataHolderSingleton.endpoint
                if (endpoint != null) {
                    try {
                        val encodedEndpoint = Base64.getEncoder().encodeToString(endpoint.toByteArray())

                        // Явно навигируем на экран TaskXDetail с очисткой стека
                        Timber.d("Выполняется явная навигация на taskx_detail с taskId=$taskId, endpoint=$endpoint")
                        navController.navigate(TaskXRoutes.TaskXDetail.createRoute(taskId, endpoint)) {
                            // Удаляем текущий экран и все промежуточные экраны до TaskXDetail
                            popUpTo(TaskXRoutes.TaskXGraph.route) {
                                saveState = false
                                inclusive = false
                            }
                            // Предотвращаем создание дубликатов экрана
                            launchSingleTop = true
                            restoreState = false
                        }
                        Timber.d("Навигация на TaskXDetail выполнена")
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка при навигации на TaskXDetail")
                        // Аварийный вариант - просто пытаемся вернуться назад
                        navController.popBackStack()
                    }
                } else {
                    Timber.w("Невозможно выполнить явную навигацию: endpoint не задан")
                    navController.popBackStack()
                }
            }

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
                            "Error: task's data unavailable",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { navController.popBackStack() }) {
                            Text("Go back")
                        }
                    }
                }
            } else {
                val viewModel = remember(taskId, actionId) {
                    screenContainer.createActionWizardViewModel(taskId, actionId)
                }

                ActionWizardScreen(
                    viewModel = viewModel,
                    // Передаем нашу надежную функцию навигации
                    navigateBack = navigateBackToTaskDetail
                )
            }
        }
    }
}