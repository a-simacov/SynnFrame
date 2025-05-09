package com.synngate.synnframe.presentation.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.synngate.synnframe.data.barcodescanner.BarcodeScannerFactory
import com.synngate.synnframe.data.barcodescanner.ScannerService
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.local.database.AppDatabase
import com.synngate.synnframe.data.remote.api.AppUpdateApi
import com.synngate.synnframe.data.remote.api.AppUpdateApiImpl
import com.synngate.synnframe.data.remote.api.AuthApi
import com.synngate.synnframe.data.remote.api.AuthApiImpl
import com.synngate.synnframe.data.remote.api.DynamicMenuApi
import com.synngate.synnframe.data.remote.api.DynamicMenuApiImpl
import com.synngate.synnframe.data.remote.api.ProductApi
import com.synngate.synnframe.data.remote.api.ProductApiImpl
import com.synngate.synnframe.data.remote.api.TaskXApi
import com.synngate.synnframe.data.remote.api.TaskXApiImpl
import com.synngate.synnframe.data.remote.api.ValidationApiServiceImpl
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.data.remote.service.ApiServiceImpl
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.data.repository.DynamicMenuRepositoryImpl
import com.synngate.synnframe.data.repository.LogRepositoryImpl
import com.synngate.synnframe.data.repository.ProductRepositoryImpl
import com.synngate.synnframe.data.repository.ServerRepositoryImpl
import com.synngate.synnframe.data.repository.SettingsRepositoryImpl
import com.synngate.synnframe.data.repository.TaskXRepositoryImpl
import com.synngate.synnframe.data.repository.UserRepositoryImpl
import com.synngate.synnframe.data.repository.WizardBinRepositoryImpl
import com.synngate.synnframe.data.repository.WizardPalletRepositoryImpl
import com.synngate.synnframe.data.service.ClipboardServiceImpl
import com.synngate.synnframe.data.service.DeviceInfoServiceImpl
import com.synngate.synnframe.data.service.FileServiceImpl
import com.synngate.synnframe.data.service.LoggingServiceImpl
import com.synngate.synnframe.data.service.ServerCoordinatorImpl
import com.synngate.synnframe.data.service.SoundServiceImpl
import com.synngate.synnframe.data.service.SynchronizationControllerImpl
import com.synngate.synnframe.data.service.WebServerControllerImpl
import com.synngate.synnframe.data.service.WebServerManagerImpl
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.repository.DynamicMenuRepository
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.service.ActionExecutionService
import com.synngate.synnframe.domain.service.ActionStepExecutionService
import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.DeviceInfoService
import com.synngate.synnframe.domain.service.FileService
import com.synngate.synnframe.domain.service.FinalActionsValidator
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.ServerCoordinator
import com.synngate.synnframe.domain.service.SoundService
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
import com.synngate.synnframe.presentation.service.notification.NotificationChannelManager
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
import com.synngate.synnframe.presentation.ui.products.mapper.ProductUiMapper
import com.synngate.synnframe.presentation.ui.server.ServerDetailViewModel
import com.synngate.synnframe.presentation.ui.server.ServerListViewModel
import com.synngate.synnframe.presentation.ui.settings.SettingsViewModel
import com.synngate.synnframe.presentation.ui.sync.SyncHistoryViewModel
import com.synngate.synnframe.presentation.ui.taskx.TaskXDetailViewModel
import com.synngate.synnframe.presentation.ui.taskx.TaskXListViewModel
import com.synngate.synnframe.presentation.ui.wizard.ActionWizardViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.ui.wizard.action.bin.BinSelectionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.pallet.PalletSelectionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.product.ProductSelectionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.quantity.ProductQuantityStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.taskproduct.TaskProductSelectionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.service.BinLookupService
import com.synngate.synnframe.presentation.ui.wizard.service.PalletLookupService
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService
import com.synngate.synnframe.util.network.NetworkMonitor
import com.synngate.synnframe.util.resources.ResourceProvider
import com.synngate.synnframe.util.resources.ResourceProviderImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Реализация контейнера зависимостей для всего приложения.
 * Содержит зависимости, которые живут на протяжении всего жизненного цикла приложения.
 */
