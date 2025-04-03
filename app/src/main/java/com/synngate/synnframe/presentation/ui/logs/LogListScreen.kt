package com.synngate.synnframe.presentation.ui.logs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.presentation.common.dialog.DateFilterSummary
import com.synngate.synnframe.presentation.common.dialog.DateTimeFilterDialog
import com.synngate.synnframe.presentation.common.filter.StatusFilterChips
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.list.LogListItem
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.logs.model.LogListUiEvent

@Composable
fun LogListScreen(
    viewModel: LogListViewModel,
    navigateToLogDetail: (Int) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LogListUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is LogListUiEvent.NavigateToLogDetail -> {
                    navigateToLogDetail(event.logId)
                }
            }
        }
    }

    if (state.isDeleteAllConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteAllConfirmation() },
            title = { Text(stringResource(R.string.delete_all_logs_title)) },
            text = { Text(stringResource(R.string.delete_all_logs_message)) },
            confirmButton = {
                Button(onClick = { viewModel.onDeleteAllConfirmed() }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.hideDeleteAllConfirmation() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (state.isCleanupDialogVisible) {
        LogCleanupDialog(
            onDismiss = { viewModel.hideCleanupDialog() },
            onConfirm = { days -> viewModel.onCleanupConfirmed(days) }
        )
    }

    if (state.isDateFilterDialogVisible) {
        DateTimeFilterDialog(
            fromDate = state.dateFromFilter,
            toDate = state.dateToFilter,
            onApply = { fromDate, toDate ->
                viewModel.updateDateFilter(fromDate, toDate)
            },
            onDismiss = { viewModel.hideDateFilterDialog() }
        )
    }

    AppScaffold(
        title = stringResource(id = R.string.logs),
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        actions = {
            FilterByDateButton(onClick = { viewModel.showDateFilterDialog() })
            CleanupLogsButton(onClick = { viewModel.showCleanupDialog() })
            DeleteAllLogsButton(onClick = { viewModel.showDeleteAllConfirmation() })
        },
        isLoading = state.isLoading
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchTextField(
                value = state.messageFilter,
                onValueChange = { viewModel.updateMessageFilter(it) },
                label = stringResource(id = R.string.search_logs),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onSearch = { viewModel.loadLogs() }
            )

            val logTypes = remember { LogType.entries.toList() }

            StatusFilterChips(
                items = logTypes,
                selectedItems = state.selectedTypes,
                onSelectionChanged = { viewModel.updateTypeFilter(it) },
                itemToString = { viewModel.formatLogType(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            // Если активен фильтр по дате, показываем информацию о нем
            if (state.hasActiveDateFilter) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    DateFilterSummary(
                        fromDate = state.dateFromFilter,
                        toDate = state.dateToFilter,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    IconButton(
                        onClick = { viewModel.clearDateFilter() },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_date_filter)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            if (state.logs.isEmpty()) {
                EmptyScreenContent(
                    message = if (state.hasActiveFilters)
                        stringResource(R.string.no_logs_with_filter)
                    else
                        stringResource(R.string.no_logs)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.logs,
                        key = { it.id }
                    ) { log ->
                        LogListItem(
                            message = log.getShortMessage(),
                            type = log.type,
                            createdAt = viewModel.formatLogDate(log.createdAt),
                            onClick = { viewModel.navigateToLogDetail(log.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteAllLogsButton(onClick: () -> Unit) {
    IconButton(
        onClick = { onClick.invoke() }
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.delete_all_logs)
        )
    }
}

@Composable
private fun CleanupLogsButton(onClick: () -> Unit) {
    IconButton(
        onClick = { onClick.invoke() }
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = stringResource(R.string.cleanup_old_logs)
        )
    }
}

@Composable
private fun FilterByDateButton(onClick: () -> Unit) {
    IconButton(
        onClick = { onClick.invoke() }
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = stringResource(R.string.date_filter)
        )
    }
}

@Composable
fun LogCleanupDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var daysToKeep by remember { mutableStateOf("30") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cleanup_logs_title)) },
        text = {
            Column {
                Text(stringResource(R.string.cleanup_logs_message))
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = daysToKeep,
                    onValueChange = {
                        daysToKeep = it
                        isError = it.toIntOrNull() == null || (it.toIntOrNull() ?: 0) <= 0
                    },
                    label = { Text(stringResource(R.string.days_to_keep)) },
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(
                        text = stringResource(R.string.invalid_days_value),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysToKeep.toIntOrNull() ?: 0
                    if (days > 0) {
                        onConfirm(days)
                    } else {
                        isError = true
                    }
                },
                enabled = !isError && daysToKeep.isNotEmpty()
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}