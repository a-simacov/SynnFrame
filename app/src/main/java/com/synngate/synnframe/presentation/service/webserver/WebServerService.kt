// File: com.synngate.synnframe.presentation.service.webserver.WebServerService.kt

package com.synngate.synnframe.presentation.service.webserver

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Stop
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.synngate.synnframe.R
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.data.service.WebServerControllerImpl
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.WebServerController
//import com.synngate.synnframe.presentation.MainActivity
import com.synngate.synnframe.presentation.service.base.BaseForegroundService
import com.synngate.synnframe.presentation.service.base.launchSafely
import com.synngate.synnframe.presentation.service.notification.NotificationChannelManager
import com.synngate.synnframe.presentation.ui.MainActivity
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.CallLogging
//import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
//import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Foreground-сервис для локального веб-сервера на базе Ktor
 */
class WebServerService : BaseForegroundService() {

    // Инъекция зависимостей
    // Заменяем на получение через SynnFrameApplication
    override val loggingService: LoggingService by lazy {
        (application as SynnFrameApplication).appContainer.loggingService
    }

    private val webServerController: WebServerControllerImpl by lazy {
        (application as SynnFrameApplication).appContainer.webServerController
    }

    // Сервер Ktor
    private var server: ApplicationEngine? = null

    private var serverPort: Int = DEFAULT_PORT
    private var currentServerHost = "localhost"

    override val serviceName: String = "WebServerService"
    override val notificationId: Int = NotificationChannelManager.NOTIFICATION_ID_WEB_SERVER

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("WebServerService onStartCommand, action: ${intent?.action}")

        // Получаем порт из Intent, если указан
        intent?.getIntExtra(EXTRA_PORT, DEFAULT_PORT)?.let {
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

                loggingService.logInfo("Локальный веб-сервер запущен на порту $port")
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
        // Настройка плагинов
        install(ContentNegotiation) {
            json()
        }

        install(CallLogging)

        install(StatusPages) {
            exception<Throwable> { call, cause ->
                Timber.e(cause, "Error handling request")
                call.respond(
                    io.ktor.http.HttpStatusCode.InternalServerError,
                    "Internal Server Error"
                )
            }
        }

        // Настройка аутентификации
        install(Authentication) {
            basic("auth-basic") {
                realm = "Access to API"
                validate { credentials ->
                    // Простая проверка учетных данных
                    // В реальном приложении здесь должна быть более сложная логика
                    if (credentials.name == "admin" && credentials.password == "admin") {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }

        // Настройка маршрутизации
        routing {
            // Публичные маршруты (без аутентификации)
            route("/echo") {
                get {
                    val startTime = System.currentTimeMillis()

                    // Подготовка ответа
                    val response = mapOf(
                        "status" to "ok",
                        "timestamp" to System.currentTimeMillis(),
                        "serverVersion" to "1.0.0"
                    )

                    call.respond(response)

                    // Логирование запроса
                    val duration = System.currentTimeMillis() - startTime
                    logRequest(call, duration)
                }
            }

            // Защищенные маршруты (требуют аутентификации)
            authenticate("auth-basic") {
                route("/tasks") {
                    post {
                        val startTime = System.currentTimeMillis()

                        try {
                            // Получение данных запроса
                            val tasksData = call.receive<Map<String, Any>>()

                            // Здесь будет логика обработки полученных данных о заданиях

                            call.respond(mapOf("status" to "ok", "message" to "Tasks received"))
                        } catch (e: Exception) {
                            Timber.e(e, "Error processing tasks data")
                            call.respond(
                                io.ktor.http.HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid tasks data")
                            )
                        }

                        // Логирование запроса
                        val duration = System.currentTimeMillis() - startTime
                        logRequest(call, duration)
                    }
                }

                route("/products") {
                    post {
                        val startTime = System.currentTimeMillis()

                        try {
                            // Получение данных запроса
                            val productsData = call.receive<Map<String, Any>>()

                            // Здесь будет логика обработки полученных данных о товарах

                            call.respond(mapOf("status" to "ok", "message" to "Products received"))
                        } catch (e: Exception) {
                            Timber.e(e, "Error processing products data")
                            call.respond(
                                io.ktor.http.HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid products data")
                            )
                        }

                        // Логирование запроса
                        val duration = System.currentTimeMillis() - startTime
                        logRequest(call, duration)
                    }
                }
            }
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

            webServerController.addRequestInfo(requestInfo)

            // Логирование в Timber
            Timber.d(
                "Request: ${requestInfo.method} ${requestInfo.path}, " +
                        "Response: ${requestInfo.statusCode}, " +
                        "Time: ${requestInfo.responseTime}ms, " +
                        "Client: ${requestInfo.clientIp}"
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
                loggingService.logInfo("Локальный веб-сервер остановлен")
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
        private const val DEFAULT_PORT = 8080

        // Используем константы из BaseForegroundService
        const val ACTION_START_SERVICE = BaseForegroundService.ACTION_START_SERVICE
        const val ACTION_STOP_SERVICE = BaseForegroundService.ACTION_STOP_SERVICE
    }
}