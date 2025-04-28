package com.synngate.synnframe.presentation.ui.dynamicmenu

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
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.DynamicTasksScreenEvents
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.initialize
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.rememberScreenComponentRegistry
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

    // Создаем объект с событиями для передачи компонентам
    val events = remember {
        DynamicTasksScreenEvents(
            onSearchValueChanged = viewModel::onSearchValueChanged,
            onSearch = viewModel::onSearch,
            onRefresh = viewModel::onRefresh,
            onTaskClick = { task -> navigateToTaskDetail(task) }
        )
    }

    // Создаем и инициализируем реестр компонентов
    val componentRegistry = rememberScreenComponentRegistry(events)
    LaunchedEffect(componentRegistry) {
        componentRegistry.initialize()
    }

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
            // Разделяем компоненты на две группы:
            // 1. Компоненты с фиксированным размером
            // 2. Компоненты с весом (weight)

            // Отображаем компоненты с фиксированным размером
            state.screenSettings.screenElements.forEach { elementType ->
                val component = componentRegistry.createComponent(elementType, state)
                if (component != null && !component.usesWeight()) {
                    component.Render(Modifier.fillMaxWidth())
                }
            }

            // Отображаем компоненты с весом
            state.screenSettings.screenElements.forEach { elementType ->
                val component = componentRegistry.createComponent(elementType, state)
                if (component != null && component.usesWeight()) {
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