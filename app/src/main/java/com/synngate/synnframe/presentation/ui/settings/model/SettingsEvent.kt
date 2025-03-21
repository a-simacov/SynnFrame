package com.synngate.synnframe.presentation.ui.settings.model

import android.content.Intent
import android.net.Uri

sealed class SettingsEvent {
    /**
     * Показать сообщение в snackbar
     */
    data class ShowSnackbar(val message: String) : SettingsEvent()

    /**
     * Навигация назад
     */
    data object NavigateBack : SettingsEvent()

    /**
     * Навигация к экрану серверов
     */
    data object NavigateToServerList : SettingsEvent()

    /**
     * Обновление настроек успешно
     */
    data object SettingsUpdated : SettingsEvent()

    data class RequestInstallPermission(val intent: Intent?) : SettingsEvent()

    /**
     * Установка обновления
     */
    data class InstallUpdate(val uri: Uri) : SettingsEvent()

    data object NavigateToSyncHistory : SettingsEvent()
}