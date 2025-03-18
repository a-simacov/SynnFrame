// Файл: com.synngate.synnframe.data.service.LoggingServiceImpl.kt

package com.synngate.synnframe.data.service

import com.synngate.synnframe.domain.entity.Log
import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.service.LoggingService
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Реализация сервиса логирования
 */
class LoggingServiceImpl(private val logRepository: LogRepository) : LoggingService {

    override suspend fun logInfo(message: String): Long {
        Timber.i(message)
        val log = Log(
            message = message,
            type = LogType.INFO,
            createdAt = LocalDateTime.now()
        )
        return logRepository.addLog(log)
    }

    override suspend fun logWarning(message: String): Long {
        Timber.w(message)
        val log = Log(
            message = message,
            type = LogType.WARNING,
            createdAt = LocalDateTime.now()
        )
        return logRepository.addLog(log)
    }

    override suspend fun logError(message: String): Long {
        Timber.e(message)
        val log = Log(
            message = message,
            type = LogType.ERROR,
            createdAt = LocalDateTime.now()
        )
        return logRepository.addLog(log)
    }
}