package com.synngate.synnframe.presentation.ui.server.model

import com.synngate.synnframe.domain.entity.Server

data class ServerListState(
    val servers: List<Server> = emptyList(),

    val isLoading: Boolean = false,

    val error: String? = null,

    val showServersOnStartup: Boolean = true,

    val activeServerId: Int? = null
)

sealed class ServerListEvent {
    data class NavigateToServerDetail(val serverId: Int? = null) : ServerListEvent()

    data class ShowSnackbar(val message: String) : ServerListEvent()

    data class ShowDeleteConfirmation(val serverId: Int, val serverName: String) : ServerListEvent()
}