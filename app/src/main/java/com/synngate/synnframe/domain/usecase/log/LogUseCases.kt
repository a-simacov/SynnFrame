// Файл: com.synngate.synnframe.domain.usecase.log.LogUseCases.kt

package com.synngate.synnframe.domain.usecase.log

import com.synngate.synnframe.domain.entity.Log
import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Use Case класс для операций с логами
 */
class LogUseCases(
    private val logRepository: LogRepository,
    private val loggingService: LoggingService
) : BaseUseCase {

    // Базовые операции
    fun getLogs(): Flow<List<Log>> =
        logRepository.getLogs()

    fun getFilteredLogs(
        messageFilter: String? = null,
        typeFilter: List<LogType>? = null,
        dateFromFilter: LocalDateTime? = null,
        dateToFilter: LocalDateTime? = null
    ): Flow<List<Log>> {
        return logRepository.getFilteredLogs(
            messageFilter, typeFilter, dateFromFilter, dateToFilter
        )
    }

    suspend fun getLogById(id: Int): Log? =
        logRepository.getLogById(id)

    // Операции с бизнес-логикой
    suspend fun cleanupOldLogs(daysToKeep: Int = 30): Result<Int> {
        return try {
            val cutoffDate = LocalDateTime.now().minusDays(daysToKeep.toLong())
            val deletedCount = logRepository.deleteLogsOlderThan(cutoffDate)

            logInfo("Удалено логов старше $daysToKeep дней: $deletedCount")
            Result.success(deletedCount)
        } catch (e: Exception) {
            Timber.e(e, "Exception during logs cleanup")
            Result.failure(e)
        }
    }

    suspend fun deleteLog(id: Int): Result<Unit> {
        return try {
            val log = logRepository.getLogById(id)
            if (log == null) {
                return Result.failure(IllegalArgumentException("Log not found: $id"))
            }

            logRepository.deleteLog(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during log deletion")
            Result.failure(e)
        }
    }

    suspend fun deleteAllLogs(): Result<Unit> {
        return try {
            logRepository.deleteAllLogs()
            logInfo("Все логи удалены")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during all logs deletion")
            Result.failure(e)
        }
    }

    suspend fun log(type: LogType, message: String): Long {
        return when (type) {
            LogType.INFO -> logInfo(message)
            LogType.WARNING -> logWarning(message)
            LogType.ERROR -> logError(message)
        }
    }

    suspend fun logInfo(message: String): Long {
        return loggingService.logInfo(message)
    }

    suspend fun logWarning(message: String): Long {
        return loggingService.logWarning(message)
    }

    suspend fun logError(message: String): Long {
        return loggingService.logError(message)
    }

}