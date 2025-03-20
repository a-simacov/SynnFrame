// File: com.synngate.synnframe.presentation.service.notification.NotificationChannelManager.kt

package com.synngate.synnframe.presentation.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.synngate.synnframe.R

/**
 * Менеджер для создания и управления каналами уведомлений для foreground-сервисов
 */
class NotificationChannelManager(private val context: Context) {

    companion object {
        // Каналы уведомлений
        const val CHANNEL_WEB_SERVER = "web_server_channel"
        const val CHANNEL_SYNCHRONIZATION = "sync_channel"

        // Идентификаторы уведомлений
        const val NOTIFICATION_ID_WEB_SERVER = 1001
        const val NOTIFICATION_ID_SYNCHRONIZATION = 1002
    }

    /**
     * Создание всех необходимых каналов уведомлений
     */
    fun createNotificationChannels() {
        createWebServerChannel()
        createSynchronizationChannel()
    }

    /**
     * Создание канала уведомлений для веб-сервера
     */
    private fun createWebServerChannel() {
        val name = context.getString(R.string.web_server_channel_name)
        val description = context.getString(R.string.web_server_channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(CHANNEL_WEB_SERVER, name, importance).apply {
            this.description = description
            enableVibration(false)
            setShowBadge(false)
        }

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /**
     * Создание канала уведомлений для сервиса синхронизации
     */
    private fun createSynchronizationChannel() {
        val name = context.getString(R.string.sync_channel_name)
        val description = context.getString(R.string.sync_channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(CHANNEL_SYNCHRONIZATION, name, importance).apply {
            this.description = description
            enableVibration(false)
            setShowBadge(false)
        }

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}