class AppContainer(private val applicationContext: Context) : DiContainer(){

    // Единый экземпляр DataStore для настроек приложения
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    // DataStore для хранения настроек приложения
    val appSettingsDataStore by lazy {
        Timber.d("Creating AppSettingsDataStore")
        AppSettingsDataStore(applicationContext.dataStore)
    }

    // Database
    val database by lazy {
        Timber.d("Creating AppDatabase")
        AppDatabase.getInstance(applicationContext)
    }

    // DAO
    val serverDao by lazy { database.serverDao() }
    val userDao by lazy { database.userDao() }
    val logDao by lazy { database.logDao() }
    val productDao by lazy { database.productDao() }

    // HTTP Client
    @OptIn(ExperimentalSerializationApi::class)
    val httpClient by lazy {
        Timber.d("Creating HttpClient")
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    useArrayPolymorphism = true
                    explicitNulls = false
                    coerceInputValues = true
                    encodeDefaults = true
                })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 10000
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.tag("HttpClient").d(message)
                    }
                }
                level = LogLevel.ALL
            }
        }
    }

    val networkMonitor: NetworkMonitor by lazy {
        Timber.d("Creating NetworkMonitor")
        NetworkMonitor(applicationContext)
    }

    // Добавляем ResourceProvider
    val resourceProvider: ResourceProvider by lazy {
        Timber.d("Creating ResourceProvider")
        ResourceProviderImpl(applicationContext)
    }

    // Добавляем UI-мапперы
    val productUiMapper: ProductUiMapper by lazy {
        Timber.d("Creating ProductUiMapper")
        ProductUiMapper(resourceProvider)
    }

    // Сервисы
    val serverProvider by lazy {
        Timber.d("Creating ServerProvider")
        object : ServerProvider {
            override suspend fun getActiveServer() = serverRepository.getActiveServer().first()
            override suspend fun getCurrentUserId() = userRepository.getCurrentUser().first()?.id
        }
    }

    // Репозитории
    val logRepository: LogRepository by lazy {
        Timber.d("Creating LogRepository")
        LogRepositoryImpl(logDao)
    }

    val serverRepository: ServerRepository by lazy {
        Timber.d("Creating ServerRepository")
        ServerRepositoryImpl(serverDao, apiService)
    }

    val userRepository: UserRepository by lazy {
        Timber.d("Creating UserRepository")
        UserRepositoryImpl(userDao, authApi, appSettingsDataStore)
    }

    val productRepository: ProductRepository by lazy {
        Timber.d("Creating ProductRepository")
        ProductRepositoryImpl(productDao, productApi, database)
    }

    val settingsRepository: SettingsRepository by lazy {
        Timber.d("Creating SettingsRepository")
        SettingsRepositoryImpl(
            appSettingsDataStore,
            appUpdateApi
        )
    }

    val dynamicMenuRepository: DynamicMenuRepository by lazy {
        Timber.d("Creating OperationMenuRepository")
        DynamicMenuRepositoryImpl(dynamicMenuApi)
    }

    // API сервисы
    val apiService: ApiService by lazy {
        Timber.d("Creating ApiService")
        ApiServiceImpl(httpClient, serverProvider)
    }

    val authApi: AuthApi by lazy {
        Timber.d("Creating AuthApi")
        AuthApiImpl(apiService)
    }

    val productApi: ProductApi by lazy {
        Timber.d("Creating ProductApi")
        ProductApiImpl(httpClient, serverProvider)
    }

    val appUpdateApi: AppUpdateApi by lazy {
        Timber.d("Creating AppUpdateApi")
        AppUpdateApiImpl(httpClient, serverProvider)
    }

    val dynamicMenuApi: DynamicMenuApi by lazy {
        Timber.d("Creating OperationMenuApi")
        DynamicMenuApiImpl(httpClient, serverProvider)
    }

    // Добавляем ValidationApiService
    val validationApiService by lazy {
        Timber.d("Creating ValidationApiService")
        ValidationApiServiceImpl(httpClient, serverProvider)
    }

    // Сервисы
    val loggingService: LoggingService by lazy {
        Timber.d("Creating LoggingService")
        LoggingServiceImpl(logRepository)
    }

    val clipboardService: ClipboardService by lazy {
        Timber.d("Creating ClipboardService")
        ClipboardServiceImpl(applicationContext)
    }

    val serverCoordinator: ServerCoordinator by lazy {
        Timber.d("Creating ServerCoordinator")
        ServerCoordinatorImpl(
            serverRepository,
            appSettingsDataStore
        )
    }

    val deviceInfoService: DeviceInfoService by lazy {
        Timber.d("Creating DeviceInfoService")
        DeviceInfoServiceImpl(applicationContext)
    }

    val webServerController by lazy {
        WebServerControllerImpl(applicationContext)
    }

    val webServerManager: WebServerManager by lazy {
        Timber.d("Creating WebServerManager")
        WebServerManagerImpl(webServerController)
    }

    val updateInstaller: UpdateInstaller by lazy {
        Timber.d("Creating UpdateInstaller")
        UpdateInstallerImpl(applicationContext)
    }

    val soundService: SoundService by lazy {
        SoundServiceImpl(applicationContext)
    }

    val fileService: FileService by lazy {
        FileServiceImpl(applicationContext)
    }

    val notificationChannelManager by lazy {
        NotificationChannelManager(applicationContext)
    }

    val synchronizationController: SynchronizationController by lazy {
        SynchronizationControllerImpl(
            applicationContext,
            productUseCases,
            appSettingsDataStore,
            database
        )
    }

    // Сервисы для работы с визардом действий
    val validationService by lazy {
        Timber.d("Creating ValidationService")
        ValidationService(validationApiService)
    }

    val actionExecutionService by lazy {
        Timber.d("Creating ActionExecutionService")
        ActionExecutionService(
            taskContextManager = taskContextManager,
            taskXRepository = taskXRepository // Добавляем TaskXRepository
        )
    }

    val actionStepExecutionService by lazy {
        Timber.d("Creating ActionStepExecutionService")
        ActionStepExecutionService(
            validationService = validationService,
            taskContextManager = taskContextManager // Добавляем параметр taskContextManager
        )
    }

    val productLookupService by lazy {
        Timber.d("Creating ProductLookupService")
        ProductLookupService(productRepository)
    }

    val wizardBinRepository by lazy {
        Timber.d("Creating WizardBinRepository")
        // Здесь используем WizardBinRepository, как показано в импортированных файлах
        WizardBinRepositoryImpl(httpClient, serverProvider)
    }

    val wizardPalletRepository by lazy {
        Timber.d("Creating WizardPalletRepository")
        // Здесь используем WizardPalletRepository, как показано в импортированных файлах
        WizardPalletRepositoryImpl(httpClient, serverProvider)
    }

    // Сервисы для поиска объектов (lookup services)
    val binLookupService by lazy {
        Timber.d("Creating BinLookupService")
        BinLookupService(
            taskContextManager = taskContextManager,
            wizardBinRepository = wizardBinRepository
        )
    }

    val palletLookupService by lazy {
        Timber.d("Creating PalletLookupService")
        PalletLookupService(
            taskContextManager = taskContextManager,
            wizardPalletRepository = wizardPalletRepository
        )
    }

    val actionWizardController by lazy {
        Timber.d("Creating ActionWizardController")
        ActionWizardController(
            actionExecutionService = actionExecutionService,
            actionStepExecutionService = actionStepExecutionService,
            taskContextManager = taskContextManager,
            taskXRepository = taskXRepository // Добавляем TaskXRepository
        )
    }

    val actionWizardContextFactory by lazy {
        Timber.d("Creating ActionWizardContextFactory")
        ActionWizardContextFactory()
    }

    val taskXApi: TaskXApi by lazy {
        Timber.d("Creating TaskXApi")
        TaskXApiImpl(httpClient, serverProvider)
    }

    val taskXRepository: TaskXRepository by lazy {
        Timber.d("Creating TaskXRepository")
        TaskXRepositoryImpl(taskXApi, taskContextManager)
    }

    // Use Cases
    val serverUseCases by lazy {
        ServerUseCases(serverRepository, serverCoordinator)
    }

    val userUseCases by lazy {
        UserUseCases(userRepository)
    }

    val productUseCases by lazy {
        ProductUseCases(productRepository)
    }

    val logUseCases by lazy {
        LogUseCases(logRepository, loggingService)
    }

    val settingsUseCases by lazy {
        SettingsUseCases(settingsRepository, fileService, applicationContext)
    }

    val dynamicMenuUseCases: DynamicMenuUseCases by lazy {
        Timber.d("Creating OperationMenuUseCases")
        DynamicMenuUseCases(dynamicMenuRepository)
    }

    val taskXUseCases: TaskXUseCases by lazy {
        Timber.d("Creating TaskXUseCases")
        TaskXUseCases(taskXRepository, taskContextManager)
    }

    val taskContextManager: TaskContextManager by lazy {
        Timber.d("Creating TaskContextManager")
        TaskContextManager()
    }

    val finalActionsValidator by lazy {
        Timber.d("Creating FinalActionsValidator")
        FinalActionsValidator()
    }

    // Создание контейнера для уровня навигации
    fun createNavigationContainer(): NavigationContainer {
        return createChildContainer { NavigationContainer(this) }
    }

    // Фабрика для создания сканеров
    val barcodeScannerFactory by lazy {
        BarcodeScannerFactory(applicationContext, settingsRepository)
    }

    // Сервис управления сканером
    val scannerService by lazy {
        ScannerService(barcodeScannerFactory).also {
            // Автоматически инициализируем сканер при создании сервиса
            it.initialize()
        }
    }

    init {
        // Инициализация каналов уведомлений
        notificationChannelManager.createNotificationChannels()
    }

    override fun dispose() {
        super.dispose()
        scannerService.dispose()
    }
}

