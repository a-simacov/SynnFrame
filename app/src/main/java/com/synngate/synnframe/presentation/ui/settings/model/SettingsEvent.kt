package com.synngate.synnframe.presentation.ui.settings.model

import android.content.Intent
import android.net.Uri

sealed class SettingsEvent {

    data class ShowSnackbar(val message: String) : SettingsEvent()

    data object NavigateBack : SettingsEvent()

    data object SettingsUpdated : SettingsEvent()

    data class RequestInstallPermission(val intent: Intent?) : SettingsEvent()

    data class InstallUpdate(val uri: Uri) : SettingsEvent()

    data object NavigateToSyncHistory : SettingsEvent()

    data class ChangeAppLanguage(val languageCode: String) : SettingsEvent()
}