// Файл: com.synngate.synnframe.data.sync.RetrySettings.kt

package com.synngate.synnframe.data.sync

data class RetrySettings(
    val maxAttempts: Int,
    val initialDelaySeconds: Long,
    val maxDelaySeconds: Long,
    val backoffFactor: Double
) {

    fun toRetryStrategy(): RetryStrategy {
        return RetryStrategy(
            maxAttempts = maxAttempts,
            initialDelaySeconds = initialDelaySeconds,
            maxDelaySeconds = maxDelaySeconds,
            backoffFactor = backoffFactor
        )
    }

    companion object {
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