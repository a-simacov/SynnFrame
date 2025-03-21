package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Очередь операций синхронизации
 */
@Entity(tableName = "sync_operations")
@TypeConverters(SyncOperationTypeConverters::class)
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

/**
 * Конвертеры типов для Room
 */
class SyncOperationTypeConverters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }

    @TypeConverter
    fun fromOperationType(value: OperationType): String {
        return value.name
    }

    @TypeConverter
    fun toOperationType(value: String): OperationType {
        return OperationType.valueOf(value)
    }
}