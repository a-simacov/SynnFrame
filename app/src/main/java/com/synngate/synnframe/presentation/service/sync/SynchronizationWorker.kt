// File: com.synngate.synnframe.presentation.service.sync.SynchronizationWorker.kt

package com.synngate.synnframe.presentation.service.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.util.network.NetworkErrorClassifier
import timber.log.Timber

/**
 * Worker для выполнения периодической синхронизации через WorkManager
 */
class SynchronizationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // Получаем зависимости через appContainer вместо Koin
    private val synchronizationController: SynchronizationController =
        (appContext.applicationContext as SynnFrameApplication).appContainer.synchronizationController

    override suspend fun doWork(): Result {
        Timber.d("SynchronizationWorker: starting sync")

        return try {
            // Запускаем синхронизацию
            val syncResult = synchronizationController.startManualSync()

            // Возвращаем результат на основе успешности синхронизации
            if (syncResult.isSuccess) {
                val result = syncResult.getOrNull()
                if (result != null && result.successful) {
                    Timber.d("SynchronizationWorker: sync completed successfully")
                    Result.success()
                } else {
                    val error = result?.errorMessage ?: "Неизвестная ошибка"
                    Timber.w("SynchronizationWorker: sync failed with error: $error")

                    // Для таймаутов всегда ставим retry
                    if (error.contains("timeout", ignoreCase = true) ||
                        error.contains("timed out", ignoreCase = true) ||
                        error.contains("connect", ignoreCase = true)) {
                        Timber.d("SynchronizationWorker: timeout error, scheduling retry")
                        Result.retry()
                    } else {
                        // Проверяем обычные критерии
                        val exception = syncResult.exceptionOrNull()
                        if (exception != null && NetworkErrorClassifier.isRetryable(exception)) {
                            Timber.d("SynchronizationWorker: error is retryable, scheduling retry")
                            Result.retry()
                        } else {
                            Timber.d("SynchronizationWorker: error is not retryable, marking as failure")
                            Result.failure()
                        }
                    }
                }
            } else {
                val error = syncResult.exceptionOrNull()
                Timber.e(error, "SynchronizationWorker: sync failed with exception")

                // Определяем, стоит ли повторять попытку с прямой проверкой на таймаут
                if (error != null && (
                            error.message?.contains("timeout", ignoreCase = true) == true ||
                                    error.message?.contains("connect", ignoreCase = true) == true ||
                                    NetworkErrorClassifier.isRetryable(error))
                ) {
                    Timber.d("SynchronizationWorker: error is retryable, scheduling retry")
                    Result.retry()
                } else {
                    Timber.d("SynchronizationWorker: error is not retryable, marking as failure")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SynchronizationWorker: exception during sync")
            Result.retry()
        }
    }
}