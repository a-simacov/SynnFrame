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
 * Менеджер областей навигации c поддержкой сохранения ViewModels в пределах графа
 */
class NavigationScopeManager(
    private val navController: NavController,
    private val onCreateScreenContainer: () -> ScreenContainer
) {
    // Карта временных контейнеров для экранов (удаляются при выходе из экрана)
    private val ephemeralScreenContainers = mutableMapOf<String, ScreenContainer>()

    // Карта постоянных контейнеров для экранов в рамках графа (сохраняются пока активен граф)
    private val persistentScreenContainers = mutableMapOf<String, ScreenContainer>()

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
     * Получение временного контейнера для экрана (удаляется при выходе с экрана)
     */
    fun getEphemeralScreenContainer(route: String): ScreenContainer {
        return ephemeralScreenContainers.getOrPut(route) {
            Timber.d("Creating ephemeral ScreenContainer for screen: $route")
            onCreateScreenContainer()
        }
    }

    /**
     * Получение постоянного контейнера для экрана (сохраняется в пределах графа)
     */
    fun getPersistentScreenContainer(route: String, graphRoute: String?): ScreenContainer {
        // Используем уникальный ключ, включающий и граф, и экран
        val key = if (graphRoute != null) "$graphRoute:$route" else route

        return persistentScreenContainers.getOrPut(key) {
            Timber.d("Creating persistent ScreenContainer for screen: $route in graph: $graphRoute")
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
            // Определяем графы для предыдущего и текущего экрана
            val previousGraph = getGraphForRoute(previousRoute)
            val currentGraph = getGraphForRoute(currentRoute)

            // Проверяем, не является ли предыдущий экран частью текущей иерархии
            val isBackNavigation = !destination.hierarchy.any { it.route == previousRoute }

            if (isBackNavigation) {
                // Всегда удаляем временный контейнер для предыдущего экрана
                ephemeralScreenContainers.remove(previousRoute)?.let {
                    Timber.d("Disposing ephemeral ScreenContainer for screen: $previousRoute")
                    it.dispose()
                }

                // Если произошел выход из графа, удаляем все связанные постоянные контейнеры
                if (previousGraph != null && previousGraph != currentGraph) {
                    // Удаляем постоянные контейнеры, связанные с предыдущим графом
                    val keysToRemove = persistentScreenContainers.keys
                        .filter { it.startsWith("$previousGraph:") }

                    keysToRemove.forEach { key ->
                        persistentScreenContainers.remove(key)?.let {
                            Timber.d("Disposing persistent ScreenContainer for key: $key")
                            it.dispose()
                        }
                    }

                    // Удаляем контейнер графа
                    graphContainers.remove(previousGraph)?.let {
                        Timber.d("Disposing graph ScreenContainer for graph: $previousGraph")
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
    fun getGraphForRoute(route: String?): String? {
        if (route == null) return null

        return when {
            route.startsWith("server_list") || route.startsWith("server_detail") -> "servers_graph"
            route.startsWith("log_list") || route.startsWith("log_detail") -> "logs_graph"
            route.startsWith("task_list") || route.startsWith("task_detail") -> "tasks_graph"
            route.startsWith("product_list") || route.startsWith("product_detail") -> "products_graph"
            route.startsWith("taskx_list") || route.startsWith("taskx_detail") -> "taskx_graph" // Добавляем маршруты заданий X
            else -> null
        }
    }

    /**
     * Очистка всех ресурсов
     */
    fun dispose() {
        ephemeralScreenContainers.values.forEach { it.dispose() }
        ephemeralScreenContainers.clear()

        persistentScreenContainers.values.forEach { it.dispose() }
        persistentScreenContainers.clear()

        graphContainers.values.forEach { it.dispose() }
        graphContainers.clear()
    }
}

/**
 * Composable функция для получения временного контейнера экрана
 * (будет удален при выходе из экрана)
 */
@Composable
fun rememberEphemeralScreenContainer(
    navController: NavController,
    navBackStackEntry: NavBackStackEntry,
    navigationScopeManager: NavigationScopeManager
): ScreenContainer {
    val route = navBackStackEntry.destination.route ?: "unknown"
    val screenContainer = remember(route) {
        navigationScopeManager.getEphemeralScreenContainer(route)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Отслеживаем жизненный цикл для экрана
    DisposableEffect(lifecycleOwner, route) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY &&
                !navBackStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                // Экран навсегда уничтожается, очищаем контейнер
                Timber.d("Screen $route destroyed, ephemeral container will be disposed")
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return screenContainer
}

/**
 * Composable функция для получения постоянного контейнера экрана
 * (сохраняется в пределах графа навигации)
 */
@Composable
fun rememberPersistentScreenContainer(
    navController: NavController,
    navBackStackEntry: NavBackStackEntry,
    navigationScopeManager: NavigationScopeManager
): ScreenContainer {
    val route = navBackStackEntry.destination.route ?: "unknown"
    val graphRoute = navigationScopeManager.getGraphForRoute(route)

    // Используем стабильные ключи для remember
    return remember(navBackStackEntry.id, graphRoute) {
        Timber.d("Creating persistent container for screen: $route in graph: $graphRoute")
        navigationScopeManager.getPersistentScreenContainer(route, graphRoute)
    }
}