/**
 * Контейнер для уровня навигации, содержит зависимости для всей навигации
 */
class NavigationContainer(private val appContainer: AppContainer) : DiContainer() {

    // Создание контейнеров для экранов
    fun createScreenContainer(): ScreenContainer {
        return createChildContainer { ScreenContainer(appContainer) }
    }
}

/**
 * Контейнер для уровня экрана, содержит ViewModels и другие зависимости
 * с жизненным циклом, привязанным к экрану
 */
class ScreenContainer(private val appContainer: AppContainer) : DiContainer() {

    // Фабрики для создания ViewModels

    fun createLogListViewModel(): LogListViewModel {
        return getOrCreateViewModel("LogListViewModel") {
            LogListViewModel(
                appContainer.logUseCases,
            )
        }
    }

    fun createLogDetailViewModel(logId: Int): LogDetailViewModel {
        return getOrCreateViewModel("LogDetailViewModel_$logId") {
            LogDetailViewModel(
                logId,
                appContainer.logUseCases,
                appContainer.clipboardService,
            )
        }
    }

    fun createServerListViewModel(): ServerListViewModel {
        return getOrCreateViewModel("ServerListViewModel") {
            ServerListViewModel(
                serverUseCases = appContainer.serverUseCases,
                settingsUseCases = appContainer.settingsUseCases,
            )
        }
    }

