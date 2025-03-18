// Файл: com.synngate.synnframe.presentation.ui.settings.SettingsState.kt

package com.synngate.synnframe.presentation.ui.settings.model

import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.presentation.theme.ThemeMode

/**
 * Состояние экрана настроек
 */
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
    val showUpdateConfirmDialog: Boolean = false
)