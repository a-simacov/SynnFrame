package com.synngate.synnframe.presentation.ui.server

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.presentation.di.ClearableViewModel
import com.synngate.synnframe.presentation.di.ServerListViewModel
import com.synngate.synnframe.presentation.ui.server.model.ServerListEvent
import com.synngate.synnframe.presentation.ui.server.model.ServerListState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class ServerListViewModelImpl(
    private val serverUseCases: ServerUseCases,
    private val settingsUseCases: SettingsUseCases,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<ServerListState, ServerListEvent>(ServerListState()), ServerListViewModel {

    init {
        loadServers()
        observeServers()
        observeSettings()
    }

    /**
     * Загружает начальные данные
     */
    private fun loadServers() {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                // Загружаем настройку из SettingsUseCases
                val showOnStartup = settingsUseCases.showServersOnStartup.first()

                // Отображаем активный сервер
                val activeServer = serverUseCases.getActiveServer().first()

                updateState { state ->
                    state.copy(
                        showServersOnStartup = showOnStartup,
                        activeServerId = activeServer?.id,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading initial data")
                updateState { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    /**
     * Наблюдает за изменениями в списке серверов
     */
    private fun observeServers() {
        serverUseCases.getServers()
            .onEach { servers ->
                updateState { state ->
                    state.copy(
                        servers = servers,
                        isLoading = false,
                        error = null
                    )
                }
            }
            .catch { e ->
                Timber.e(e, "Error observing servers")
                updateState { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Наблюдает за изменениями в настройках
     */
    private fun observeSettings() {
        launchIO {
            settingsUseCases.showServersOnStartup.collectLatest { showOnStartup ->
                updateState { state ->
                    state.copy(showServersOnStartup = showOnStartup)
                }
            }
        }
    }

    /**
     * Обрабатывает нажатие на сервер в списке
     */
    fun onServerClick(serverId: Int) {
        sendEvent(ServerListEvent.NavigateToServerDetail(serverId))
    }

    /**
     * Обрабатывает нажатие на кнопку "Добавить сервер"
     */
    fun onAddServerClick() {
        sendEvent(ServerListEvent.NavigateToServerDetail(null))
    }

    /**
     * Обрабатывает нажатие на кнопку "Удалить" для сервера
     */
    fun onDeleteServerClick(serverId: Int, serverName: String) {
        sendEvent(ServerListEvent.ShowDeleteConfirmation(serverId, serverName))
    }

    /**
     * Удаляет сервер
     */
    fun deleteServer(serverId: Int) {
        launchIO {
            updateState { it.copy(isLoading = true) }

            val result = serverUseCases.deleteServer(serverId)

            if (result.isSuccess) {
                sendEvent(ServerListEvent.ShowSnackbar("Сервер успешно удален"))
            } else {
                val error = result.exceptionOrNull()?.message ?: "Не удалось удалить сервер"
                sendEvent(ServerListEvent.ShowSnackbar(error))
                Timber.e(result.exceptionOrNull(), "Error deleting server")
            }

            updateState { it.copy(isLoading = false) }
        }
    }

    /**
     * Обрабатывает изменение настройки "Показывать при запуске"
     */
    fun setShowServersOnStartup(show: Boolean) {
        launchIO {
            val result = settingsUseCases.setShowServersOnStartup(show)

            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Не удалось изменить настройку"
                sendEvent(ServerListEvent.ShowSnackbar(error))
                Timber.e(result.exceptionOrNull(), "Error setting show on startup")
            }
        }
    }

    /**
     * Переход к экрану логина
     */
    fun navigateToLogin() {
        sendEvent(ServerListEvent.NavigateToLogin)
    }
}