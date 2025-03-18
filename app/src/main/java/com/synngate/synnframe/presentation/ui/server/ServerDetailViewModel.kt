package com.synngate.synnframe.presentation.ui.server

import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.presentation.di.ServerDetailViewModel
import com.synngate.synnframe.presentation.ui.server.model.ServerDetailEvent
import com.synngate.synnframe.presentation.ui.server.model.ServerDetailState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import timber.log.Timber

class ServerDetailViewModelImpl(
    private val serverId: Int?,
    private val serverUseCases: ServerUseCases,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<ServerDetailState, ServerDetailEvent>(
    ServerDetailState(serverId = serverId, isEditMode = serverId != null)
), ServerDetailViewModel {

    init {
        if (serverId != null) {
            loadServer(serverId)
        }
    }

    /**
     * Загружает данные сервера
     */
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
                            error = "Сервер не найден"
                        )
                    }
                    sendEvent(ServerDetailEvent.NavigateBack)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading server")
                updateState { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка загрузки сервера"
                    )
                }
                sendEvent(ServerDetailEvent.ShowSnackbar("Ошибка загрузки сервера: ${e.message}"))
            }
        }
    }

    /**
     * Обновляет имя сервера
     */
    fun updateName(name: String) {
        updateState { it.copy(name = name, validationError = null) }
    }

    /**
     * Обновляет хост сервера
     */
    fun updateHost(host: String) {
        updateState { it.copy(host = host, validationError = null) }
    }

    /**
     * Обновляет порт сервера
     */
    fun updatePort(port: String) {
        updateState { it.copy(port = port, validationError = null) }
    }

    /**
     * Обновляет точку подключения к API
     */
    fun updateApiEndpoint(apiEndpoint: String) {
        updateState { it.copy(apiEndpoint = apiEndpoint, validationError = null) }
    }

    /**
     * Обновляет логин для доступа к API
     */
    fun updateLogin(login: String) {
        updateState { it.copy(login = login, validationError = null) }
    }

    /**
     * Обновляет пароль для доступа к API
     */
    fun updatePassword(password: String) {
        updateState { it.copy(password = password, validationError = null) }
    }

    /**
     * Обновляет статус активности сервера
     */
    fun updateIsActive(isActive: Boolean) {
        updateState { it.copy(isActive = isActive) }
    }

    /**
     * Проверяет подключение к серверу
     */
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
            updateState { it.copy(isTestingConnection = true, connectionStatus = "Статус: проверка подключения...") }

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
                    connectionStatus = "Статус: подключение успешно"
                ) }
                sendEvent(ServerDetailEvent.ConnectionSuccess)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                updateState { it.copy(
                    isTestingConnection = false,
                    connectionStatus = "Статус: ошибка подключения"
                ) }
                sendEvent(ServerDetailEvent.ConnectionError(error))
            }
        }
    }

    /**
     * Сохраняет сервер
     */
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
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    updateState { it.copy(isSaving = false, error = error) }
                    sendEvent(ServerDetailEvent.ShowSnackbar("Ошибка сохранения сервера: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving server")
                updateState { it.copy(isSaving = false, error = e.message) }
                sendEvent(ServerDetailEvent.ShowSnackbar("Ошибка сохранения сервера: ${e.message}"))
            }
        }
    }

    /**
     * Проверяет, что все необходимые поля заполнены и значения валидны
     */
    private fun validateFields(): String? {
        val state = uiState.value

        return when {
            state.name.isBlank() -> "Имя сервера не может быть пустым"
            state.host.isBlank() -> "Хост сервера не может быть пустым"
            state.port.isBlank() -> "Порт сервера не может быть пустым"
            state.port.toIntOrNull() == null -> "Порт должен быть числом"
            state.port.toIntOrNull() !in 1..65535 -> "Порт должен быть от 1 до 65535"
            state.apiEndpoint.isBlank() -> "Точка подключения к API не может быть пустой"
            state.login.isBlank() -> "Логин не может быть пустым"
            else -> null
        }
    }

    /**
     * Возвращается на предыдущий экран
     */
    fun navigateBack() {
        sendEvent(ServerDetailEvent.NavigateBack)
    }
}