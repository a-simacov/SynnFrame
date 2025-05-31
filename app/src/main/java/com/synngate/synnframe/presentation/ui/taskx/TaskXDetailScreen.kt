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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.taskx.components.ActionFilterChipList
import com.synngate.synnframe.presentation.ui.taskx.components.ActionFilterChips
import com.synngate.synnframe.presentation.ui.taskx.components.ActionSearchBar
import com.synngate.synnframe.presentation.ui.taskx.components.BufferItemChipList
import com.synngate.synnframe.presentation.ui.taskx.components.ExpandableTaskInfoCard
import com.synngate.synnframe.presentation.ui.taskx.components.PlannedActionCard
import com.synngate.synnframe.presentation.ui.taskx.components.TaskHeaderComponent
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
        title = task?.name ?: "Загрузка задания...",
        onNavigateBack = viewModel::onBackPressed,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        isLoading = state.isLoading,
        showTopBar = false
    ) { paddingValues ->
        if (task == null) {
            EmptyScreenContent(
                message = "Загрузка задания...",
                modifier = Modifier.padding(paddingValues)
            )
            return@AppScaffold
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(2.dp)
        ) {
            ExpandableTaskInfoCard(
                header = {
                    // Заголовок с индикатором прогресса и кнопками управления
                    TaskHeaderComponent(
                        task = task,
                        showSearchBar = state.showSearchBar,
                        showBufferItems = state.showBufferItems,
                        showFilters = state.showFilters,
                        hasBufferItems = state.bufferItems.isNotEmpty(),
                        hasFilters = state.activeFilters.isNotEmpty(),
                        onToggleSearch = viewModel::toggleSearchBar,
                        onToggleBufferDisplay = viewModel::toggleBufferDisplay,
                        onToggleFilters = viewModel::toggleFilters,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                content = {
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

                    Text(
                        text = "Статус: ${task.status.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                },
                initiallyExpanded = false,
                modifier = Modifier.fillMaxWidth()
            )

            ActionFilterChips(
                currentFilter = state.actionFilter,
                onFilterChange = viewModel::onFilterChange,
                hasInitialActions = state.hasInitialActions(),
                hasFinalActions = state.hasFinalActions()
            )

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

                if (state.activeFilters.isNotEmpty()) {
                    ActionFilterChipList(
                        filters = state.activeFilters,
                        onRemove = viewModel::removeFilter,
                        onClearAll = viewModel::clearAllFilters
                    )
                }
            }

            if (state.showBufferItems && state.bufferItems.isNotEmpty()) {
                BufferItemChipList(
                    items = state.bufferItems,
                    onRemove = viewModel::removeBufferItem,
                    onClearAll = viewModel::clearAllBufferItems
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
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

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