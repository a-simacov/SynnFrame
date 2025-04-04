package com.synngate.synnframe.presentation.service.webserver

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.synngate.synnframe.R
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.data.remote.dto.ProductDto
import com.synngate.synnframe.data.remote.dto.TaskDto
import com.synngate.synnframe.data.remote.dto.TaskTypeDto
import com.synngate.synnframe.data.sync.SyncHistoryRecord
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.WebServerController
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.tasktype.TaskTypeUseCases
import com.synngate.synnframe.presentation.service.base.BaseForegroundService
import com.synngate.synnframe.presentation.service.base.launchSafely
import com.synngate.synnframe.presentation.service.notification.NotificationChannelManager
import com.synngate.synnframe.presentation.service.webserver.controller.WebServerControllerFactory
import com.synngate.synnframe.presentation.service.webserver.util.respondError
import com.synngate.synnframe.presentation.ui.MainActivity
import com.synngate.synnframe.util.network.NetworkMonitor
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Foreground-сервис для локального веб-сервера на базе Ktor
 */
class WebServerService : BaseForegroundService() {

    // Инъекция зависимостей
    override val loggingService: LoggingService by lazy {
        (application as SynnFrameApplication).appContainer.loggingService
    }

    private val webServerController: WebServerController by lazy {
        (application as SynnFrameApplication).appContainer.webServerController
    }

    // Добавляем доступ к репозиториям и use cases
    private val taskRepository: TaskRepository by lazy {
        (application as SynnFrameApplication).appContainer.taskRepository
    }

    private val productRepository: ProductRepository by lazy {
        (application as SynnFrameApplication).appContainer.productRepository
    }

    private val userRepository: UserRepository by lazy {
        (application as SynnFrameApplication).appContainer.userRepository
    }

    private val serverRepository: ServerRepository by lazy {
        (application as SynnFrameApplication).appContainer.serverRepository
    }

    private val taskUseCases: TaskUseCases by lazy {
        (application as SynnFrameApplication).appContainer.taskUseCases
    }

    private val productUseCases: ProductUseCases by lazy {
        (application as SynnFrameApplication).appContainer.productUseCases
    }

    private val taskTypeUseCases: TaskTypeUseCases by lazy {
        (application as SynnFrameApplication).appContainer.taskTypeUseCases
    }

    private val networkMonitor: NetworkMonitor by lazy {
        (application as SynnFrameApplication).appContainer.networkMonitor
    }

    private val syncHistoryDao by lazy {
        (application as SynnFrameApplication).appContainer.database.syncHistoryDao()
    }

    private val taskTypeRepository by lazy {
        (application as SynnFrameApplication).appContainer.taskTypeRepository
    }

    // Провайдер аутентификации
    private val authProvider by lazy {
        WebServerAuthProvider(serverRepository)
    }

    // Интегратор для обновления информации в SynchronizationController
    private val syncIntegrator by lazy {
        WebServerSyncIntegrator(
            (application as SynnFrameApplication).appContainer.synchronizationController,
            loggingService
        )
    }

    // Сервер Ktor
    private var server: ApplicationEngine? = null

    // Добавляем отсутствующие поля
    private var serverPort: Int = WebServerConstants.PORT_DEFAULT
    private var currentServerHost = "localhost"

    override val serviceName: String = "WebServerService"
    override val notificationId: Int = NotificationChannelManager.NOTIFICATION_ID_WEB_SERVER

    private val controllerFactory by lazy {
        WebServerControllerFactory(
            loggingService,
            userRepository,
            taskRepository,
            productRepository,
            taskTypeUseCases,
            syncIntegrator,
            // Передаем метод saveSyncHistoryRecord как лямбду
            saveSyncHistoryRecord = { tasksDownloaded, productsDownloaded, taskTypesDownloaded, duration ->
                saveSyncHistoryRecord(
                    tasksDownloaded = tasksDownloaded,
                    productsDownloaded = productsDownloaded,
                    taskTypesDownloaded = taskTypesDownloaded,
                    duration = duration
                )
            }
        )
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("WebServerService onCreate")

        // Подписка на обновления адреса
        serviceScope.launch {
            webServerController.serverHost.collect { host ->
                currentServerHost = host
                // При изменении хоста обновим уведомление
                if (isServiceRunning) {
                    val notification = createNotification()
                    startForeground(notificationId, notification)
                }
            }
        }
        notifyControllerAboutServiceState(true)
    }

