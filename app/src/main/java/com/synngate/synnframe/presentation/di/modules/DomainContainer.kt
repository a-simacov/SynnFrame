package com.synngate.synnframe.presentation.di.modules

import com.synngate.synnframe.data.service.ServerCoordinatorImpl
import com.synngate.synnframe.data.service.SynchronizationControllerImpl
import com.synngate.synnframe.data.service.WebServerControllerImpl
import com.synngate.synnframe.data.service.WebServerManagerImpl
import com.synngate.synnframe.domain.model.wizard.WizardStateMachine
import com.synngate.synnframe.domain.service.ActionExecutionService
import com.synngate.synnframe.domain.service.ActionSearchService
import com.synngate.synnframe.domain.service.ActionSearchServiceImpl
import com.synngate.synnframe.domain.service.FinalActionsValidator
import com.synngate.synnframe.domain.service.ServerCoordinator
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.domain.service.UpdateInstaller
import com.synngate.synnframe.domain.service.UpdateInstallerImpl
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.service.WebServerManager
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.api.DomainAPI
import com.synngate.synnframe.presentation.di.modules.api.ModuleAPI
import com.synngate.synnframe.presentation.ui.wizard.service.BinLookupService
import com.synngate.synnframe.presentation.ui.wizard.service.PalletLookupService
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService
import timber.log.Timber

/**
 * Модульный контейнер для бизнес-логики (use cases, сервисы, валидаторы и т.д.)
 *
 * @param appContainer Основной контейнер приложения
 * @param coreContainer Контейнер базовых компонентов
 * @param dataContainer Контейнер компонентов доступа к данным
 */
class DomainContainer(
    appContainer: AppContainer,
    private val coreContainer: CoreContainer,
    private val dataContainer: DataContainer,
    private val networkContainer: NetworkContainer
) : ModuleContainer(appContainer), DomainAPI, ModuleAPI {

    override val moduleName: String = "Domain"

    // TaskContextManager - центральный компонент для управления контекстом задач
    override val taskContextManager: TaskContextManager by lazy {
        Timber.d("Creating TaskContextManager")
        TaskContextManager()
    }

    // Use Cases
    override val serverUseCases: ServerUseCases by lazy {
        Timber.d("Creating ServerUseCases")
        ServerUseCases(dataContainer.serverRepository, serverCoordinator)
    }

    override val userUseCases: UserUseCases by lazy {
        Timber.d("Creating UserUseCases")
        UserUseCases(dataContainer.userRepository)
    }

    override val productUseCases: ProductUseCases by lazy {
        Timber.d("Creating ProductUseCases")
        ProductUseCases(dataContainer.productRepository)
    }

    override val logUseCases: LogUseCases by lazy {
        Timber.d("Creating LogUseCases")
        LogUseCases(dataContainer.logRepository, coreContainer.loggingService)
    }

    override val settingsUseCases: SettingsUseCases by lazy {
        Timber.d("Creating SettingsUseCases")
        SettingsUseCases(
            dataContainer.settingsRepository,
            coreContainer.fileService,
            appContainer.applicationContext
        )
    }

    override val dynamicMenuUseCases: DynamicMenuUseCases by lazy {
        Timber.d("Creating DynamicMenuUseCases")
        DynamicMenuUseCases(dataContainer.dynamicMenuRepository)
    }

    override val taskXUseCases: TaskXUseCases by lazy {
        Timber.d("Creating TaskXUseCases")
        TaskXUseCases(dataContainer.taskXRepository, taskContextManager)
    }

    // Бизнес-сервисы
    override val actionExecutionService: ActionExecutionService by lazy {
        Timber.d("Creating ActionExecutionService")
        ActionExecutionService(
            taskContextManager = taskContextManager,
            taskXRepository = dataContainer.taskXRepository
        )
    }

    override val validationService: ValidationService by lazy {
        Timber.d("Creating ValidationService")
        ValidationService(networkContainer.validationApiService)
    }

    override val finalActionsValidator: FinalActionsValidator by lazy {
        Timber.d("Creating FinalActionsValidator")
        FinalActionsValidator()
    }

    override val actionSearchService: ActionSearchService by lazy {
        Timber.d("Creating ActionSearchService")
        ActionSearchServiceImpl(
            actionSearchApi = networkContainer.actionSearchApi,
            productRepository = dataContainer.productRepository
        )
    }

    // Контроллер и менеджер веб-сервера
    val webServerController by lazy {
        WebServerControllerImpl(appContainer.applicationContext)
    }

    override val webServerManager: WebServerManager by lazy {
        Timber.d("Creating WebServerManager")
        WebServerManagerImpl(webServerController)
    }

    override val updateInstaller: UpdateInstaller by lazy {
        Timber.d("Creating UpdateInstaller")
        UpdateInstallerImpl(appContainer.applicationContext)
    }

    override val synchronizationController: SynchronizationController by lazy {
        Timber.d("Creating SynchronizationController")
        SynchronizationControllerImpl(
            appContainer.applicationContext,
            productUseCases,
            coreContainer.appSettingsDataStore,
            dataContainer.database
        )
    }

    override val serverCoordinator: ServerCoordinator by lazy {
        Timber.d("Creating ServerCoordinator")
        ServerCoordinatorImpl(
            dataContainer.serverRepository,
            coreContainer.appSettingsDataStore
        )
    }

    // Сервисы для визарда
    val productLookupService by lazy {
        Timber.d("Creating ProductLookupService")
        ProductLookupService(dataContainer.productRepository)
    }

    val binLookupService by lazy {
        Timber.d("Creating BinLookupService")
        BinLookupService(
            taskContextManager = taskContextManager,
            wizardBinRepository = dataContainer.wizardBinRepository
        )
    }

    val palletLookupService by lazy {
        Timber.d("Creating PalletLookupService")
        PalletLookupService(
            taskContextManager = taskContextManager,
            wizardPalletRepository = dataContainer.wizardPalletRepository
        )
    }

    val wizardStateMachine by lazy {
        Timber.d("Creating WizardStateMachine")
        WizardStateMachine(
            taskContextManager = taskContextManager,
            actionExecutionService = actionExecutionService
        )
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Domain module initialized")
    }

    override fun cleanup() {
        Timber.d("Cleaning up Domain module")
    }
}