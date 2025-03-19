package com.synngate.synnframe.presentation.navigation

import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.synngate.synnframe.presentation.di.LogsGraphContainer
import com.synngate.synnframe.presentation.ui.logs.LogDetailScreen
import com.synngate.synnframe.presentation.ui.logs.LogListScreen
import timber.log.Timber

/**
 * Навигационный граф для экранов логов
 */
fun NavGraphBuilder.logsNavGraph(
    navController: NavHostController,
    logsGraphContainer: LogsGraphContainer,
    lifecycleOwner: LifecycleOwner
) {
    navigation(
        startDestination = Screen.LogList.route,
        route = "logs_graph"
    ) {
        // Экран списка логов
        composable(Screen.LogList.route) { entry ->
            // Отслеживаем жизненный цикл для очистки ресурсов
            val observer = LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                    if (!entry.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                        Timber.d("LogList screen destroyed, clearing container")
                        logsGraphContainer.clear()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            // Создаем ViewModel и отображаем экран
            LogListScreen(
                viewModel = logsGraphContainer.createLogListViewModel(),
                navigateToLogDetail = { logId ->
                    navController.navigate(Screen.LogDetail.createRoute(logId))
                },
                navigateBack = {
                    navController.popBackStack()
                }
            )

            // Удаляем наблюдателя при выходе из композиции
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
        }

        // Экран детального просмотра лога
        composable(
            route = Screen.LogDetail.route,
            arguments = listOf(
                navArgument("logId") {
                    type = NavType.IntType
                }
            )
        ) { entry ->
            // Получаем ID лога из аргументов
            val logId = entry.arguments?.getInt("logId") ?: 0

            // Отслеживаем жизненный цикл для очистки ресурсов
            val observer = LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                    if (!entry.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                        Timber.d("LogDetail screen destroyed, clearing container")
                        logsGraphContainer.clear()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            // Создаем ViewModel и отображаем экран
            LogDetailScreen(
                viewModel = logsGraphContainer.createLogDetailViewModel(logId),
                navigateBack = {
                    navController.popBackStack()
                }
            )

            // Удаляем наблюдателя при выходе из композиции
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
        }
    }
}