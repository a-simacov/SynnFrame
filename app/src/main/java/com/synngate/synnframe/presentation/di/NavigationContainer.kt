package com.synngate.synnframe.presentation.di

import com.synngate.synnframe.presentation.di.modules.FeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.AuthFeatureContainer
import timber.log.Timber

/**
 * Контейнер для уровня навигации.
 * Управляет доступом к функциональным контейнерам и создает контейнеры для экранов.
 */
class NavigationContainer(private val appContainer: AppContainer) : DiContainer() {

    // Кэш функциональных контейнеров для графов навигации
    private val featureContainerCache = mutableMapOf<String, FeatureContainer>()

    /**
     * Получение контейнера для графа навигации по его маршруту
     *
     * @param graphRoute Маршрут графа навигации
     * @return Контейнер соответствующего типа
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : FeatureContainer> getNavigationGraphContainer(graphRoute: String): T {
        return when (graphRoute) {
            "auth_graph" -> getAuthFeatureContainer() as T
            "products_graph" -> getProductsFeatureContainer() as T
            "taskx_graph" -> getTasksFeatureContainer() as T
            "logs_graph" -> getLogsFeatureContainer() as T
            "settings_graph" -> getSettingsFeatureContainer() as T
            "dynamic_nav_graph" -> getDynamicFeatureContainer() as T
            else -> throw IllegalArgumentException("Unknown graph route: $graphRoute")
        }
    }

    /**
     * Ленивое получение контейнера для авторизации
     */
    private fun getAuthFeatureContainer(): AuthFeatureContainer {
        return featureContainerCache.getOrPut("auth") {
            appContainer.getFeatureContainer("auth") {
                AuthFeatureContainer(
                    appContainer,
                    appContainer.getCoreContainer(),
                    appContainer.getDomainContainer()
                )
            }
        } as AuthFeatureContainer
    }

    /**
     * Временные заглушки для остальных функциональных контейнеров
     * Будут реализованы в следующих этапах
     */
    private fun getProductsFeatureContainer(): FeatureContainer {
        // В дальнейшем будет реализовано
        throw NotImplementedError("Products feature container not implemented in this phase")
    }

    private fun getTasksFeatureContainer(): FeatureContainer {
        // В дальнейшем будет реализовано
        throw NotImplementedError("Tasks feature container not implemented in this phase")
    }

    private fun getLogsFeatureContainer(): FeatureContainer {
        // В дальнейшем будет реализовано
        throw NotImplementedError("Logs feature container not implemented in this phase")
    }

    private fun getSettingsFeatureContainer(): FeatureContainer {
        // В дальнейшем будет реализовано
        throw NotImplementedError("Settings feature container not implemented in this phase")
    }

    private fun getDynamicFeatureContainer(): FeatureContainer {
        // В дальнейшем будет реализовано
        throw NotImplementedError("Dynamic feature container not implemented in this phase")
    }

    /**
     * Создание контейнера для экрана
     *
     * @param navGraphRoute Маршрут графа навигации (опционально)
     * @return Созданный контейнер экрана
     */
    fun createScreenContainer(navGraphRoute: String? = null): ScreenContainer {
        return createChildContainer {
            ScreenContainer(appContainer, this, navGraphRoute)
        }
    }

    /**
     * Освобождение ресурсов контейнера
     */
    override fun dispose() {
        Timber.d("Disposing NavigationContainer")

        // Очистка кэша контейнеров
        featureContainerCache.clear()

        super.dispose()
    }
}