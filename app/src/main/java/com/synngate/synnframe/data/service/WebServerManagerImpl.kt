package com.synngate.synnframe.data.service

import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.WebServerController
import com.synngate.synnframe.domain.service.WebServerManager
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Реализация менеджера веб-сервера, взаимодействующая с WebServerController
 */
class WebServerManagerImpl(
    private val webServerController: WebServerController,
    private val loggingService: LoggingService
) : WebServerManager {

    override val isRunning: Flow<Boolean> = webServerController.isRunning

    override suspend fun startServer(): Result<Unit> {
        return try {
            val result = webServerController.startService()
            if (result.isSuccess) {
                loggingService.logInfo("Локальный веб-сервер запущен")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error starting web server")
            loggingService.logError("Ошибка запуска веб-сервера: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun stopServer(): Result<Unit> {
        return try {
            val result = webServerController.stopService()
            if (result.isSuccess) {
                loggingService.logInfo("Локальный веб-сервер остановлен")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error stopping web server")
            loggingService.logError("Ошибка остановки веб-сервера: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun toggleServer(): Result<Boolean> {
        return try {
            webServerController.toggleService()
        } catch (e: Exception) {
            Timber.e(e, "Error toggling web server")
            loggingService.logError("Ошибка переключения веб-сервера: ${e.message}")
            Result.failure(e)
        }
    }
}