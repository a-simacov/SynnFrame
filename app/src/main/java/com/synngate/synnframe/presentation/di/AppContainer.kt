package com.synngate.synnframe.presentation.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
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
import com.synngate.synnframe.data.remote.api.ProductApi
import com.synngate.synnframe.data.remote.api.ProductApiImpl
import com.synngate.synnframe.data.remote.api.TaskApi
import com.synngate.synnframe.data.remote.api.TaskApiImpl
import com.synngate.synnframe.data.remote.api.TaskTypeApi
import com.synngate.synnframe.data.remote.api.TaskTypeApiImpl
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.data.remote.service.ApiServiceImpl
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.data.repository.LogRepositoryImpl
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
import com.synngate.synnframe.domain.repository.BinXRepository
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
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.DeviceInfoService
import com.synngate.synnframe.domain.service.FactLineDataCacheService
import com.synngate.synnframe.domain.service.FileService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.ServerCoordinator
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.service.UpdateInstaller
import com.synngate.synnframe.domain.service.UpdateInstallerImpl
import com.synngate.synnframe.domain.service.WebServerManager
import com.synngate.synnframe.domain.service.WizardController
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.tasktype.TaskTypeUseCases
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.domain.usecase.wizard.FactLineWizardUseCases
import com.synngate.synnframe.presentation.service.notification.NotificationChannelManager
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
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
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
    val taskDao by lazy { database.taskDao() }
    val taskTypeDao by lazy { database.taskTypeDao() }
    val factLineActionDao by lazy { database.factLineActionDao() }

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

    // Репозитории для заданий X
    val taskXRepository: TaskXRepository by lazy {
        Timber.d("Creating TaskXRepository")
        MockTaskXRepository(taskTypeXRepository)
    }

    val taskTypeXRepository: TaskTypeXRepository by lazy {
        Timber.d("Creating TaskTypeXRepository")
        MockTaskTypeXRepository()
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

    val factLineDataCacheService by lazy {
        FactLineDataCacheService(
            wizardProductRepository,
            wizardBinRepository,
            wizardPalletRepository
        )
    }

    val factLineWizardUseCases by lazy {
        FactLineWizardUseCases(
            factLineDataCacheService,
            taskXUseCases,
            taskTypeXRepository
        )
    }

    val wizardController: WizardController by lazy {
        WizardController(factLineWizardUseCases)
    }


    fun createFactLineWizardViewModel(): FactLineWizardViewModel {
        return FactLineWizardViewModel(factLineWizardUseCases)
    }

    // Создание контейнера для уровня навигации
    fun createNavigationContainer(): NavigationContainer {
        return createChildContainer { NavigationContainer(this) }
    }

    init {
        // Инициализация каналов уведомлений
        notificationChannelManager.createNotificationChannels()
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

    fun createFactLineWizardViewModel(): FactLineWizardViewModel {
        return getOrCreateViewModel("FactLineWizardViewModel") {
            FactLineWizardViewModel(
                factLineWizardUseCases = appContainer.factLineWizardUseCases
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
                factLineWizardViewModel = createFactLineWizardViewModel(),
                wizardController = appContainer.wizardController
            )
        }
    }
}