    override fun onDestroy() {
        notifyControllerAboutServiceState(false)
        super.onDestroy()
    }

    private fun notifyControllerAboutServiceState(isRunning: Boolean) {
        // Получаем экземпляр контроллера, который уже инициализирован в lazy-свойстве
        try {
            webServerController.updateRunningState(isRunning)
        } catch (e: Exception) {
            Timber.e(e, "Error updating controller state")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("WebServerService onStartCommand, action: ${intent?.action}")

        // Получаем порт из Intent, если указан
        intent?.getIntExtra(EXTRA_PORT, WebServerConstants.PORT_DEFAULT)?.let {
            serverPort = it
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override suspend fun onServiceStart() {
        Timber.d("Starting web server on port $serverPort")
        startWebServer(serverPort)
    }

    override suspend fun onServiceStop() {
        Timber.d("Stopping web server")
        stopWebServer()
    }

    override fun createNotification(): Notification {
        // Создаем Intent для открытия приложения при клике на уведомление
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Создаем Intent для остановки сервиса
        val stopIntent = Intent(this, WebServerService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Создаем уведомление
        return NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_WEB_SERVER)
            .setContentTitle(getString(R.string.web_server_notification_title))
            .setContentText(getString(R.string.web_server_notification_text) + " ($currentServerHost:$serverPort)")
            .setSmallIcon(R.drawable.ic_web_server)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Запуск веб-сервера Ktor
     */
    private suspend fun startWebServer(port: Int) {
        withContext(Dispatchers.IO) {
            try {
                server = embeddedServer(Netty, port = port) {
                    configureServer()
                }.start(wait = false)

                loggingService.logInfo(String.format(WebServerConstants.LOG_WEB_SERVER_STARTED, port))
                Timber.i("Web server started on port $port")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                loggingService.logError("Ошибка запуска веб-сервера: ${e.message}")
                Timber.e(e, "Error starting web server")
            }
        }
    }

    /**
     * Конфигурация сервера Ktor
     */
    private fun Application.configureServer() {
        // Инициализируем контроллеры
        val echoController = controllerFactory.createEchoController()
        val productsController = controllerFactory.createProductsController()
        val tasksController = controllerFactory.createTasksController()
        val taskTypesController = controllerFactory.createTaskTypesController()

        // Настройка плагинов
        install(ContentNegotiation) {
            json()
        }

        install(CallLogging)

        install(StatusPages) {
            exception<Throwable> { call, cause ->
                Timber.e(cause, "Error handling request")
                call.respondError(cause.message ?: "Internal Server Error")
            }
        }

        // Настройка аутентификации с использованием провайдера
        install(Authentication) {
            basic(WebServerConstants.AUTH_SCHEME) {
                realm = WebServerConstants.AUTH_REALM
                validate { credentials ->
                    if (authProvider.validateCredentials(credentials.name, credentials.password)) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }

        routing {
            // Публичные маршруты
            route(WebServerConstants.ROUTE_ECHO) {
                get {
                    val startTime = System.currentTimeMillis()
                    echoController.handleEcho(call)
                    logRequest(call, System.currentTimeMillis() - startTime)
                }
            }

            // Защищенные маршруты
            authenticate(WebServerConstants.AUTH_SCHEME) {
                route(WebServerConstants.ROUTE_TASKS) {
                    post {
                        val startTime = System.currentTimeMillis()
                        tasksController.handleTasks(call)
                        logRequest(call, System.currentTimeMillis() - startTime)
                    }
                }

                route(WebServerConstants.ROUTE_PRODUCTS) {
                    post {
                        val startTime = System.currentTimeMillis()
                        productsController.handleProducts(call)
                        logRequest(call, System.currentTimeMillis() - startTime)
                    }
                }

                route(WebServerConstants.ROUTE_TASK_TYPES) {
                    post {
                        val startTime = System.currentTimeMillis()
                        taskTypesController.handleTaskTypes(call)
                        logRequest(call, System.currentTimeMillis() - startTime)
                    }
                }
            }
        }
    }

    /**
     * Обработка полученных заданий
     */
    private suspend fun processTasks(call: ApplicationCall) {
        val startTime = System.currentTimeMillis()

        // Получаем данные запроса и десериализуем в список TaskDto
        val tasksData = call.receive<List<TaskDto>>()

        if (tasksData.isEmpty()) {
            call.respond(mapOf("status" to "warning", "message" to "No tasks received"))
            return
        }

        try {
            // Конвертируем DTO в доменные модели
            val tasks = tasksData.map { it.toDomainModel() }

            // Для каждого задания проверяем, есть ли оно уже в базе
            val newTasks = mutableListOf<com.synngate.synnframe.domain.entity.Task>()
            val updatedTasks = mutableListOf<com.synngate.synnframe.domain.entity.Task>()

            for (task in tasks) {
                val existingTask = taskRepository.getTaskById(task.id)
                if (existingTask == null) {
                    newTasks.add(task)
                } else if (existingTask.status == com.synngate.synnframe.domain.entity.TaskStatus.TO_DO) {
                    // Обновляем только задания в статусе "К выполнению"
                    updatedTasks.add(task)
                }
            }

            // Добавляем новые задания
            if (newTasks.isNotEmpty()) {
                taskRepository.addTasks(newTasks)
            }

            // Обновляем существующие задания
            for (task in updatedTasks) {
                taskRepository.updateTask(task)
            }

            // Логируем операцию
            val duration = System.currentTimeMillis() - startTime
            loggingService.logInfo(
                String.format(WebServerConstants.LOG_TASKS_RECEIVED, tasks.size, newTasks.size, updatedTasks.size, duration)
            )

            // Сохраняем запись в историю синхронизаций
            saveSyncHistoryRecord(
                tasksDownloaded = tasks.size,
                productsDownloaded = 0,
                taskTypesDownloaded = 0,
                duration = duration
            )

            // Обновляем UI через интегратор
            syncIntegrator.updateSyncProgress(
                tasksDownloaded = tasks.size,
                operation = WebServerConstants.OPERATION_TASKS_RECEIVED
            )

            // Отправляем детальный ответ
            call.respond(mapOf(
                "status" to "ok",
                "message" to "Tasks processed successfully",
                "total" to tasks.size,
                "new" to newTasks.size,
                "updated" to updatedTasks.size,
                "processingTime" to duration
            ))
        } catch (e: Exception) {
            Timber.e(e, "Error processing tasks")
            loggingService.logError(String.format(WebServerConstants.LOG_ERROR_TASKS, e.message))

            call.respond(
                io.ktor.http.HttpStatusCode.InternalServerError,
                mapOf("error" to "Error processing tasks: ${e.message}")
            )
        }
    }

    /**
     * Обработка полученных товаров
     */
    private suspend fun processProducts(call: ApplicationCall) {
        val startTime = System.currentTimeMillis()

        // Получаем данные запроса и десериализуем в список ProductDto
        val productsData = call.receive<List<ProductDto>>()

        if (productsData.isEmpty()) {
            call.respond(mapOf("status" to "warning", "message" to "No products received"))
            return
        }

        try {
            // Конвертируем DTO в доменные модели
            val products = productsData.map { it.toDomainModel() }

            // Для каждого товара проверяем, есть ли он уже в базе
            val newProducts = mutableListOf<com.synngate.synnframe.domain.entity.Product>()
            val updatedProducts = mutableListOf<com.synngate.synnframe.domain.entity.Product>()

            for (product in products) {
                val existingProduct = productRepository.getProductById(product.id)
                if (existingProduct == null) {
                    newProducts.add(product)
                } else {
                    // Все товары обновляем, так как у них нет статуса
                    updatedProducts.add(product)
                }
            }

            // Если мы получили все товары из справочника, может иметь смысл очистить старые
            val shouldClearExisting = products.size > 100 &&
                    products.size > (productUseCases.getProductsCount().first() * 0.9)

            // Очищаем существующие товары, если получили почти полный справочник
            if (shouldClearExisting) {
                productRepository.deleteAllProducts()
                productRepository.addProducts(products)

                loggingService.logInfo(String.format(WebServerConstants.LOG_PRODUCTS_FULL_UPDATE, products.size))
            } else {
                // Добавляем новые товары
                if (newProducts.isNotEmpty()) {
                    productRepository.addProducts(newProducts)
                }

                // Обновляем существующие товары
                for (product in updatedProducts) {
                    productRepository.updateProduct(product)
                }
            }

            // Логируем операцию
            val duration = System.currentTimeMillis() - startTime
            if (!shouldClearExisting) {
                loggingService.logInfo(
                    String.format(WebServerConstants.LOG_PRODUCTS_RECEIVED, products.size, newProducts.size, updatedProducts.size, duration)
                )
            }

            // Сохраняем запись в историю синхронизаций
            saveSyncHistoryRecord(
                tasksDownloaded = 0,
                productsDownloaded = products.size,
                taskTypesDownloaded = 0,
                duration = duration
            )

            // Обновляем UI через интегратор
            syncIntegrator.updateSyncProgress(
                productsDownloaded = products.size,
                operation = WebServerConstants.OPERATION_PRODUCTS_RECEIVED
            )

            // Отправляем детальный ответ
            call.respond(mapOf(
                "status" to "ok",
                "message" to if (shouldClearExisting) "Products database completely updated" else "Products processed successfully",
                "total" to products.size,
                "new" to if (shouldClearExisting) products.size else newProducts.size,
                "updated" to if (shouldClearExisting) 0 else updatedProducts.size,
                "fullUpdate" to shouldClearExisting,
                "processingTime" to duration
            ))
        } catch (e: Exception) {
            Timber.e(e, "Error processing products")
            loggingService.logError(String.format(WebServerConstants.LOG_ERROR_PRODUCTS, e.message))

            call.respond(
                io.ktor.http.HttpStatusCode.InternalServerError,
                mapOf("error" to "Error processing products: ${e.message}")
            )
        }
    }

    /**
     * Обработка полученных типов заданий
     */
    private suspend fun processTaskTypes(call: ApplicationCall) {
        val startTime = System.currentTimeMillis()

        // Получаем данные запроса и десериализуем в список TaskTypeDto
        val taskTypesData = call.receive<List<TaskTypeDto>>()

        if (taskTypesData.isEmpty()) {
            call.respond(mapOf("status" to "warning", "message" to "No task types received"))
            return
        }

        try {
            // Здесь мы будем использовать useCase напрямую, а не Repository,
            // чтобы избежать проблем с непубличным классом TaskTypeRepositoryImpl

            // Первый подход: использовать существующий метод syncTaskTypes,
            // который загружает типы заданий с сервера
            val result = taskTypeUseCases.syncTaskTypes()

            // Проверяем результат
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0

                // Логируем операцию
                val duration = System.currentTimeMillis() - startTime
                loggingService.logInfo(
                    String.format(WebServerConstants.LOG_TASK_TYPES_RECEIVED, count, duration)
                )

                // Сохраняем запись в историю синхронизаций
                saveSyncHistoryRecord(
                    tasksDownloaded = 0,
                    productsDownloaded = 0,
                    taskTypesDownloaded = count,
                    duration = duration
                )

                // Обновляем UI через интегратор
                syncIntegrator.updateSyncProgress(
                    taskTypesDownloaded = count,
                    operation = WebServerConstants.OPERATION_TASK_TYPES_RECEIVED
                )

                // Отправляем детальный ответ
                call.respond(mapOf(
                    "status" to "ok",
                    "message" to "Task types processed successfully",
                    "count" to count,
                    "processingTime" to duration
                ))
            } else {
                val error = result.exceptionOrNull()
                throw error ?: Exception("Unknown error processing task types")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing task types")
            loggingService.logError(String.format(WebServerConstants.LOG_ERROR_TASK_TYPES, e.message))

            call.respond(
                io.ktor.http.HttpStatusCode.InternalServerError,
                mapOf("error" to "Error processing task types: ${e.message}")
            )
        }
    }

    /**
     * Сохранение записи в историю синхронизаций
     */
    private suspend fun saveSyncHistoryRecord(
        tasksDownloaded: Int = 0,
        productsDownloaded: Int = 0,
        taskTypesDownloaded: Int = 0,
        duration: Long = WebServerConstants.DEFAULT_SYNC_DURATION
    ) {
        try {
            val startTime = LocalDateTime.now().minusNanos(duration * 1_000_000) // Конвертируем мс в наносекунды
            val endTime = LocalDateTime.now()

            // Получаем информацию о сетевом подключении
            val networkState = networkMonitor.getCurrentNetworkState()
            val networkType = when (networkState) {
                is com.synngate.synnframe.util.network.NetworkState.Available -> networkState.type.name
                else -> "UNKNOWN"
            }

            val isMetered = (networkState as? com.synngate.synnframe.util.network.NetworkState.Available)?.isMetered ?: false

            // Создаем уникальный идентификатор для записи
            val syncId = "${WebServerConstants.SYNC_PREFIX}${UUID.randomUUID()}"

            // Создаем запись истории синхронизации
            val record = SyncHistoryRecord(
                id = syncId,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                networkType = networkType,
                meteredConnection = isMetered,
                tasksUploaded = 0, // Через веб-сервер не выгружаются задания
                tasksDownloaded = tasksDownloaded,
                productsDownloaded = productsDownloaded,
                taskTypesDownloaded = taskTypesDownloaded,
                successful = true,
                retryAttempts = 0,
                totalOperations = tasksDownloaded + productsDownloaded + taskTypesDownloaded
            )

            // Сохраняем запись в базу данных
            syncHistoryDao.insertHistory(record)

            // Обновляем счетчики в интерфейсе
            if (tasksDownloaded > 0 || productsDownloaded > 0 || taskTypesDownloaded > 0) {
                syncIntegrator.updateSyncProgress(
                    tasksDownloaded = tasksDownloaded,
                    productsDownloaded = productsDownloaded,
                    taskTypesDownloaded = taskTypesDownloaded,
                    operation = WebServerConstants.OPERATION_DATA_RECEIVED
                )
            }

            Timber.d("Sync history record saved: $syncId")
        } catch (e: Exception) {
            Timber.e(e, "Error saving sync history record")
        }
    }

    /**
     * Логирование запроса
     */
    private fun logRequest(call: ApplicationCall, duration: Long) {
        serviceScope.launchSafely {
            val requestInfo = WebServerController.RequestInfo(
                timestamp = System.currentTimeMillis(),
                method = call.request.httpMethod.value,
                path = call.request.path(),
                statusCode = call.response.status()?.value ?: 0,
                responseTime = duration,
                clientIp = call.request.origin.remoteHost
            )

            // Вместо прямого вызова приватного метода addRequestInfo,
            // будем логировать запрос в Timber и в логи системы
            Timber.d(
                "Request: ${requestInfo.method} ${requestInfo.path}, " +
                        "Response: ${requestInfo.statusCode}, " +
                        "Time: ${requestInfo.responseTime}ms, " +
                        "Client: ${requestInfo.clientIp}"
            )

            // Также добавим запись в логи приложения
            loggingService.logInfo(
                "Веб-сервер: ${requestInfo.method} ${requestInfo.path}, " +
                        "Код: ${requestInfo.statusCode}, " +
                        "Время: ${requestInfo.responseTime}мс, " +
                        "Клиент: ${requestInfo.clientIp}"
            )
        }
    }

    /**
     * Остановка веб-сервера
     */
    private suspend fun stopWebServer() {
        withContext(Dispatchers.IO) {
            try {
                server?.stop(1, 5, TimeUnit.SECONDS)
                server = null
                loggingService.logInfo(WebServerConstants.LOG_WEB_SERVER_STOPPED)
                Timber.i("Web server stopped")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                loggingService.logError("Ошибка остановки веб-сервера: ${e.message}")
                Timber.e(e, "Error stopping web server")
            }
        }
    }

    companion object {
        const val EXTRA_PORT = "extra_port"

        // Используем константы из BaseForegroundService
        const val ACTION_START_SERVICE = BaseForegroundService.ACTION_START_SERVICE
        const val ACTION_STOP_SERVICE = BaseForegroundService.ACTION_STOP_SERVICE
    }
}