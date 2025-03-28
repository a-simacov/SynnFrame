package com.synngate.synnframe.presentation.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.presentation.common.filter.StatusFilterChips
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.tasks.components.TaskListItem
import com.synngate.synnframe.presentation.ui.tasks.model.TaskListEvent

@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    navigateToTaskDetail: (String) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val showTypeMenu = state.showTypeMenu

    // В TaskListScreen добавим сохранение состояния фильтров
    var showFilterPanel by rememberSaveable { mutableStateOf(state.isFilterPanelVisible) }

    LaunchedEffect(showFilterPanel) {
        if (showFilterPanel != state.isFilterPanelVisible) {
            viewModel.toggleFilterPanel()
        }
    }

    // Обработка событий от ViewModel
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskListEvent.NavigateToTaskDetail -> {
                    navigateToTaskDetail(event.taskId)
                }
                is TaskListEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is TaskListEvent.ShowDatePicker -> {
                    // В реальном приложении здесь был бы вызов диалога выбора даты
                    // На данном этапе мы просто игнорируем это событие
                }

                is TaskListEvent.CreateTask -> {
                    viewModel.createNewTask()
                }
            }
        }
    }

    AppScaffold(
        title = stringResource(id = R.string.tasks),
        subtitle = stringResource(id = R.string.tasks_count, state.tasksCount),
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        isSyncing = state.isSyncing,
        lastSyncTime = state.lastSyncTime,
        actions = {
            // Кнопка синхронизации с сервером
            IconButton(
                onClick = { viewModel.syncTasks() },
                enabled = !state.isSyncing
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = stringResource(id = R.string.sync_tasks)
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.createNewTask() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(id = R.string.create_task)) }
            )
        },
        isLoading = state.isLoading
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = stringResource(id = R.string.search_tasks),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = stringResource(id = R.string.search_tasks_hint)
            )

            val taskTypes = remember { TaskType.entries.toList() }
            val statusTypes = remember { TaskStatus.entries.toList() }

            Row {
                StatusFilterChips(
                    items = taskTypes,
                    selectedItems = state.selectedTypeFilters,
                    onSelectionChanged = { viewModel.updateTypeFilter(it) },
                    itemToString = { viewModel.formatTaskType(it) },
                    modifier = Modifier
                        //.fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
                StatusFilterChips(
                    items = statusTypes,
                    selectedItems = state.selectedStatusFilters,
                    onSelectionChanged = { viewModel.updateStatusFilter(it) },
                    itemToString = { viewModel.formatStatusType(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            HorizontalDivider()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(
                        id = R.string.filtered_tasks_count,
                        state.tasks.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.tasks.isEmpty()) {
                EmptyScreenContent(
                    message = if (state.searchQuery.isNotEmpty() ||
                        state.selectedStatusFilters.isNotEmpty() ||
                        state.selectedTypeFilters != null ||
                        state.dateFromFilter != null)
                        stringResource(id = R.string.no_tasks_with_filter)
                    else
                        stringResource(id = R.string.no_tasks)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.tasks,
                        key = { it.id }
                    ) { task ->
                        TaskListItem(
                            task = task,
                            onClick = { viewModel.onTaskClick(task.id) }
                        )
                    }
                }
            }
        }
    }
}