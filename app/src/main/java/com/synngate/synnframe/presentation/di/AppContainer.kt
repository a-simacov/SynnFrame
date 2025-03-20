package com.synngate.synnframe.presentation.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.local.dao.LogDao
import com.synngate.synnframe.data.local.dao.ProductDao
import com.synngate.synnframe.data.local.dao.ServerDao
import com.synngate.synnframe.data.local.dao.TaskDao
import com.synngate.synnframe.data.local.dao.UserDao
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
import com.synngate.synnframe.data.service.LoggingServiceImpl
import com.synngate.synnframe.data.service.ServerCoordinatorImpl
import com.synngate.synnframe.data.service.SoundServiceImpl
import com.synngate.synnframe.data.service.WebServerManagerStub
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.DeviceInfoService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.ServerCoordinator
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.domain.service.UpdateInstaller
import com.synngate.synnframe.domain.service.UpdateInstallerImpl
import com.synngate.synnframe.domain.service.WebServerManager
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.login.LoginViewModel
import com.synngate.synnframe.presentation.ui.logs.LogDetailViewModel
import com.synngate.synnframe.presentation.ui.logs.LogListViewModel
import com.synngate.synnframe.presentation.ui.main.MainMenuViewModel
import com.synngate.synnframe.presentation.ui.products.ProductDetailViewModel
import com.synngate.synnframe.presentation.ui.products.ProductListViewModel
import com.synngate.synnframe.presentation.ui.server.ServerDetailViewModel
import com.synngate.synnframe.presentation.ui.server.ServerListViewModel
import com.synngate.synnframe.presentation.ui.settings.SettingsViewModel
import com.synngate.synnframe.presentation.ui.tasks.TaskDetailViewModel
import com.synngate.synnframe.presentation.ui.tasks.TaskListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Реализация контейнера зависимостей для всего приложения.
 * Содержит зависимости, которые живут на протяжении всего жизненного цикла приложения.
 */
class AppContainer(private val applicationContext: Context) {

    // Единый экземпляр DataStore для настроек приложения
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    // DataStore для хранения настроек приложения
    val appSettingsDataStore by lazy {
        Timber.d("Creating AppSettingsDataStore")
        AppSettingsDataStore(applicationContext.dataStore)
    }

    private val database by lazy {
        Timber.d("Creating AppDatabase")
        AppDatabase.getInstance(applicationContext)
    }

    private val serverDao: ServerDao by lazy { database.serverDao() }
    private val userDao: UserDao by lazy { database.userDao() }
    private val logDao: LogDao by lazy { database.logDao() }
    private val productDao: ProductDao by lazy { database.productDao() }
    private val taskDao: TaskDao by lazy { database.taskDao() }

    // Репозиторий логов (создаем раньше других, так как он нужен для логирования)
    private val logRepository: LogRepository by lazy {
        Timber.d("Creating LogRepository")
        LogRepositoryImpl(logDao)
    }

