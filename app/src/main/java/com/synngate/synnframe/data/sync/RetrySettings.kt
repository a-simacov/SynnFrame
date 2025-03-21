// Файл: com.synngate.synnframe.data.sync.RetrySettings.kt

package com.synngate.synnframe.data.sync

/**
 * Настройки повторных попыток для отображения и редактирования в UI
 */
data class RetrySettings(
    val maxAttempts: Int,
    val initialDelaySeconds: Long,
    val maxDelaySeconds: Long,
    val backoffFactor: Double
) {
    /**
     * Преобразование в стратегию повторных попыток
     */
    fun toRetryStrategy(): RetryStrategy {
        return RetryStrategy(
            maxAttempts = maxAttempts,
            initialDelaySeconds = initialDelaySeconds,
            maxDelaySeconds = maxDelaySeconds,
            backoffFactor = backoffFactor
        )
    }

    companion object {
        /**
         * Создание из стратегии повторных попыток
         */
        fun fromRetryStrategy(strategy: RetryStrategy): RetrySettings {
            return RetrySettings(
                maxAttempts = strategy.maxAttempts,
                initialDelaySeconds = strategy.initialDelaySeconds,
                maxDelaySeconds = strategy.maxDelaySeconds,
                backoffFactor = strategy.backoffFactor
            )
        }
    }
}