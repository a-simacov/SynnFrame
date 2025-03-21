// File: com.synngate.synnframe.data.service.SynchronizationControllerImpl.kt

package com.synngate.synnframe.data.service

import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.local.database.AppDatabase
import com.synngate.synnframe.data.local.entity.OperationType
import com.synngate.synnframe.data.sync.SyncHistoryRecord
import com.synngate.synnframe.data.sync.SyncProgress
import com.synngate.synnframe.data.sync.SyncStatus
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.presentation.service.base.BaseForegroundService
import com.synngate.synnframe.presentation.service.sync.SynchronizationService
import com.synngate.synnframe.presentation.service.sync.SynchronizationWorker
import com.synngate.synnframe.util.network.NetworkErrorClassifier
import com.synngate.synnframe.util.network.NetworkMonitor
import com.synngate.synnframe.util.network.NetworkState
import com.synngate.synnframe.util.network.RetryStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // Добавляем новое поле для прогресса
    private val _syncProgressFlow = MutableStateFlow<SyncProgress>(SyncProgress())
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

    // Формат даты для отображения в уведомлении
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    private val retryStrategy = RetryStrategy(
        maxAttempts = 5,
        initialDelayMs = 1000,
        maxDelayMs = 60000
    )

    private val networkMonitor: NetworkMonitor
    private val syncQueueManager: SyncQueueManager

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

        // Инициализация NetworkMonitor
        networkMonitor = NetworkMonitor(context)
        networkMonitor.initialize()

        // Инициализация SyncQueueManager
        syncQueueManager = SyncQueueManager(
            appDatabase.syncOperationDao(),
            networkMonitor,
            loggingService
        )

        // Настраиваем наблюдение за состоянием сети
        coroutineScope.launch {
            networkMonitor.networkState.collect { state ->
                Timber.d("Изменение состояния сети: $state")

                // Если сеть стала доступна, запускаем обработку ожидающих операций
                if (state is NetworkState.Available) {
                    Timber.d("Сеть доступна, обрабатываем отложенные операции")
                    processQueuedOperations()
                }
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

    /**
     * Модифицированная версия startManualSync, использующая очередь
     */
    override suspend fun startManualSync(): Result<SynchronizationController.SyncResult> {
        // Если синхронизация уже идет, возвращаем ошибку
        if (_syncStatus.value == SynchronizationController.SyncStatus.SYNCING) {
            return Result.failure(IllegalStateException("Синхронизация уже выполняется"))
        }

        // Если нет сети, добавляем операцию в очередь
        if (!networkMonitor.isNetworkAvailable()) {
            Timber.d("Нет сети, добавляем операцию FULL_SYNC в очередь")
            val operationId = syncQueueManager.enqueueOperation(
                operationType = OperationType.FULL_SYNC,
                targetId = "manual-sync-${System.currentTimeMillis()}",
                executeImmediately = false
            )

            loggingService.logInfo("Синхронизация поставлена в очередь и будет выполнена при появлении сети")

            return Result.failure(
                NoNetworkException("Нет подключения к сети. Синхронизация будет выполнена при появлении сети.")
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

    /**
     * Обработка отложенных операций синхронизации
     */
    private suspend fun processQueuedOperations() {
        // Если синхронизация уже идет или сервис не запущен, пропускаем
        if (_syncStatus.value == SynchronizationController.SyncStatus.SYNCING ||
            !_isRunning.value) {
            return
        }

        // Если сеть недоступна, пропускаем
        if (!networkMonitor.isNetworkAvailable()) {
            Timber.d("Сеть недоступна, пропускаем обработку операций")
            return
        }

        // Получаем операции, готовые для выполнения
        val operations = syncQueueManager.getReadyOperations()
        if (operations.isEmpty()) {
            Timber.d("Нет операций, готовых для выполнения")
            return
        }

        Timber.d("Начинаем обработку ${operations.size} операций")
        _syncStatus.value = SynchronizationController.SyncStatus.SYNCING

        try {
            for (operation in operations) {
                // Проверяем, не потеряли ли мы сеть во время обработки
                if (!networkMonitor.isNetworkAvailable()) {
                    Timber.d("Сеть стала недоступна во время обработки операций")
                    break
                }

                Timber.d("Обработка операции ${operation.id}: ${operation.operationType}")

                try {
                    when (operation.operationType) {
                        OperationType.UPLOAD_TASK -> {
                            // Выгрузка задания
                            val result = taskUseCases.uploadTask(operation.targetId)
                            if (result.isSuccess) {
                                syncQueueManager.markOperationCompleted(operation.id)
                            } else {
                                val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                                syncQueueManager.handleFailedAttempt(operation.id, error)
                            }
                        }

                        OperationType.DOWNLOAD_TASKS -> {
                            // Загрузка заданий
                            val result = taskUseCases.syncTasks()
                            if (result.isSuccess) {
                                syncQueueManager.markOperationCompleted(operation.id)
                            } else {
                                val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                                syncQueueManager.handleFailedAttempt(operation.id, error)
                            }
                        }

                        OperationType.DOWNLOAD_PRODUCTS -> {
                            // Загрузка товаров
                            val result = productUseCases.syncProductsWithServer()
                            if (result.isSuccess) {
                                syncQueueManager.markOperationCompleted(operation.id)
                            } else {
                                val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                                syncQueueManager.handleFailedAttempt(operation.id, error)
                            }
                        }

                        OperationType.FULL_SYNC -> {
                            // Полная синхронизация
                            val syncResult = performFullSync()
                            if (syncResult.successful) {
                                syncQueueManager.markOperationCompleted(operation.id)
                            } else {
                                syncQueueManager.handleFailedAttempt(operation.id, syncResult.errorMessage ?: "Ошибка синхронизации")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при обработке операции ${operation.id}")
                    syncQueueManager.handleFailedAttempt(operation.id, e.message ?: "Неизвестная ошибка")
                }
            }
        } finally {
            _syncStatus.value = SynchronizationController.SyncStatus.IDLE
        }
    }

    // Метод для обновления прогресса
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
                putExtra(SynchronizationService.EXTRA_PROGRESS, progress)
            }
            context.startService(intent)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка отправки обновления прогресса в сервис")
        }
    }

    /**
     * Выполнение полной синхронизации с использованием стратегии повторных попыток
     */
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
                        "Попытка $attempt загрузки заданий не удалась. " +
                                "Повтор через ${delay}мс. Ошибка: ${e.message}"
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
                        "Попытка $attempt синхронизации товаров не удалась. " +
                                "Повтор через ${delay}мс. Ошибка: ${e.message}"
                    )
                },
                tag = "ProductsSync"
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

            loggingService.logError("Ошибка полной синхронизации: ${e.message}")

            return syncResult
        }
    }

    // Метод для сохранения записи в историю синхронизаций
    private suspend fun saveSyncHistoryRecord(
        id: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        duration: Long,
        tasksUploaded: Int,
        tasksDownloaded: Int,
        productsDownloaded: Int,
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
                successful = successful,
                errorMessage = errorMessage,
                retryAttempts = _syncProgressFlow.value.errorCount,
                totalOperations = tasksUploaded + tasksDownloaded + productsDownloaded
            )

            // Сохраняем в базу данных
            syncHistoryDao.insertHistory(record)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сохранения истории синхронизации")
        }
    }

    // Добавляем метод для получения истории синхронизаций
    override fun getSyncHistory(): Flow<List<SyncHistoryRecord>> {
        return syncHistoryDao.getAllHistory()
    }

    /**
     * Адаптивная выгрузка задания с учетом типа сети
     *
     * @param taskId Идентификатор задания
     * @param forceUpload Флаг принудительной выгрузки
     * @return Результат операции (true - выгружено, false - поставлено в очередь)
     */
    suspend fun uploadTask(taskId: String, forceUpload: Boolean = false): Result<Boolean> {
        // Если нет сети, добавляем в очередь и возвращаем результат
        if (!networkMonitor.isNetworkAvailable() && !forceUpload) {
            Timber.d("Нет сети, добавляем операцию UPLOAD_TASK в очередь для задания $taskId")
            syncQueueManager.enqueueOperation(
                operationType = OperationType.UPLOAD_TASK,
                targetId = taskId
            )
            return Result.success(false)
        }

        // Проверка политики выгрузки по типу сети
        if (!forceUpload && networkMonitor.isCellularAvailable() && networkMonitor.isMeteredConnection()) {
            // Получаем настройки мобильной выгрузки с безопасными значениями по умолчанию
            val allowMobileUpload = try {
                appSettingsDataStore.allowMobileUpload.first()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка получения настройки allowMobileUpload")
                false  // По умолчанию - не разрешать
            }

            val mobileSizeLimit = try {
                appSettingsDataStore.mobileSizeLimit.first()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка получения настройки mobileSizeLimit")
                500_000  // По умолчанию 500 KB
            }

            if (!allowMobileUpload) {
                Timber.d("Мобильная сеть, выгрузка не разрешена, добавляем в очередь")
                syncQueueManager.enqueueOperation(
                    operationType = OperationType.UPLOAD_TASK,
                    targetId = taskId
                )
                return Result.success(false)
            }

            // Проверяем размер задания для мобильной сети
            val taskSize = estimateTaskSize(taskId)
            if (taskSize > mobileSizeLimit) {
                Timber.d("Задание слишком большое для мобильной сети ($taskSize байт), добавляем в очередь")
                syncQueueManager.enqueueOperation(
                    operationType = OperationType.UPLOAD_TASK,
                    targetId = taskId
                )
                return Result.success(false)
            }
        }

        // Выполняем выгрузку задания с повторными попытками
        return try {
            val result = retryStrategy.executeWithRetry(
                operation = {
                    val uploadResult = taskUseCases.uploadTask(taskId)
                    uploadResult.getOrThrow()
                },
                shouldRetry = { e -> NetworkErrorClassifier.isRetryable(e) },
                onError = { e, attempt, delay ->
                    loggingService.logWarning(
                        "Попытка $attempt выгрузки задания $taskId не удалась. " +
                                "Повтор через ${delay}мс. Ошибка: ${e.message}"
                    )
                },
                tag = "TaskUpload"
            )

            loggingService.logInfo("Задание $taskId успешно выгружено")
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при выгрузке задания $taskId")

            // Добавляем в очередь, если ошибка временная
            if (NetworkErrorClassifier.isRetryable(e)) {
                syncQueueManager.enqueueOperation(
                    operationType = OperationType.UPLOAD_TASK,
                    targetId = taskId
                )
            }

            Result.failure(e)
        }
    }

    /**
     * Приблизительная оценка размера задания в байтах
     */
    private suspend fun estimateTaskSize(taskId: String): Int {
        val task = taskUseCases.getTaskById(taskId) ?: return 0

        // Базовый размер задания
        var size = 200  // Постоянные поля

        // Добавляем размер строковых полей
        size += (task.name.length + task.barcode.length) * 2  // UTF-8 ~ 2 байта на символ

        // Добавляем размер для строк плана
        size += task.planLines.size * 100  // ~100 байт на строку плана

        // Добавляем размер для строк факта
        size += task.factLines.size * 100  // ~100 байт на строку факта

        return size
    }

    /**
     * Специальный класс для исключения "Нет сети"
     */
    class NoNetworkException(message: String) : Exception(message)

    companion object {
        private const val DEFAULT_SYNC_INTERVAL_SECONDS = 300 // 5 минут
        private const val WORK_NAME_PERIODIC_SYNC = "periodic_sync_work"
    }
}