package com.synngate.synnframe.presentation.di.modules.api

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.DeviceInfoService
import com.synngate.synnframe.domain.service.FileService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.presentation.service.notification.NotificationChannelManager
import com.synngate.synnframe.util.resources.ResourceProvider

/**
 * Интерфейс для предоставления базовых сервисов приложения.
 * Включает сервисы для работы с настройками, ресурсами, логированием и др.
 */
interface CoreAPI {
    /**
     * DataStore для хранения настроек приложения
     */
    val appSettingsDataStore: AppSettingsDataStore

    /**
     * Провайдер ресурсов для доступа к строкам, изображениям и другим ресурсам
     */
    val resourceProvider: ResourceProvider

    /**
     * Сервис логирования для записи событий и ошибок
     */
    val loggingService: LoggingService

    /**
     * Менеджер каналов уведомлений
     */
    val notificationChannelManager: NotificationChannelManager

    /**
     * Сервис для доступа к информации об устройстве
     */
    val deviceInfoService: DeviceInfoService

    /**
     * Сервис для работы с файловой системой
     */
    val fileService: FileService

    /**
     * Сервис для воспроизведения звуков
     */
    val soundService: SoundService

    /**
     * Сервис для работы с буфером обмена
     */
    val clipboardService: ClipboardService

    /**
     * Доступ к базовому DataStore
     */
    val preferencesDataStore: DataStore<Preferences>
}