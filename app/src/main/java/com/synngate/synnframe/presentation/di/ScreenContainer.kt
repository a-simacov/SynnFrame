package com.synngate.synnframe.presentation.di

import androidx.lifecycle.ViewModel
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.presentation.di.modules.feature.AuthFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.DynamicFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.LogsFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.MainFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.ProductsFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.SettingsFeatureContainer
import com.synngate.synnframe.presentation.di.modules.feature.TasksFeatureContainer
import com.synngate.synnframe.presentation.ui.dynamicmenu.menu.DynamicMenuViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.DynamicProductDetailViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.DynamicProductsViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.DynamicTaskDetailViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.DynamicTasksViewModel
import com.synngate.synnframe.presentation.ui.login.LoginViewModel
import com.synngate.synnframe.presentation.ui.logs.LogDetailViewModel
import com.synngate.synnframe.presentation.ui.logs.LogListViewModel
import com.synngate.synnframe.presentation.ui.main.MainMenuViewModel
import com.synngate.synnframe.presentation.ui.products.ProductDetailViewModel
import com.synngate.synnframe.presentation.ui.products.ProductListViewModel
import com.synngate.synnframe.presentation.ui.server.ServerDetailViewModel
import com.synngate.synnframe.presentation.ui.server.ServerListViewModel
import com.synngate.synnframe.presentation.ui.settings.SettingsViewModel
import com.synngate.synnframe.presentation.ui.sync.SyncHistoryViewModel
import com.synngate.synnframe.presentation.ui.taskx.TaskXDetailViewModel
import com.synngate.synnframe.presentation.ui.taskx.TaskXListViewModel
import com.synngate.synnframe.presentation.ui.wizard.ActionWizardViewModel
import timber.log.Timber

/**
 * Контейнер для уровня экрана.
 * Содержит фабрики для создания ViewModel-ей для конкретного экрана,
 * делегируя создание соответствующим функциональным контейнерам.
 */