    fun createServerDetailViewModel(serverId: Int?): ServerDetailViewModel {
        return getOrCreateViewModel("ServerDetailViewModel_${serverId ?: "new"}") {
            ServerDetailViewModel(
                serverId = serverId,
                serverUseCases = appContainer.serverUseCases,
            )
        }
    }

    fun createLoginViewModel(): LoginViewModel {
        return getOrCreateViewModel("LoginViewModel") {
            LoginViewModel(
                userUseCases = appContainer.userUseCases,
                serverUseCases = appContainer.serverUseCases,
                deviceInfoService = appContainer.deviceInfoService,
                ioDispatcher = Dispatchers.IO
            )
        }
    }

    fun createMainMenuViewModel(): MainMenuViewModel {
        return getOrCreateViewModel("MainMenuViewModel") {
            MainMenuViewModel(
                userUseCases = appContainer.userUseCases,
                productUseCases = appContainer.productUseCases,
                synchronizationController = appContainer.synchronizationController,
            )
        }
    }

    fun createProductListViewModel(isSelectionMode: Boolean): ProductListViewModel {
        return getOrCreateViewModel("ProductListViewModel_${if (isSelectionMode) "selection" else "normal"}") {
            ProductListViewModel(
                productUseCases = appContainer.productUseCases,
                soundService = appContainer.soundService,
                synchronizationController = appContainer.synchronizationController,
                productUiMapper = appContainer.productUiMapper, // Добавляем маппер
                resourceProvider = appContainer.resourceProvider, // Добавляем providerResources
                isSelectionMode = isSelectionMode
            )
        }
    }

