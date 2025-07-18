package com.synngate.synnframe.presentation.ui.server

import com.synngate.synnframe.data.barcodescanner.DeviceType
import com.synngate.synnframe.data.barcodescanner.ScannerService
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.service.ServerQrService
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.presentation.ui.server.model.ServerDetailEvent
import com.synngate.synnframe.presentation.ui.server.model.ServerDetailState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.first
import timber.log.Timber

class ServerDetailViewModel(
    private val serverId: Int?,
    private val serverUseCases: ServerUseCases,
    private val serverQrService: ServerQrService,
    private val settingsUseCases: SettingsUseCases? = null,
    private val scannerService: ScannerService? = null
) : BaseViewModel<ServerDetailState, ServerDetailEvent>(
    ServerDetailState(serverId = serverId, isEditMode = serverId != null)
) {

    init {
        if (serverId != null) {
            loadServer(serverId)
        }

        loadScannerType()
    }

    private fun loadServer(id: Int) {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                val server = serverUseCases.getServerById(id)

                if (server != null) {
                    updateState { state ->
                        state.copy(
                            server = server,
                            name = server.name,
                            host = server.host,
                            port = server.port.toString(),
                            apiEndpoint = server.apiEndpoint,
                            login = server.login,
                            password = server.password,
                            isActive = server.isActive,
                            isLoading = false
                        )
                    }
                } else {
                    updateState { state ->
                        state.copy(
                            isLoading = false,
                            error = "Server not found"
                        )
                    }
                    sendEvent(ServerDetailEvent.NavigateBack)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading server")
                updateState { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message ?: "Error loading server"
                    )
                }
                sendEvent(ServerDetailEvent.ShowSnackbar("Error loading server: ${e.message}"))
            }
        }
    }

    fun updateName(name: String) {
        updateState { it.copy(name = name, validationError = null) }
    }

    fun updateHost(host: String) {
        updateState { it.copy(host = host, validationError = null) }
    }

    fun updatePort(port: String) {
        updateState { it.copy(port = port, validationError = null) }
    }

    fun updateApiEndpoint(apiEndpoint: String) {
        updateState { it.copy(apiEndpoint = apiEndpoint, validationError = null) }
    }

    fun updateLogin(login: String) {
        updateState { it.copy(login = login, validationError = null) }
    }

    fun updatePassword(password: String) {
        updateState { it.copy(password = password, validationError = null) }
    }

    fun updateIsActive(isActive: Boolean) {
        updateState { it.copy(isActive = isActive) }
    }

    fun testConnection() {
        val state = uiState.value

        // Проверяем, что все необходимые поля заполнены
        val validationError = validateFields()
        if (validationError != null) {
            updateState { it.copy(validationError = validationError) }
            sendEvent(ServerDetailEvent.ShowSnackbar(validationError))
            return
        }

        launchIO {
            updateState { it.copy(isTestingConnection = true, connectionStatus = "Status: checking connection...") }

            // Создаем объект сервера для тестирования
            val server = Server(
                id = state.serverId ?: 0,
                name = state.name,
                host = state.host,
                port = state.port.toInt(),
                apiEndpoint = state.apiEndpoint,
                login = state.login,
                password = state.password,
                isActive = state.isActive
            )

            // Используем серверный UseCase для проверки подключения
            val result = serverUseCases.testConnection(server)

            if (result.isSuccess) {
                updateState { it.copy(
                    isTestingConnection = false,
                    connectionStatus = "Status: connection successful"
                ) }
                sendEvent(ServerDetailEvent.ConnectionSuccess)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                updateState { it.copy(
                    isTestingConnection = false,
                    connectionStatus = "Status: connection error"
                ) }
                sendEvent(ServerDetailEvent.ConnectionError(error))
            }
        }
    }

    fun saveServer() {
        val state = uiState.value

        // Проверяем, что все необходимые поля заполнены
        val validationError = validateFields()
        if (validationError != null) {
            updateState { it.copy(validationError = validationError) }
            sendEvent(ServerDetailEvent.ShowSnackbar(validationError))
            return
        }

        launchIO {
            updateState { it.copy(isSaving = true) }

            // Создаем объект сервера для сохранения
            val server = Server(
                id = state.serverId ?: 0,
                name = state.name,
                host = state.host,
                port = state.port.toInt(),
                apiEndpoint = state.apiEndpoint,
                login = state.login,
                password = state.password,
                isActive = state.isActive
            )

            try {
                val result = if (state.isEditMode) {
                    // Обновляем существующий сервер
                    serverUseCases.updateServer(server).map { server.id }
                } else {
                    // Добавляем новый сервер
                    serverUseCases.addServer(server).map { it.toInt() }
                }

                if (result.isSuccess) {
                    // Если сервер активный, то делаем его активным
                    if (server.isActive) {
                        serverUseCases.setActiveServer(result.getOrNull()!!)
                    }

                    updateState { it.copy(isSaving = false) }
                    sendEvent(ServerDetailEvent.ServerSaved(result.getOrNull()!!))
                    sendEvent(ServerDetailEvent.NavigateBack)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    updateState { it.copy(isSaving = false, error = error) }
                    sendEvent(ServerDetailEvent.ShowSnackbar("Error saving server: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving server")
                updateState { it.copy(isSaving = false, error = e.message) }
                sendEvent(ServerDetailEvent.ShowSnackbar("Error saving server: ${e.message}"))
            }
        }
    }

    private fun validateFields(): String? {
        val state = uiState.value

        return when {
            state.name.isBlank() -> "Server name cannot be empty"
            state.host.isBlank() -> "Server host cannot be empty"
            state.port.isBlank() -> "Server port cannot be empty"
            state.port.toIntOrNull() == null -> "Port must be a number"
            state.port.toIntOrNull() !in 1..65535 -> "Port must be between 1 and 65535"
            state.apiEndpoint.isBlank() -> "API endpoint cannot be empty"
            state.login.isBlank() -> "Login cannot be empty"
            else -> null
        }
    }

    /**
     * Обрабатывает результат сканирования QR-кода
     */
    fun handleQrCodeScan(scannedValue: String) {
        try {
            val server = serverQrService.qrStringToServer(scannedValue)

            if (server != null) {
                updateState { state ->
                    state.copy(
                        name = server.name,
                        host = server.host,
                        port = server.port.toString(),
                        apiEndpoint = server.apiEndpoint,
                        login = server.login,
                        password = server.password
                    )
                }
                sendEvent(ServerDetailEvent.ShowSnackbar("Server data successfully loaded from QR code"))
            } else {
                sendEvent(ServerDetailEvent.ShowSnackbar("Failed to recognize server data from QR code"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing QR code")
            sendEvent(ServerDetailEvent.ShowSnackbar("Error processing QR code: ${e.message}"))
        }
    }

    /**
     * Генерирует QR-код для текущего сервера
     */
    fun generateQrCode() {
        val state = uiState.value

        // Проверяем, что все необходимые поля заполнены
        val validationError = validateFields()
        if (validationError != null) {
            updateState { it.copy(validationError = validationError) }
            sendEvent(ServerDetailEvent.ShowSnackbar(validationError))
            return
        }

        try {
            // Создаем объект сервера из текущего состояния
            val server = Server(
                id = state.serverId ?: 0,
                name = state.name,
                host = state.host,
                port = state.port.toInt(),
                apiEndpoint = state.apiEndpoint,
                login = state.login,
                password = state.password,
                isActive = state.isActive
            )

            // Преобразуем сервер в строку для QR-кода
            val qrString = serverQrService.serverToQrString(server)

            // Отправляем событие для отображения QR-кода
            sendEvent(ServerDetailEvent.ShowQrCode(qrString))
        } catch (e: Exception) {
            Timber.e(e, "Error generating QR code")
            sendEvent(ServerDetailEvent.ShowSnackbar("Error generating QR code: ${e.message}"))
        }
    }

    private fun loadScannerType() {
        settingsUseCases?.let { useCases ->
            launchIO {
                try {
                    val deviceType = useCases.deviceType.first()
                    val scannerAvailable = scannerService?.hasRealScanner() ?: false

                    updateState { state ->
                        state.copy(
                            currentScannerType = deviceType,
                            isScannerAvailable = scannerAvailable,
                            // Показываем опции выбора сканера только если:
                            // 1. Нет добавленных серверов (первый запуск)
                            // 2. Сканер доступен
                            showScannerTypeOptions = !state.isEditMode && scannerAvailable
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error loading scanner type")
                    // Если не удалось загрузить тип сканера, скрываем опции выбора
                    updateState { state ->
                        state.copy(showScannerTypeOptions = false)
                    }
                }
            }
        }
    }

    fun updateScannerType(deviceType: DeviceType) {
        updateState { it.copy(currentScannerType = deviceType) }

        settingsUseCases?.let { useCases ->
            launchIO {
                try {
                    // Сохраняем выбранный тип сканера
                    useCases.setDeviceType(deviceType, true)

                    // Перезапускаем сканер для применения изменений
                    scannerService?.restart()

                    // Уведомляем пользователя
                    sendEvent(ServerDetailEvent.ShowSnackbar("Scanner type changed to ${deviceType.name}"))
                } catch (e: Exception) {
                    Timber.e(e, "Error changing scanner type: ${e.message}")
                    sendEvent(ServerDetailEvent.ShowSnackbar("Error changing scanner type"))
                }
            }
        }
    }

    fun navigateBack() {
        sendEvent(ServerDetailEvent.NavigateBack)
    }
}