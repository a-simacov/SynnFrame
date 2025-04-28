package com.synngate.synnframe.presentation.ui.dynamicmenu.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.createComponentGroups
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.rememberGenericScreenComponentRegistry
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.component.initializeTaskComponents
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTasksEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTasksState

@Composable
fun DynamicTasksScreen(
    viewModel: DynamicTasksViewModel,
    navigateToTaskDetail: (DynamicTask) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Создаем и инициализируем универсальный реестр компонентов
    val componentRegistry = rememberGenericScreenComponentRegistry<DynamicTasksState>()

    // Инициализируем реестр для заданий
    LaunchedEffect(Unit) {
        componentRegistry.initializeTaskComponents(
            tasksProvider = { it.tasks },
            isLoadingProvider = { it.isLoading },
            errorProvider = { it.error },
            onTaskClickProvider = { { task -> navigateToTaskDetail(task) } },
            searchValueProvider = { it.searchValue },
            onSearchValueChangedProvider = { { value -> viewModel.onSearchValueChanged(value) } },
            onSearchProvider = { { viewModel.onSearch() } }
        )
    }

    // Получаем сгруппированные компоненты
    val componentGroups = createComponentGroups(state, componentRegistry)

    // Обработка событий навигации
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DynamicTasksEvent.NavigateBack -> {
                    navigateBack()
                }
                is DynamicTasksEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = androidx.compose.material3.SnackbarDuration.Short
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
                .padding(horizontal = 16.dp)
        ) {
            componentGroups.fixedComponents.forEach { component ->
                component.Render(Modifier.fillMaxWidth())
            }

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