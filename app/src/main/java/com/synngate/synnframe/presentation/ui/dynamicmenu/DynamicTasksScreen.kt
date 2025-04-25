package com.synngate.synnframe.presentation.ui.dynamicmenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTasksEvent

@Composable
fun DynamicTasksScreen(
    viewModel: DynamicTasksViewModel,
    navigateToTaskDetail: (DynamicTask) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DynamicTasksEvent.NavigateBack -> {
                    navigateBack()
                }
                is DynamicTasksEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is DynamicTasksEvent.NavigateToTaskDetail -> {
                    navigateToTaskDetail(event.task)
                }
            }
        }
    }

    AppScaffold(
        title = state.menuItemName.ifEmpty { stringResource(id = R.string.operation_tasks_title) },
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        onDismissNotification = {
            viewModel.clearError()
        },
        actions = {
            IconButton(onClick = { viewModel.onRefresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.refresh)
                )
            }
        },
        isLoading = state.isLoading
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.hasElement(ScreenElementType.SEARCH)) {
                Spacer(modifier = Modifier.height(8.dp))

                SearchTextField(
                    value = state.searchValue,
                    onValueChange = viewModel::onSearchValueChanged,
                    label = stringResource(id = R.string.search_value),
                    onSearch = viewModel::onSearch,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = viewModel::onSearch,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.search))
                }
            }

            if (state.hasElement(ScreenElementType.SHOW_LIST)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (state.tasks.isEmpty() && !state.isLoading) {
                        Text(
                            text = if (state.error == null) {
                                stringResource(id = R.string.no_tasks_available)
                            } else {
                                formatErrorMessage(state.error)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        DynamicTasksList(
                            tasks = state.tasks,
                            onTaskClick = { task -> navigateToTaskDetail(task) }
                        )
                    }
                }
            }

            if (!state.hasElement(ScreenElementType.SEARCH) && !state.hasElement(ScreenElementType.SHOW_LIST)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Не указаны элементы для отображения на этом экране",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun formatErrorMessage(errorMessage: String?): String {
    if (errorMessage == null) return ""

    return errorMessage
        .replace("\n", ". ")
        .replace("..", ".")
        .replace(". .", ".")
}

@Composable
private fun DynamicTasksList(
    tasks: List<DynamicTask>,
    onTaskClick: (DynamicTask) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(tasks) { task ->
            DynamicTaskItem(
                task = task,
                onClick = { onTaskClick(task) }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DynamicTaskItem(
    task: DynamicTask,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(id = R.string.task_id_fmt, task.id),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}