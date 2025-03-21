package com.synngate.synnframe.presentation.service.sync

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.synngate.synnframe.R
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.data.sync.SyncProgress
import com.synngate.synnframe.data.sync.SyncStatus
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.presentation.service.base.BaseForegroundService
import com.synngate.synnframe.presentation.service.notification.NotificationChannelManager
import com.synngate.synnframe.presentation.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Улучшенный сервис синхронизации с расширенными уведомлениями
 */
class SynchronizationService : BaseForegroundService() {

    // Получаем зависимости через appContainer
    override val loggingService: LoggingService by lazy {
        (application as SynnFrameApplication).appContainer.loggingService
    }

    private val synchronizationController: SynchronizationController by lazy {
        (application as SynnFrameApplication).appContainer.synchronizationController
    }

    // Текущий прогресс синхронизации
    private var currentProgress: SyncProgress? = null

    // Форматер для даты и времени в уведомлении
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    // Метаданные сервиса
    override val serviceName: String = "SynchronizationService"
    override val notificationId: Int = NotificationChannelManager.NOTIFICATION_ID_SYNCHRONIZATION

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("SynchronizationService onStartCommand, action: ${intent?.action}")

        // Обрабатываем дополнительные команды
        when (intent?.action) {
            ACTION_UPDATE_PROGRESS -> {
                // Создаем новый объект SyncProgress из параметров Intent
                val progressId = intent.getStringExtra(EXTRA_PROGRESS_ID) ?: System.currentTimeMillis().toString()
                val statusName = intent.getStringExtra(EXTRA_PROGRESS_STATUS) ?: SyncStatus.STARTED.name
                val tasksUploaded = intent.getIntExtra(EXTRA_TASKS_UPLOADED, 0)
                val tasksDownloaded = intent.getIntExtra(EXTRA_TASKS_DOWNLOADED, 0)
                val productsDownloaded = intent.getIntExtra(EXTRA_PRODUCTS_DOWNLOADED, 0)
                val currentOperation = intent.getStringExtra(EXTRA_CURRENT_OPERATION) ?: ""
                val progressPercent = intent.getIntExtra(EXTRA_PROGRESS_PERCENT, 0)
                val errorCount = intent.getIntExtra(EXTRA_ERROR_COUNT, 0)
                val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)

                val progress = SyncProgress(
                    id = progressId,
                    status = SyncStatus.valueOf(statusName),
                    tasksUploaded = tasksUploaded,
                    tasksDownloaded = tasksDownloaded,
                    productsDownloaded = productsDownloaded,
                    currentOperation = currentOperation,
                    progressPercent = progressPercent,
                    errorCount = errorCount,
                    lastErrorMessage = errorMessage
                )

                currentProgress = progress

                // Обновляем уведомление с новым прогрессом
                if (isServiceRunning) {
                    val notification = createNotification()
                    startForeground(notificationId, notification)
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override suspend fun onServiceStart() {
        Timber.d("Starting synchronization service")

        // Запускаем наблюдение за прогрессом синхронизации
        serviceScope.launch(Dispatchers.IO) {
            synchronizationController.syncProgressFlow.collect { progress ->
                currentProgress = progress

                // Обновляем уведомление при изменении прогресса
                if (isServiceRunning) {
                    val notification = createNotification()
                    startForeground(notificationId, notification)
                }
            }
        }
    }

    override suspend fun onServiceStop() {
        Timber.d("Stopping synchronization service")
        loggingService.logInfo("Остановлен сервис синхронизации")
    }

    override fun createNotification(): Notification {
        // Создаем Intent для открытия приложения при клике на уведомление
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            notificationIntent.putExtra(EXTRA_OPEN_SYNC_SCREEN, true)
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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

        // Создаем Intent для принудительного запуска синхронизации
        val syncIntent = Intent(this, SynchronizationService::class.java).apply {
            action = ACTION_FORCE_SYNC
        }
        val syncPendingIntent = PendingIntent.getService(
            this, 2, syncIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Получаем текущий прогресс синхронизации
        val progress = currentProgress ?: SyncProgress()
        val progressMessage = progress.getProgressMessage()
        val progressPercentage = progress.calculateOverallProgress()

        // Создаем заголовок уведомления
        val notificationTitle = when (progress.status) {
            SyncStatus.STARTED -> getString(R.string.sync_notification_title_started)
            SyncStatus.IN_PROGRESS -> getString(R.string.sync_notification_title_in_progress)
            SyncStatus.COMPLETED -> getString(R.string.sync_notification_title_completed)
            SyncStatus.FAILED -> getString(R.string.sync_notification_title_failed)
        }

        // Формируем текст уведомления
        val contentText = when (progress.status) {
            SyncStatus.STARTED -> progressMessage
            SyncStatus.IN_PROGRESS -> progressMessage
            SyncStatus.COMPLETED -> getString(
                R.string.sync_notification_completed,
                progress.tasksUploaded,
                progress.tasksDownloaded,
                progress.productsDownloaded
            )
            SyncStatus.FAILED -> progressMessage
        }

        // Дополнительная строка с информацией о времени
        val timeInfo = when {
            progress.endTime != null -> getString(
                R.string.sync_notification_finished_at,
                progress.endTime.format(dateTimeFormatter)
            )
            progress.status == SyncStatus.IN_PROGRESS -> getString(
                R.string.sync_notification_started_at,
                progress.startTime.format(dateTimeFormatter)
            )
            else -> null
        }

        // Строим уведомление
        val builder = NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_SYNCHRONIZATION)
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                contentText + (timeInfo?.let { "\n$it" } ?: "")
            ))
            .setSmallIcon(R.drawable.ic_sync) // Или другая подходящая иконка
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Добавляем прогресс-бар для синхронизации в процессе
        if (progress.status == SyncStatus.IN_PROGRESS) {
            builder.setProgress(100, progressPercentage, false)
        }

        // Добавляем действия в зависимости от статуса
        when (progress.status) {
            SyncStatus.STARTED, SyncStatus.IN_PROGRESS -> {
                builder.addAction(
                    R.drawable.ic_stop,
                    getString(R.string.action_stop),
                    stopPendingIntent
                )
            }
            SyncStatus.COMPLETED, SyncStatus.FAILED -> {
                builder.addAction(
                    R.drawable.ic_sync,
                    getString(R.string.action_sync_now),
                    syncPendingIntent
                )

                // Добавляем кнопку для очистки уведомления
                if (progress.status == SyncStatus.COMPLETED) {
                    val clearIntent = Intent(this, SynchronizationService::class.java).apply {
                        action = ACTION_CLEAR_NOTIFICATION
                    }
                    val clearPendingIntent = PendingIntent.getService(
                        this, 3, clearIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )

                    builder.addAction(
                        R.drawable.ic_clear,
                        getString(R.string.action_clear),
                        clearPendingIntent
                    )
                }
            }
        }

        return builder.build()
    }

    companion object {
        // Константы для Intent
        const val ACTION_UPDATE_PROGRESS = "com.synngate.synnframe.ACTION_UPDATE_PROGRESS"
        const val ACTION_FORCE_SYNC = "com.synngate.synnframe.ACTION_FORCE_SYNC"
        const val ACTION_CLEAR_NOTIFICATION = "com.synngate.synnframe.ACTION_CLEAR_NOTIFICATION"

        // Дополнительные данные для Intent
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_OPEN_SYNC_SCREEN = "extra_open_sync_screen"

        // Константы для параметров Intent
        const val EXTRA_PROGRESS_ID = "extra_progress_id"
        const val EXTRA_PROGRESS_STATUS = "extra_progress_status"
        const val EXTRA_TASKS_UPLOADED = "extra_tasks_uploaded"
        const val EXTRA_TASKS_DOWNLOADED = "extra_tasks_downloaded"
        const val EXTRA_PRODUCTS_DOWNLOADED = "extra_products_downloaded"
        const val EXTRA_CURRENT_OPERATION = "extra_current_operation"
        const val EXTRA_PROGRESS_PERCENT = "extra_progress_percent"
        const val EXTRA_ERROR_COUNT = "extra_error_count"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
    }
}