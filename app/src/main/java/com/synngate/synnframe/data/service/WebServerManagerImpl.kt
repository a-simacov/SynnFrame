package com.synngate.synnframe.data.service

import com.synngate.synnframe.domain.service.WebServerController
import com.synngate.synnframe.domain.service.WebServerManager
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class WebServerManagerImpl(
    private val webServerController: WebServerController,
) : WebServerManager {

    override val isRunning: Flow<Boolean> = webServerController.isRunning

    override suspend fun startServer(): Result<Unit> {
        return try {
            webServerController.startService()
        } catch (e: Exception) {
            Timber.e("Error starting web server: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun stopServer(): Result<Unit> {
        return try {
            webServerController.stopService()
        } catch (e: Exception) {
            Timber.e("Error stopping web server: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun toggleServer(): Result<Boolean> {
        return try {
            webServerController.toggleService()
        } catch (e: Exception) {
            Timber.e("Error toggling web server: ${e.message}")
            Result.failure(e)
        }
    }
}