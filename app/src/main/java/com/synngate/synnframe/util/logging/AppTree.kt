package com.synngate.synnframe.util.logging

import android.util.Log
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.domain.service.LoggingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AppTree(
    private val loggingService: LoggingService,
    private val logLevelProvider: LogLevelProvider
) : Timber.Tree() {

    interface LogLevelProvider {
        fun getCurrentLogLevel(): LogLevel
    }

    // Улучшенная защита от рекурсии через ThreadLocal
    private val isProcessingLog = ThreadLocal.withInitial { false }

    // Кэш для дедупликации логов
    private val recentLogsCache = ConcurrentHashMap<String, Long>()

    // Константы для настройки дедупликации
    private val DEDUPLICATION_WINDOW_MS = 1000L // 1 секунда
    private val CACHE_CLEANUP_INTERVAL_MS = 30000L // 30 секунд
    private var lastCleanupTime = System.currentTimeMillis()

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        // Если текущий поток уже обрабатывает лог, отклоняем для предотвращения рекурсии
        if (isProcessingLog.get()) {
            return false
        }

        // В Debug-сборке всегда разрешаем все логи для консоли
        if (BuildConfig.DEBUG) {
            return true
        }

        // В Release применяем настройки уровня логирования
        val currentLevel = logLevelProvider.getCurrentLogLevel()
        return when (currentLevel) {
            LogLevel.FULL -> true
            LogLevel.INFO -> priority >= Log.INFO
            LogLevel.WARNING -> priority >= Log.WARN
            LogLevel.ERROR -> priority >= Log.ERROR
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Защита от рекурсии на уровне потока
        if (isProcessingLog.get()) {
            return
        }

        try {
            isProcessingLog.set(true)

            // Создаем уникальный ключ из приоритета, тега и сообщения
            val logKey = "$priority|${tag ?: ""}|$message"

            // Проверяем, был ли такой лог недавно
            val now = System.currentTimeMillis()
            val lastLogTime = recentLogsCache.put(logKey, now)

            // Если точно такой же лог был записан менее секунды назад, пропускаем его
            if (lastLogTime != null && (now - lastLogTime) < DEDUPLICATION_WINDOW_MS) {
                return
            }

            // Периодически очищаем кэш
            if (now - lastCleanupTime > CACHE_CLEANUP_INTERVAL_MS) {
                cleanupCache(now)
                lastCleanupTime = now
            }

            // Выводим в консоль если разрешено
            when (priority) {
                Log.VERBOSE, Log.DEBUG -> Log.d(tag, message, t)
                Log.INFO -> Log.i(tag, message, t)
                Log.WARN -> Log.w(tag, message, t)
                Log.ERROR, Log.ASSERT -> Log.e(tag, message, t)
            }

            // Применяем фильтрацию по уровню для записи в БД
            val currentLevel = logLevelProvider.getCurrentLogLevel()
            val shouldSaveToDb = when (currentLevel) {
                LogLevel.FULL -> priority >= Log.DEBUG
                LogLevel.INFO -> priority >= Log.INFO
                LogLevel.WARNING -> priority >= Log.WARN
                LogLevel.ERROR -> priority >= Log.ERROR
            }

            if (shouldSaveToDb) {
                // Запуск в отдельной корутине для асинхронного сохранения
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val fullMessage = if (tag != null) "[$tag] $message" else message
                        val errorDetails = t?.let { " Exception: ${it.message}" } ?: ""
                        val finalMessage = fullMessage + errorDetails

                        when (priority) {
                            Log.DEBUG, Log.VERBOSE -> loggingService.logInfo("(Debug) $finalMessage")
                            Log.INFO -> loggingService.logInfo(finalMessage)
                            Log.WARN -> loggingService.logWarning(finalMessage)
                            Log.ERROR, Log.ASSERT -> loggingService.logError(finalMessage)
                        }
                    } catch (e: Exception) {
                        // Используем стандартный Log, не Timber!
                        Log.e("AppTree", "Failed to save log to database", e)
                    }
                }
            }
        } finally {
            // Важно: освобождаем блокировку в блоке finally
            isProcessingLog.set(false)
        }
    }

    /**
     * Очистка устаревших записей из кэша
     */
    private fun cleanupCache(currentTime: Long) {
        val cutoffTime = currentTime - TimeUnit.MINUTES.toMillis(5) // Удаляем логи старше 5 минут
        recentLogsCache.entries.removeIf { (_, timestamp) ->
            currentTime - timestamp > cutoffTime
        }
    }
}