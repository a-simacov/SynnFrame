package com.synngate.synnframe.presentation.ui.dynamicmenu.task

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.createComponentGroups
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.rememberGenericScreenComponentRegistry
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.component.initializeTaskComponents
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTasksEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTasksState
import kotlinx.coroutines.launch

@Composable
fun DynamicTasksScreen(
    viewModel: DynamicTasksViewModel,
    navigateToTaskDetail: (taskId: String, endpoint: String) -> Unit, // Обновлен параметр
    navigateToTaskXDetail: (taskId: String) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DynamicTasksEvent.NavigateBack -> {
                    navigateBack()
                }
                is DynamicTasksEvent.ShowSnackbar -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is DynamicTasksEvent.NavigateToTaskDetail -> {
                    navigateToTaskDetail(event.taskId, viewModel.endpoint)
                }
                is DynamicTasksEvent.NavigateToTaskXDetail -> {
                    navigateToTaskXDetail(event.taskId)
                }
            }
        }
    }

    // Создаем и настраиваем реестр компонентов
    val componentRegistry = rememberGenericScreenComponentRegistry<DynamicTasksState>()

    // Инициализируем компоненты для заданий
    componentRegistry.initializeTaskComponents(
        tasksProvider = { it.tasks },
        isLoadingProvider = { it.isLoading },
        errorProvider = { it.error },
        onTaskClickProvider = { { task -> viewModel.onTaskClick(task) } },
        searchValueProvider = { it.searchValue },
        onSearchValueChangedProvider = { { value -> viewModel.onSearchValueChanged(value) } },
        onSearchProvider = { { viewModel.onSearch() } }
    )

    // Создаем группы компонентов на основе настроек экрана
    val componentGroups = createComponentGroups(state, componentRegistry)

    AppScaffold(
        title = state.menuItemName,
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        onDismissNotification = { viewModel.clearError() },
        actions = {
            IconButton(onClick = { viewModel.loadDynamicTasks() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.refresh)
                )
            }
        },
        isLoading = state.isLoading
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!state.hasElement(ScreenElementType.SHOW_LIST) && !state.isLoading && state.tasks.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_tasks_available),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                ) {
                    // Отображаем компоненты без веса (обычно поиск, фильтры и т.д.)
                    componentGroups.fixedComponents.forEach { component ->
                        component.Render(Modifier.fillMaxWidth())
                    }

                    // Отображаем компоненты с весом (обычно списки, которые должны занимать оставшееся пространство)
                    componentGroups.weightedComponents.forEach { component ->
                        component.Render(
                            Modifier
                                .fillMaxWidth()
                                .weight(component.getWeight())
                        )
                    }
                }
            }
        }
    }
}