    fun createProductDetailViewModel(productId: String): ProductDetailViewModel {
        return getOrCreateViewModel("ProductDetailViewModel_$productId") {
            ProductDetailViewModel(
                productId = productId,
                productUseCases = appContainer.productUseCases,
                clipboardService = appContainer.clipboardService,
                productUiMapper = appContainer.productUiMapper, // Добавляем маппер
                resourceProvider = appContainer.resourceProvider, // Добавляем providerResources
            )
        }
    }

    fun createSettingsViewModel(): SettingsViewModel {
        return getOrCreateViewModel("SettingsViewModel") {
            SettingsViewModel(
                appContainer.settingsUseCases,
                appContainer.serverUseCases,
                appContainer.webServerManager,
                appContainer.synchronizationController,
                appContainer.updateInstaller
            )
        }
    }

    fun createSyncHistoryViewModel(): SyncHistoryViewModel {
        return getOrCreateViewModel("SyncHistoryViewModel") {
            SyncHistoryViewModel(
                synchronizationController = appContainer.synchronizationController
            )
        }
    }

    fun createTaskXListViewModel(): TaskXListViewModel {
        return getOrCreateViewModel("TaskXListViewModel") {
            TaskXListViewModel(
                taskXUseCases = appContainer.taskXUseCases,
                userUseCases = appContainer.userUseCases
            )
        }
    }

    fun createTaskXDetailViewModel(taskId: String): TaskXDetailViewModel {
        return getOrCreateViewModel("TaskXDetailViewModel_$taskId") {
            // Проверяем, есть ли данные в TaskContextManager
            val contextTask = appContainer.taskContextManager.lastStartedTaskX.value
            val contextTaskType = appContainer.taskContextManager.lastTaskTypeX.value

            // Если задача в контексте и совпадает по ID, используем её вместе с типом
            if (contextTask != null && contextTask.id == taskId && contextTaskType != null) {
                TaskXDetailViewModel(
                    taskId = taskId,
                    taskXUseCases = appContainer.taskXUseCases,
                    userUseCases = appContainer.userUseCases,
                    finalActionsValidator = appContainer.finalActionsValidator,
                    actionExecutionService = appContainer.actionExecutionService, // Добавлено
                    preloadedTask = contextTask,
                    preloadedTaskType = contextTaskType
                )
            } else {
                // Иначе создаём обычный ViewModel, который загрузит данные
                TaskXDetailViewModel(
                    taskId = taskId,
                    taskXUseCases = appContainer.taskXUseCases,
                    userUseCases = appContainer.userUseCases,
                    finalActionsValidator = appContainer.finalActionsValidator,
                    actionExecutionService = appContainer.actionExecutionService // Добавлено
                )
            }
        }
    }

