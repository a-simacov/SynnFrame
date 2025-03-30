package com.synngate.synnframe.presentation.ui.logs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.ErrorScreenContent
import com.synngate.synnframe.presentation.common.scaffold.LoadingScreenContent
import com.synngate.synnframe.presentation.common.status.LogTypeIndicator
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.logs.model.LogDetailEvent
import java.time.format.DateTimeFormatter

@Composable
fun LogDetailScreen(
    viewModel: LogDetailViewModel,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Обрабатываем события из ViewModel
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LogDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is LogDetailEvent.NavigateBack -> {
                    navigateBack()
                }
                is LogDetailEvent.ShowDeleteConfirmation -> {
                    // Состояние диалога обновляется в ViewModel
                }
                is LogDetailEvent.HideDeleteConfirmation -> {
                    // Состояние диалога обновляется в ViewModel
                }
            }
        }
    }

    // Диалог подтверждения удаления
    if (state.showDeleteConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_log_title),
            message = stringResource(R.string.delete_log_message),
            onConfirm = {
                viewModel.deleteLog()
            },
            onDismiss = {
                viewModel.hideDeleteConfirmation()
            }
        )
    }

    AppScaffold(
        title = stringResource(id = R.string.log_details),
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        isLoading = state.isLoading || state.isDeletingLog,
        actions = {
            // Кнопки в верхней панели
            IconButton(
                onClick = { viewModel.copyLogToClipboard() }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_to_clipboard)
                )
            }

            IconButton(
                onClick = { viewModel.showDeleteConfirmation() }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_log)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    LoadingScreenContent(
                        message = stringResource(R.string.loading_log)
                    )
                }
                state.error != null && state.log == null -> {
                    ErrorScreenContent(
                        message = state.error ?: stringResource(R.string.error_loading_log),
                        onRetry = { viewModel.loadLog() }
                    )
                }
                state.log != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            state.log?.let { log ->
                                LogTypeIndicator(
                                    type = log.type,
                                    modifier = Modifier
                                )

                                Text(
                                    text = "ID: ${log.id}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                                Text(
                                    text = log.createdAt.format(formatter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f, fill = false)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.log_message),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val scrollState = rememberScrollState()

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = state.log?.message ?: "",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}