    private val httpClient by lazy {
        Timber.d("Creating HttpClient")
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.tag("HttpClient").d(message)
                    }
                }
                level = LogLevel.BODY
            }
            defaultRequest {
                // Общие настройки для всех запросов
            }
        }
    }

    private val serverProvider by lazy {
        Timber.d("Creating ServerProvider")
        object : ServerProvider {
            override suspend fun getActiveServer() = serverRepository.getActiveServer().first()
            override suspend fun getCurrentUserId() = userRepository.getCurrentUser().first()?.id
        }
    }

    private val apiService: ApiService by lazy {
        Timber.d("Creating ApiService")
        ApiServiceImpl(httpClient, serverProvider)
    }

    private val authApi: AuthApi by lazy {
        Timber.d("Creating AuthApi")
        AuthApiImpl(apiService)
    }

    private val productApi: ProductApi by lazy {
        Timber.d("Creating ProductApi")
        ProductApiImpl(httpClient, serverProvider)
    }

    private val taskApi: TaskApi by lazy {
        Timber.d("Creating TaskApi")
        TaskApiImpl(httpClient, serverProvider, apiService)
    }

    private val appUpdateApi: AppUpdateApi by lazy {
        Timber.d("Creating AppUpdateApi")
        AppUpdateApiImpl(httpClient, serverProvider)
    }

    private val serverRepository: ServerRepository by lazy {
        Timber.d("Creating ServerRepository")
        ServerRepositoryImpl(serverDao, apiService)
    }

    private val userRepository: UserRepository by lazy {
        Timber.d("Creating UserRepository")
        UserRepositoryImpl(userDao, authApi, appSettingsDataStore)
    }

    private val productRepository: ProductRepository by lazy {
        Timber.d("Creating ProductRepository")
        ProductRepositoryImpl(productDao, productApi)
    }

    private val taskRepository: TaskRepository by lazy {
        Timber.d("Creating TaskRepository")
        TaskRepositoryImpl(taskDao, taskApi)
    }

    private val settingsRepository: SettingsRepository by lazy {
        Timber.d("Creating SettingsRepository")
        SettingsRepositoryImpl(
            appSettingsDataStore,
            appUpdateApi
        )
    }

    private val loggingService: LoggingService by lazy {
        Timber.d("Creating LoggingService")
        LoggingServiceImpl(logRepository)
    }

    private val clipboardService: ClipboardService by lazy {
        Timber.d("Creating ClipboardService")
        ClipboardServiceImpl(applicationContext)
    }

    private val serverCoordinator: ServerCoordinator by lazy {
        Timber.d("Creating ServerCoordinator")
        ServerCoordinatorImpl(
            serverRepository,
            appSettingsDataStore,
            loggingService
        )
    }

    private val deviceInfoService: DeviceInfoService by lazy {
        Timber.d("Creating DeviceInfoService")
        DeviceInfoServiceImpl(applicationContext)
    }

    private val webServerManager: WebServerManager by lazy {
        Timber.d("Creating WebServerManager")
        WebServerManagerStub(loggingService)
    }

    private val updateInstaller: UpdateInstaller by lazy {
        Timber.d("Creating UpdateInstaller")
        UpdateInstallerImpl(applicationContext, loggingService)
    }

    private val soundService: SoundService by lazy {
        SoundServiceImpl(applicationContext)
    }

    private val serverUseCases by lazy {
        ServerUseCases(serverRepository, serverCoordinator, loggingService)
    }

    private val userUseCases by lazy {
        UserUseCases(userRepository, loggingService)
    }

    private val taskUseCases by lazy {
        TaskUseCases(taskRepository, loggingService)
    }

    private val productUseCases by lazy {
        ProductUseCases(productRepository, loggingService)
    }

    private val logUseCases by lazy {
        LogUseCases(logRepository, loggingService)
    }

    private val settingsUseCases by lazy {
        SettingsUseCases(settingsRepository, loggingService, applicationContext)
    }

    fun createNavHostContainer(): NavHostContainer {
        Timber.d("Creating NavHostContainer")
        return NavHostContainerImpl(this)
    }

    inner class NavHostContainerImpl(
        private val appContainer: AppContainer
    ) : NavHostContainer {

        override val clearables: MutableList<Clearable> = mutableListOf()

        override fun createServerListGraphContainer(): ServerListGraphContainer {
            val container = ServerListGraphContainerImpl(appContainer)
            addClearable(container)
            return container
        }

        override fun createTasksGraphContainer(): TasksGraphContainer {
            val container = TasksGraphContainerImpl(appContainer)
            addClearable(container)
            return container
        }

        override fun createProductsGraphContainer(): ProductsGraphContainer {
            val container = ProductsGraphContainerImpl(appContainer)
            addClearable(container)
            return container
        }

        override fun createLogsGraphContainer(): LogsGraphContainer {
            val container = LogsGraphContainerImpl(appContainer)
            addClearable(container)
            return container
        }

        override fun createSettingsScreenContainer(): SettingsScreenContainer {
            val container = SettingsScreenContainerImpl(appContainer)
            addClearable(container)
            return container
        }

        override fun createLoginScreenContainer(): LoginScreenContainer {
            val container = LoginScreenContainerImpl(appContainer)
            addClearable(container)
            return container
        }

        override fun createMainMenuScreenContainer(): MainMenuScreenContainer {
            val container = MainMenuScreenContainerImpl(appContainer)
            addClearable(container)
            return container
        }
    }

    /**
     * Базовый класс для контейнеров подграфов
     */
    abstract inner class BaseGraphContainer : GraphContainer {
        override val clearables: MutableList<Clearable> = mutableListOf()
    }

    inner class ServerListGraphContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), ServerListGraphContainer {

        override fun createServerListViewModel(): ServerListViewModel {
            Timber.d("Creating ServerListViewModel")
            val viewModel = ServerListViewModel(
                serverUseCases = appContainer.serverUseCases,
                settingsUseCases = appContainer.settingsUseCases,
                ioDispatcher = Dispatchers.IO
            )
            addClearable(viewModel)
            return viewModel
        }

        override fun createServerDetailViewModel(serverId: Int?): ServerDetailViewModel {
            Timber.d("Creating ServerDetailViewModel for serverId=$serverId")
            val viewModel = ServerDetailViewModel(
                serverId = serverId,
                serverUseCases = appContainer.serverUseCases,
                ioDispatcher = Dispatchers.IO
            )
            addClearable(viewModel)
            return viewModel
        }
    }

    inner class TasksGraphContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), TasksGraphContainer {

        override fun createTaskListViewModel(): TaskListViewModel {
            Timber.d("Creating TaskListViewModel")
            val viewModel = TaskListViewModel(
                taskUseCases = appContainer.taskUseCases,
                userUseCases = appContainer.userUseCases,
                ioDispatcher = Dispatchers.IO
            )
            addClearable(viewModel)
            return viewModel
        }

        override fun createTaskDetailViewModel(taskId: String): TaskDetailViewModel {
            Timber.d("Creating TaskDetailViewModel for taskId=$taskId")
            val viewModel = TaskDetailViewModel(
                taskId = taskId,
                taskUseCases = appContainer.taskUseCases,
                productUseCases = appContainer.productUseCases,
                userUseCases = appContainer.userUseCases,
                soundService = soundService,
                ioDispatcher = Dispatchers.IO
            )
            addClearable(viewModel)
            return viewModel
        }
    }

    inner class ProductsGraphContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), ProductsGraphContainer {

        override fun createProductListViewModel(isSelectionMode: Boolean): ProductListViewModel {
            Timber.d("Creating ProductListViewModel")
            val viewModel = ProductListViewModel(
                productUseCases = appContainer.productUseCases,
                loggingService = appContainer.loggingService,
                ioDispatcher = Dispatchers.IO,
                isSelectionMode = isSelectionMode
            )
            addClearable(viewModel)
            return viewModel
        }

        override fun createProductDetailViewModel(productId: String): ProductDetailViewModel {
            Timber.d("Creating ProductDetailViewModel for productId=$productId")
            val viewModel = ProductDetailViewModel(
                productId = productId,
                productUseCases = appContainer.productUseCases,
                loggingService = appContainer.loggingService,
                clipboardService = appContainer.clipboardService,  // Добавляем ClipboardService
                ioDispatcher = Dispatchers.IO
            )
            addClearable(viewModel)
            return viewModel
        }
    }

    inner class LogsGraphContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), LogsGraphContainer {

        override fun createLogListViewModel(): LogListViewModel {
            Timber.d("Creating LogListViewModel")
            val viewModel = LogListViewModel(
                logUseCases = appContainer.logUseCases,
                loggingService = appContainer.loggingService,
                ioDispatcher = Dispatchers.IO
            )
            addClearable(viewModel)
            return viewModel
        }

        override fun createLogDetailViewModel(logId: Int): LogDetailViewModel {
            Timber.d("Creating LogDetailViewModel for logId=$logId")
            val viewModel = LogDetailViewModel(
                logId,
                appContainer.logUseCases,
                appContainer.loggingService,
                appContainer.clipboardService,
                Dispatchers.IO
            )
            addClearable(viewModel)
            return viewModel
        }
    }

    inner class SettingsScreenContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), SettingsScreenContainer {

        override fun createSettingsViewModel(): SettingsViewModel {
            Timber.d("Creating SettingsViewModel")
            val viewModel = SettingsViewModel(
                appContainer.settingsUseCases,
                appContainer.serverUseCases,
                appContainer.loggingService,
                appContainer.webServerManager,
                appContainer.updateInstaller,
                Dispatchers.IO
            )
            addClearable(viewModel)
            return viewModel
        }
    }

    inner class LoginScreenContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), LoginScreenContainer {

        override fun createLoginViewModel(): LoginViewModel {
            Timber.d("Creating LoginViewModel")
            val viewModel = LoginViewModel(
                userUseCases = appContainer.userUseCases,
                serverUseCases = appContainer.serverUseCases,
                deviceInfoService = appContainer.deviceInfoService,
                ioDispatcher = Dispatchers.IO
            )
            addClearable(viewModel)
            return viewModel
        }
    }

    inner class MainMenuScreenContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), MainMenuScreenContainer {

        override fun createMainMenuViewModel(): MainMenuViewModel {
            Timber.d("Creating MainMenuViewModel")
            val viewModel = MainMenuViewModel(
                userUseCases = appContainer.userUseCases,
                taskUseCases = appContainer.taskUseCases,
                productUseCases = appContainer.productUseCases,
                ioDispatcher = Dispatchers.IO
            )
            addClearable(viewModel)
            return viewModel
        }
    }
}