// Файл: com.synngate.synnframe.data.sync.RetrySettings.kt

package com.synngate.synnframe.data.sync

data class RetrySettings(
    val maxAttempts: Int,
    val initialDelaySeconds: Long,
    val maxDelaySeconds: Long,
    val backoffFactor: Double
)