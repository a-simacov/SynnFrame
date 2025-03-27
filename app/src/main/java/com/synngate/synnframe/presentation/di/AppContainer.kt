package com.synngate.synnframe.presentation.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
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
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.data.remote.service.ApiServiceImpl
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.data.repository.LogRepositoryImpl
import com.synngate.synnframe.data.repository.ProductRepositoryImpl
import com.synngate.synnframe.data.repository.ServerRepositoryImpl
import com.synngate.synnframe.data.repository.SettingsRepositoryImpl
import com.synngate.synnframe.data.repository.TaskRepositoryImpl
import com.synngate.synnframe.data.repository.UserRepositoryImpl
import com.synngate.synnframe.data.service.ClipboardServiceImpl
import com.synngate.synnframe.data.service.DeviceInfoServiceImpl
import com.synngate.synnframe.data.service.FileServiceImpl
import com.synngate.synnframe.data.service.LoggingServiceImpl
import com.synngate.synnframe.data.service.ServerCoordinatorImpl
import com.synngate.synnframe.data.service.SoundServiceImpl
import com.synngate.synnframe.data.service.SynchronizationControllerImpl
import com.synngate.synnframe.data.service.WebServerControllerImpl
import com.synngate.synnframe.data.service.WebServerManagerImpl
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.DeviceInfoService
import com.synngate.synnframe.domain.service.FileService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.ServerCoordinator
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.service.UpdateInstaller
import com.synngate.synnframe.domain.service.UpdateInstallerImpl
import com.synngate.synnframe.domain.service.WebServerManager
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.service.notification.NotificationChannelManager
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
import com.synngate.synnframe.presentation.ui.tasks.TaskDetailViewModel
import com.synngate.synnframe.presentation.ui.tasks.TaskListViewModel
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
    private val database by lazy {
        Timber.d("Creating AppDatabase")
        AppDatabase.getInstance(applicationContext)
    }

    // DAO
    val serverDao by lazy { database.serverDao() }
    val userDao by lazy { database.userDao() }
    val logDao by lazy { database.logDao() }
    val productDao by lazy { database.productDao() }
    val taskDao by lazy { database.taskDao() }

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
                level = LogLevel.BODY
            }
        }
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
            appSettingsDataStore,
            loggingService
        )
    }

    val deviceInfoService: DeviceInfoService by lazy {
        Timber.d("Creating DeviceInfoService")
        DeviceInfoServiceImpl(applicationContext)
    }

    val webServerController by lazy {
        WebServerControllerImpl(applicationContext, loggingService)
    }

    val webServerManager: WebServerManager by lazy {
        Timber.d("Creating WebServerManager")
        WebServerManagerImpl(webServerController, loggingService)
    }

    val updateInstaller: UpdateInstaller by lazy {
        Timber.d("Creating UpdateInstaller")
        UpdateInstallerImpl(applicationContext, loggingService)
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
            appSettingsDataStore,
            loggingService,
            database
        )
    }

    // Use Cases
    val serverUseCases by lazy {
        ServerUseCases(serverRepository, serverCoordinator, loggingService)
    }

    val userUseCases by lazy {
        UserUseCases(userRepository, loggingService)
    }

    val taskUseCases by lazy {
        TaskUseCases(taskRepository, loggingService)
    }

    val productUseCases by lazy {
        ProductUseCases(productRepository, loggingService)
    }

    val logUseCases by lazy {
        LogUseCases(logRepository, loggingService)
    }

    val settingsUseCases by lazy {
        SettingsUseCases(settingsRepository, loggingService, fileService, applicationContext)
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
                appContainer.loggingService,
                Dispatchers.IO
            )
        }
    }

    fun createLogDetailViewModel(logId: Int): LogDetailViewModel {
        return getOrCreateViewModel("LogDetailViewModel_$logId") {
            LogDetailViewModel(
                logId,
                appContainer.logUseCases,
                appContainer.loggingService,
                appContainer.clipboardService,
                Dispatchers.IO
            )
        }
    }

    fun createServerListViewModel(): ServerListViewModel {
        return getOrCreateViewModel("ServerListViewModel") {
            ServerListViewModel(
                serverUseCases = appContainer.serverUseCases,
                settingsUseCases = appContainer.settingsUseCases,
                ioDispatcher = Dispatchers.IO
            )
        }
    }

    fun createServerDetailViewModel(serverId: Int?): ServerDetailViewModel {
        return getOrCreateViewModel("ServerDetailViewModel_${serverId ?: "new"}") {
            ServerDetailViewModel(
                serverId = serverId,
                serverUseCases = appContainer.serverUseCases,
                ioDispatcher = Dispatchers.IO
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
                ioDispatcher = Dispatchers.IO
            )
        }
    }

    fun createTaskListViewModel(): TaskListViewModel {
        return getOrCreateViewModel("TaskListViewModel") {
            TaskListViewModel(
                taskUseCases = appContainer.taskUseCases,
                userUseCases = appContainer.userUseCases,
                ioDispatcher = Dispatchers.IO
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
                soundService = appContainer.soundService,
                ioDispatcher = Dispatchers.IO
            )
        }
    }

    fun createProductListViewModel(isSelectionMode: Boolean): ProductListViewModel {
        return getOrCreateViewModel("ProductListViewModel_${if (isSelectionMode) "selection" else "normal"}") {
            ProductListViewModel(
                productUseCases = appContainer.productUseCases,
                loggingService = appContainer.loggingService,
                ioDispatcher = Dispatchers.IO,
                isSelectionMode = isSelectionMode
            )
        }
    }

    fun createProductDetailViewModel(productId: String): ProductDetailViewModel {
        return getOrCreateViewModel("ProductDetailViewModel_$productId") {
            ProductDetailViewModel(
                productId = productId,
                productUseCases = appContainer.productUseCases,
                loggingService = appContainer.loggingService,
                clipboardService = appContainer.clipboardService,
                ioDispatcher = Dispatchers.IO
            )
        }
    }

    fun createSettingsViewModel(): SettingsViewModel {
        return getOrCreateViewModel("SettingsViewModel") {
            SettingsViewModel(
                appContainer.settingsUseCases,
                appContainer.serverUseCases,
                appContainer.loggingService,
                appContainer.webServerManager,
                appContainer.synchronizationController,
                appContainer.updateInstaller,
                Dispatchers.IO
            )
        }
    }

    fun createSyncHistoryViewModel(): SyncHistoryViewModel {
        return getOrCreateViewModel("SyncHistoryViewModel") {
            SyncHistoryViewModel(
                synchronizationController = appContainer.synchronizationController,
                ioDispatcher = Dispatchers.IO
            )
        }
    }
}