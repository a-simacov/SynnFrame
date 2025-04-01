// File: com.synngate.synnframe.data.service.SynchronizationControllerImpl.kt

package com.synngate.synnframe.data.service

import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.local.dao.SyncHistoryDao
import com.synngate.synnframe.data.local.database.AppDatabase
import com.synngate.synnframe.data.local.entity.OperationType
import com.synngate.synnframe.data.sync.SyncHistoryRecord
import com.synngate.synnframe.data.sync.SyncProgress
import com.synngate.synnframe.data.sync.SyncStatus
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.tasktype.TaskTypeUseCases
import com.synngate.synnframe.presentation.service.base.BaseForegroundService
import com.synngate.synnframe.presentation.service.sync.SynchronizationService
import com.synngate.synnframe.presentation.service.sync.SynchronizationService.Companion.ACTION_RESET_STATE
import com.synngate.synnframe.presentation.service.sync.SynchronizationWorker
import com.synngate.synnframe.util.network.NetworkErrorClassifier
import com.synngate.synnframe.util.network.NetworkMonitor
import com.synngate.synnframe.util.network.NetworkState
import com.synngate.synnframe.util.network.RetryStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class SynchronizationControllerImpl(
    private val context: Context,
    private val taskUseCases: TaskUseCases,
    private val productUseCases: ProductUseCases,
    private val taskTypeUseCases: TaskTypeUseCases,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val loggingService: LoggingService,
    private val appDatabase: AppDatabase
) : SynchronizationController {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: Flow<Boolean> = _isRunning

    private val _syncStatus = MutableStateFlow(SynchronizationController.SyncStatus.IDLE)
    override val syncStatus: Flow<SynchronizationController.SyncStatus> = _syncStatus

    private val _lastSyncInfo = MutableStateFlow<SynchronizationController.SyncInfo?>(null)
    override val lastSyncInfo: Flow<SynchronizationController.SyncInfo?> = _lastSyncInfo

    private val _syncProgressFlow = MutableStateFlow(SyncProgress())
    override val syncProgressFlow: Flow<SyncProgress> = _syncProgressFlow.asStateFlow()

    private val _periodicSyncInfo = MutableStateFlow(
        SynchronizationController.PeriodicSyncInfo(
            enabled = false,
            intervalSeconds = DEFAULT_SYNC_INTERVAL_SECONDS,
            nextScheduledSync = null
        )
    )
    override val periodicSyncInfo: Flow<SynchronizationController.PeriodicSyncInfo> =
        _periodicSyncInfo

    private val retryStrategy = RetryStrategy(
        maxAttempts = 5,
        initialDelayMs = 1000,
        maxDelayMs = 60000
    )

    private val syncHistoryDao: SyncHistoryDao = appDatabase.syncHistoryDao()

    private val networkMonitor: NetworkMonitor
    private val syncQueueManager: SyncQueueManager

    init {
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

        networkMonitor = NetworkMonitor(context)
        networkMonitor.initialize()

        syncQueueManager = SyncQueueManager(
            appDatabase.syncOperationDao(),
            networkMonitor,
            loggingService
        )

        // Настраиваем наблюдение за состоянием сети
        coroutineScope.launch {
            networkMonitor.networkState.collect { state ->
                Timber.d("Network state changed: $state")

                // Если сеть стала доступна, запускаем обработку ожидающих операций
                if (state is NetworkState.Available) {
                    Timber.d("Network is available, processing queued operations")
                    processQueuedOperations()
                }
            }
        }

        // Запускаем периодическую обработку отложенных операций с проверкой состояния
        coroutineScope.launch {
            while (true) {
                try {
                    // Проверяем, не находится ли контроллер в состоянии ошибки
                    if (_syncStatus.value == SynchronizationController.SyncStatus.ERROR) {
                        resetSyncState()
                        loggingService.logInfo("Reset error state for a new sync attempt")
                    }

                    // Обрабатываем отложенные операции только если контроллер не в процессе синхронизации
                    if (_syncStatus.value != SynchronizationController.SyncStatus.SYNCING) {
                        processQueuedOperations()
                    }

                    delay(CHECK_QUEUE_INTERVAL_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Error processing queued operations")
                }
            }
        }

        // Добавим механизм периодического сброса состояния ошибки
        coroutineScope.launch {
            while (true) {
                try {
                    // Проверяем состояние контроллера
                    val currentStatus = _syncStatus.value

                    // Если в состоянии ошибки более 30 секунд - принудительно сбрасываем
                    if (currentStatus == SynchronizationController.SyncStatus.ERROR) {
                        Timber.d("Resetting ERROR state to IDLE")
                        resetSyncState()

                        // Также добавляем операцию в очередь
                        syncQueueManager.enqueueOperation(
                            operationType = OperationType.FULL_SYNC,
                            targetId = "retry-sync-${System.currentTimeMillis()}",
                            executeImmediately = false
                        )
                    }

                    delay(30000) // Проверяем каждые 30 секунд
                } catch (e: Exception) {
                    Timber.e(e, "Error in state reset monitoring")
                }
            }
        }
    }

    override suspend fun startService(): Result<Unit> {
        return try {
            resetSyncState()

            val intent = Intent(context, SynchronizationService::class.java).apply {
                action = BaseForegroundService.ACTION_START_SERVICE
            }
            context.startForegroundService(intent)
            _isRunning.value = true
            loggingService.logInfo("Sync service started")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error starting synchronization service")
            loggingService.logError("Error starting synchronization service: ${e.message}")
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
            loggingService.logInfo("Sync service stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error stopping synchronization service")
            loggingService.logError("Error stopping synchronization service: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun toggleService(): Result<Boolean> {
        return if (_isRunning.value) {
            stopService().map { false }
        } else {
            resetSyncState()
            startService().map { true }
        }
    }

    // Добавить метод для сброса состояния синхронизации
    private fun resetSyncState() {
        // Сброс состояния контроллера
        _syncStatus.value = SynchronizationController.SyncStatus.IDLE

        // Отправка команды сервису для сброса состояния уведомления
        val intent = Intent(context, SynchronizationService::class.java).apply {
            action = ACTION_RESET_STATE
        }
        context.startService(intent)
    }


    override suspend fun startManualSync(): Result<SynchronizationController.SyncResult> {
        // Если синхронизация уже идет, возвращаем ошибку
        if (_syncStatus.value == SynchronizationController.SyncStatus.SYNCING) {
            return Result.failure(IllegalStateException("Syncing is already in process"))
        }

        // Если нет сети, добавляем операцию в очередь
        if (!networkMonitor.isNetworkAvailable()) {
            Timber.d("No network, adding operation FULL_SYNC to queue")
            val operationId = syncQueueManager.enqueueOperation(
                operationType = OperationType.FULL_SYNC,
                targetId = "manual-sync-${System.currentTimeMillis()}",
                executeImmediately = false
            )

            loggingService.logInfo("Sync is enqueued and will be proceeded when network appears")

            return Result.failure(
                NoNetworkException("No network connection. Sync will start when network appears.")
            )
        }

        // Есть сеть, выполняем синхронизацию
        _syncStatus.value = SynchronizationController.SyncStatus.SYNCING

        try {
            val syncResult = performFullSync()
            _syncStatus.value = SynchronizationController.SyncStatus.IDLE
            return Result.success(syncResult)
        } catch (e: Exception) {
            _syncStatus.value = SynchronizationController.SyncStatus.ERROR

            // Добавляем операцию повторной синхронизации в очередь если ошибка временная
            if (NetworkErrorClassifier.isRetryable(e)) {
                syncQueueManager.enqueueOperation(
                    operationType = OperationType.FULL_SYNC,
                    targetId = "retry-sync-${System.currentTimeMillis()}",
                    executeImmediately = false
                )

                loggingService.logWarning("Sync failed: ${e.message}. Retry attempt planned.")
            }

            return Result.failure(e)
        }
    }

    override suspend fun updatePeriodicSync(enabled: Boolean, intervalSeconds: Int?): Result<Unit> {
        return try {
            val interval = intervalSeconds ?: _periodicSyncInfo.value.intervalSeconds

            appSettingsDataStore.setPeriodicUpload(enabled, interval)

            _periodicSyncInfo.value = SynchronizationController.PeriodicSyncInfo(
                enabled = enabled,
                intervalSeconds = interval,
                nextScheduledSync = calculateNextSyncTime(enabled, interval)
            )

            // Планируем или отменяем периодическую синхронизацию
            if (enabled) {
                schedulePeriodicSync(interval)
                loggingService.logInfo("Periodical sync is on with interval $interval seconds")
            } else {
                cancelPeriodicSync()
                loggingService.logInfo("Periodical sync is off")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating periodic sync settings")
            loggingService.logError("Error updating periodic sync settings: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateLastProductsSync(productsCount: Int) {
        val now = LocalDateTime.now()
        val syncInfo = SynchronizationController.SyncInfo(
            timestamp = now,
            tasksUploadedCount = 0,
            tasksDownloadedCount = 0,
            productsDownloadedCount = productsCount,
            durationMillis = 0,
            successful = true
        )

        _lastSyncInfo.value = syncInfo

        // Сохраняем запись в историю синхронизаций
        saveSyncHistoryRecord(
            id = System.currentTimeMillis().toString(),
            startTime = now.minusSeconds(1), // Предполагаем, что синхронизация длилась 1 секунду
            endTime = now,
            duration = 1000, // 1 секунда в миллисекундах
            tasksUploaded = 0,
            tasksDownloaded = 0,
            productsDownloaded = productsCount,
            taskTypesDownloaded = 0,
            successful = true
        )
    }

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

    private fun schedulePeriodicSync(intervalSeconds: Int) {
        // Создаем запрос на периодическую работу без ограничений
        val periodicWorkRequest = PeriodicWorkRequestBuilder<SynchronizationWorker>(
            intervalSeconds.toLong(),
            TimeUnit.SECONDS
        ).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()

        // Отменяем предыдущую запланированную работу и планируем новую
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWorkRequest
        )

        Timber.d("Scheduled periodic sync with interval $intervalSeconds seconds")
    }

    private fun cancelPeriodicSync() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC_SYNC)
        Timber.d("Canceled periodic sync")
    }

    private fun calculateNextSyncTime(enabled: Boolean, intervalSeconds: Int): LocalDateTime? {
        if (!enabled) return null
        return LocalDateTime.now().plusSeconds(intervalSeconds.toLong())
    }

    private suspend fun processQueuedOperations() {
        // Если синхронизация уже идет или сервис не запущен, пропускаем
        if (_syncStatus.value == SynchronizationController.SyncStatus.SYNCING ||
            !_isRunning.value
        ) {
            return
        }

        // Если сеть недоступна, пропускаем
        if (!networkMonitor.isNetworkAvailable()) {
            Timber.d("Network is unavailable, skipping operations processing")
            return
        }

        // Получаем операции, готовые для выполнения
        val operations = syncQueueManager.getReadyOperations()
        if (operations.isEmpty()) {
            Timber.d("No operations, ready to execute")
            return
        }

        Timber.d("Starting processing ${operations.size} operations")
        _syncStatus.value = SynchronizationController.SyncStatus.SYNCING

        try {
            for (operation in operations) {
                // Проверяем, не потеряли ли мы сеть во время обработки
                if (!networkMonitor.isNetworkAvailable()) {
                    Timber.d("Network became unavailable while processing operations")
                    break
                }

                Timber.d("Operation processing ${operation.id}: ${operation.operationType}")

                try {
                    when (operation.operationType) {
                        OperationType.UPLOAD_TASK -> {
                            // Выгрузка задания
                            val result = taskUseCases.uploadTask(operation.targetId)
                            if (result.isSuccess) {
                                syncQueueManager.markOperationCompleted(operation.id)
                            } else {
                                val error =
                                    result.exceptionOrNull()?.message ?: "Unknown error"
                                syncQueueManager.handleFailedAttempt(operation.id, error)
                            }
                        }

                        OperationType.DOWNLOAD_TASKS -> {
                            // Загрузка заданий
                            val result = taskUseCases.syncTasks()
                            if (result.isSuccess) {
                                syncQueueManager.markOperationCompleted(operation.id)
                            } else {
                                val error =
                                    result.exceptionOrNull()?.message ?: "Unknown error"
                                syncQueueManager.handleFailedAttempt(operation.id, error)
                            }
                        }

                        OperationType.DOWNLOAD_PRODUCTS -> {
                            // Загрузка товаров
                            val result = productUseCases.syncProductsWithServer()
                            if (result.isSuccess) {
                                syncQueueManager.markOperationCompleted(operation.id)
                            } else {
                                val error =
                                    result.exceptionOrNull()?.message ?: "Unknown error"
                                syncQueueManager.handleFailedAttempt(operation.id, error)
                            }
                        }

                        OperationType.FULL_SYNC -> {
                            // Полная синхронизация
                            val syncResult = performFullSync()
                            if (syncResult.successful) {
                                syncQueueManager.markOperationCompleted(operation.id)
                            } else {
                                syncQueueManager.handleFailedAttempt(
                                    operation.id,
                                    syncResult.errorMessage ?: "Sync error"
                                )
                            }
                        }

                        OperationType.DOWNLOAD_TASK_TYPES -> {
                            // Загрузка типов заданий
                            val result = taskTypeUseCases.syncTaskTypes()
                            if (result.isSuccess) {
                                syncQueueManager.markOperationCompleted(operation.id)
                            } else {
                                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                                syncQueueManager.handleFailedAttempt(operation.id, error)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error while operation processing ${operation.id}")
                    syncQueueManager.handleFailedAttempt(
                        operation.id,
                        e.message ?: "Unknown error"
                    )
                }
            }
        } finally {
            _syncStatus.value = SynchronizationController.SyncStatus.IDLE
        }
    }

    private fun updateProgress(update: (SyncProgress) -> SyncProgress) {
        val currentProgress = _syncProgressFlow.value
        val newProgress = update(currentProgress)
        _syncProgressFlow.value = newProgress

        // Отправляем обновление в сервис синхронизации
        if (_isRunning.value) {
            sendProgressUpdate(newProgress)
        }
    }

    // Метод для отправки обновления прогресса в сервис
    private fun sendProgressUpdate(progress: SyncProgress) {
        try {
            val intent = Intent(context, SynchronizationService::class.java).apply {
                action = SynchronizationService.ACTION_UPDATE_PROGRESS
                putExtra(SynchronizationService.EXTRA_PROGRESS_ID, progress.id)
                putExtra(SynchronizationService.EXTRA_PROGRESS_STATUS, progress.status.name)
                putExtra(SynchronizationService.EXTRA_TASKS_UPLOADED, progress.tasksUploaded)
                putExtra(SynchronizationService.EXTRA_TASKS_DOWNLOADED, progress.tasksDownloaded)
                putExtra(
                    SynchronizationService.EXTRA_PRODUCTS_DOWNLOADED,
                    progress.productsDownloaded
                )
                putExtra(SynchronizationService.EXTRA_CURRENT_OPERATION, progress.currentOperation)
                putExtra(SynchronizationService.EXTRA_PROGRESS_PERCENT, progress.progressPercent)
                putExtra(SynchronizationService.EXTRA_ERROR_COUNT, progress.errorCount)
                putExtra(SynchronizationService.EXTRA_ERROR_MESSAGE, "${progress.endTime}:${progress.lastErrorMessage}")
            }
            context.startService(intent)
        } catch (e: Exception) {
            Timber.e(e, "Error on sending updates to the service")
        }
    }

    private suspend fun performFullSync(): SynchronizationController.SyncResult {
        val startTime = System.currentTimeMillis()
        val syncId = System.currentTimeMillis().toString()

        // Инициализируем прогресс
        updateProgress {
            SyncProgress(
                id = syncId,
                startTime = LocalDateTime.now(),
                status = SyncStatus.STARTED,
                currentOperation = "Подготовка"
            )
        }

        try {
            // Получаем список невыгруженных заданий
            val completedTasks = taskUseCases.getCompletedNotUploadedTasks().getOrThrow()

            // Обновляем прогресс с информацией о количестве заданий
            updateProgress { progress ->
                progress.copy(
                    tasksToUpload = completedTasks.size,
                    status = SyncStatus.IN_PROGRESS,
                    currentOperation = "Выгрузка заданий"
                )
            }

            // Выгрузка заданий с отслеживанием прогресса
            var uploadedCount = 0
            val taskUploadResults = mutableListOf<Result<Boolean>>()

            for (task in completedTasks) {
                updateProgress { progress ->
                    progress.copy(
                        currentOperation = "Выгрузка задания ${task.id}",
                        tasksUploaded = uploadedCount
                    )
                }

                val result = taskUseCases.uploadTask(task.id)
                taskUploadResults.add(result)

                if (result.isSuccess && result.getOrNull() == true) {
                    uploadedCount++
                    updateProgress { progress ->
                        progress.copy(tasksUploaded = uploadedCount)
                    }
                }
            }

            // Переходим к загрузке заданий
            updateProgress { progress ->
                progress.copy(
                    currentOperation = "Загрузка заданий с сервера"
                )
            }

            // Загрузка заданий
            val tasksDownloadedCount = retryStrategy.executeWithRetry(
                operation = {
                    val result = taskUseCases.syncTasks()
                    result.getOrThrow()
                },
                shouldRetry = { e -> NetworkErrorClassifier.isRetryable(e) },
                onError = { e, attempt, delay ->
                    updateProgress { progress ->
                        progress.copy(
                            errorCount = progress.errorCount + 1,
                            lastErrorMessage = "Ошибка загрузки заданий: ${e.message}"
                        )
                    }

                    loggingService.logWarning(
                        "Attempt $attempt downloading tasks failed. " +
                                "Repeat after ${delay}ms. Error: ${e.message}"
                    )
                },
                tag = "TasksDownload"
            )

            // Обновляем прогресс
            updateProgress { progress ->
                progress.copy(
                    tasksDownloaded = tasksDownloadedCount,
                    currentOperation = "Загрузка товаров с сервера"
                )
            }

            // Синхронизация товаров
            val productsDownloadedCount = retryStrategy.executeWithRetry(
                operation = {
                    val result = productUseCases.syncProductsWithServer()
                    result.getOrThrow()
                },
                shouldRetry = { e -> NetworkErrorClassifier.isRetryable(e) },
                onError = { e, attempt, delay ->
                    updateProgress { progress ->
                        progress.copy(
                            errorCount = progress.errorCount + 1,
                            lastErrorMessage = "Ошибка загрузки товаров: ${e.message}"
                        )
                    }

                    loggingService.logWarning(
                        "Attempt $attempt products sync failed. " +
                                "Repeat after ${delay}ms. Error: ${e.message}"
                    )
                },
                tag = "ProductsSync"
            )

            updateProgress { progress ->
                progress.copy(
                    currentOperation = "Загрузка типов заданий"
                )
            }

            // Синхронизация типов заданий
            val taskTypesDownloadedCount = retryStrategy.executeWithRetry(
                operation = {
                    val result = taskTypeUseCases.syncTaskTypes()
                    result.getOrThrow()
                },
                shouldRetry = { e -> NetworkErrorClassifier.isRetryable(e) },
                onError = { e, attempt, delay ->
                    updateProgress { progress ->
                        progress.copy(
                            errorCount = progress.errorCount + 1,
                            lastErrorMessage = "Ошибка загрузки типов заданий: ${e.message}"
                        )
                    }

                    loggingService.logWarning(
                        "Attempt $attempt task types sync failed. " +
                                "Repeat after ${delay}ms. Error: ${e.message}"
                    )
                },
                tag = "TaskTypesSync"
            )

            // Вычисляем время выполнения
            val durationMillis = System.currentTimeMillis() - startTime

            // Обновляем финальный прогресс
            updateProgress { progress ->
                progress.copy(
                    status = SyncStatus.COMPLETED,
                    endTime = LocalDateTime.now(),
                    tasksUploaded = uploadedCount,
                    tasksDownloaded = tasksDownloadedCount,
                    productsDownloaded = productsDownloadedCount,
                    taskTypesDownloaded = taskTypesDownloadedCount,
                    progressPercent = 100,
                    currentOperation = "Синхронизация завершена"
                )
            }

            // Сохраняем запись в историю синхронизаций
            saveSyncHistoryRecord(
                id = syncId,
                startTime = _syncProgressFlow.value.startTime,
                endTime = LocalDateTime.now(),
                duration = durationMillis,
                tasksUploaded = uploadedCount,
                tasksDownloaded = tasksDownloadedCount,
                productsDownloaded = productsDownloadedCount,
                taskTypesDownloaded = taskTypesDownloadedCount,
                successful = true
            )

            // Подготавливаем результат
            val syncResult = SynchronizationController.SyncResult(
                successful = true,
                tasksUploadedCount = uploadedCount,
                tasksDownloadedCount = tasksDownloadedCount,
                productsDownloadedCount = productsDownloadedCount,
                durationMillis = durationMillis,
                errorMessage = null
            )

            // Сохраняем информацию о синхронизации
            saveSyncInfo(syncResult)

            loggingService.logInfo(
                "Полная синхронизация завершена успешно. " +
                        "Выгружено заданий: $uploadedCount, " +
                        "загружено заданий: $tasksDownloadedCount, " +
                        "загружено товаров: $productsDownloadedCount. " +
                        "Время: ${durationMillis}мс"
            )

            return syncResult
        } catch (e: Exception) {
            // Вычисляем время выполнения
            val durationMillis = System.currentTimeMillis() - startTime

            // Обновляем прогресс с ошибкой
            updateProgress { progress ->
                progress.copy(
                    status = SyncStatus.FAILED,
                    endTime = LocalDateTime.now(),
                    errorCount = progress.errorCount + 1,
                    lastErrorMessage = e.message,
                    currentOperation = "Синхронизация не удалась"
                )
            }

            // Сохраняем запись в историю синхронизаций
            saveSyncHistoryRecord(
                id = syncId,
                startTime = _syncProgressFlow.value.startTime,
                endTime = LocalDateTime.now(),
                duration = durationMillis,
                tasksUploaded = _syncProgressFlow.value.tasksUploaded,
                tasksDownloaded = _syncProgressFlow.value.tasksDownloaded,
                productsDownloaded = _syncProgressFlow.value.productsDownloaded,
                taskTypesDownloaded = _syncProgressFlow.value.taskTypesDownloaded,
                successful = false,
                errorMessage = e.message
            )

            // Подготавливаем результат с ошибкой
            val syncResult = SynchronizationController.SyncResult(
                successful = false,
                tasksUploadedCount = _syncProgressFlow.value.tasksUploaded,
                tasksDownloadedCount = _syncProgressFlow.value.tasksDownloaded,
                productsDownloadedCount = _syncProgressFlow.value.productsDownloaded,
                durationMillis = durationMillis,
                errorMessage = e.message
            )

            // Сохраняем информацию о синхронизации
            saveSyncInfo(syncResult)

            loggingService.logError("Full sync error: ${e.message}")

            return syncResult
        }
    }

    private suspend fun saveSyncHistoryRecord(
        id: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        duration: Long,
        tasksUploaded: Int,
        tasksDownloaded: Int,
        productsDownloaded: Int,
        taskTypesDownloaded: Int,
        successful: Boolean,
        errorMessage: String? = null
    ) {
        try {
            // Определяем тип сети
            val networkState = networkMonitor.networkState.first()
            val networkType = when (networkState) {
                is NetworkState.Available -> networkState.type.name
                else -> "UNAVAILABLE"
            }
            val meteredConnection = when (networkState) {
                is NetworkState.Available -> networkState.isMetered
                else -> false
            }

            // Создаем запись истории
            val record = SyncHistoryRecord(
                id = id,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                networkType = networkType,
                meteredConnection = meteredConnection,
                tasksUploaded = tasksUploaded,
                tasksDownloaded = tasksDownloaded,
                productsDownloaded = productsDownloaded,
                taskTypesDownloaded = taskTypesDownloaded,
                successful = successful,
                errorMessage = errorMessage,
                retryAttempts = _syncProgressFlow.value.errorCount,
                totalOperations = tasksUploaded + tasksDownloaded + productsDownloaded
            )

            // Сохраняем в базу данных
            syncHistoryDao.insertHistory(record)
        } catch (e: Exception) {
            Timber.e(e, "Error in saving sync history")
        }
    }

    // Добавляем метод для получения истории синхронизаций
    override fun getSyncHistory(): Flow<List<SyncHistoryRecord>> {
        return syncHistoryDao.getAllHistory()
    }

    // Добавление нового метода для синхронизации только типов заданий
    override suspend fun syncTaskTypes(): Result<Int> {
        // Если синхронизация уже идет, возвращаем ошибку
        if (_syncStatus.value == SynchronizationController.SyncStatus.SYNCING) {
            return Result.failure(IllegalStateException("Syncing is already in process"))
        }

        // Если нет сети, добавляем операцию в очередь
        if (!networkMonitor.isNetworkAvailable()) {
            Timber.d("No network, adding operation DOWNLOAD_TASK_TYPES to queue")
            val operationId = syncQueueManager.enqueueOperation(
                operationType = OperationType.DOWNLOAD_TASK_TYPES,
                targetId = "task-types-sync-${System.currentTimeMillis()}",
                executeImmediately = false
            )

            loggingService.logInfo("Task types sync is enqueued and will proceed when network appears")

            return Result.failure(
                NoNetworkException("No network connection. Sync will start when network appears.")
            )
        }

        _syncStatus.value = SynchronizationController.SyncStatus.SYNCING

        try {
            // Обновляем прогресс
            updateProgress { progress ->
                progress.copy(
                    currentOperation = "Загрузка типов заданий"
                )
            }

            // Синхронизация типов заданий
            val taskTypesCount = retryStrategy.executeWithRetry(
                operation = {
                    val result = taskTypeUseCases.syncTaskTypes()
                    result.getOrThrow()
                },
                shouldRetry = { e -> NetworkErrorClassifier.isRetryable(e) },
                onError = { e, attempt, delay ->
                    updateProgress { progress ->
                        progress.copy(
                            errorCount = progress.errorCount + 1,
                            lastErrorMessage = "Ошибка загрузки типов заданий: ${e.message}"
                        )
                    }

                    loggingService.logWarning(
                        "Attempt $attempt task types sync failed. " +
                                "Repeat after ${delay}ms. Error: ${e.message}"
                    )
                },
                tag = "TaskTypesSync"
            )

            // Обновляем финальный прогресс
            updateProgress { progress ->
                progress.copy(
                    status = SyncStatus.COMPLETED,
                    endTime = LocalDateTime.now(),
                    currentOperation = "Синхронизация типов заданий завершена"
                )
            }

            _syncStatus.value = SynchronizationController.SyncStatus.IDLE

            // Подготавливаем результат
            val syncResult = SynchronizationController.SyncResult(
                successful = true,
                taskTypesDownloadedCount = taskTypesCount,
                tasksUploadedCount = 0,
                tasksDownloadedCount = 0,
                productsDownloadedCount = 0,
                durationMillis = 0,
                errorMessage = null
            )

            loggingService.logInfo("Синхронизация типов заданий завершена успешно: $taskTypesCount")

            return Result.success(taskTypesCount)
        } catch (e: Exception) {
            _syncStatus.value = SynchronizationController.SyncStatus.ERROR

            // Обновляем прогресс с ошибкой
            updateProgress { progress ->
                progress.copy(
                    status = SyncStatus.FAILED,
                    endTime = LocalDateTime.now(),
                    errorCount = progress.errorCount + 1,
                    lastErrorMessage = e.message,
                    currentOperation = "Синхронизация типов заданий не удалась"
                )
            }

            loggingService.logError("Task types sync error: ${e.message}")

            return Result.failure(e)
        }
    }

    class NoNetworkException(message: String) : Exception(message)

    companion object {
        private const val DEFAULT_SYNC_INTERVAL_SECONDS = 300 // 5 минут
        private const val WORK_NAME_PERIODIC_SYNC = "periodic_sync_work"
        private const val CHECK_QUEUE_INTERVAL_MS = 30_000L // 30 секунд
    }
}