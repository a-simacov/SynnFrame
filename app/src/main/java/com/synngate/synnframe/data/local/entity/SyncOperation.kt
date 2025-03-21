package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.synngate.synnframe.data.local.database.RoomConverters
import java.time.LocalDateTime

/**
 * Очередь операций синхронизации
 */
@Entity(tableName = "sync_operations")
@TypeConverters(RoomConverters::class)
data class SyncOperation(
    /**
     * Уникальный идентификатор операции
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Тип операции
     */
    val operationType: OperationType,

    /**
     * Связанные данные (например, ID задания)
     */
    val targetId: String,

    /**
     * Приоритет операции (меньшее значение - выше приоритет)
     */
    val priority: Int,

    /**
     * Количество попыток
     */
    val attempts: Int = 0,

    /**
     * Время создания операции
     */
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Время последней попытки
     */
    val lastAttemptAt: LocalDateTime? = null,

    /**
     * Следующее запланированное время попытки
     */
    val nextAttemptAt: LocalDateTime? = null,

    /**
     * Признак выполнения операции
     */
    val completed: Boolean = false,

    /**
     * Последняя ошибка (если была)
     */
    val lastError: String? = null
)

/**
 * Типы операций синхронизации
 */
enum class OperationType {
    /**
     * Выгрузка задания
     */
    UPLOAD_TASK,

    /**
     * Загрузка заданий
     */
    DOWNLOAD_TASKS,

    /**
     * Загрузка товаров
     */
    DOWNLOAD_PRODUCTS,

    /**
     * Полная синхронизация
     */
    FULL_SYNC
}