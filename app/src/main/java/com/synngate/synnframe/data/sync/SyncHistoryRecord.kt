package com.synngate.synnframe.data.sync

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.synngate.synnframe.data.local.database.RoomConverters
import java.time.LocalDateTime

/**
 * Запись об истории синхронизации для хранения в базе данных
 */
@Entity(tableName = "sync_history")
@TypeConverters(RoomConverters::class)
data class SyncHistoryRecord(
    @PrimaryKey
    val id: String,

    // Временные метки
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val duration: Long,  // в миллисекундах

    // Тип сети во время синхронизации
    val networkType: String,
    val meteredConnection: Boolean,

    // Статистика синхронизации
    val tasksUploaded: Int,
    val tasksDownloaded: Int,
    val productsDownloaded: Int,

    // Результат
    val successful: Boolean,
    val errorMessage: String? = null,

    // Дополнительная информация
    val retryAttempts: Int = 0,
    val totalOperations: Int = 0
)