package com.synngate.synnframe.presentation.ui.dynamicmenu.task

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.search.SearchResultIndicator
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.di.ScreenContainer
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.SavedKeyInputDialog
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.TaskDeleteConfirmationDialog
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
    navigateToTaskXDetail: (taskId: String, endpoint: String) -> Unit,
    navigateBack: () -> Unit,
    screenContainer: ScreenContainer,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Проверяем наличие физического сканера
    val scannerService = LocalScannerService.current
    val hasPhysicalScanner = scannerService?.hasRealScanner() == true

    // Обработка сканирования через физический сканер
    // Активен только когда диалог закрыт
    if (hasPhysicalScanner && !state.showSavedKeyDialog) {
        BarcodeHandlerWithState(
            stepKey = "dynamic_tasks_${state.menuItemId}",
            onBarcodeScanned = { barcode, setProcessingState ->
                viewModel.onBarcodeScanned(barcode)
                setProcessingState(false)
            }
        )
    }

    BackHandler {
        when {
            state.showDeleteDialog -> {
                viewModel.hideDeleteDialog()
            }
            state.showSavedKeyDialog -> {
                viewModel.hideSavedKeyDialog()
            }
            else -> {
                viewModel.onNavigateBack()
            }
        }
    }

    // Обновляем список заданий при возврате на экран
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Принудительно обновляем список каждый раз при возврате на экран
            viewModel.forceRefreshTasks()
        }
    }

    // Дополнительная проверка - обновляем список при каждом рендере экрана
    LaunchedEffect(Unit) {
        viewModel.forceRefreshTasks()
    }

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
                    // Используем новый подход с передачей endpoint
                    navigateToTaskXDetail(event.taskId, event.endpoint)
                }

                is DynamicTasksEvent.RefreshTaskList -> {
                    viewModel.loadDynamicTasks()
                }
            }
        }
    }

    // Создаем и настраиваем реестр компонентов
    val componentRegistry = rememberGenericScreenComponentRegistry<DynamicTasksState>()

    // Диалог для ввода сохраняемого ключа
    if (state.showSavedKeyDialog) {
        SavedKeyInputDialog(
            onDismiss = viewModel::hideSavedKeyDialog,
            onConfirm = viewModel::validateAndSaveKey,
            isLoading = state.isValidatingKey,
            error = state.keyValidationError
        )
    }

    if (state.showDeleteDialog && state.taskToDelete != null) {
        TaskDeleteConfirmationDialog(
            task = state.taskToDelete!!,
            onConfirm = viewModel::deleteTask,
            onDismiss = viewModel::hideDeleteDialog,
            isDeleting = state.isDeleting
        )
    }

    // Инициализируем компоненты для заданий
    componentRegistry.initializeTaskComponents(
        tasksProvider = { it.tasks },
        isLoadingProvider = { it.isLoading },
        errorProvider = { it.error },
        onTaskClickProvider = { { task -> viewModel.onTaskClick(task) } },
        onTaskLongClickProvider = { { task -> viewModel.onTaskLongClick(task) } },
        searchValueProvider = { it.searchValue },
        onSearchValueChangedProvider = { { value -> viewModel.onSearchValueChanged(value) } },
        onSearchProvider = { { viewModel.onSearch() } },
        savedSearchKeyProvider = { it.savedSearchKey },
        hasValidSavedSearchKeyProvider = { it.hasValidSavedSearchKey },
        onClearSavedKeyProvider = { { viewModel.clearSavedSearchKey() } },
    )

    // Создаем группы компонентов на основе настроек экрана
    val componentGroups = createComponentGroups(state, componentRegistry)

    AppScaffold(
        title = state.menuItemName,
        onNavigateBack = viewModel::onNavigateBack,
        actions = {
            IconButton(onClick = { viewModel.loadDynamicTasks() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.refresh)
                )
            }
        },
        floatingActionButton = {
            if (state.canCreateTask()) {
                val needsKey = viewModel.shouldShowKeyHint()
                FloatingActionButton(
                    onClick = { viewModel.onFabClick() },
                    containerColor = if (needsKey) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (needsKey) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                ) {
                    Icon(
                        imageVector = if (needsKey) Icons.Default.Key else Icons.Default.Add,
                        contentDescription = if (needsKey) "Add saved key" else "Create task"
                    )
                }
            }
        },
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        onDismissNotification = { viewModel.clearError() },
        isLoading = state.isLoading || state.isCreatingTask,
        useScanner = true
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

                    // Показываем индикатор результатов поиска, если есть результаты
                    if (state.shouldShowSearchIndicator()) {
                        state.searchResultType?.let { resultType ->
                            SearchResultIndicator(
                                resultType = resultType,
                                count = state.tasks.size,
                                query = state.lastSearchQuery
                            )
                        }
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