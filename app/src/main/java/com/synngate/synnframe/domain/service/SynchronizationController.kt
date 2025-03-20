// File: com.synngate.synnframe.domain.service.SynchronizationController.kt

package com.synngate.synnframe.domain.service

import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Интерфейс для управления сервисом синхронизации
 */
interface SynchronizationController : ServiceController {
    /**
     * Текущий статус синхронизации
     */
    val syncStatus: Flow<SyncStatus>

    /**
     * Информация о последней синхронизации
     */
    val lastSyncInfo: Flow<SyncInfo?>

    /**
     * Информация о процессе периодической синхронизации
     */
    val periodicSyncInfo: Flow<PeriodicSyncInfo>

    /**
     * Запуск ручной синхронизации
     * @return Результат синхронизации
     */
    suspend fun startManualSync(): Result<SyncResult>

    /**
     * Обновление настроек периодической синхронизации
     * @param enabled Включение/выключение периодической синхронизации
     * @param intervalSeconds Интервал синхронизации в секундах
     * @return Результат операции
     */
    suspend fun updatePeriodicSync(enabled: Boolean, intervalSeconds: Int? = null): Result<Unit>

    /**
     * Статус синхронизации
     */
    enum class SyncStatus {
        IDLE,         // Синхронизация не выполняется
        SYNCING,      // Синхронизация в процессе
        ERROR         // Произошла ошибка синхронизации
    }

    /**
     * Информация о синхронизации
     */
    data class SyncInfo(
        val timestamp: LocalDateTime,
        val tasksUploadedCount: Int,
        val tasksDownloadedCount: Int,
        val productsDownloadedCount: Int,
        val durationMillis: Long,
        val successful: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Информация о периодической синхронизации
     */
    data class PeriodicSyncInfo(
        val enabled: Boolean,
        val intervalSeconds: Int,
        val nextScheduledSync: LocalDateTime?
    )

    /**
     * Результат синхронизации
     */
    data class SyncResult(
        val successful: Boolean,
        val tasksUploadedCount: Int,
        val tasksDownloadedCount: Int,
        val productsDownloadedCount: Int,
        val durationMillis: Long,
        val errorMessage: String? = null
    )
}