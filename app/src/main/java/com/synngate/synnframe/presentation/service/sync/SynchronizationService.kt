// File: com.synngate.synnframe.presentation.service.sync.SynchronizationService.kt

package com.synngate.synnframe.presentation.service.sync

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.synngate.synnframe.R
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.presentation.service.base.BaseForegroundService
import com.synngate.synnframe.presentation.service.base.launchSafely
import com.synngate.synnframe.presentation.service.notification.NotificationChannelManager
import com.synngate.synnframe.presentation.ui.MainActivity
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.format.DateTimeFormatter

/**
 * Foreground-сервис для синхронизации с внешним сервером
 */
class SynchronizationService : BaseForegroundService() {

    // Получаем зависимости через appContainer вместо inject
    override val loggingService: LoggingService by lazy {
        (application as SynnFrameApplication).appContainer.loggingService
    }

    private val synchronizationController: SynchronizationController by lazy {
        (application as SynnFrameApplication).appContainer.synchronizationController
    }

    // Формат даты для отображения в уведомлении
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    // Метаданные сервиса
    override val serviceName: String = "SynchronizationService"
    override val notificationId: Int = NotificationChannelManager.NOTIFICATION_ID_SYNCHRONIZATION

    // Текущее состояние синхронизации
    private var currentSyncStatus: SynchronizationController.SyncStatus = SynchronizationController.SyncStatus.IDLE

    // Добавляем кэш для lastSyncInfo
    private var lastSyncInfoCache: SynchronizationController.SyncInfo? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("SynchronizationService onCreate")

        // Наблюдаем за статусом синхронизации
        serviceScope.launchSafely {
            synchronizationController.syncStatus.collect { status ->
                currentSyncStatus = status

                // Обновляем уведомление при изменении статуса
                if (isServiceRunning) {
                    updateNotification()
                }
            }
        }

        // Добавляем корутину для наблюдения за lastSyncInfo
        serviceScope.launchSafely {
            synchronizationController.lastSyncInfo.collect { syncInfo ->
                lastSyncInfoCache = syncInfo

                // Обновляем уведомление при получении новой информации о синхронизации
                if (isServiceRunning) {
                    updateNotification()
                }
            }
        }
    }

    override suspend fun onServiceStart() {
        Timber.d("Starting synchronization service")

        // Проверяем, включена ли периодическая синхронизация
        val periodicSyncInfo = synchronizationController.periodicSyncInfo.first()

        if (periodicSyncInfo.enabled) {
            loggingService.logInfo(
                "Запущен сервис синхронизации. " +
                        "Интервал периодической синхронизации: ${periodicSyncInfo.intervalSeconds} секунд"
            )
        } else {
            loggingService.logInfo("Запущен сервис синхронизации без периодической синхронизации")
        }
    }

    override suspend fun onServiceStop() {
        Timber.d("Stopping synchronization service")
        loggingService.logInfo("Остановлен сервис синхронизации")
    }

    override fun createNotification(): Notification {
        // Создаем Intent для открытия приложения при клике на уведомление
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Создаем Intent для остановки сервиса
        val stopIntent = Intent(this, SynchronizationService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Определяем текст уведомления в зависимости от статуса
        val notificationText = when (currentSyncStatus) {
            SynchronizationController.SyncStatus.SYNCING -> getString(R.string.sync_notification_text)
            SynchronizationController.SyncStatus.ERROR -> getString(R.string.sync_notification_error)
            SynchronizationController.SyncStatus.IDLE -> {
                // Вместо прямого вызова first(), используем lastSyncInfo из кэша
                // Мы будем обновлять этот кэш в корутине
                lastSyncInfoCache?.let { lastSyncInfo ->
                    getString(
                        R.string.sync_notification_last_sync,
                        lastSyncInfo.timestamp.format(dateTimeFormatter)
                    )
                } ?: getString(R.string.sync_notification_ready)
            }
        }

        // Создаем уведомление
        return NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_SYNCHRONIZATION)
            .setContentTitle(getString(R.string.sync_notification_title))
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Обновление уведомления при изменении статуса
     */
    private fun updateNotification() {
        val notification = createNotification()
        startForeground(notificationId, notification)
    }

    companion object {
        // Идентификатор для периодической задачи в WorkManager
        const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
    }
}