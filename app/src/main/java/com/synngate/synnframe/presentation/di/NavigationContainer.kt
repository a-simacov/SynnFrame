package com.synngate.synnframe.presentation.di

import com.synngate.synnframe.presentation.di.modules.FeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.AuthFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.DynamicFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.LogsFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.MainFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.ProductsFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.SettingsFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.TasksFeatureContainer
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
            "main_graph" -> getMainFeatureContainer() as T
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
                    appContainer = appContainer,
                    coreContainer = appContainer.getCoreContainer(),
                    domainContainer = appContainer.getDomainContainer()
                )
            }
        } as AuthFeatureContainer
    }

    /**
     * Ленивое получение контейнера для продуктов
     */
    private fun getProductsFeatureContainer(): ProductsFeatureContainer {
        return featureContainerCache.getOrPut("products") {
            appContainer.getFeatureContainer("products") {
                ProductsFeatureContainer(
                    appContainer = appContainer,
                    coreContainer = appContainer.getCoreContainer(),
                    domainContainer = appContainer.getDomainContainer(),
                    dataContainer = appContainer.getDataContainer()
                )
            }
        } as ProductsFeatureContainer
    }

    /**
     * Ленивое получение контейнера для задач
     */
    private fun getTasksFeatureContainer(): TasksFeatureContainer {
        return featureContainerCache.getOrPut("tasks") {
            appContainer.getFeatureContainer("tasks") {
                TasksFeatureContainer(
                    appContainer = appContainer,
                    coreContainer = appContainer.getCoreContainer(),
                    domainContainer = appContainer.getDomainContainer()
                )
            }
        } as TasksFeatureContainer
    }

    /**
     * Ленивое получение контейнера для логов
     */
    private fun getLogsFeatureContainer(): LogsFeatureContainer {
        return featureContainerCache.getOrPut("logs") {
            appContainer.getFeatureContainer("logs") {
                LogsFeatureContainer(
                    appContainer = appContainer,
                    coreContainer = appContainer.getCoreContainer(),
                    domainContainer = appContainer.getDomainContainer()
                )
            }
        } as LogsFeatureContainer
    }

    /**
     * Ленивое получение контейнера для настроек
     */
    private fun getSettingsFeatureContainer(): SettingsFeatureContainer {
        return featureContainerCache.getOrPut("settings") {
            appContainer.getFeatureContainer("settings") {
                SettingsFeatureContainer(
                    appContainer = appContainer,
                    coreContainer = appContainer.getCoreContainer(),
                    domainContainer = appContainer.getDomainContainer()
                )
            }
        } as SettingsFeatureContainer
    }

    /**
     * Ленивое получение контейнера для динамического меню
     */
    private fun getDynamicFeatureContainer(): DynamicFeatureContainer {
        return featureContainerCache.getOrPut("dynamic") {
            appContainer.getFeatureContainer("dynamic") {
                DynamicFeatureContainer(
                    appContainer = appContainer,
                    coreContainer = appContainer.getCoreContainer(),
                    domainContainer = appContainer.getDomainContainer()
                )
            }
        } as DynamicFeatureContainer
    }

    /**
     * Ленивое получение контейнера для главного меню
     */
    private fun getMainFeatureContainer(): MainFeatureContainer {
        return featureContainerCache.getOrPut("main") {
            appContainer.getFeatureContainer("main") {
                MainFeatureContainer(
                    appContainer = appContainer,
                    coreContainer = appContainer.getCoreContainer(),
                    domainContainer = appContainer.getDomainContainer()
                )
            }
        } as MainFeatureContainer
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