package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.sync.SyncHistoryRecord
import com.synngate.synnframe.data.sync.SyncProgress
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface SynchronizationController : ServiceController {

    val syncStatus: Flow<SyncStatus>

    val lastSyncInfo: Flow<SyncInfo?>

    val periodicSyncInfo: Flow<PeriodicSyncInfo>

    val syncProgressFlow: Flow<SyncProgress>

    fun getSyncHistory(): Flow<List<SyncHistoryRecord>>

    suspend fun startManualSync(): Result<SyncResult>

    suspend fun updatePeriodicSync(enabled: Boolean, intervalSeconds: Int? = null): Result<Unit>

    suspend fun updateLastProductsSync(productsCount: Int)

    enum class SyncStatus {
        IDLE,
        SYNCING,
        ERROR
    }

    data class SyncInfo(
        val timestamp: LocalDateTime,
        val productsDownloadedCount: Int,
        val durationMillis: Long,
        val successful: Boolean,
        val errorMessage: String? = null
    )

    data class PeriodicSyncInfo(
        val enabled: Boolean,
        val intervalSeconds: Int,
        val nextScheduledSync: LocalDateTime?
    )

    data class SyncResult(
        val successful: Boolean,
        val productsDownloadedCount: Int,
        val durationMillis: Long,
        val errorMessage: String? = null
    )
}