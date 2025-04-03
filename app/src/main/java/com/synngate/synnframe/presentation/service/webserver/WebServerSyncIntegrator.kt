package com.synngate.synnframe.presentation.service.webserver

import com.synngate.synnframe.data.sync.SyncProgress
import com.synngate.synnframe.data.sync.SyncStatus
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.SynchronizationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * Класс для интеграции локального веб-сервера с SynchronizationController
 * Обеспечивает обновление UI с данными, полученными через веб-сервер
 */
class WebServerSyncIntegrator(
    private val synchronizationController: SynchronizationController,
    private val loggingService: LoggingService
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Обновляет прогресс синхронизации в SynchronizationController
     */
    fun updateSyncProgress(
        tasksDownloaded: Int = 0,
        productsDownloaded: Int = 0,
        taskTypesDownloaded: Int = 0,
        operation: String = WebServerConstants.OPERATION_DATA_RECEIVED
    ) {
        scope.launch {
            try {
                // Создаем объект прогресса
                val progress = SyncProgress(
                    id = "${WebServerConstants.SYNC_PREFIX}${UUID.randomUUID()}",
                    startTime = LocalDateTime.now().minusSeconds(1),
                    endTime = LocalDateTime.now(),
                    status = SyncStatus.COMPLETED,
                    tasksDownloaded = tasksDownloaded,
                    productsDownloaded = productsDownloaded,
                    taskTypesDownloaded = taskTypesDownloaded,
                    currentOperation = operation,
                    progressPercent = 100
                )

                // Обновляем SynchronizationController
                updateLastSyncInfo(tasksDownloaded, productsDownloaded, taskTypesDownloaded)
            } catch (e: Exception) {
                Timber.e(e, "Error updating sync progress")
                loggingService.logError("Ошибка обновления прогресса синхронизации: ${e.message}")
            }
        }
    }

    /**
     * Обновляет информацию о последней синхронизации
     */
    private suspend fun updateLastSyncInfo(
        tasksDownloaded: Int = 0,
        productsDownloaded: Int = 0,
        taskTypesDownloaded: Int = 0
    ) {
        try {
            // Обновляем информацию о последней загрузке товаров
            if (productsDownloaded > 0) {
                synchronizationController.updateLastProductsSync(productsDownloaded)
            }

            // Также можно попробовать другие методы для отражения данных в UI
            if (tasksDownloaded > 0 || taskTypesDownloaded > 0) {
                // Здесь только логирование, так как нет публичного метода для обновления
                Timber.d("WebServer downloaded: tasks=$tasksDownloaded, taskTypes=$taskTypesDownloaded")
                loggingService.logInfo(
                    "Через веб-сервер получено: заданий=$tasksDownloaded, типов заданий=$taskTypesDownloaded"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating last sync info")
            loggingService.logError("Ошибка обновления информации о синхронизации: ${e.message}")
        }
    }
}