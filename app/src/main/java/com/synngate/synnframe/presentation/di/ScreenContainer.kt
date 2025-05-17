package com.synngate.synnframe.presentation.di

import androidx.lifecycle.ViewModel
import com.synngate.synnframe.presentation.di.modules.feature.AuthFeatureContainer
import com.synngate.synnframe.presentation.ui.login.LoginViewModel
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
     * Получение или создание локальной ViewModel
     * Используется для ViewModel-ей, которые не делегируются функциональным контейнерам
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : ViewModel> getOrCreateLocalViewModel(key: String, factory: () -> T): T {
        return localViewModels.getOrPut(key) {
            Timber.d("Creating local ViewModel: $key in ScreenContainer")
            factory()
        } as T
    }

    /**
     * Создание ViewModel для списка продуктов
     */
    fun createProductListViewModel(isSelectionMode: Boolean): ViewModel {
        return getOrCreateLocalViewModel("ProductListViewModel_${if (isSelectionMode) "selection" else "normal"}") {
            val domainContainer = appContainer.getDomainContainer()
            val coreContainer = appContainer.getCoreContainer()
            val dataContainer = appContainer.getDataContainer()

            com.synngate.synnframe.presentation.ui.products.ProductListViewModel(
                productUseCases = domainContainer.productUseCases,
                soundService = coreContainer.soundService,
                synchronizationController = domainContainer.synchronizationController,
                productUiMapper = dataContainer.productUiMapper,
                resourceProvider = coreContainer.resourceProvider,
                isSelectionMode = isSelectionMode
            )
        }
    }

    /**
     * Создание ViewModel для деталей продукта
     */
    fun createProductDetailViewModel(productId: String): ViewModel {
        return getOrCreateLocalViewModel("ProductDetailViewModel_$productId") {
            val domainContainer = appContainer.getDomainContainer()
            val coreContainer = appContainer.getCoreContainer()
            val dataContainer = appContainer.getDataContainer()

            com.synngate.synnframe.presentation.ui.products.ProductDetailViewModel(
                productId = productId,
                productUseCases = domainContainer.productUseCases,
                clipboardService = coreContainer.clipboardService,
                productUiMapper = dataContainer.productUiMapper,
                resourceProvider = coreContainer.resourceProvider
            )
        }
    }

    /**
     * Создание ViewModel для списка задач X
     */
    fun createTaskXListViewModel(): ViewModel {
        return getOrCreateLocalViewModel("TaskXListViewModel") {
            val domainContainer = appContainer.getDomainContainer()

            com.synngate.synnframe.presentation.ui.taskx.TaskXListViewModel(
                taskXUseCases = domainContainer.taskXUseCases,
                userUseCases = domainContainer.userUseCases
            )
        }
    }

    /**
     * Создание ViewModel для деталей задачи X
     */
    fun createTaskXDetailViewModel(taskId: String): ViewModel {
        return getOrCreateLocalViewModel("TaskXDetailViewModel_$taskId") {
            val domainContainer = appContainer.getDomainContainer()

            // Проверяем, есть ли данные в TaskContextManager
            val contextTask = domainContainer.taskContextManager.lastStartedTaskX.value
            val contextTaskType = domainContainer.taskContextManager.lastTaskTypeX.value

            // Если задача в контексте и совпадает по ID, используем её вместе с типом
            if (contextTask != null && contextTask.id == taskId && contextTaskType != null) {
                com.synngate.synnframe.presentation.ui.taskx.TaskXDetailViewModel(
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
                com.synngate.synnframe.presentation.ui.taskx.TaskXDetailViewModel(
                    taskId = taskId,
                    taskXUseCases = domainContainer.taskXUseCases,
                    userUseCases = domainContainer.userUseCases,
                    finalActionsValidator = domainContainer.finalActionsValidator,
                    actionExecutionService = domainContainer.actionExecutionService
                )
            }
        }
    }

    /**
     * Создание ViewModel для экрана мастера действий
     */
    fun createActionWizardViewModel(taskId: String, actionId: String): ViewModel {
        return getOrCreateLocalViewModel("ActionWizardViewModel_${taskId}_${actionId}") {
            val domainContainer = appContainer.getDomainContainer()

            com.synngate.synnframe.presentation.ui.wizard.ActionWizardViewModel(
                taskId = taskId,
                actionId = actionId,
                wizardStateMachine = domainContainer.wizardStateMachine,
                actionStepFactoryRegistry = createActionStepFactoryRegistry()
            )
        }
    }

    /**
     * Создание ViewModel для списка логов
     */
    fun createLogListViewModel(): ViewModel {
        return getOrCreateLocalViewModel("LogListViewModel") {
            val domainContainer = appContainer.getDomainContainer()

            com.synngate.synnframe.presentation.ui.logs.LogListViewModel(
                domainContainer.logUseCases
            )
        }
    }

    /**
     * Создание ViewModel для деталей лога
     */
    fun createLogDetailViewModel(logId: Int): ViewModel {
        return getOrCreateLocalViewModel("LogDetailViewModel_$logId") {
            val domainContainer = appContainer.getDomainContainer()
            val coreContainer = appContainer.getCoreContainer()

            com.synngate.synnframe.presentation.ui.logs.LogDetailViewModel(
                logId = logId,
                logUseCases = domainContainer.logUseCases,
                clipboardService = coreContainer.clipboardService
            )
        }
    }

    /**
     * Создание ViewModel для экрана настроек
     */
    fun createSettingsViewModel(): ViewModel {
        return getOrCreateLocalViewModel("SettingsViewModel") {
            val domainContainer = appContainer.getDomainContainer()

            com.synngate.synnframe.presentation.ui.settings.SettingsViewModel(
                settingsUseCases = domainContainer.settingsUseCases,
                serverUseCases = domainContainer.serverUseCases,
                webServerManager = domainContainer.webServerManager,
                synchronizationController = domainContainer.synchronizationController,
                updateInstaller = domainContainer.updateInstaller
            )
        }
    }

    /**
     * Создание ViewModel для истории синхронизации
     */
    fun createSyncHistoryViewModel(): ViewModel {
        return getOrCreateLocalViewModel("SyncHistoryViewModel") {
            val domainContainer = appContainer.getDomainContainer()

            com.synngate.synnframe.presentation.ui.sync.SyncHistoryViewModel(
                synchronizationController = domainContainer.synchronizationController
            )
        }
    }

    /**
     * Создание ViewModel для списка серверов
     */
    fun createServerListViewModel(): ViewModel {
        return getOrCreateLocalViewModel("ServerListViewModel") {
            val domainContainer = appContainer.getDomainContainer()

            com.synngate.synnframe.presentation.ui.server.ServerListViewModel(
                serverUseCases = domainContainer.serverUseCases,
                settingsUseCases = domainContainer.settingsUseCases
            )
        }
    }

    /**
     * Создание ViewModel для деталей сервера
     */
    fun createServerDetailViewModel(serverId: Int?): ViewModel {
        return getOrCreateLocalViewModel("ServerDetailViewModel_${serverId ?: "new"}") {
            val domainContainer = appContainer.getDomainContainer()

            com.synngate.synnframe.presentation.ui.server.ServerDetailViewModel(
                serverId = serverId,
                serverUseCases = domainContainer.serverUseCases
            )
        }
    }

    /**
     * Создание ViewModel для главного меню
     */
    fun createMainMenuViewModel(): ViewModel {
        return getOrCreateLocalViewModel("MainMenuViewModel") {
            val domainContainer = appContainer.getDomainContainer()

            com.synngate.synnframe.presentation.ui.main.MainMenuViewModel(
                userUseCases = domainContainer.userUseCases,
                productUseCases = domainContainer.productUseCases,
                synchronizationController = domainContainer.synchronizationController
            )
        }
    }

    /**
     * Создание ViewModel для экрана динамического меню
     */
    fun createDynamicMenuViewModel(): ViewModel {
        return getOrCreateLocalViewModel("DynamicMenuViewModel") {
            val domainContainer = appContainer.getDomainContainer()

            com.synngate.synnframe.presentation.ui.dynamicmenu.menu.DynamicMenuViewModel(
                dynamicMenuUseCases = domainContainer.dynamicMenuUseCases
            )
        }
    }

    /**
     * Создание реестра фабрик для шагов мастера действий
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