    fun createActionStepFactoryRegistry(): ActionStepFactoryRegistry {
        // Создаем реестр напрямую
        val registry = ActionStepFactoryRegistry()

        // Регистрируем фабрики для различных типов объектов с доступом к репозиторию
        registry.registerFactory(
            ActionObjectType.CLASSIFIER_PRODUCT,
            ProductSelectionStepFactory(
                productLookupService = appContainer.productLookupService,
                validationService = appContainer.validationService
            )
        )

        registry.registerFactory(
            ActionObjectType.TASK_PRODUCT,
            TaskProductSelectionStepFactory(
                productLookupService = appContainer.productLookupService,
                validationService = appContainer.validationService
            )
        )

        registry.registerFactory(
            ActionObjectType.PRODUCT_QUANTITY,
            ProductQuantityStepFactory(
                validationService = appContainer.validationService
            )
        )

        registry.registerFactory(
            ActionObjectType.PALLET,
            PalletSelectionStepFactory(
                palletLookupService = appContainer.palletLookupService,
                validationService = appContainer.validationService
            )
        )

        registry.registerFactory(
            ActionObjectType.BIN,
            BinSelectionStepFactory(
                binLookupService = appContainer.binLookupService,
                validationService = appContainer.validationService
            )
        )

        return registry
    }

    fun createDynamicMenuViewModel(): DynamicMenuViewModel {
        return getOrCreateViewModel("DynamicMenuViewModel") {
            DynamicMenuViewModel(
                dynamicMenuUseCases = appContainer.dynamicMenuUseCases
            )
        }
    }

    fun createDynamicTasksViewModel(
        menuItemId: String,
        menuItemName: String,
        endpoint: String,
        screenSettings: ScreenSettings
    ): DynamicTasksViewModel {
        return getOrCreateViewModel("DynamicTasksViewModel_${menuItemId}_${endpoint}") {
            DynamicTasksViewModel(
                menuItemId = menuItemId,
                menuItemName = menuItemName,
                endpoint = endpoint,
                screenSettings = screenSettings,
                dynamicMenuUseCases = appContainer.dynamicMenuUseCases,
                userUseCases = appContainer.userUseCases,
                taskContextManager = appContainer.taskContextManager // Передаем TaskContextManager
            )
        }
    }

    fun createDynamicTaskDetailViewModel(
        taskId: String,
        endpoint: String
    ): DynamicTaskDetailViewModel {
        return getOrCreateViewModel("DynamicTaskDetailViewModel_${taskId}") {
            DynamicTaskDetailViewModel(
                taskId = taskId,
                endpoint = endpoint,
                dynamicMenuUseCases = appContainer.dynamicMenuUseCases,
                userUseCases = appContainer.userUseCases,
                taskContextManager = appContainer.taskContextManager // Передаем TaskContextManager
            )
        }
    }

    fun createDynamicProductDetailViewModel(product: DynamicProduct): DynamicProductDetailViewModel {
        return getOrCreateViewModel("DynamicProductDetailViewModel_${product.id}") {
            DynamicProductDetailViewModel(
                dynamicProduct = product,
                clipboardService = appContainer.clipboardService,
                productUiMapper = appContainer.productUiMapper,
                resourceProvider = appContainer.resourceProvider
            )
        }
    }

    fun createDynamicProductsViewModel(
        menuItemId: String,
        menuItemName: String,
        endpoint: String,
        screenSettings: ScreenSettings,
        isSelectionMode: Boolean = false
    ): DynamicProductsViewModel {
        return getOrCreateViewModel("DynamicProductsViewModel_${menuItemId}_${endpoint}") {
            DynamicProductsViewModel(
                menuItemId = menuItemId,
                menuItemName = menuItemName,
                endpoint = endpoint,
                screenSettings = screenSettings,
                dynamicMenuUseCases = appContainer.dynamicMenuUseCases,
                soundService = appContainer.soundService,
                productUiMapper = appContainer.productUiMapper,
                isSelectionMode = isSelectionMode
            )
        }
    }

    fun createActionWizardViewModel(taskId: String, actionId: String): ActionWizardViewModel {
        return getOrCreateViewModel("ActionWizardViewModel_${taskId}_${actionId}") {
            ActionWizardViewModel(
                taskId = taskId,
                actionId = actionId,
                actionWizardController = appContainer.actionWizardController,
                actionWizardContextFactory = appContainer.actionWizardContextFactory,
                actionStepFactoryRegistry = createActionStepFactoryRegistry()
            )
        }
    }
}