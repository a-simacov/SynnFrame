package com.synngate.synnframe.util.logging

import android.util.Log
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.domain.service.LoggingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class AppTree(
    private val loggingService: LoggingService,
    private val logLevelProvider: LogLevelProvider
) : Timber.Tree() {

    interface LogLevelProvider {
        fun getCurrentLogLevel(): LogLevel
    }

    // Флаг для предотвращения рекурсивных вызовов
    private val isProcessingLog = AtomicBoolean(false)

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        // Если мы уже обрабатываем лог, отклоняем, чтобы избежать рекурсии
        if (isProcessingLog.get()) {
            return false
        }

        // В Debug-сборке всегда разрешаем все логи для консоли
        if (BuildConfig.DEBUG) {
            return true
        }

        // В Release применяем настройки уровня логирования для консоли
        val currentLevel = logLevelProvider.getCurrentLogLevel()
        return when (currentLevel) {
            LogLevel.FULL -> true
            LogLevel.INFO -> priority >= Log.INFO
            LogLevel.WARNING -> priority >= Log.WARN
            LogLevel.ERROR -> priority >= Log.ERROR
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Защита от рекурсии
        if (isProcessingLog.getAndSet(true)) {
            return
        }

        try {
            // Выводим в консоль если разрешено
            when (priority) {
                Log.VERBOSE, Log.DEBUG -> Log.d(tag, message, t)
                Log.INFO -> Log.i(tag, message, t)
                Log.WARN -> Log.w(tag, message, t)
                Log.ERROR, Log.ASSERT -> Log.e(tag, message, t)
            }

            // Сохраняем в БД с проверкой минимально допустимого уровня
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
                        Timber.tag("AppTree").e(e, "Failed to save log to database")
                    }
                }
            }
        } finally {
            // Важно: освобождаем блокировку в блоке finally
            isProcessingLog.set(false)
        }
    }
}