class ScreenContainer(
    private val appContainer: AppContainer,
    private val navigationContainer: NavigationContainer,
    private val navGraphRoute: String? = null
) : DiContainer() {

    // Хранилище для локальных (не делегированных) ViewModel-ей
    private val localViewModels = mutableMapOf<String, ViewModel>()

    /**
     * Создание ViewModel для экрана входа в систему
     */
    fun createLoginViewModel(): LoginViewModel {
        try {
            val authContainer = navigationContainer.getNavigationGraphContainer<AuthFeatureContainer>("auth_graph")
            return authContainer.createLoginViewModel()
        } catch (e: Exception) {
            Timber.e(e, "Error creating LoginViewModel from AuthFeatureContainer")
            // Фолбэк на обычное создание ViewModel
            return getOrCreateLocalViewModel("LoginViewModel") {
                LoginViewModel(
                    userUseCases = appContainer.getDomainContainer().userUseCases,
                    serverUseCases = appContainer.getDomainContainer().serverUseCases,
                    deviceInfoService = appContainer.getCoreContainer().deviceInfoService,
                    ioDispatcher = kotlinx.coroutines.Dispatchers.IO
                )
            }
        }
    }

    /**
     * Создание ViewModel для списка продуктов
     */
    fun createProductListViewModel(isSelectionMode: Boolean): ProductListViewModel {
        try {
            val productsContainer = navigationContainer.getNavigationGraphContainer<ProductsFeatureContainer>("products_graph")
            return productsContainer.createProductListViewModel(isSelectionMode)
        } catch (e: Exception) {
            Timber.e(e, "Error creating ProductListViewModel from ProductsFeatureContainer")
            return getOrCreateLocalViewModel("ProductListViewModel_${if (isSelectionMode) "selection" else "normal"}") {
                val domainContainer = appContainer.getDomainContainer()
                val coreContainer = appContainer.getCoreContainer()
                val dataContainer = appContainer.getDataContainer()

                ProductListViewModel(
                    productUseCases = domainContainer.productUseCases,
                    soundService = coreContainer.soundService,
                    synchronizationController = domainContainer.synchronizationController,
                    productUiMapper = dataContainer.productUiMapper,
                    resourceProvider = coreContainer.resourceProvider,
                    isSelectionMode = isSelectionMode
                )
            }
        }
    }

    /**
     * Создание ViewModel для деталей продукта
     */
    fun createProductDetailViewModel(productId: String): ProductDetailViewModel {
        try {
            val productsContainer = navigationContainer.getNavigationGraphContainer<ProductsFeatureContainer>("products_graph")
            return productsContainer.createProductDetailViewModel(productId)
        } catch (e: Exception) {
            Timber.e(e, "Error creating ProductDetailViewModel from ProductsFeatureContainer")
            return getOrCreateLocalViewModel("ProductDetailViewModel_$productId") {
                val domainContainer = appContainer.getDomainContainer()
                val coreContainer = appContainer.getCoreContainer()
                val dataContainer = appContainer.getDataContainer()

                ProductDetailViewModel(
                    productId = productId,
                    productUseCases = domainContainer.productUseCases,
                    clipboardService = coreContainer.clipboardService,
                    productUiMapper = dataContainer.productUiMapper,
                    resourceProvider = coreContainer.resourceProvider
                )
            }
        }
    }

    /**
     * Создание ViewModel для списка задач X
     */
    fun createTaskXListViewModel(): TaskXListViewModel {
        try {
            val tasksContainer = navigationContainer.getNavigationGraphContainer<TasksFeatureContainer>("taskx_graph")
            return tasksContainer.createTaskXListViewModel()
        } catch (e: Exception) {
            Timber.e(e, "Error creating TaskXListViewModel from TasksFeatureContainer")
            return getOrCreateLocalViewModel("TaskXListViewModel") {
                val domainContainer = appContainer.getDomainContainer()

                TaskXListViewModel(
                    taskXUseCases = domainContainer.taskXUseCases,
                    userUseCases = domainContainer.userUseCases
                )
            }
        }
    }

    /**
     * Создание ViewModel для деталей задачи X
     */
    fun createTaskXDetailViewModel(taskId: String): TaskXDetailViewModel {
        try {
            val tasksContainer = navigationContainer.getNavigationGraphContainer<TasksFeatureContainer>("taskx_graph")
            return tasksContainer.createTaskXDetailViewModel(taskId)
        } catch (e: Exception) {
            Timber.e(e, "Error creating TaskXDetailViewModel from TasksFeatureContainer")
            return getOrCreateLocalViewModel("TaskXDetailViewModel_$taskId") {
                val domainContainer = appContainer.getDomainContainer()

                // Проверяем, есть ли данные в TaskContextManager
                val contextTask = domainContainer.taskContextManager.lastStartedTaskX.value
                val contextTaskType = domainContainer.taskContextManager.lastTaskTypeX.value

                // Если задача в контексте и совпадает по ID, используем её вместе с типом
                if (contextTask != null && contextTask.id == taskId && contextTaskType != null) {
                    TaskXDetailViewModel(
                        taskId = taskId,
                        taskXUseCases = domainContainer.taskXUseCases,
                        userUseCases = domainContainer.userUseCases,
                        finalActionsValidator = domainContainer.finalActionsValidator,
                        actionExecutionService = domainContainer.actionExecutionService,
                        actionSearchService = domainContainer.actionSearchService,
                        preloadedTask = contextTask,
                        preloadedTaskType = contextTaskType
                    )
                } else {
                    // Иначе создаём обычный ViewModel, который загрузит данные
                    TaskXDetailViewModel(
                        taskId = taskId,
                        taskXUseCases = domainContainer.taskXUseCases,
                        userUseCases = domainContainer.userUseCases,
                        finalActionsValidator = domainContainer.finalActionsValidator,
                        actionExecutionService = domainContainer.actionExecutionService,
                        actionSearchService = domainContainer.actionSearchService
                    )
                }
            }
        }
    }

    /**
     * Создание ViewModel для экрана мастера действий
     */
    fun createActionWizardViewModel(taskId: String, actionId: String): ActionWizardViewModel {
        try {
            val tasksContainer = navigationContainer.getNavigationGraphContainer<TasksFeatureContainer>("taskx_graph")
            return tasksContainer.createActionWizardViewModel(taskId, actionId)
        } catch (e: Exception) {
            Timber.e(e, "Error creating ActionWizardViewModel from TasksFeatureContainer")
            return getOrCreateLocalViewModel("ActionWizardViewModel_${taskId}_${actionId}") {
                val domainContainer = appContainer.getDomainContainer()

                ActionWizardViewModel(
                    taskId = taskId,
                    actionId = actionId,
                    wizardStateMachine = domainContainer.wizardStateMachine,
                    actionStepFactoryRegistry = createActionStepFactoryRegistry()
                )
            }
        }
    }

    /**
     * Создание ViewModel для списка логов
     */
    fun createLogListViewModel(): LogListViewModel {
        try {
            val logsContainer = navigationContainer.getNavigationGraphContainer<LogsFeatureContainer>("logs_graph")
            return logsContainer.createLogListViewModel()
        } catch (e: Exception) {
            Timber.e(e, "Error creating LogListViewModel from LogsFeatureContainer")
            return getOrCreateLocalViewModel("LogListViewModel") {
                val domainContainer = appContainer.getDomainContainer()

                LogListViewModel(
                    domainContainer.logUseCases
                )
            }
        }
    }

    /**
     * Создание ViewModel для деталей лога
     */
    fun createLogDetailViewModel(logId: Int): LogDetailViewModel {
        try {
            val logsContainer = navigationContainer.getNavigationGraphContainer<LogsFeatureContainer>("logs_graph")
            return logsContainer.createLogDetailViewModel(logId)
        } catch (e: Exception) {
            Timber.e(e, "Error creating LogDetailViewModel from LogsFeatureContainer")
            return getOrCreateLocalViewModel("LogDetailViewModel_$logId") {
                val domainContainer = appContainer.getDomainContainer()
                val coreContainer = appContainer.getCoreContainer()

                LogDetailViewModel(
                    logId = logId,
                    logUseCases = domainContainer.logUseCases,
                    clipboardService = coreContainer.clipboardService
                )
            }
        }
    }

    /**
     * Создание ViewModel для экрана настроек
     */
    fun createSettingsViewModel(): SettingsViewModel {
        try {
            val settingsContainer = navigationContainer.getNavigationGraphContainer<SettingsFeatureContainer>("settings_graph")
            return settingsContainer.createSettingsViewModel()
        } catch (e: Exception) {
            Timber.e(e, "Error creating SettingsViewModel from SettingsFeatureContainer")
            return getOrCreateLocalViewModel("SettingsViewModel") {
                val domainContainer = appContainer.getDomainContainer()

                SettingsViewModel(
                    settingsUseCases = domainContainer.settingsUseCases,
                    serverUseCases = domainContainer.serverUseCases,
                    webServerManager = domainContainer.webServerManager,
                    synchronizationController = domainContainer.synchronizationController,
                    updateInstaller = domainContainer.updateInstaller
                )
            }
        }
    }

    /**
     * Создание ViewModel для истории синхронизации
     */
    fun createSyncHistoryViewModel(): SyncHistoryViewModel {
        try {
            val settingsContainer = navigationContainer.getNavigationGraphContainer<SettingsFeatureContainer>("settings_graph")
            return settingsContainer.createSyncHistoryViewModel()
        } catch (e: Exception) {
            Timber.e(e, "Error creating SyncHistoryViewModel from SettingsFeatureContainer")
            return getOrCreateLocalViewModel("SyncHistoryViewModel") {
                val domainContainer = appContainer.getDomainContainer()

                SyncHistoryViewModel(
                    synchronizationController = domainContainer.synchronizationController
                )
            }
        }
    }

    /**
     * Создание ViewModel для списка серверов
     */
    fun createServerListViewModel(): ServerListViewModel {
        try {
            val settingsContainer = navigationContainer.getNavigationGraphContainer<SettingsFeatureContainer>("settings_graph")
            return settingsContainer.createServerListViewModel()
        } catch (e: Exception) {
            Timber.e(e, "Error creating ServerListViewModel from SettingsFeatureContainer")
            return getOrCreateLocalViewModel("ServerListViewModel") {
                val domainContainer = appContainer.getDomainContainer()

                ServerListViewModel(
                    serverUseCases = domainContainer.serverUseCases,
                    settingsUseCases = domainContainer.settingsUseCases
                )
            }
        }
    }

    /**
     * Создание ViewModel для деталей сервера
     */
    fun createServerDetailViewModel(serverId: Int?): ServerDetailViewModel {
        try {
            val settingsContainer = navigationContainer.getNavigationGraphContainer<SettingsFeatureContainer>("settings_graph")
            return settingsContainer.createServerDetailViewModel(serverId)
        } catch (e: Exception) {
            Timber.e(e, "Error creating ServerDetailViewModel from SettingsFeatureContainer")
            return getOrCreateLocalViewModel("ServerDetailViewModel_${serverId ?: "new"}") {
                val domainContainer = appContainer.getDomainContainer()

                ServerDetailViewModel(
                    serverId = serverId,
                    serverUseCases = domainContainer.serverUseCases
                )
            }
        }
    }

    /**
     * Создание ViewModel для главного меню
     */
    fun createMainMenuViewModel(): MainMenuViewModel {
        try {
            val mainContainer = navigationContainer.getNavigationGraphContainer<MainFeatureContainer>("main_graph")
            return mainContainer.createMainMenuViewModel()
        } catch (e: Exception) {
            Timber.e(e, "Error creating MainMenuViewModel from MainFeatureContainer")
            return getOrCreateLocalViewModel("MainMenuViewModel") {
                val domainContainer = appContainer.getDomainContainer()

                MainMenuViewModel(
                    userUseCases = domainContainer.userUseCases,
                    productUseCases = domainContainer.productUseCases,
                    synchronizationController = domainContainer.synchronizationController
                )
            }
        }
    }

    /**
     * Создание ViewModel для экрана динамического меню
     */
    fun createDynamicMenuViewModel(): DynamicMenuViewModel {
        try {
            val dynamicContainer = navigationContainer.getNavigationGraphContainer<DynamicFeatureContainer>("dynamic_nav_graph")
            return dynamicContainer.createDynamicMenuViewModel()
        } catch (e: Exception) {
            Timber.e(e, "Error creating DynamicMenuViewModel from DynamicFeatureContainer")
            return getOrCreateLocalViewModel("DynamicMenuViewModel") {
                val domainContainer = appContainer.getDomainContainer()

                DynamicMenuViewModel(
                    dynamicMenuUseCases = domainContainer.dynamicMenuUseCases
                )
            }
        }
    }

    /**
     * Создание ViewModel для экрана динамических задач
     */
    fun createDynamicTasksViewModel(
        menuItemId: String,
        menuItemName: String,
        endpoint: String,
        screenSettings: ScreenSettings
    ): DynamicTasksViewModel {
        try {
            val dynamicContainer = navigationContainer.getNavigationGraphContainer<DynamicFeatureContainer>("dynamic_nav_graph")
            return dynamicContainer.createDynamicTasksViewModel(menuItemId, menuItemName, endpoint, screenSettings)
        } catch (e: Exception) {
            Timber.e(e, "Error creating DynamicTasksViewModel from DynamicFeatureContainer")
            return getOrCreateLocalViewModel("DynamicTasksViewModel_${menuItemId}_${endpoint.hashCode()}") {
                val domainContainer = appContainer.getDomainContainer()

                DynamicTasksViewModel(
                    menuItemId = menuItemId,
                    menuItemName = menuItemName,
                    endpoint = endpoint,
                    screenSettings = screenSettings,
                    dynamicMenuUseCases = domainContainer.dynamicMenuUseCases,
                    userUseCases = domainContainer.userUseCases,
                    taskContextManager = domainContainer.taskContextManager
                )
            }
        }
    }

    /**
     * Создание ViewModel для экрана деталей динамической задачи
     */
    fun createDynamicTaskDetailViewModel(
        taskId: String,
        endpoint: String
    ): DynamicTaskDetailViewModel {
        try {
            val dynamicContainer = navigationContainer.getNavigationGraphContainer<DynamicFeatureContainer>("dynamic_nav_graph")
            return dynamicContainer.createDynamicTaskDetailViewModel(taskId, endpoint)
        } catch (e: Exception) {
            Timber.e(e, "Error creating DynamicTaskDetailViewModel from DynamicFeatureContainer")
            return getOrCreateLocalViewModel("DynamicTaskDetailViewModel_${taskId}_${endpoint.hashCode()}") {
                val domainContainer = appContainer.getDomainContainer()

                DynamicTaskDetailViewModel(
                    taskId = taskId,
                    endpoint = endpoint,
                    dynamicMenuUseCases = domainContainer.dynamicMenuUseCases,
                    userUseCases = domainContainer.userUseCases,
                    taskContextManager = domainContainer.taskContextManager
                )
            }
        }
    }

    /**
     * Создание ViewModel для экрана динамических продуктов
     */
    fun createDynamicProductsViewModel(
        menuItemId: String,
        menuItemName: String,
        endpoint: String,
        screenSettings: ScreenSettings
    ): DynamicProductsViewModel {
        try {
            val dynamicContainer = navigationContainer.getNavigationGraphContainer<DynamicFeatureContainer>("dynamic_nav_graph")
            return dynamicContainer.createDynamicProductsViewModel(menuItemId, menuItemName, endpoint, screenSettings)
        } catch (e: Exception) {
            Timber.e(e, "Error creating DynamicProductsViewModel from DynamicFeatureContainer")
            return getOrCreateLocalViewModel("DynamicProductsViewModel_${menuItemId}_${endpoint.hashCode()}") {
                val domainContainer = appContainer.getDomainContainer()
                val coreContainer = appContainer.getCoreContainer()
                val dataContainer = appContainer.getDataContainer()

                DynamicProductsViewModel(
                    menuItemId = menuItemId,
                    menuItemName = menuItemName,
                    endpoint = endpoint,
                    screenSettings = screenSettings,
                    dynamicMenuUseCases = domainContainer.dynamicMenuUseCases,
                    soundService = coreContainer.soundService,
                    productUiMapper = dataContainer.productUiMapper,
                    isSelectionMode = false
                )
            }
        }
    }

    /**
     * Создание ViewModel для экрана деталей динамического продукта
     */
    fun createDynamicProductDetailViewModel(product: DynamicProduct): DynamicProductDetailViewModel {
        try {
            val dynamicContainer = navigationContainer.getNavigationGraphContainer<DynamicFeatureContainer>("dynamic_nav_graph")
            return dynamicContainer.createDynamicProductDetailViewModel(product)
        } catch (e: Exception) {
            Timber.e(e, "Error creating DynamicProductDetailViewModel from DynamicFeatureContainer")
            return getOrCreateLocalViewModel("DynamicProductDetailViewModel_${product.id}") {
                val coreContainer = appContainer.getCoreContainer()
                val dataContainer = appContainer.getDataContainer()

                DynamicProductDetailViewModel(
                    dynamicProduct = product,
                    clipboardService = coreContainer.clipboardService,
                    productUiMapper = dataContainer.productUiMapper,
                    resourceProvider = coreContainer.resourceProvider
                )
            }
        }
    }

    /**
     * Получение или создание локальной ViewModel
     * Используется для ViewModel-ей, которые не делегируются функциональным контейнерам
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : ViewModel> getOrCreateLocalViewModel(key: String, factory: () -> T): T {
        return localViewModels.getOrPut(key) {
            Timber.d("Creating local ViewModel: $key in ScreenContainer")
            factory()
        } as T
    }

    /**
     * Создание реестра фабрик для шагов мастера действий
     * Метод-хелпер для создания ActionWizardViewModel
     */
    private fun createActionStepFactoryRegistry(): com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry {
        val domainContainer = appContainer.getDomainContainer()

        // Создаем реестр
        val registry = com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry()

        // Регистрируем фабрики для различных типов объектов
        registry.registerFactory(
            com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType.CLASSIFIER_PRODUCT,
            com.synngate.synnframe.presentation.ui.wizard.action.product.ProductSelectionStepFactory(
                productLookupService = domainContainer.productLookupService,
                validationService = domainContainer.validationService
            )
        )

        registry.registerFactory(
            com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType.TASK_PRODUCT,
            com.synngate.synnframe.presentation.ui.wizard.action.taskproduct.TaskProductSelectionStepFactory(
                productLookupService = domainContainer.productLookupService,
                validationService = domainContainer.validationService
            )
        )

        registry.registerFactory(
            com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType.PRODUCT_QUANTITY,
            com.synngate.synnframe.presentation.ui.wizard.action.quantity.ProductQuantityStepFactory(
                validationService = domainContainer.validationService
            )
        )

        registry.registerFactory(
            com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType.PALLET,
            com.synngate.synnframe.presentation.ui.wizard.action.pallet.PalletSelectionStepFactory(
                palletLookupService = domainContainer.palletLookupService,
                validationService = domainContainer.validationService
            )
        )

        registry.registerFactory(
            com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType.BIN,
            com.synngate.synnframe.presentation.ui.wizard.action.bin.BinSelectionStepFactory(
                binLookupService = domainContainer.binLookupService,
                validationService = domainContainer.validationService
            )
        )

        return registry
    }

    /**
     * Освобождение ресурсов контейнера
     */
    override fun dispose() {
        Timber.d("Disposing ScreenContainer")

        // Освобождаем локальные ViewModel-и
        localViewModels.values.forEach { viewModel ->
            if (viewModel is Disposable) {
                viewModel.dispose()
            }
        }
        localViewModels.clear()

        super.dispose()
    }
}