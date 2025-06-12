package com.synngate.synnframe.data.sync

import java.time.LocalDateTime

data class SyncProgress(
    val id: String = System.currentTimeMillis().toString(),
    val startTime: LocalDateTime = LocalDateTime.now(),
    val endTime: LocalDateTime? = null,
    val status: SyncStatus = SyncStatus.STARTED,

    val productsDownloaded: Int = 0,

    val currentOperation: String = "",

    val progressPercent: Int = 0,

    val errorCount: Int = 0,
    val lastErrorMessage: String? = null
) {

    fun calculateOverallProgress(): Int {
        // Если синхронизация завершена или в ошибке, возвращаем конечные значения
        if (status == SyncStatus.COMPLETED) return 100
        if (status == SyncStatus.FAILED) return 0

        // Примерно оцениваем прогресс загрузки (по 25% на загрузку заданий и товаров)
        val downloadProgress = when {
            productsDownloaded > 0 -> 100f  // Только товары загружены
            else -> 0f  // Ничего не загружено
        }

        return downloadProgress.toInt()
    }

    fun getProgressMessage(): String {
        return when (status) {
            SyncStatus.STARTED -> "Synchronization started"
            SyncStatus.IN_PROGRESS -> "Synchronization: "
            SyncStatus.COMPLETED -> "Synchronization completed successfully"
            SyncStatus.FAILED -> "Synchronization failed: ${lastErrorMessage ?: "unknown error"}"
        }
    }
}

enum class SyncStatus {
    STARTED,     // Синхронизация начата
    IN_PROGRESS, // Синхронизация в процессе
    COMPLETED,   // Синхронизация завершена успешно
    FAILED       // Синхронизация не удалась
}