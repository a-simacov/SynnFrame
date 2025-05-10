package com.synngate.synnframe.data.sync

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.synngate.synnframe.data.local.database.RoomConverters
import java.time.LocalDateTime

@Entity(tableName = "sync_history")
@TypeConverters(RoomConverters::class)
data class SyncHistoryRecord(
    @PrimaryKey
    val id: String,

    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val duration: Long,  // в миллисекундах

    val networkType: String,
    val meteredConnection: Boolean,

    val productsDownloaded: Int,

    val successful: Boolean,
    val errorMessage: String? = null,

    val retryAttempts: Int = 0,
    val totalOperations: Int = 0
)