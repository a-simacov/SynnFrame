// File: com.synngate.synnframe.presentation.service.base.BaseForegroundService.kt

package com.synngate.synnframe.presentation.service.base

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.synngate.synnframe.domain.service.LoggingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Базовый абстрактный класс для всех foreground-сервисов приложения
 */
abstract class BaseForegroundService : Service() {

    // Scope для запуска корутин сервиса
    protected val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Биндер для связи с клиентами сервиса
    private val binder = LocalBinder()

    // Логирование
    protected abstract val loggingService: LoggingService

    // Название сервиса для логов
    protected abstract val serviceName: String

    // ID для уведомления
    protected abstract val notificationId: Int

    // Флаг, указывающий, запущен ли сервис
    protected var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("$serviceName: onCreate()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("$serviceName: onStartCommand()")

        val action = intent?.action

        when (action) {
            ACTION_START_SERVICE -> startForegroundService()
            ACTION_STOP_SERVICE -> stopForegroundService()
        }

        // Если сервис будет убит системой, пусть перезапустится
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Timber.d("$serviceName: onBind()")
        return binder
    }

    override fun onDestroy() {
        Timber.d("$serviceName: onDestroy()")
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Запуск сервиса в режиме foreground
     */
    protected open fun startForegroundService() {
        Timber.d("$serviceName: Starting foreground service")

        isServiceRunning = true

        // Запуск сервиса с уведомлением
        startForeground(notificationId, createNotification())

        // Логирование начала работы сервиса
        serviceScope.launchSafely {
            loggingService.logInfo("$serviceName запущен")
        }

        // Выполнение специфичной для сервиса инициализации
        serviceScope.launchSafely {
            onServiceStart()
        }
    }

    /**
     * Остановка foreground-сервиса
     */
    protected open fun stopForegroundService() {
        Timber.d("$serviceName: Stopping foreground service")

        isServiceRunning = false

        // Выполнение специфичной для сервиса остановки
        serviceScope.launchSafely {
            onServiceStop()
        }

        // Логирование остановки сервиса
        serviceScope.launchSafely {
            loggingService.logInfo("$serviceName остановлен")
        }

        // Остановка foreground-режима и самого сервиса
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Создание уведомления для foreground-сервиса
     */
    protected abstract fun createNotification(): Notification

    /**
     * Действия при запуске сервиса (реализуются в конкретных сервисах)
     */
    protected abstract suspend fun onServiceStart()

    /**
     * Действия при остановке сервиса (реализуются в конкретных сервисах)
     */
    protected abstract suspend fun onServiceStop()

    /**
     * Биндер для связи с сервисом
     */
    inner class LocalBinder : Binder() {
        fun getService(): BaseForegroundService = this@BaseForegroundService
    }

    companion object {
        // Действия для Intent
        const val ACTION_START_SERVICE = "com.synngate.synnframe.ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.synngate.synnframe.ACTION_STOP_SERVICE"
    }
}

/**
 * Безопасный запуск корутины с обработкой ошибок
 */
fun CoroutineScope.launchSafely(block: suspend () -> Unit) {
    launch {
        try {
            block()
        } catch (e: Exception) {
            Timber.e(e, "Error in service coroutine")
        }
    }
}