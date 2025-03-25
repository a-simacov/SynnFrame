// Файл: com.synngate.synnframe.data.sync.RetryStrategy.kt

package com.synngate.synnframe.data.sync

import com.synngate.synnframe.data.local.entity.OperationType
import com.synngate.synnframe.util.network.NetworkState
import com.synngate.synnframe.util.network.ConnectionType
import kotlin.math.pow

/**
 * Стратегия повторных попыток для операций синхронизации
 */
class RetryStrategy(
    val maxAttempts: Int,
    val initialDelaySeconds: Long,
    val maxDelaySeconds: Long,
    val backoffFactor: Double
) {
    companion object {
        // Предопределенные стратегии
        val AGGRESSIVE = RetryStrategy(
            maxAttempts = 8,
            initialDelaySeconds = 10L,
            maxDelaySeconds = 600L, // 10 минут
            backoffFactor = 1.5
        )

        val NORMAL = RetryStrategy(
            maxAttempts = 5,
            initialDelaySeconds = 30L,
            maxDelaySeconds = 1800L, // 1 час
            backoffFactor = 2.0
        )

        val CONSERVATIVE = RetryStrategy(
            maxAttempts = 3,
            initialDelaySeconds = 300L,
            maxDelaySeconds = 7200L, // 2 часа
            backoffFactor = 3.0
        )

        /**
         * Получение стратегии по типу операции
         */
        fun forOperationType(operationType: OperationType): RetryStrategy {
            return when (operationType) {
                OperationType.UPLOAD_TASK -> AGGRESSIVE
                OperationType.DOWNLOAD_TASKS -> NORMAL
                OperationType.DOWNLOAD_PRODUCTS -> NORMAL
                OperationType.FULL_SYNC -> CONSERVATIVE
            }
        }
    }

    /**
     * Рассчитать задержку для указанной попытки
     * @param attemptNumber номер попытки (начиная с 1)
     * @param networkState текущее состояние сети (если null, состояние сети не учитывается)
     * @return время задержки в секундах
     */
    fun calculateDelay(attemptNumber: Int, networkState: NetworkState? = null): Long {
        // Базовая задержка по формуле с экспоненциальным ростом
        val baseDelay = (initialDelaySeconds * backoffFactor.pow(attemptNumber - 1.0)).toLong()

        // Ограничиваем максимальной задержкой
        val cappedDelay = minOf(baseDelay, maxDelaySeconds)

        // Если состояние сети не передано, возвращаем обычную задержку
        if (networkState == null) {
            return cappedDelay
        }

        // Корректируем задержку в зависимости от состояния сети
        return when (networkState) {
            is NetworkState.Available -> {
                when (networkState.type) {
                    ConnectionType.WIFI -> cappedDelay // Стабильное соединение
                    ConnectionType.ETHERNET -> (cappedDelay * 0.5).toLong() // Более быстрое соединение
                    ConnectionType.CELLULAR -> (cappedDelay * 1.5).toLong() // Мобильная сеть
                    else -> (cappedDelay * 2.0).toLong() // Другие типы соединения
                }
            }
            is NetworkState.Unavailable ->
                (cappedDelay * 3.0).toLong() // Нет соединения
        }
    }
}