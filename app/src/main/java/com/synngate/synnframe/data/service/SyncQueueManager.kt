package com.synngate.synnframe.data.service

import com.synngate.synnframe.data.local.dao.SyncOperationDao
import com.synngate.synnframe.data.local.entity.OperationType
import com.synngate.synnframe.data.local.entity.SyncOperation
import com.synngate.synnframe.data.sync.RetryStrategy
import com.synngate.synnframe.util.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime

class SyncQueueManager(
    private val syncOperationDao: SyncOperationDao,
    private val networkMonitor: NetworkMonitor,
    private val defaultStrategy: RetryStrategy = RetryStrategy.NORMAL
) {
    // Scope для запуска корутин
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun enqueueOperation(
        operationType: OperationType,
        targetId: String,
        priority: Int = getPriorityForType(operationType),
        executeImmediately: Boolean = false
    ): Long {
        // Проверяем, нет ли уже такой операции в очереди
        val existingOperations = syncOperationDao.getOperationsForTarget(targetId)
        val alreadyEnqueued = existingOperations.any {
            !it.completed && it.operationType == operationType
        }

        if (alreadyEnqueued) {
            Timber.d("Операция типа $operationType для $targetId уже в очереди")
            return existingOperations.first {
                !it.completed && it.operationType == operationType
            }.id
        }

        // Создаем и добавляем новую операцию
        val operation = SyncOperation(
            operationType = operationType,
            targetId = targetId,
            priority = priority,
            attempts = 0,
            createdAt = LocalDateTime.now(),
            completed = false
        )

        val id = syncOperationDao.insertOperation(operation)
        Timber.d("Добавлена операция в очередь: $operationType, $targetId, ID=$id")

        // Если нужно выполнить немедленно и есть сеть, запускаем процесс синхронизации
        if (executeImmediately && networkMonitor.isNetworkAvailable()) {
            scope.launch {
                Timber.d("Выполнение новой операции сразу: $id")
                // В полной реализации это будет вызов synchronizationController.processQueuedOperations()
            }
        }

        return id
    }

    private fun getPriorityForType(operationType: OperationType): Int {
        return when (operationType) {
            OperationType.UPLOAD_TASK -> 1     // Наивысший приоритет
            OperationType.DOWNLOAD_TASKS -> 2
            OperationType.DOWNLOAD_TASK_TYPES -> 1
            OperationType.DOWNLOAD_PRODUCTS -> 3
            OperationType.FULL_SYNC -> 4       // Наименьший приоритет
        }
    }

    suspend fun markOperationCompleted(id: Long) {
        syncOperationDao.markOperationAsCompleted(id)
        Timber.i("Sync operation $id finished successfully")
    }

    suspend fun handleFailedAttempt(id: Long, error: String): Boolean {
        val operation = syncOperationDao.getOperationById(id)
            ?: return false

        val newAttempts = operation.attempts + 1
        val now = LocalDateTime.now()

        // Увеличиваем счетчик попыток и устанавливаем время последней попытки
        syncOperationDao.incrementAttempts(id, now)

        // Устанавливаем текст ошибки
        syncOperationDao.setOperationError(id, error)

        // Получаем стратегию для данного типа операции
        val strategy = RetryStrategy.forOperationType(operation.operationType)

        // Если превышено максимальное количество попыток, отмечаем операцию как завершенную (с ошибкой)
        if (newAttempts >= strategy.maxAttempts) {
            syncOperationDao.markOperationAsCompleted(id)
            Timber.w("Sync operation $id failed after ${strategy.maxAttempts} attempts: $error")
            return false
        }

        // Получаем текущее состояние сети для адаптации задержки - используем существующий метод
        val networkState = networkMonitor.getCurrentNetworkState()

        // Рассчитываем задержку с учетом состояния сети
        val delaySeconds = strategy.calculateDelay(newAttempts, networkState)

        // Вычисляем время следующей попытки
        val nextAttemptTime = now.plusSeconds(delaySeconds)

        // Устанавливаем время следующей попытки
        syncOperationDao.setNextAttemptTime(id, nextAttemptTime)

        // Записываем событие в лог с подробной информацией
        Timber.w(
            "Sync operation $id (${operation.operationType}) failed (attempt $newAttempts/${strategy.maxAttempts}). " +
                    "Next attempt at $nextAttemptTime (delay ${delaySeconds}sec). " +
                    "Network state: ${networkState.javaClass.simpleName}. " +
                    "Error: $error"
        )

        return true
    }

    suspend fun getReadyOperations(): List<SyncOperation> {
        return syncOperationDao.getReadyOperations()
    }
}