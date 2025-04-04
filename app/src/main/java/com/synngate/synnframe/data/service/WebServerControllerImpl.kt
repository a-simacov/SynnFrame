// File: com.synngate.synnframe.data.service.WebServerControllerImpl.kt

package com.synngate.synnframe.data.service

import android.content.Context
import android.content.Intent
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.WebServerController
import com.synngate.synnframe.presentation.service.webserver.WebServerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.net.NetworkInterface
import java.util.Collections

class WebServerControllerImpl(
    private val context: Context,
    private val loggingService: LoggingService
) : WebServerController {

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: Flow<Boolean> = _isRunning.asStateFlow()

    private val _serverHost = MutableStateFlow(getLocalIpAddress())
    override val serverHost: Flow<String> = _serverHost

    private val _serverPort = MutableStateFlow(DEFAULT_PORT)
    override val serverPort: Flow<Int> = _serverPort

    private val _lastRequests = MutableStateFlow<List<WebServerController.RequestInfo>>(emptyList())
    override val lastRequests: Flow<List<WebServerController.RequestInfo>> = _lastRequests

    init {
        checkServiceStatus()
    }

    override suspend fun startService(): Result<Unit> {
        return try {
            val intent = Intent(context, WebServerService::class.java).apply {
                action = WebServerService.ACTION_START_SERVICE
                putExtra(WebServerService.EXTRA_PORT, _serverPort.value)
            }
            context.startForegroundService(intent)
            _isRunning.value = true
            loggingService.logInfo("Локальный веб-сервер запущен на порту ${_serverPort.value}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error starting web server")
            loggingService.logError("Ошибка запуска веб-сервера: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun stopService(): Result<Unit> {
        return try {
            val intent = Intent(context, WebServerService::class.java).apply {
                action = WebServerService.ACTION_STOP_SERVICE
            }
            context.startService(intent)
            _isRunning.value = false
            loggingService.logInfo("Локальный веб-сервер остановлен")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error stopping web server")
            loggingService.logError("Ошибка остановки веб-сервера: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun toggleService(): Result<Boolean> {
        return if (_isRunning.value) {
            stopService().map { false }
        } else {
            startService().map { true }
        }
    }

    override fun updateRunningState(isRunning: Boolean) {
        _isRunning.value = isRunning
        // Можно также сохранить состояние для восстановления при перезагрузке приложения
    }

    override suspend fun clearRequestsLog() {
        _lastRequests.value = emptyList()
    }

    fun addRequestInfo(requestInfo: WebServerController.RequestInfo) {
        val currentList = _lastRequests.value.toMutableList()
        currentList.add(0, requestInfo) // Добавляем в начало списка
        _lastRequests.value = currentList.take(MAX_REQUEST_LOG_SIZE) // Ограничиваем размер списка
    }

    private fun checkServiceStatus() {
        // Проверка статуса сервиса будет реализована позже
        // Это может быть реализовано через биндинг к сервису или через флаг в SharedPreferences
        _isRunning.value = false
    }

    private fun getLocalIpAddress(): String {
        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in networkInterfaces) {
                if (!networkInterface.isLoopback) {
                    val addresses = Collections.list(networkInterface.inetAddresses)
                    for (address in addresses) {
                        if (!address.isLoopbackAddress && address.hostAddress.indexOf(':') < 0) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting local IP address")
        }
        return "localhost"
    }

    companion object {
        private const val DEFAULT_PORT = 8080
        private const val MAX_REQUEST_LOG_SIZE = 100
    }
}