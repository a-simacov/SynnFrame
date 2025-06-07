package com.synngate.synnframe.presentation.ui.server

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.buttons.NavigationButton
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scaffold.LoadingScreenContent
import com.synngate.synnframe.presentation.ui.server.model.ServerListEvent
import kotlinx.coroutines.launch

@Composable
fun ServerListScreen(
    viewModel: ServerListViewModel,
    navigateToServerDetail: (Int?) -> Unit,
    navigateToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var serverToDelete by remember { mutableStateOf<Pair<Int, String>?>(null) }

    // Добавляем map для хранения состояний swipe для каждого сервера
    val swipeStates = remember { mutableMapOf<Int, SwipeToDismissBoxState>() }

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ServerListEvent.NavigateToServerDetail -> navigateToServerDetail(event.serverId)
                is ServerListEvent.NavigateToLogin -> navigateToLogin()
                is ServerListEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is ServerListEvent.ShowDeleteConfirmation -> {
                    serverToDelete = Pair(event.serverId, event.serverName)
                    showDeleteDialog = true
                }
            }
        }
    }

    if (showDeleteDialog && serverToDelete != null) {
        ConfirmationDialog(
            title = stringResource(id = R.string.delete_server_title),
            message = stringResource(id = R.string.delete_server_message, serverToDelete?.second ?: ""),
            onConfirm = {
                serverToDelete?.first?.let { serverId ->
                    viewModel.deleteServer(serverId)
                }
                showDeleteDialog = false
                serverToDelete = null
            },
            onDismiss = {
                // При нажатии "Отмена" сбрасываем состояние свайпа для этого сервера
                serverToDelete?.first?.let { serverId ->
                    swipeStates[serverId]?.let { state ->
                        coroutineScope.launch {
                            state.reset()
                        }
                    }
                }
                showDeleteDialog = false
                serverToDelete = null
            }
        )
    }

    AppScaffold(
        title = stringResource(id = R.string.servers_title),
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddServerClick() },
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add_server)
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Переключатель "Показывать при запуске"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Switch(
                        checked = state.showServersOnStartup,
                        onCheckedChange = { viewModel.setShowServersOnStartup(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.server_show_on_startup),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                NavigationButton(
                    text = stringResource(id = R.string.continue_button),
                    onClick = { viewModel.navigateToLogin() },
                    enabled = state.activeServerId != null
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            LoadingScreenContent(
                message = stringResource(id = R.string.loading_servers),
                modifier = Modifier.padding(paddingValues)
            )
        } else if (state.servers.isEmpty()) {
            EmptyScreenContent(
                message = stringResource(id = R.string.servers_empty),
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                //contentPadding = PaddingValues(4.dp)
            ) {
                items(
                    items = state.servers,
                    key = { server -> server.id }
                ) { server ->
                    // Получаем или создаем состояние свайпа для этого сервера
                    val swipeState = swipeStates.getOrPut(server.id) {
                        rememberSwipeToDismissBoxState(
                            initialValue = SwipeToDismissBoxValue.Settled,
                            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.onDeleteServerClick(server.id, server.name)
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                    }

                    ServerListItem(
                        name = server.name,
                        id = server.id.toString(),
                        host = server.host,
                        port = server.port.toString(),
                        isActive = server.isActive,
                        onClick = { viewModel.onServerClick(server.id) },
                        dismissState = swipeState
                    )
                }
            }
        }

        // Обработка ошибок
        state.error?.let { error ->
            LaunchedEffect(error) {
                snackbarHostState.showSnackbar(error)
            }
        }
    }
}

@Composable
fun ServerListItem(
    name: String,
    id: String,
    host: String,
    port: String,
    isActive: Boolean,
    onClick: () -> Unit,
    dismissState: SwipeToDismissBoxState,
    modifier: Modifier = Modifier
) {
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.errorContainer,
                label = "Dismiss Background"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(color, MaterialTheme.shapes.medium)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            // Карточка сервера
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .clickable(onClick = onClick),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "$name ($id)",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isActive)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$host:$port",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isActive)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (isActive) {
                            Text(
                                text = stringResource(id = R.string.active),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    )
}