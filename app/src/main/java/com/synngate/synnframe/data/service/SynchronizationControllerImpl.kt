// File: com.synngate.synnframe.data.service.SynchronizationControllerImpl.kt

package com.synngate.synnframe.data.service

import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.presentation.service.base.BaseForegroundService
import com.synngate.synnframe.presentation.service.sync.SynchronizationService
import com.synngate.synnframe.presentation.service.sync.SynchronizationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Реализация контроллера синхронизации
 */
class SynchronizationControllerImpl(
    private val context: Context,
    private val taskUseCases: TaskUseCases,
    private val productUseCases: ProductUseCases,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val loggingService: LoggingService
) : SynchronizationController {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: Flow<Boolean> = _isRunning

    private val _syncStatus = MutableStateFlow(SynchronizationController.SyncStatus.IDLE)
    override val syncStatus: Flow<SynchronizationController.SyncStatus> = _syncStatus

    private val _lastSyncInfo = MutableStateFlow<SynchronizationController.SyncInfo?>(null)
    override val lastSyncInfo: Flow<SynchronizationController.SyncInfo?> = _lastSyncInfo

    private val _periodicSyncInfo = MutableStateFlow(
        SynchronizationController.PeriodicSyncInfo(
            enabled = false,
            intervalSeconds = DEFAULT_SYNC_INTERVAL_SECONDS,
            nextScheduledSync = null
        )
    )
    override val periodicSyncInfo: Flow<SynchronizationController.PeriodicSyncInfo> =
        _periodicSyncInfo

    // Формат даты для отображения в уведомлении
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    init {
        // Инициализируем значения из DataStore
        coroutineScope.launch {
            val enabled = appSettingsDataStore.periodicUploadEnabled.first()
            val interval = appSettingsDataStore.uploadIntervalSeconds.first()

            _periodicSyncInfo.value = SynchronizationController.PeriodicSyncInfo(
                enabled = enabled,
                intervalSeconds = interval,
                nextScheduledSync = calculateNextSyncTime(enabled, interval)
            )

            if (enabled) {
                schedulePeriodicSync(interval)
            }
        }
    }

    override suspend fun startService(): Result<Unit> {
        return try {
            val intent = Intent(context, SynchronizationService::class.java).apply {
                action = BaseForegroundService.ACTION_START_SERVICE
            }
            context.startForegroundService(intent)
            _isRunning.value = true
            loggingService.logInfo("Сервис синхронизации запущен")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error starting synchronization service")
            loggingService.logError("Ошибка запуска сервиса синхронизации: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun stopService(): Result<Unit> {
        return try {
            val intent = Intent(context, SynchronizationService::class.java).apply {
                action = BaseForegroundService.ACTION_STOP_SERVICE
            }
            context.startService(intent)
            _isRunning.value = false
            loggingService.logInfo("Сервис синхронизации остановлен")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error stopping synchronization service")
            loggingService.logError("Ошибка остановки сервиса синхронизации: ${e.message}")
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

    override suspend fun startManualSync(): Result<SynchronizationController.SyncResult> {
        // Проверяем, не идет ли уже синхронизация
        if (_syncStatus.value == SynchronizationController.SyncStatus.SYNCING) {
            return Result.failure(IllegalStateException("Синхронизация уже выполняется"))
        }

        _syncStatus.value = SynchronizationController.SyncStatus.SYNCING

        val startTime = System.currentTimeMillis()

        try {
            // Запускаем синхронизацию заданий
            val tasksUploadResult = taskUseCases.uploadCompletedTasks()
            val tasksUploadedCount = tasksUploadResult.getOrDefault(0)

            // Загружаем новые задания с сервера
            val tasksSyncResult = taskUseCases.syncTasks()
            val tasksDownloadedCount = tasksSyncResult.getOrDefault(0)

            // Синхронизируем товары
            val productsSyncResult = productUseCases.syncProductsWithServer()
            val productsDownloadedCount = productsSyncResult.getOrDefault(0)

            // Вычисляем время выполнения
            val durationMillis = System.currentTimeMillis() - startTime

            // Подготавливаем результат
            val syncResult = SynchronizationController.SyncResult(
                successful = true,
                tasksUploadedCount = tasksUploadedCount,
                tasksDownloadedCount = tasksDownloadedCount,
                productsDownloadedCount = productsDownloadedCount,
                durationMillis = durationMillis,
                errorMessage = null
            )

            // Сохраняем информацию о синхронизации
            saveSyncInfo(syncResult)

            // Устанавливаем статус "IDLE"
            _syncStatus.value = SynchronizationController.SyncStatus.IDLE

            // Логируем успешное завершение
            loggingService.logInfo(
                "Синхронизация завершена успешно. " +
                        "Выгружено заданий: $tasksUploadedCount, " +
                        "загружено заданий: $tasksDownloadedCount, " +
                        "загружено товаров: $productsDownloadedCount. " +
                        "Время: ${durationMillis}мс"
            )

            return Result.success(syncResult)
        } catch (e: Exception) {
            // Вычисляем время выполнения
            val durationMillis = System.currentTimeMillis() - startTime

            // Подготавливаем результат с ошибкой
            val syncResult = SynchronizationController.SyncResult(
                successful = false,
                tasksUploadedCount = 0,
                tasksDownloadedCount = 0,
                productsDownloadedCount = 0,
                durationMillis = durationMillis,
                errorMessage = e.message
            )

            // Сохраняем информацию о синхронизации
            saveSyncInfo(syncResult)

            // Устанавливаем статус "ERROR"
            _syncStatus.value = SynchronizationController.SyncStatus.ERROR

            // Логируем ошибку
            Timber.e(e, "Error during synchronization")
            loggingService.logError("Ошибка синхронизации: ${e.message}")

            return Result.failure(e)
        }
    }

    override suspend fun updatePeriodicSync(enabled: Boolean, intervalSeconds: Int?): Result<Unit> {
        return try {
            // Получаем текущий интервал, если новый не указан
            val interval = intervalSeconds ?: _periodicSyncInfo.value.intervalSeconds

            // Сохраняем настройки
            appSettingsDataStore.setPeriodicUpload(enabled, interval)

            // Обновляем состояние
            _periodicSyncInfo.value = SynchronizationController.PeriodicSyncInfo(
                enabled = enabled,
                intervalSeconds = interval,
                nextScheduledSync = calculateNextSyncTime(enabled, interval)
            )

            // Планируем или отменяем периодическую синхронизацию
            if (enabled) {
                schedulePeriodicSync(interval)
                loggingService.logInfo("Периодическая синхронизация включена с интервалом $interval секунд")
            } else {
                cancelPeriodicSync()
                loggingService.logInfo("Периодическая синхронизация отключена")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating periodic sync settings")
            loggingService.logError("Ошибка обновления настроек периодической синхронизации: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Сохраняет информацию о синхронизации
     */
    private fun saveSyncInfo(syncResult: SynchronizationController.SyncResult) {
        val syncInfo = SynchronizationController.SyncInfo(
            timestamp = LocalDateTime.now(),
            tasksUploadedCount = syncResult.tasksUploadedCount,
            tasksDownloadedCount = syncResult.tasksDownloadedCount,
            productsDownloadedCount = syncResult.productsDownloadedCount,
            durationMillis = syncResult.durationMillis,
            successful = syncResult.successful,
            errorMessage = syncResult.errorMessage
        )

        _lastSyncInfo.value = syncInfo
    }

    /**
     * Планирует периодическую синхронизацию через WorkManager
     */
    private fun schedulePeriodicSync(intervalSeconds: Int) {
        // Создаем запрос на периодическую работу без ограничений
        val periodicWorkRequest = PeriodicWorkRequestBuilder<SynchronizationWorker>(
            intervalSeconds.toLong(),
            TimeUnit.SECONDS
        ).build()

        // Отменяем предыдущую запланированную работу и планируем новую
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWorkRequest
        )

        Timber.d("Scheduled periodic sync with interval $intervalSeconds seconds")
    }

    /**
     * Отменяет периодическую синхронизацию
     */
    private fun cancelPeriodicSync() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC_SYNC)
        Timber.d("Canceled periodic sync")
    }

    /**
     * Рассчитывает время следующей синхронизации
     */
    private fun calculateNextSyncTime(enabled: Boolean, intervalSeconds: Int): LocalDateTime? {
        if (!enabled) return null
        return LocalDateTime.now().plusSeconds(intervalSeconds.toLong())
    }

    companion object {
        private const val DEFAULT_SYNC_INTERVAL_SECONDS = 300 // 5 минут
        private const val WORK_NAME_PERIODIC_SYNC = "periodic_sync_work"
    }
}