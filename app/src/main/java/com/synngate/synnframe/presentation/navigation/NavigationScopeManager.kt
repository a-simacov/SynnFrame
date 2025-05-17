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

    // Список уничтоженных графов для предотвращения утечек
    private val destroyedGraphs = mutableSetOf<String>()

    init {
        // Отслеживаем изменения навигации
        navController.addOnDestinationChangedListener { _, destination, _ ->
            handleDestinationChange(destination)
        }
    }

    /**
     * Получение временного контейнера для экрана (удаляется при выходе с экрана)
     */
    fun getEphemeralScreenContainer(route: String, graphRoute: String? = null): ScreenContainer {
        val key = if (graphRoute != null) "$route:$graphRoute" else route

        // Проверяем, если граф уже был уничтожен, очищаем перед созданием
        if (graphRoute != null && destroyedGraphs.contains(graphRoute)) {
            cleanupContainersForGraph(graphRoute)
            destroyedGraphs.remove(graphRoute)
        }

        return ephemeralScreenContainers.getOrPut(key) {
            Timber.d("Creating ephemeral ScreenContainer for screen: $route with graph: $graphRoute")
            onCreateScreenContainer()
        }
    }

    /**
     * Получение постоянного контейнера для экрана (сохраняется в пределах графа)
     */
    fun getPersistentScreenContainer(route: String, graphRoute: String?): ScreenContainer {
        // Используем уникальный ключ, включающий и граф, и экран
        val key = if (graphRoute != null) "$graphRoute:$route" else route

        // Проверяем, если граф уже был уничтожен, очищаем перед созданием
        if (graphRoute != null && destroyedGraphs.contains(graphRoute)) {
            cleanupContainersForGraph(graphRoute)
            destroyedGraphs.remove(graphRoute)
        }

        return persistentScreenContainers.getOrPut(key) {
            Timber.d("Creating persistent ScreenContainer for screen: $route in graph: $graphRoute")
            onCreateScreenContainer()
        }
    }

    /**
     * Получение контейнера для графа навигации
     */
    fun getGraphContainer(graphRoute: String): ScreenContainer {
        // Проверяем, если граф уже был уничтожен, очищаем перед созданием
        if (destroyedGraphs.contains(graphRoute)) {
            cleanupContainersForGraph(graphRoute)
            destroyedGraphs.remove(graphRoute)
        }

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
                cleanupEphemeralContainers(previousRoute)

                // Если произошел выход из графа, удаляем все связанные постоянные контейнеры
                if (previousGraph != null && previousGraph != currentGraph) {
                    cleanupContainersForGraph(previousGraph)

                    // Добавляем граф в список уничтоженных, чтобы очистить при повторном входе
                    destroyedGraphs.add(previousGraph)
                }
            }
        }

        previousDestination = destination
    }

    /**
     * Очистка временных контейнеров для экрана
     */
    private fun cleanupEphemeralContainers(route: String) {
        val ephemeralKeys = ephemeralScreenContainers.keys
            .filter { it.startsWith("$route:") || it == route }

        ephemeralKeys.forEach { key ->
            val container = ephemeralScreenContainers[key]
            container?.let {
                Timber.d("Explicitly clearing ViewModel states for key: $key")
                ephemeralScreenContainers.remove(key)
                it.dispose()
                Timber.d("Disposed ephemeral ScreenContainer for key: $key")
            }
        }
    }

    /**
     * Очистка контейнеров для графа навигации
     */
    private fun cleanupContainersForGraph(graphRoute: String) {
        // Удаляем постоянные контейнеры, связанные с графом
        val keysToRemove = persistentScreenContainers.keys
            .filter { it.startsWith("$graphRoute:") }

        keysToRemove.forEach { key ->
            persistentScreenContainers.remove(key)?.let {
                Timber.d("Disposing persistent ScreenContainer for key: $key")
                it.dispose()
            }
        }

        // Удаляем контейнер графа
        graphContainers.remove(graphRoute)?.let {
            Timber.d("Disposing graph ScreenContainer for graph: $graphRoute")
            it.dispose()
        }
    }

    /**
     * Получение графа для маршрута
     */
    fun getGraphForRoute(route: String?): String? {
        if (route == null) return null

        return when {
            route.startsWith("server_list") || route.startsWith("server_detail") -> "servers_graph"
            route.startsWith("log_list") || route.startsWith("log_detail") -> "logs_graph"
            route.startsWith("product_list") || route.startsWith("product_detail") -> "products_graph"
            route.startsWith("taskx_list") || route.startsWith("taskx_detail") ||
                    route.startsWith("action_wizard") -> "taskx_graph"
            route.startsWith("settings") || route.startsWith("sync_history") -> "settings_graph"
            route.startsWith("dynamic_menu") || route.startsWith("dynamic_task") ||
                    route.startsWith("dynamic_product") -> "dynamic_nav_graph"
            route.startsWith("main_menu") -> "main_graph"
            route.startsWith("login") -> "auth_graph"
            else -> null
        }
    }

    /**
     * Очистка всех ресурсов при завершении работы
     */
    fun dispose() {
        Timber.d("Disposing NavigationScopeManager and all containers")

        // Сначала освобождаем временные контейнеры
        ephemeralScreenContainers.values.forEach {
            try {
                it.dispose()
            } catch (e: Exception) {
                Timber.e(e, "Error disposing ephemeral container")
            }
        }
        ephemeralScreenContainers.clear()

        // Затем освобождаем постоянные контейнеры
        persistentScreenContainers.values.forEach {
            try {
                it.dispose()
            } catch (e: Exception) {
                Timber.e(e, "Error disposing persistent container")
            }
        }
        persistentScreenContainers.clear()

        // Наконец, освобождаем контейнеры графов
        graphContainers.values.forEach {
            try {
                it.dispose()
            } catch (e: Exception) {
                Timber.e(e, "Error disposing graph container")
            }
        }
        graphContainers.clear()

        // Очищаем список уничтоженных графов
        destroyedGraphs.clear()

        Timber.d("NavigationScopeManager disposed successfully")
    }
}

/**
 * Composable функция для получения временного контейнера экрана
 * (будет удален при выходе из экрана)
 */
@Composable
fun rememberEphemeralScreenContainer(
    navBackStackEntry: NavBackStackEntry,
    navigationScopeManager: NavigationScopeManager,
    graphRoute: String? = null
): ScreenContainer {
    val route = navBackStackEntry.destination.route ?: "unknown"
    val actualGraphRoute = graphRoute ?: navigationScopeManager.getGraphForRoute(route)

    val screenContainer = remember(route, actualGraphRoute) {
        navigationScopeManager.getEphemeralScreenContainer(route, actualGraphRoute)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Отслеживаем жизненный цикл для экрана
    DisposableEffect(lifecycleOwner, route, actualGraphRoute) {
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