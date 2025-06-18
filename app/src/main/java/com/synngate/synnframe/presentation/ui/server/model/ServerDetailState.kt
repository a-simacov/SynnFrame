package com.synngate.synnframe.presentation.ui.server.model

import com.synngate.synnframe.data.barcodescanner.DeviceType
import com.synngate.synnframe.domain.entity.Server

data class ServerDetailState(
    val server: Server? = null,
    val serverId: Int? = null, // null для нового сервера
    val isEditMode: Boolean = false,

    val name: String = "",
    val host: String = "",
    val port: String = "",
    val apiEndpoint: String = "",

    val login: String = "",
    val password: String = "",

    val isActive: Boolean = false,
    val connectionStatus: String = "Status: connection awaiting",
    val isTestingConnection: Boolean = false,

    val isSaving: Boolean = false,
    val isLoading: Boolean = false,

    val validationError: String? = null,

    val error: String? = null,

    val currentScannerType: DeviceType = DeviceType.STANDARD,
    val isScannerAvailable: Boolean = false,
    val showScannerTypeOptions: Boolean = false
)

sealed class ServerDetailEvent {

    data object NavigateBack : ServerDetailEvent()

    data class ShowSnackbar(val message: String) : ServerDetailEvent()

    data class ServerSaved(val serverId: Int) : ServerDetailEvent()

    data object ConnectionSuccess : ServerDetailEvent()

    data class ConnectionError(val message: String) : ServerDetailEvent()

    data class ShowQrCode(val content: String) : ServerDetailEvent()
}