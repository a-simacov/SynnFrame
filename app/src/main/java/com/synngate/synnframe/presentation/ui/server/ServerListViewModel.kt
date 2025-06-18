package com.synngate.synnframe.presentation.ui.server

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.presentation.ui.server.model.ServerListEvent
import com.synngate.synnframe.presentation.ui.server.model.ServerListState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class ServerListViewModel(
    private val serverUseCases: ServerUseCases,
    private val settingsUseCases: SettingsUseCases,
) : BaseViewModel<ServerListState, ServerListEvent>(ServerListState()) {

    init {
        loadServers()
        observeServers()
        observeSettings()
    }

    private fun loadServers() {
        launchIO {
            val serversCount = serverUseCases.getServers().first().size
            Timber.d("Database contains $serversCount servers on startup")

            updateState { it.copy(isLoading = true) }

            try {
                val showOnStartup = settingsUseCases.showServersOnStartup.first()
                val activeServer = serverUseCases.getActiveServer().first()

                val servers = serverUseCases.getServers().first()

                updateState { state ->
                    state.copy(
                        servers = servers,
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
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    private fun observeServers() {
        serverUseCases.getServers()
            .onEach { servers ->
                Timber.d("Received servers: ${servers.size}")
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
                        error = e.message ?: "Неизвестная ошибка"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSettings() {
        launchIO {
            settingsUseCases.showServersOnStartup.collectLatest { showOnStartup ->
                updateState { state ->
                    state.copy(showServersOnStartup = showOnStartup)
                }
            }
        }
    }

    fun onServerClick(serverId: Int) {
        sendEvent(ServerListEvent.NavigateToServerDetail(serverId))
    }

    fun onAddServerClick() {
        sendEvent(ServerListEvent.NavigateToServerDetail(null))
    }

    fun onDeleteServerClick(serverId: Int, serverName: String) {
        sendEvent(ServerListEvent.ShowDeleteConfirmation(serverId, serverName))
    }

    fun deleteServer(serverId: Int) {
        launchIO {
            updateState { it.copy(isLoading = true) }

            val result = serverUseCases.deleteServer(serverId)

            if (result.isSuccess) {
                sendEvent(ServerListEvent.ShowSnackbar("Server successfully deleted"))
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to delete server"
                sendEvent(ServerListEvent.ShowSnackbar(error))
                Timber.e(result.exceptionOrNull(), "Error deleting server")
            }

            updateState { it.copy(isLoading = false) }
        }
    }
}