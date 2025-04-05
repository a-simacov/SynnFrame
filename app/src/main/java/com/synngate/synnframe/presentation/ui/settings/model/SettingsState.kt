package com.synngate.synnframe.presentation.ui.settings.model

import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.local.entity.OperationType
import com.synngate.synnframe.data.sync.RetrySettings
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.presentation.theme.ThemeMode
import java.time.LocalDateTime

data class SettingsState(
    // Активный сервер
    val activeServer: Server? = null,

    // Опция отображения экрана серверов при запуске
    val showServersOnStartup: Boolean = true,

    // Настройки периодической выгрузки
    val periodicUploadEnabled: Boolean = false,
    val uploadIntervalSeconds: Int = 300, // 5 минут по умолчанию

    // Состояние локального веб-сервера
    val isWebServerRunning: Boolean = false,

    // Настройки темы и языка
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageCode: String = "ru", // Русский по умолчанию

    // Высота кнопки навигации
    val navigationButtonHeight: Float = 72f, // 72dp по умолчанию

    // Состояния процессов
    val isLoading: Boolean = false,
    val isCheckingForUpdates: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val isInstallingUpdate: Boolean = false,

    // Информация об обновлении
    val lastVersion: String? = null,
    val releaseDate: String? = null,
    val downloadedUpdatePath: String? = null,

    // Сообщение об ошибке
    val error: String? = null,

    // Диалоги
    val showUpdateConfirmDialog: Boolean = false,

    // Поля для сервиса синхронизации
    val isSyncServiceRunning: Boolean = false,
    val isManualSyncing: Boolean = false,
    val isSyncingTaskTypes: Boolean = false,
    val syncStatus: SynchronizationController.SyncStatus = SynchronizationController.SyncStatus.IDLE,
    val lastSyncInfo: SynchronizationController.SyncInfo? = null,
    val periodicSyncEnabled: Boolean = false,
    val syncIntervalSeconds: Int = 300, // 5 минут
    val nextScheduledSync: LocalDateTime? = null,

    val downloadProgress: Int = 0,

    // Настройки повторных попыток
    val retrySettings: Map<OperationType, RetrySettings> = mapOf(
        OperationType.UPLOAD_TASK to RetrySettings(5, 60, 3600, 2.0),
        OperationType.DOWNLOAD_TASKS to RetrySettings(5, 60, 3600, 2.0),
        OperationType.DOWNLOAD_PRODUCTS to RetrySettings(5, 60, 3600, 2.0),
        OperationType.FULL_SYNC to RetrySettings(5, 60, 3600, 2.0)
    ),

// Отображение раздела с настройками повторных попыток
    val showRetrySettings: Boolean = false,

    val binCodePattern: String = AppSettingsDataStore.DEFAULT_BIN_PATTERN
)