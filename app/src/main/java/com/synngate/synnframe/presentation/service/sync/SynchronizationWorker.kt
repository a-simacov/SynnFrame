// File: com.synngate.synnframe.presentation.service.sync.SynchronizationWorker.kt

package com.synngate.synnframe.presentation.service.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.domain.service.SynchronizationController
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
                    Timber.w("SynchronizationWorker: sync failed with error: ${result?.errorMessage}")
                    Result.retry()
                }
            } else {
                val error = syncResult.exceptionOrNull()
                Timber.e(error, "SynchronizationWorker: sync failed with exception")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "SynchronizationWorker: exception during sync")
            Result.retry()
        }
    }
}