package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synngate.synnframe.domain.entity.Log
import com.synngate.synnframe.domain.entity.LogType
import java.time.LocalDateTime

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val message: String,
    val type: String,
    val createdAt: LocalDateTime
) {

    fun toDomainModel(): Log {
        return Log(
            id = id,
            message = message,
            type = LogType.fromString(type),
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomainModel(log: Log): LogEntity {
            return LogEntity(
                id = log.id,
                message = log.message,
                type = log.type.name,
                createdAt = log.createdAt
            )
        }
    }
}