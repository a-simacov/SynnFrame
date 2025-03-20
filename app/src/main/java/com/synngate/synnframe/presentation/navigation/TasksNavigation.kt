package com.synngate.synnframe.presentation.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.synngate.synnframe.presentation.di.TasksGraphContainer
import com.synngate.synnframe.presentation.ui.tasks.TaskDetailScreen
import com.synngate.synnframe.presentation.ui.tasks.TaskListScreen
import timber.log.Timber

/**
 * Навигационный граф для экранов заданий
 */
fun NavGraphBuilder.tasksNavGraph(
    navController: NavHostController,
    tasksGraphContainer: TasksGraphContainer,
    lifecycleOwner: LifecycleOwner,
    navigateToProductsList: (Boolean) -> Unit
) {
    navigation(
        startDestination = Screen.TaskList.route,
        route = "tasks_graph"
    ) {
        // Экран списка заданий
        composable(
            route = Screen.TaskList.route
        ) { entry ->
            // Отслеживаем жизненный цикл для очистки ресурсов
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        Timber.d("TaskList screen destroyed, clearing container")
                        tasksGraphContainer.clear()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            // Создаем ViewModel и отображаем экран
            TaskListScreen(
                viewModel = tasksGraphContainer.createTaskListViewModel(),
                navigateToTaskDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
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

        // Экран деталей задания
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            // Получаем ID задания из аргументов
            val taskId = entry.arguments?.getString("taskId") ?: ""

            // Отслеживаем жизненный цикл для очистки ресурсов
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        Timber.d("TaskDetail screen destroyed, clearing container")
                        tasksGraphContainer.clear()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            // Создаем ViewModel и отображаем экран
            TaskDetailScreen(
                viewModel = tasksGraphContainer.createTaskDetailViewModel(taskId),
                navigateBack = {
                    navController.popBackStack()
                },
                navigateToProductsList = {
                    // Вызываем функцию для перехода к списку товаров в режиме выбора
                    navigateToProductsList(true)
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