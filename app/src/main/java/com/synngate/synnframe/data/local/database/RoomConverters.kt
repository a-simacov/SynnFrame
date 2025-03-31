package com.synngate.synnframe.data.local.database

import androidx.room.TypeConverter
import com.synngate.synnframe.data.local.entity.OperationType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RoomConverters {

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