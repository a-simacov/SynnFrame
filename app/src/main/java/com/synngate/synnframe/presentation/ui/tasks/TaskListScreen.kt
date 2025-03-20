package com.synngate.synnframe.presentation.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.presentation.common.filter.DateRangeFilter
import com.synngate.synnframe.presentation.common.filter.FilterPanel
import com.synngate.synnframe.presentation.common.filter.TypeDropdownFilter
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scaffold.LoadingScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.tasks.components.TaskListItem
import com.synngate.synnframe.presentation.ui.tasks.components.TaskStatusFilterChips
import com.synngate.synnframe.presentation.ui.tasks.model.TaskListEvent
import java.time.format.DateTimeFormatter

@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    navigateToTaskDetail: (String) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Получаем состояние из ViewModel
    val state by viewModel.uiState.collectAsState()

    // Для отображения Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Форматтер для дат
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    // Меню с типами заданий
    var showTypeMenu by remember { mutableStateOf(false) }

    // В TaskListScreen добавим сохранение состояния фильтров
    var showFilterPanel by rememberSaveable { mutableStateOf(state.isFilterPanelVisible) }

// При изменении обновляем состояние во ViewModel
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
            }
        }
    }

    // Основной интерфейс
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
            // Кнопка для создания нового задания (можно добавить позже)
            ExtendedFloatingActionButton(
                onClick = { /* Создание нового задания */ },
                icon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(id = R.string.create_task)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Поле поиска
            SearchTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = stringResource(id = R.string.search_tasks),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = stringResource(id = R.string.search_tasks_hint)
            )

            // Фильтры по статусам
            TaskStatusFilterChips(
                selectedStatuses = state.selectedStatusFilters,
                onStatusSelected = { viewModel.toggleStatusFilter(it) },
                onClearFilters = { viewModel.clearStatusFilters() }
            )

            // Кнопка отображения/скрытия панели дополнительных фильтров
            OutlinedButton(
                onClick = { viewModel.toggleFilterPanel() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (state.isFilterPanelVisible)
                        stringResource(id = R.string.hide_filters)
                    else stringResource(id = R.string.show_filters)
                )
            }

            // Панель дополнительных фильтров
            AnimatedVisibility(
                visible = state.isFilterPanelVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FilterPanel(
                    isVisible = true,
                    onVisibilityChange = { viewModel.toggleFilterPanel() }
                ) {
                    // Фильтр по датам
                    DateRangeFilter(
                        fromDate = state.dateFromFilter,
                        toDate = state.dateToFilter,
                        onFromDateChange = { viewModel.updateDateFromFilter(it) },
                        onToDateChange = { viewModel.updateDateToFilter(it) },
                        onApply = { /* Применить фильтр, уже реализовано в ViewModel */ },
                        onClear = { viewModel.clearDateFilter() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Фильтр по типу задания
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.task_type_filter),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = { showTypeMenu = true }
                        ) {
                            Text(
                                text = when (state.selectedTypeFilter) {
                                    TaskType.RECEIPT -> stringResource(id = R.string.task_type_receipt)
                                    TaskType.PICK -> stringResource(id = R.string.task_type_pick)
                                    null -> stringResource(id = R.string.all_types)
                                }
                            )
                        }

                        // Выпадающее меню для выбора типа задания
                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.all_types)) },
                                onClick = {
                                    viewModel.updateTypeFilter(null)
                                    showTypeMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.task_type_receipt)) },
                                onClick = {
                                    viewModel.updateTypeFilter(TaskType.RECEIPT)
                                    showTypeMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.task_type_pick)) },
                                onClick = {
                                    viewModel.updateTypeFilter(TaskType.PICK)
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            // Отображение количества заданий и результатов фильтрации
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

            // Список заданий
            if (state.isLoading) {
                LoadingScreenContent(message = stringResource(id = R.string.loading_tasks))
            } else if (state.tasks.isEmpty()) {
                EmptyScreenContent(
                    message = if (state.searchQuery.isNotEmpty() ||
                        state.selectedStatusFilters.isNotEmpty() ||
                        state.selectedTypeFilter != null ||
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
                        key = { it.id } // Используем ключ для оптимизации
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