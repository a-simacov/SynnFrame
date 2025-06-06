package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.synngate.synnframe.data.local.database.RoomConverters
import java.time.LocalDateTime

@Entity(tableName = "sync_operations")
@TypeConverters(RoomConverters::class)
data class SyncOperation(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val operationType: OperationType,

    val targetId: String,

    val priority: Int,

    val attempts: Int = 0,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    val lastAttemptAt: LocalDateTime? = null,

    val nextAttemptAt: LocalDateTime? = null,

    val completed: Boolean = false,

    val lastError: String? = null
)

enum class OperationType {
    DOWNLOAD_PRODUCTS,
    FULL_SYNC
}