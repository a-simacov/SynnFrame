package com.synngate.synnframe.presentation.ui.taskx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.presentation.common.dialog.DateFilterSummary
import com.synngate.synnframe.presentation.common.dialog.DateTimeFilterDialog
import com.synngate.synnframe.presentation.common.filter.FilterPanel
import com.synngate.synnframe.presentation.common.filter.StatusFilterChips
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXListEvent

@Composable
fun TaskXListScreen(
    viewModel: TaskXListViewModel,
    navigateToTaskDetail: (String) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isFilterPanelVisible by remember { mutableStateOf(false) }

    // Обработка событий
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskXListEvent.NavigateToTaskDetail -> {
                    navigateToTaskDetail(event.taskId)
                }
                is TaskXListEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Показ диалога выбора даты при необходимости
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
        title = stringResource(id = R.string.taskx_title),
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        isLoading = state.isLoading,
        actions = {
            // Кнопка фильтра по дате
            IconButton(onClick = { viewModel.showDateFilterDialog() }) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = stringResource(id = R.string.date_filter)
                )
            }
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
                label = stringResource(id = R.string.taskx_search_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onSearch = { viewModel.loadTasks() }
            )

            // Панель фильтров
            FilterPanel(
                isVisible = isFilterPanelVisible,
                onVisibilityChange = { isFilterPanelVisible = it }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Фильтр по статусу
                    StatusFilterChips(
                        items = TaskXStatus.values().toList(),
                        selectedItems = state.selectedStatuses,
                        onSelectionChanged = { viewModel.updateStatusFilter(it) },
                        itemToString = { viewModel.formatTaskStatus(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Здесь могут быть дополнительные фильтры (по типу и т.д.)

                    // Кнопка сброса фильтров
                    if (state.hasActiveFilters) {
                        Button(
                            onClick = { viewModel.clearFilters() },
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 8.dp)
                        ) {
                            Text(text = stringResource(id = R.string.reset_filters))
                        }
                    }
                }
            }

            // Если активен фильтр по дате, показываем информацию о нем
            if (state.dateFromFilter != null && state.dateToFilter != null) {
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
                        onClick = { viewModel.updateDateFilter(null, null) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_date_filter)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Список заданий или сообщение о пустом списке
            if (state.tasks.isEmpty()) {
                EmptyScreenContent(
                    message = if (state.hasActiveFilters)
                        stringResource(R.string.no_tasks_with_filter)
                    else
                        stringResource(R.string.taskx_empty)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.tasks,
                        key = { it.id }
                    ) { task ->
                        TaskXListItem(
                            task = task,
                            formatStatus = { viewModel.formatTaskStatus(it) },
                            formatDate = { viewModel.formatDate(it) },
                            formatTaskType = { viewModel.formatTaskType(it) },
                            onClick = { viewModel.navigateToTaskDetail(task.id) }
                        )
                    }
                }
            }
        }
    }
}