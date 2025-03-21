package com.synngate.synnframe.data.sync

import java.time.LocalDateTime

/**
 * Модель для отслеживания прогресса синхронизации
 */
data class SyncProgress(
    // Общая информация
    val id: String = System.currentTimeMillis().toString(),
    val startTime: LocalDateTime = LocalDateTime.now(),
    val endTime: LocalDateTime? = null,
    val status: SyncStatus = SyncStatus.STARTED,

    // Прогресс выгрузки заданий
    val tasksToUpload: Int = 0,
    val tasksUploaded: Int = 0,

    // Прогресс загрузки заданий
    val tasksDownloaded: Int = 0,

    // Прогресс загрузки товаров
    val productsDownloaded: Int = 0,

    // Текущая операция
    val currentOperation: String = "",

    // Общий прогресс в процентах (0-100)
    val progressPercent: Int = 0,

    // Информация об ошибках
    val errorCount: Int = 0,
    val lastErrorMessage: String? = null
) {
    /**
     * Вычисление общего прогресса на основе выполненных операций
     */
    fun calculateOverallProgress(): Int {
        // Если синхронизация завершена или в ошибке, возвращаем конечные значения
        if (status == SyncStatus.COMPLETED) return 100
        if (status == SyncStatus.FAILED) return 0

        // Если нет операций, возвращаем начальный прогресс
        if (tasksToUpload == 0) return if (currentOperation.isEmpty()) 0 else 10

        // Вычисляем прогресс выгрузки заданий (50% от общего)
        val uploadProgress = if (tasksToUpload > 0) {
            (tasksUploaded.toFloat() / tasksToUpload) * 50
        } else 0f

        // Примерно оцениваем прогресс загрузки (по 25% на загрузку заданий и товаров)
        val downloadProgress = when {
            tasksDownloaded > 0 && productsDownloaded > 0 -> 50f  // Обе операции выполнены
            tasksDownloaded > 0 -> 25f  // Только задания загружены
            productsDownloaded > 0 -> 25f  // Только товары загружены
            else -> 0f  // Ничего не загружено
        }

        return (uploadProgress + downloadProgress).toInt()
    }

    /**
     * Получение информативного сообщения о текущем прогрессе
     */
    fun getProgressMessage(): String {
        return when (status) {
            SyncStatus.STARTED -> "Синхронизация начата"
            SyncStatus.IN_PROGRESS -> "Синхронизация: $currentOperation" +
                    if (tasksToUpload > 0) " (${tasksUploaded}/${tasksToUpload})" else ""
            SyncStatus.COMPLETED -> "Синхронизация завершена успешно"
            SyncStatus.FAILED -> "Синхронизация не удалась: ${lastErrorMessage ?: "неизвестная ошибка"}"
        }
    }
}

/**
 * Статусы процесса синхронизации
 */
enum class SyncStatus {
    STARTED,     // Синхронизация начата
    IN_PROGRESS, // Синхронизация в процессе
    COMPLETED,   // Синхронизация завершена успешно
    FAILED       // Синхронизация не удалась
}