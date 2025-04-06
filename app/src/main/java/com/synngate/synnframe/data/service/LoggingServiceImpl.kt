package com.synngate.synnframe.data.service

import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.service.LoggingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

class LoggingServiceImpl(
    private val logRepository: LogRepository
) : LoggingService {

    // Кэш для дедупликации логов
    private val recentLogCache = ConcurrentHashMap<String, Long>()
    private val DEDUPLICATION_WINDOW_MS = 1000L // 1 секунда

    // Общий кортин-скоуп для логирования
    private val logScope = CoroutineScope(Dispatchers.IO)

    private fun shouldLog(message: String): Boolean {
        val now = System.currentTimeMillis()
        val lastLogTime = recentLogCache.put(message, now)

        // Пропускаем лог, если точно такой же был записан менее секунды назад
        return lastLogTime == null || (now - lastLogTime) >= DEDUPLICATION_WINDOW_MS
    }

    override suspend fun logInfo(message: String): Long {
        if (!shouldLog(message)) return -1L

        return logRepository.addLog(
            com.synngate.synnframe.domain.entity.Log(
                message = message,
                type = LogType.INFO,
                createdAt = LocalDateTime.now()
            )
        )
    }

    override suspend fun logWarning(message: String): Long {
        if (!shouldLog(message)) return -1L

        return logRepository.addLog(
            com.synngate.synnframe.domain.entity.Log(
                message = message,
                type = LogType.WARNING,
                createdAt = LocalDateTime.now()
            )
        )
    }

    override suspend fun logError(message: String): Long {
        if (!shouldLog(message)) return -1L

        return logRepository.addLog(
            com.synngate.synnframe.domain.entity.Log(
                message = message,
                type = LogType.ERROR,
                createdAt = LocalDateTime.now()
            )
        )
    }
}