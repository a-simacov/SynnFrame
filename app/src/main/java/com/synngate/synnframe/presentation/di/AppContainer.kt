package com.synngate.synnframe.presentation.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.synngate.synnframe.data.barcodescanner.BarcodeScannerFactory
import com.synngate.synnframe.data.barcodescanner.ScannerService
import com.synngate.synnframe.data.datasource.BinDataSource
import com.synngate.synnframe.data.datasource.PalletDataSource
import com.synngate.synnframe.data.datasource.ProductDataSource
import com.synngate.synnframe.data.datasource.mock.MockBinDataSource
import com.synngate.synnframe.data.datasource.mock.MockPalletDataSource
import com.synngate.synnframe.data.datasource.mock.MockProductDataSource
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
import com.synngate.synnframe.data.remote.api.TaskApi
import com.synngate.synnframe.data.remote.api.TaskApiImpl
import com.synngate.synnframe.data.remote.api.TaskTypeApi
import com.synngate.synnframe.data.remote.api.TaskTypeApiImpl
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.data.remote.service.ApiServiceImpl
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.data.repository.DynamicMenuRepositoryImpl
import com.synngate.synnframe.data.repository.LogRepositoryImpl
import com.synngate.synnframe.data.repository.MockActionTemplateRepository
import com.synngate.synnframe.data.repository.MockBinXRepository
import com.synngate.synnframe.data.repository.MockPalletRepository
import com.synngate.synnframe.data.repository.MockTaskTypeXRepository
import com.synngate.synnframe.data.repository.MockTaskXRepository
import com.synngate.synnframe.data.repository.ProductRepositoryImpl
import com.synngate.synnframe.data.repository.ServerRepositoryImpl
import com.synngate.synnframe.data.repository.SettingsRepositoryImpl
import com.synngate.synnframe.data.repository.TaskRepositoryImpl
import com.synngate.synnframe.data.repository.TaskTypeRepositoryImpl
import com.synngate.synnframe.data.repository.UserRepositoryImpl
import com.synngate.synnframe.data.repository.WizardBinRepositoryImpl
import com.synngate.synnframe.data.repository.WizardPalletRepositoryImpl
import com.synngate.synnframe.data.repository.WizardProductRepositoryImpl
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
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.repository.ActionTemplateRepository
import com.synngate.synnframe.domain.repository.BinXRepository
import com.synngate.synnframe.domain.repository.DynamicMenuRepository
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.PalletRepository
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.domain.repository.TaskTypeRepository
import com.synngate.synnframe.domain.repository.TaskTypeXRepository
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.repository.WizardBinRepository
import com.synngate.synnframe.domain.repository.WizardPalletRepository
import com.synngate.synnframe.domain.repository.WizardProductRepository
import com.synngate.synnframe.domain.service.ActionDataCacheService
import com.synngate.synnframe.domain.service.ActionExecutionService
import com.synngate.synnframe.domain.service.ActionStepExecutionService
import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.DeviceInfoService
import com.synngate.synnframe.domain.service.FileService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.ServerCoordinator
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.service.UpdateInstaller
import com.synngate.synnframe.domain.service.UpdateInstallerImpl
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.service.WebServerManager
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.tasktype.TaskTypeUseCases
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.service.notification.NotificationChannelManager
import com.synngate.synnframe.presentation.ui.dynamicmenu.DynamicMenuViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.DynamicProductDetailViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.DynamicProductsViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.DynamicTaskDetailViewModel
import com.synngate.synnframe.presentation.ui.dynamicmenu.DynamicTasksViewModel
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
import com.synngate.synnframe.presentation.ui.tasks.TaskDetailViewModel
import com.synngate.synnframe.presentation.ui.tasks.TaskListViewModel
import com.synngate.synnframe.presentation.ui.taskx.TaskXDetailViewModel
import com.synngate.synnframe.presentation.ui.taskx.TaskXListViewModel
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.ui.wizard.action.BinSelectionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.PalletSelectionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.ProductSelectionStepFactory
import com.synngate.synnframe.util.network.NetworkMonitor
import com.synngate.synnframe.util.resources.ResourceProvider
import com.synngate.synnframe.util.resources.ResourceProviderImpl
import com.synngate.synnframe.util.serialization.appJson
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
    val taskDao by lazy { database.taskDao() }
    val taskTypeDao by lazy { database.taskTypeDao() }
    val factLineActionDao by lazy { database.factLineActionDao() }

    // HTTP Client
    @OptIn(ExperimentalSerializationApi::class)
    val httpClient by lazy {
        Timber.d("Creating HttpClient")
        HttpClient(Android) {
            install(ContentNegotiation) {
                // Используем общий JSON-форматтер приложения
                json(appJson)
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

    val productDataSource: ProductDataSource by lazy {
        MockProductDataSource()
    }

    val binDataSource: BinDataSource by lazy {
        MockBinDataSource()
    }

    val palletDataSource: PalletDataSource by lazy {
        MockPalletDataSource()
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

    val taskRepository: TaskRepository by lazy {
        Timber.d("Creating TaskRepository")
        TaskRepositoryImpl(taskDao, taskApi)
    }

    val settingsRepository: SettingsRepository by lazy {
        Timber.d("Creating SettingsRepository")
        SettingsRepositoryImpl(
            appSettingsDataStore,
            appUpdateApi
        )
    }

    val taskTypeRepository: TaskTypeRepository by lazy {
        TaskTypeRepositoryImpl(taskTypeDao, factLineActionDao, taskTypeApi)
    }

    val wizardProductRepository: WizardProductRepository by lazy {
        WizardProductRepositoryImpl(productDataSource)
    }

    val wizardBinRepository: WizardBinRepository by lazy {
        WizardBinRepositoryImpl(binDataSource)
    }

    val wizardPalletRepository: WizardPalletRepository by lazy {
        WizardPalletRepositoryImpl(palletDataSource)
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

    val taskApi: TaskApi by lazy {
        Timber.d("Creating TaskApi")
        TaskApiImpl(httpClient, serverProvider, apiService)
    }

    val appUpdateApi: AppUpdateApi by lazy {
        Timber.d("Creating AppUpdateApi")
        AppUpdateApiImpl(httpClient, serverProvider)
    }

    val taskTypeApi: TaskTypeApi by lazy {
        TaskTypeApiImpl(httpClient, serverProvider)
    }

    val dynamicMenuApi: DynamicMenuApi by lazy {
        Timber.d("Creating OperationMenuApi")
        DynamicMenuApiImpl(httpClient, serverProvider)
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
            taskUseCases,
            productUseCases,
            taskTypeUseCases,
            appSettingsDataStore,
            database
        )
    }

    // Сервисы для работы с визардом действий
    val validationService by lazy {
        Timber.d("Creating ValidationService")
        ValidationService()
    }

    val actionDataCacheService by lazy {
        Timber.d("Creating ActionDataCacheService")
        ActionDataCacheService(
            productRepository = wizardProductRepository,
            binRepository = wizardBinRepository,
            palletRepository = wizardPalletRepository
        )
    }

    val actionExecutionService by lazy {
        Timber.d("Creating ActionExecutionService")
        ActionExecutionService(
            taskXRepository = taskXRepository,
            validationService = validationService
        )
    }

    val actionStepExecutionService by lazy {
        Timber.d("Creating ActionStepExecutionService")
        ActionStepExecutionService(
            taskXRepository = taskXRepository,
            validationService = validationService,
            actionDataCacheService = actionDataCacheService
        )
    }

    val actionWizardController by lazy {
        Timber.d("Creating ActionWizardController")
        ActionWizardController(
            taskXRepository = taskXRepository,
            actionExecutionService = actionExecutionService,
            actionStepExecutionService = actionStepExecutionService
        )
    }

    val actionWizardContextFactory by lazy {
        Timber.d("Creating ActionWizardContextFactory")
        ActionWizardContextFactory()
    }

    // Use Cases
    val serverUseCases by lazy {
        ServerUseCases(serverRepository, serverCoordinator)
    }

    val userUseCases by lazy {
        UserUseCases(userRepository)
    }

    val taskUseCases by lazy {
        TaskUseCases(taskRepository)
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

    val taskTypeUseCases by lazy {
        TaskTypeUseCases(taskTypeRepository)
    }

    val dynamicMenuUseCases: DynamicMenuUseCases by lazy {
        Timber.d("Creating OperationMenuUseCases")
        DynamicMenuUseCases(dynamicMenuRepository)
    }

    // Репозитории для заданий X
    val actionTemplateRepository: ActionTemplateRepository by lazy {
        Timber.d("Creating ActionTemplateRepository")
        MockActionTemplateRepository()
    }

    val taskTypeXRepository: TaskTypeXRepository by lazy {
        Timber.d("Creating TaskTypeXRepository")
        MockTaskTypeXRepository(actionTemplateRepository)
    }

    val taskXRepository: TaskXRepository by lazy {
        Timber.d("Creating TaskXRepository")
        MockTaskXRepository(taskTypeXRepository)
    }

    val binXRepository: BinXRepository by lazy {
        Timber.d("Creating BinXRepository")
        MockBinXRepository()
    }

    val palletRepository: PalletRepository by lazy {
        Timber.d("Creating PalletRepository")
        MockPalletRepository()
    }

    // UseCase для заданий X
    val taskXUseCases: TaskXUseCases by lazy {
        Timber.d("Creating TaskXUseCases")
        TaskXUseCases(
            taskXRepository = taskXRepository,
            taskTypeXRepository = taskTypeXRepository,
            binXRepository = binXRepository,
            palletRepository = palletRepository
        )
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
                taskUseCases = appContainer.taskUseCases,
                productUseCases = appContainer.productUseCases,
                synchronizationController = appContainer.synchronizationController,
            )
        }
    }

    fun createTaskListViewModel(): TaskListViewModel {
        return getOrCreateViewModel("TaskListViewModel") {
            TaskListViewModel(
                taskUseCases = appContainer.taskUseCases,
                userUseCases = appContainer.userUseCases,
                taskTypeUseCases = appContainer.taskTypeUseCases,
            )
        }
    }

    fun createTaskDetailViewModel(taskId: String): TaskDetailViewModel {
        return getOrCreateViewModel("TaskDetailViewModel_$taskId") {
            TaskDetailViewModel(
                taskId = taskId,
                taskUseCases = appContainer.taskUseCases,
                productUseCases = appContainer.productUseCases,
                userUseCases = appContainer.userUseCases,
                settingsUseCases = appContainer.settingsUseCases,
                taskTypeUseCases = appContainer.taskTypeUseCases,
                soundService = appContainer.soundService,
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

    // Обновленный метод для TaskXDetailViewModel
    fun createTaskXDetailViewModel(taskId: String): TaskXDetailViewModel {
        return getOrCreateViewModel("TaskXDetailViewModel_$taskId") {
            TaskXDetailViewModel(
                taskId = taskId,
                taskXUseCases = appContainer.taskXUseCases,
                userUseCases = appContainer.userUseCases,
                actionWizardController = appContainer.actionWizardController,
                actionWizardContextFactory = appContainer.actionWizardContextFactory,
                actionStepFactoryRegistry = createActionStepFactoryRegistry()
            )
        }
    }

    fun createActionDataViewModel(): ActionDataViewModel {
        return getOrCreateViewModel("ActionDataViewModel") {
            ActionDataViewModel(
                actionDataCacheService = appContainer.actionDataCacheService
            )
        }
    }

    // Создаем реестр фабрик компонентов шагов
    fun createActionStepFactoryRegistry(): ActionStepFactoryRegistry {
        // Создаем реестр напрямую, а не через getOrCreateViewModel
        val registry = ActionStepFactoryRegistry()

        // Получаем ViewModel для фабрик - обновлено название метода
        val actionDataViewModel = createActionDataViewModel()

        // Регистрируем фабрики для различных типов объектов
        registry.registerFactory(
            ActionObjectType.CLASSIFIER_PRODUCT,
            ProductSelectionStepFactory(actionDataViewModel)
        )

        registry.registerFactory(
            ActionObjectType.TASK_PRODUCT,
            ProductSelectionStepFactory(actionDataViewModel)
        )

        registry.registerFactory(
            ActionObjectType.PALLET,
            PalletSelectionStepFactory(actionDataViewModel)
        )

        registry.registerFactory(
            ActionObjectType.BIN,
            BinSelectionStepFactory(actionDataViewModel)
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
                dynamicMenuUseCases = appContainer.dynamicMenuUseCases
            )
        }
    }

    fun createDynamicTaskDetailViewModel(task: DynamicTask): DynamicTaskDetailViewModel {
        return getOrCreateViewModel("DynamicTaskDetailViewModel_${task.getId()}") {
            DynamicTaskDetailViewModel(task)
        }
    }

    fun createDynamicProductDetailViewModel(product: DynamicProduct): DynamicProductDetailViewModel {
        return getOrCreateViewModel("DynamicProductDetailViewModel_${product.getId()}") {
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
}