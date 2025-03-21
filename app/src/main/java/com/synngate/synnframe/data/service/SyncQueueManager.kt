package com.synngate.synnframe.data.service

import com.synngate.synnframe.data.local.dao.SyncOperationDao
import com.synngate.synnframe.data.local.entity.OperationType
import com.synngate.synnframe.data.local.entity.SyncOperation
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.util.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import kotlin.math.min
import kotlin.math.pow

/**
 * Менеджер очереди синхронизации
 */
class SyncQueueManager(
    private val syncOperationDao: SyncOperationDao,
    private val networkMonitor: NetworkMonitor,
    private val loggingService: LoggingService
) {
    // Константы для расчета задержки между попытками
    private val maxAttempts = 5
    private val initialDelaySeconds = 60L
    private val maxDelaySeconds = 3600L  // 1 час
    private val backoffFactor = 2.0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Получение потока всех незавершенных операций
     */
    fun getPendingOperations(): Flow<List<SyncOperation>> {
        return syncOperationDao.getPendingOperations()
    }

    /**
     * Добавление операции в очередь
     */
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
                // Здесь будет вызов функции для выполнения синхронизации
                // В полной реализации это будет вызов synchronizationController.processQueuedOperations()
                // Реализуем эту функцию позже
            }
        }

        return id
    }

    /**
     * Получение приоритета для типа операции
     */
    private fun getPriorityForType(operationType: OperationType): Int {
        return when (operationType) {
            OperationType.UPLOAD_TASK -> 1     // Наивысший приоритет
            OperationType.DOWNLOAD_TASKS -> 2
            OperationType.DOWNLOAD_PRODUCTS -> 3
            OperationType.FULL_SYNC -> 4       // Наименьший приоритет
        }
    }

    /**
     * Отметка операции как завершенной
     */
    suspend fun markOperationCompleted(id: Long) {
        syncOperationDao.markOperationAsCompleted(id)
        loggingService.logInfo("Операция синхронизации $id завершена успешно")
    }

    /**
     * Обработка неудачной попытки с увеличением счетчика и расчетом времени следующей попытки
     */
    suspend fun handleFailedAttempt(id: Long, error: String): Boolean {
        val operations = syncOperationDao.getReadyOperations()
        val operation = operations.find { it.id == id } ?: return false

        val newAttempts = operation.attempts + 1
        val now = LocalDateTime.now()

        // Увеличиваем счетчик попыток и устанавливаем время последней попытки
        syncOperationDao.incrementAttempts(id, now)

        // Устанавливаем текст ошибки
        syncOperationDao.setOperationError(id, error)

        // Если превышено максимальное количество попыток, отмечаем операцию как завершенную (с ошибкой)
        if (newAttempts >= maxAttempts) {
            syncOperationDao.markOperationAsCompleted(id)
            loggingService.logWarning("Операция синхронизации $id не удалась после $maxAttempts попыток: $error")
            return false
        }

        // Рассчитываем время следующей попытки с экспоненциальной задержкой
        val delaySeconds = min(
            initialDelaySeconds * backoffFactor.pow(newAttempts - 1).toLong(),
            maxDelaySeconds
        )
        val nextAttemptTime = now.plusSeconds(delaySeconds)

        // Устанавливаем время следующей попытки
        syncOperationDao.setNextAttemptTime(id, nextAttemptTime)

        loggingService.logWarning("Операция синхронизации $id не удалась (попытка $newAttempts). Следующая попытка в $nextAttemptTime. Ошибка: $error")
        return true
    }

    /**
     * Очистка старых завершенных операций
     */
    suspend fun cleanupOldOperations(daysToKeep: Int = 7) {
        val cutoffTime = LocalDateTime.now().minusDays(daysToKeep.toLong())
        syncOperationDao.deleteOldCompletedOperations(cutoffTime)
        Timber.d("Очищены завершенные операции старше $daysToKeep дней")
    }

    /**
     * Получение операций, готовых для выполнения
     */
    suspend fun getReadyOperations(): List<SyncOperation> {
        return syncOperationDao.getReadyOperations()
    }
}