package com.synngate.synnframe.presentation.service.webserver

import com.synngate.synnframe.domain.service.SynchronizationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Класс для интеграции локального веб-сервера с SynchronizationController
 * Обеспечивает обновление UI с данными, полученными через веб-сервер
 */
class WebServerSyncIntegrator(
    private val synchronizationController: SynchronizationController
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Обновляет прогресс синхронизации в SynchronizationController
     */
    fun updateSyncProgress(
        tasksDownloaded: Int = 0,
        productsDownloaded: Int = 0,
        taskTypesDownloaded: Int = 0
    ) {
        scope.launch {
            try {
                updateLastSyncInfo(tasksDownloaded, productsDownloaded, taskTypesDownloaded)
            } catch (e: Exception) {
                Timber.e(e, "Error updating sync progress")
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
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating last sync info")
        }
    }
}