package com.synngate.synnframe.presentation.ui.taskx

import TaskCompletionDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scaffold.SearchButton
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.common.status.TaskXStatusIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.ActionFilterChipList
import com.synngate.synnframe.presentation.ui.taskx.components.ActionFilterChips
import com.synngate.synnframe.presentation.ui.taskx.components.ActionSearchBar
import com.synngate.synnframe.presentation.ui.taskx.components.BufferItemChipList
import com.synngate.synnframe.presentation.ui.taskx.components.ExpandableTaskInfoCard
import com.synngate.synnframe.presentation.ui.taskx.components.PlannedActionCard
import com.synngate.synnframe.presentation.ui.taskx.components.TaskProgressIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.TaskXExitDialog
import com.synngate.synnframe.presentation.ui.taskx.components.ValidationErrorDialog
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import kotlinx.coroutines.launch

@Composable
fun TaskXDetailScreen(
    viewModel: TaskXDetailViewModel,
    navigateBack: () -> Unit,
    navigateToActionWizard: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val task = state.task

    val scannerService = LocalScannerService.current
    if (scannerService?.hasRealScanner() == true) {
        ScannerListener(onBarcodeScanned = { barcode ->
            viewModel.searchByScanner(barcode)
        })
    }

    BackHandler {
        viewModel.onBackPressed()
    }

    // Обработка возврата из визарда
    LaunchedEffect(Unit) {
        viewModel.onReturnFromWizard()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskXDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is TaskXDetailEvent.NavigateToActionWizard -> {
                    navigateToActionWizard(event.taskId, event.actionId)
                }
                is TaskXDetailEvent.NavigateBack -> {
                    navigateBack()
                }
                is TaskXDetailEvent.NavigateBackWithMessage -> {
                    navigateBack()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    if (state.showValidationErrorDialog && state.validationErrorMessage != null) {
        ValidationErrorDialog(
            errorMessage = state.validationErrorMessage!!,
            onDismiss = viewModel::dismissValidationErrorDialog
        )
    }

    if (state.showExitDialog) {
        TaskXExitDialog(
            onDismiss = viewModel::dismissExitDialog,
            onContinue = viewModel::continueWork,
            onPause = viewModel::pauseTask,
            onComplete = viewModel::completeTask,
            onExitWithoutSaving = viewModel::exitWithoutSaving,
            canComplete = state.getPendingActionsCount() == 0,
            isProcessing = state.isProcessingAction
        )
    }

    if (state.showCameraScannerForSearch) {
        UniversalScannerDialog(
            onBarcodeScanned = { barcode ->
                viewModel.searchByScanner(barcode)
                viewModel.hideCameraScannerForSearch()
            },
            onClose = {
                viewModel.hideCameraScannerForSearch()
            },
            instructionText = "Отсканируйте штрихкод для поиска действия"
        )
    }

    if (state.showCompletionDialog) {
        TaskCompletionDialog(
            onDismiss = viewModel::dismissCompletionDialog,
            onConfirm = viewModel::completeTask,
            isProcessing = state.isProcessingAction
        )
    }

    LaunchedEffect(state.actionUiModels) {
        viewModel.checkTaskCompletion()
    }

    AppScaffold(
        title = task?.name ?: stringResource(R.string.loading_task),
        onNavigateBack = viewModel::onBackPressed,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        isLoading = state.isLoading,
        actions = {
            if (state.bufferItems.isNotEmpty()) {
                IconButton(onClick = viewModel::toggleBufferDisplay) {
                    Icon(
                        imageVector = Icons.Default.Storage, // Нужно добавить импорт
                        contentDescription = if (state.showBufferItems) "Скрыть буфер" else "Показать буфер",
                        tint = if (state.showBufferItems) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // Добавляем кнопку поиска, если тип задания поддерживает поиск
            if (state.task?.taskType?.isActionSearchEnabled() == true) {
                SearchButton(
                    isSearchActive = state.showSearchBar,
                    onToggleSearch = viewModel::toggleSearchBar
                )
            }
        }
    ) { paddingValues ->
        if (task == null) {
            EmptyScreenContent(
                message = stringResource(R.string.loading_task),
                modifier = Modifier.padding(paddingValues)
            )
            return@AppScaffold
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(4.dp)
        ) {
            ExpandableTaskInfoCard(
                title = stringResource(R.string.task_details),
                initiallyExpanded = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Имя: ${task.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Штрихкод: ${task.barcode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                TaskXStatusIndicator(
                    status = task.status,
                )

                task.startedAt?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Начато: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                task.lastModifiedAt?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Изменено: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TaskProgressIndicator(
                task = task,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            ActionFilterChips(
                currentFilter = state.actionFilter,
                onFilterChange = viewModel::onFilterChange,
                hasInitialActions = state.hasInitialActions(),
                hasFinalActions = state.hasFinalActions()
            )

            // Поисковая строка - всегда располагается под чипами фильтра
            if (state.showSearchBar) {
                ActionSearchBar(
                    query = state.searchValue,
                    onQueryChange = viewModel::setSearchValue,
                    onSearch = { viewModel.searchByText(state.searchValue) },
                    onClear = viewModel::clearSearchValue,
                    onScannerClick = viewModel::showCameraScannerForSearch,
                    isSearching = state.isSearching,
                    error = state.searchError,
                    visible = state.showSearchBar
                )
            }

            if (state.showBufferItems && state.bufferItems.isNotEmpty()) {
                BufferItemChipList(
                    items = state.bufferItems,
                    onRemove = viewModel::removeBufferItem,
                    onClearAll = viewModel::clearAllBufferItems
                )
            }

            // Отображение активных фильтров
            if (state.activeFilters.isNotEmpty()) {
                ActionFilterChipList(
                    filters = state.activeFilters,
                    onRemove = viewModel::removeFilter,
                    onClearAll = viewModel::clearAllFilters
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Используем getFilteredActions вместо getDisplayActions
                val displayActions = state.getFilteredActions()

                if (displayActions.isEmpty()) {
                    item {
                        Text(
                            text = "Нет действий для отображения",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                } else {
                    items(
                        items = displayActions,
                        key = { it.id }
                    ) { actionUI ->
                        PlannedActionCard(
                            actionUI = actionUI,
                            onClick = { viewModel.onActionClick(actionUI.id) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}