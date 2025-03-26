package com.synngate.synnframe.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.synngate.synnframe.presentation.di.ScreenContainer
import timber.log.Timber

/**
 * Менеджер областей навигации, который отслеживает жизненный цикл экранов и графов навигации.
 */
class NavigationScopeManager(
    private val navController: NavController,
    private val onCreateScreenContainer: () -> ScreenContainer
) {
    // Карта контейнеров для экранов
    private val screenContainers = mutableMapOf<String, ScreenContainer>()

    // Карта контейнеров для графов навигации
    private val graphContainers = mutableMapOf<String, ScreenContainer>()

    // Предыдущее направление (для отслеживания выхода)
    private var previousDestination: NavDestination? = null

    init {
        // Отслеживаем изменения навигации
        navController.addOnDestinationChangedListener { _, destination, _ ->
            handleDestinationChange(destination)
        }
    }

    /**
     * Получение контейнера для экрана
     */
    fun getScreenContainer(route: String): ScreenContainer {
        return screenContainers.getOrPut(route) {
            Timber.d("Creating ScreenContainer for screen: $route")
            onCreateScreenContainer()
        }
    }

    /**
     * Получение контейнера для графа навигации
     */
    fun getGraphContainer(graphRoute: String): ScreenContainer {
        return graphContainers.getOrPut(graphRoute) {
            Timber.d("Creating ScreenContainer for graph: $graphRoute")
            onCreateScreenContainer()
        }
    }

    /**
     * Обработка изменения направления навигации
     */
    private fun handleDestinationChange(destination: NavDestination) {
        val previousRoute = previousDestination?.route
        val currentRoute = destination.route

        // Если изменился экран, проверяем необходимость очистки
        if (previousRoute != null && previousRoute != currentRoute) {
            // Проверяем, не является ли предыдущий экран частью текущей иерархии
            val isBackNavigation = !destination.hierarchy.any { it.route == previousRoute }

            if (isBackNavigation) {
                // Удаляем контейнер для предыдущего экрана
                screenContainers.remove(previousRoute)?.let {
                    Timber.d("Disposing ScreenContainer for screen: $previousRoute")
                    it.dispose()
                }

                // Проверяем, не покинули ли мы граф навигации
                val previousGraph = getGraphForRoute(previousRoute)
                val currentGraph = getGraphForRoute(currentRoute)

                if (previousGraph != null && previousGraph != currentGraph) {
                    graphContainers.remove(previousGraph)?.let {
                        Timber.d("Disposing ScreenContainer for graph: $previousGraph")
                        it.dispose()
                    }
                }
            }
        }

        previousDestination = destination
    }

    /**
     * Получение графа для маршрута
     */
    private fun getGraphForRoute(route: String?): String? {
        if (route == null) return null

        return when {
            route.startsWith("server_list") || route.startsWith("server_detail") -> "servers_graph"
            route.startsWith("log_list") || route.startsWith("log_detail") -> "logs_graph"
            route.startsWith("task_list") || route.startsWith("task_detail") -> "tasks_graph"
            route.startsWith("product_list") || route.startsWith("product_detail") -> "products_graph"
            else -> null
        }
    }

    /**
     * Очистка всех ресурсов
     */
    fun dispose() {
        screenContainers.values.forEach { it.dispose() }
        screenContainers.clear()

        graphContainers.values.forEach { it.dispose() }
        graphContainers.clear()
    }
}

/**
 * Composable функция для получения контейнера экрана с учетом жизненного цикла
 */
@Composable
fun rememberScreenContainer(
    navController: NavController,
    navBackStackEntry: NavBackStackEntry,
    navigationScopeManager: NavigationScopeManager
): ScreenContainer {
    val route = navBackStackEntry.destination.route ?: "unknown"
    val screenContainer = remember(route) {
        navigationScopeManager.getScreenContainer(route)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Отслеживаем жизненный цикл для экрана
    DisposableEffect(lifecycleOwner, route) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY &&
                !navBackStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                // Экран навсегда уничтожается, очищаем контейнер
                Timber.d("Screen $route destroyed, container will be disposed")
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return screenContainer
}