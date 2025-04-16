package com.synngate.synnframe.presentation.service.base

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseForegroundService : Service() {

    // Scope для запуска корутин сервиса
    protected val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Биндер для связи с клиентами сервиса
    private val binder = LocalBinder()

    protected abstract val serviceName: String

    protected abstract val notificationId: Int

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

    protected open fun startForegroundService() {
        Timber.d("$serviceName: Starting foreground service")

        isServiceRunning = true

        // Запуск сервиса с уведомлением
        startForeground(notificationId, createNotification())

        // Логирование начала работы сервиса
        serviceScope.launchSafely {
            Timber.i("$serviceName запущен")
        }

        // Выполнение специфичной для сервиса инициализации
        serviceScope.launchSafely {
            onServiceStart()
        }
    }

    protected open fun stopForegroundService() {
        Timber.d("$serviceName: Stopping foreground service")

        isServiceRunning = false

        // Выполнение специфичной для сервиса остановки
        serviceScope.launchSafely {
            onServiceStop()
        }

        // Логирование остановки сервиса
        serviceScope.launchSafely {
            Timber.i("$serviceName остановлен")
        }

        // Остановка foreground-режима и самого сервиса
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    protected abstract fun createNotification(): Notification

    /**
     * Действия при запуске сервиса (реализуются в конкретных сервисах)
     */
    protected abstract suspend fun onServiceStart()

    /**
     * Действия при остановке сервиса (реализуются в конкретных сервисах)
     */
    protected abstract suspend fun onServiceStop()

    inner class LocalBinder : Binder() {
        fun getService(): BaseForegroundService = this@BaseForegroundService
    }

    companion object {
        // Действия для Intent
        const val ACTION_START_SERVICE = "com.synngate.synnframe.ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.synngate.synnframe.ACTION_STOP_SERVICE"
    }
}

fun CoroutineScope.launchSafely(block: suspend () -> Unit) {
    launch {
        try {
            block()
        } catch (e: Exception) {
            Timber.e(e, "Error in service coroutine")
        }
    }
}