package com.synngate.synnframe.presentation.service.webserver

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.synngate.synnframe.R
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.data.sync.SyncHistoryRecord
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.service.WebServerController
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
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class WebServerService : BaseForegroundService() {

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

    private val taskTypeUseCases: TaskTypeUseCases by lazy {
        (application as SynnFrameApplication).appContainer.taskTypeUseCases
    }

    private val networkMonitor: NetworkMonitor by lazy {
        (application as SynnFrameApplication).appContainer.networkMonitor
    }

    private val syncHistoryDao by lazy {
        (application as SynnFrameApplication).appContainer.database.syncHistoryDao()
    }

    // Провайдер аутентификации
    private val authProvider by lazy {
        WebServerAuthProvider(serverRepository)
    }

    // Интегратор для обновления информации в SynchronizationController
    private val syncIntegrator by lazy {
        WebServerSyncIntegrator(
            (application as SynnFrameApplication).appContainer.synchronizationController
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

    private suspend fun startWebServer(port: Int) {
        withContext(Dispatchers.IO) {
            try {
                server = embeddedServer(Netty, port = port) {
                    configureServer()
                }.start(wait = false)

                Timber.i(String.format(WebServerConstants.LOG_WEB_SERVER_STARTED, port))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error starting web server")
            }
        }
    }

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
            Timber.i(
                "Web server: ${requestInfo.method} ${requestInfo.path}, " +
                        "Code: ${requestInfo.statusCode}, " +
                        "Time: ${requestInfo.responseTime}мс, " +
                        "Client: ${requestInfo.clientIp}"
            )
        }
    }

    private suspend fun stopWebServer() {
        withContext(Dispatchers.IO) {
            try {
                server?.stop(1, 5, TimeUnit.SECONDS)
                server = null
                Timber.i(WebServerConstants.LOG_WEB_SERVER_STOPPED)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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