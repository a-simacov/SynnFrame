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
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

    // База данных Room
    private val database by lazy {
        Timber.d("Creating AppDatabase")
        AppDatabase.getInstance(applicationContext)
    }

    // DAO объекты
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

    // HTTP клиент
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

    // Провайдер данных о сервере для API
    private val serverProvider by lazy {
        Timber.d("Creating ServerProvider")
        object : ServerProvider {
            override suspend fun getActiveServer() = serverRepository.getActiveServer().first()
            override suspend fun getCurrentUserId() = userRepository.getCurrentUser().first()?.id
        }
    }

    // API сервисы
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

    // Репозитории
    private val serverRepository: ServerRepository by lazy {
        Timber.d("Creating ServerRepository")
        ServerRepositoryImpl(serverDao, apiService, appSettingsDataStore, logRepository)
    }

    private val userRepository: UserRepository by lazy {
        Timber.d("Creating UserRepository")
        UserRepositoryImpl(userDao, authApi, appSettingsDataStore, logRepository)
    }

    private val productRepository: ProductRepository by lazy {
        Timber.d("Creating ProductRepository")
        ProductRepositoryImpl(productDao, productApi, logRepository)
    }

    private val taskRepository: TaskRepository by lazy {
        Timber.d("Creating TaskRepository")
        TaskRepositoryImpl(taskDao, taskApi, logRepository)
    }

    private val settingsRepository: SettingsRepository by lazy {
        Timber.d("Creating SettingsRepository")
        SettingsRepositoryImpl(appSettingsDataStore, appUpdateApi, logRepository, applicationContext)
    }

    // Use Cases
    fun createTaskUseCases(): TaskUseCases {
        return TaskUseCases(
            taskRepository = createTaskRepository(),
            logRepository = createLogRepository()
        )
    }

    fun createProductUseCases(): ProductUseCases {
        return ProductUseCases(
            productRepository = createProductRepository(),
            logRepository = createLogRepository()
        )
    }

    fun createServerUseCases(): ServerUseCases {
        return ServerUseCases(
            serverRepository = createServerRepository(),
            logRepository = createLogRepository()
        )
    }

    fun createUserUseCases(): UserUseCases {
        return UserUseCases(
            userRepository = createUserRepository(),
            logRepository = createLogRepository()
        )
    }

    fun createLogUseCases(): LogUseCases {
        return LogUseCases(
            logRepository = createLogRepository()
        )
    }

    fun createSettingsUseCases(): SettingsUseCases {
        return SettingsUseCases(
            settingsRepository = createSettingsRepository(),
            logRepository = createLogRepository()
        )
    }

    // Вспомогательные методы для создания репозиториев
    private fun createTaskRepository(): TaskRepository {
        return TaskRepositoryImpl(taskDao, taskApi, logRepository)
    }

    private fun createProductRepository(): ProductRepository {
        return ProductRepositoryImpl(productDao, productApi, logRepository)
    }

    private fun createServerRepository(): ServerRepository {
        return ServerRepositoryImpl(serverDao, apiService, appSettingsDataStore, logRepository)
    }

    private fun createUserRepository(): UserRepository {
        return UserRepositoryImpl(userDao, authApi, appSettingsDataStore, logRepository)
    }

    private fun createLogRepository(): LogRepository {
        return logRepository
    }

    private fun createSettingsRepository(): SettingsRepository {
        return SettingsRepositoryImpl(appSettingsDataStore, appUpdateApi, logRepository, applicationContext)
    }


    /**
     * Создание контейнера для навигационного хоста
     */
    fun createNavHostContainer(): NavHostContainer {
        Timber.d("Creating NavHostContainer")
        return NavHostContainerImpl(this)
    }

    /**
     * Внутренний класс-контейнер для навигационного хоста
     */
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
    }

    /**
     * Базовый класс для контейнеров подграфов
     */
    abstract inner class BaseGraphContainer : GraphContainer {
        override val clearables: MutableList<Clearable> = mutableListOf()
    }

    /**
     * Контейнер для подграфа серверов
     */
    inner class ServerListGraphContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), ServerListGraphContainer {

        override fun createServerListViewModel(): ServerListViewModel {
            Timber.d("Creating ServerListViewModel")
            return ServerListViewModelImpl(
                appContainer.serverRepository,
                appContainer.appSettingsDataStore,
                Dispatchers.IO
            )
        }

        override fun createServerDetailViewModel(serverId: Int?): ServerDetailViewModel {
            Timber.d("Creating ServerDetailViewModel for serverId=$serverId")
            return ServerDetailViewModelImpl(
                serverId,
                appContainer.serverRepository,
                appContainer.appSettingsDataStore,
                Dispatchers.IO
            )
        }

        /**
         * Реализация ViewModel для списка серверов
         */
        inner class ServerListViewModelImpl(
            private val serverRepository: ServerRepository,
            private val appSettingsDataStore: AppSettingsDataStore,
            private val ioDispatcher: CoroutineDispatcher
        ) : ServerListViewModel, Clearable {

            init {
                addClearable(this)
            }

            override fun clear() {
                Timber.d("Clearing ServerListViewModel")
                // Освобождаем ресурсы, если необходимо
            }
        }

        /**
         * Реализация ViewModel для деталей сервера
         */
        inner class ServerDetailViewModelImpl(
            private val serverId: Int?,
            private val serverRepository: ServerRepository,
            private val appSettingsDataStore: AppSettingsDataStore,
            private val ioDispatcher: CoroutineDispatcher
        ) : ServerDetailViewModel, Clearable {

            init {
                addClearable(this)
            }

            override fun clear() {
                Timber.d("Clearing ServerDetailViewModel for serverId=$serverId")
                // Освобождаем ресурсы, если необходимо
            }
        }
    }

    /**
     * Контейнер для подграфа заданий
     */
    inner class TasksGraphContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), TasksGraphContainer {

        override fun createTaskListViewModel(): TaskListViewModel {
            Timber.d("Creating TaskListViewModel")
            return TaskListViewModelImpl(
                appContainer.taskRepository,
                appContainer.userRepository,
                Dispatchers.IO
            )
        }

        override fun createTaskDetailViewModel(taskId: String): TaskDetailViewModel {
            Timber.d("Creating TaskDetailViewModel for taskId=$taskId")
            return TaskDetailViewModelImpl(
                taskId,
                appContainer.taskRepository,
                appContainer.productRepository,
                appContainer.userRepository,
                Dispatchers.IO
            )
        }

        /**
         * Реализация ViewModel для списка заданий
         */
        inner class TaskListViewModelImpl(
            private val taskRepository: TaskRepository,
            private val userRepository: UserRepository,
            private val ioDispatcher: CoroutineDispatcher
        ) : TaskListViewModel, Clearable {

            init {
                addClearable(this)
            }

            override fun clear() {
                Timber.d("Clearing TaskListViewModel")
                // Освобождаем ресурсы, если необходимо
            }
        }

        /**
         * Реализация ViewModel для деталей задания
         */
        inner class TaskDetailViewModelImpl(
            private val taskId: String,
            private val taskRepository: TaskRepository,
            private val productRepository: ProductRepository,
            private val userRepository: UserRepository,
            private val ioDispatcher: CoroutineDispatcher
        ) : TaskDetailViewModel, Clearable {

            init {
                addClearable(this)
            }

            override fun clear() {
                Timber.d("Clearing TaskDetailViewModel for taskId=$taskId")
                // Освобождаем ресурсы, если необходимо
            }
        }
    }

    /**
     * Контейнер для подграфа товаров
     */
    inner class ProductsGraphContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), ProductsGraphContainer {

        override fun createProductListViewModel(): ProductListViewModel {
            Timber.d("Creating ProductListViewModel")
            return ProductListViewModelImpl(
                appContainer.productRepository,
                Dispatchers.IO
            )
        }

        override fun createProductDetailViewModel(productId: String): ProductDetailViewModel {
            Timber.d("Creating ProductDetailViewModel for productId=$productId")
            return ProductDetailViewModelImpl(
                productId,
                appContainer.productRepository,
                Dispatchers.IO
            )
        }

        /**
         * Реализация ViewModel для списка товаров
         */
        inner class ProductListViewModelImpl(
            private val productRepository: ProductRepository,
            private val ioDispatcher: CoroutineDispatcher
        ) : ProductListViewModel, Clearable {

            init {
                addClearable(this)
            }

            override fun clear() {
                Timber.d("Clearing ProductListViewModel")
                // Освобождаем ресурсы, если необходимо
            }
        }

        /**
         * Реализация ViewModel для деталей товара
         */
        inner class ProductDetailViewModelImpl(
            private val productId: String,
            private val productRepository: ProductRepository,
            private val ioDispatcher: CoroutineDispatcher
        ) : ProductDetailViewModel, Clearable {

            init {
                addClearable(this)
            }

            override fun clear() {
                Timber.d("Clearing ProductDetailViewModel for productId=$productId")
                // Освобождаем ресурсы, если необходимо
            }
        }
    }

    /**
     * Контейнер для подграфа логов
     */
    inner class LogsGraphContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), LogsGraphContainer {

        override fun createLogListViewModel(): LogListViewModel {
            Timber.d("Creating LogListViewModel")
            return LogListViewModelImpl(
                appContainer.logRepository,
                Dispatchers.IO
            )
        }

        override fun createLogDetailViewModel(logId: Int): LogDetailViewModel {
            Timber.d("Creating LogDetailViewModel for logId=$logId")
            return LogDetailViewModelImpl(
                logId,
                appContainer.logRepository,
                Dispatchers.IO
            )
        }

        /**
         * Реализация ViewModel для списка логов
         */
        inner class LogListViewModelImpl(
            private val logRepository: LogRepository,
            private val ioDispatcher: CoroutineDispatcher
        ) : LogListViewModel, Clearable {

            init {
                addClearable(this)
            }

            override fun clear() {
                Timber.d("Clearing LogListViewModel")
                // Освобождаем ресурсы, если необходимо
            }
        }

        /**
         * Реализация ViewModel для деталей лога
         */
        inner class LogDetailViewModelImpl(
            private val logId: Int,
            private val logRepository: LogRepository,
            private val ioDispatcher: CoroutineDispatcher
        ) : LogDetailViewModel, Clearable {

            init {
                addClearable(this)
            }

            override fun clear() {
                Timber.d("Clearing LogDetailViewModel for logId=$logId")
                // Освобождаем ресурсы, если необходимо
            }
        }
    }

    /**
     * Контейнер для экрана настроек
     */
    inner class SettingsScreenContainerImpl(
        private val appContainer: AppContainer
    ) : BaseGraphContainer(), SettingsScreenContainer {

        override fun createSettingsViewModel(): SettingsViewModel {
            Timber.d("Creating SettingsViewModel")
            return SettingsViewModelImpl(
                appContainer.settingsRepository,
                appContainer.serverRepository,
                Dispatchers.IO
            )
        }

        /**
         * Реализация ViewModel для экрана настроек
         */
        inner class SettingsViewModelImpl(
            private val settingsRepository: SettingsRepository,
            private val serverRepository: ServerRepository,
            private val ioDispatcher: CoroutineDispatcher
        ) : SettingsViewModel, Clearable {

            init {
                addClearable(this)
            }

            override fun clear() {
                Timber.d("Clearing SettingsViewModel")
                // Освобождаем ресурсы, если необходимо
            }
        }
    }
}