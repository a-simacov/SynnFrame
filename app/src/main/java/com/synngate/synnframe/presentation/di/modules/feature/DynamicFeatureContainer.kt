package com.synngate.synnframe.presentation.di.modules.feature

import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.CoreContainer
import com.synngate.synnframe.presentation.di.modules.DomainContainer
import com.synngate.synnframe.presentation.di.modules.FeatureContainer
import com.synngate.synnframe.presentation.ui.dynamicmenu.menu.DynamicMenuViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.DynamicProductDetailViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.DynamicProductsViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.DynamicTaskDetailViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.DynamicTasksViewModel
import timber.log.Timber

/**
 * Контейнер для функциональности динамического меню.
 * Содержит зависимости и фабрики для ViewModel-ей, связанных с динамическими меню, задачами и продуктами.
 */
class DynamicFeatureContainer(
    appContainer: AppContainer,
    coreContainer: CoreContainer,
    domainContainer: DomainContainer
) : FeatureContainer(appContainer, coreContainer, domainContainer) {

    override val moduleName: String = "Dynamic"

    /**
     * Создание ViewModel для экрана динамического меню
     *
     * @return ViewModel для динамического меню
     */
    fun createDynamicMenuViewModel(): DynamicMenuViewModel {
        return getViewModel("DynamicMenuViewModel") {
            DynamicMenuViewModel(
                dynamicMenuUseCases = domainContainer.dynamicMenuUseCases
            )
        }
    }

    /**
     * Создание ViewModel для экрана динамических задач
     *
     * @param menuItemId Идентификатор пункта меню
     * @param menuItemName Название пункта меню
     * @param endpoint Эндпоинт для получения данных
     * @param screenSettings Настройки экрана
     * @return ViewModel для динамических задач
     */
    fun createDynamicTasksViewModel(
        menuItemId: String,
        menuItemName: String,
        endpoint: String,
        screenSettings: ScreenSettings
    ): DynamicTasksViewModel {
        return getViewModel("DynamicTasksViewModel_${menuItemId}_${endpoint.hashCode()}") {
            DynamicTasksViewModel(
                menuItemId = menuItemId,
                menuItemName = menuItemName,
                endpoint = endpoint,
                screenSettings = screenSettings,
                dynamicMenuUseCases = domainContainer.dynamicMenuUseCases,
                userUseCases = domainContainer.userUseCases,
                taskContextManager = appContainer.taskContextManager
            )
        }
    }

    /**
     * Создание ViewModel для экрана деталей динамической задачи
     *
     * @param taskId Идентификатор задачи
     * @param endpoint Эндпоинт для получения данных
     * @return ViewModel для деталей динамической задачи
     */
    fun createDynamicTaskDetailViewModel(
        taskId: String,
        endpoint: String
    ): DynamicTaskDetailViewModel {
        return getViewModel("DynamicTaskDetailViewModel_${taskId}_${endpoint.hashCode()}") {
            DynamicTaskDetailViewModel(
                taskId = taskId,
                endpoint = endpoint,
                dynamicMenuUseCases = domainContainer.dynamicMenuUseCases,
                userUseCases = domainContainer.userUseCases,
                taskContextManager = domainContainer.taskContextManager
            )
        }
    }

    /**
     * Создание ViewModel для экрана динамических продуктов
     *
     * @param menuItemId Идентификатор пункта меню
     * @param menuItemName Название пункта меню
     * @param endpoint Эндпоинт для получения данных
     * @param screenSettings Настройки экрана
     * @return ViewModel для динамических продуктов
     */
    fun createDynamicProductsViewModel(
        menuItemId: String,
        menuItemName: String,
        endpoint: String,
        screenSettings: ScreenSettings
    ): DynamicProductsViewModel {
        return getViewModel("DynamicProductsViewModel_${menuItemId}_${endpoint.hashCode()}") {
            DynamicProductsViewModel(
                menuItemId = menuItemId,
                menuItemName = menuItemName,
                endpoint = endpoint,
                screenSettings = screenSettings,
                dynamicMenuUseCases = domainContainer.dynamicMenuUseCases,
                soundService = coreContainer.soundService,
                productUiMapper = appContainer.getDataContainer().productUiMapper,
                isSelectionMode = false
            )
        }
    }

    /**
     * Создание ViewModel для экрана деталей динамического продукта
     *
     * @param product Продукт для отображения
     * @return ViewModel для деталей динамического продукта
     */
    fun createDynamicProductDetailViewModel(product: DynamicProduct): DynamicProductDetailViewModel {
        return getViewModel("DynamicProductDetailViewModel_${product.id}") {
            DynamicProductDetailViewModel(
                dynamicProduct = product,
                clipboardService = coreContainer.clipboardService,
                productUiMapper = appContainer.getDataContainer().productUiMapper,
                resourceProvider = coreContainer.resourceProvider
            )
        }
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Dynamic feature initialized")
    }
}