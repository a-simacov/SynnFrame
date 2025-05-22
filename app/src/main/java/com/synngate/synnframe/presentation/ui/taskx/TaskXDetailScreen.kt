package com.synngate.synnframe.presentation.ui.taskx

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.taskx.components.CompactActionDisplayModeSwitcher
import com.synngate.synnframe.presentation.ui.taskx.components.CompactActionSearchBar
import com.synngate.synnframe.presentation.ui.taskx.components.CompactNextActionButton
import com.synngate.synnframe.presentation.ui.taskx.components.CompactSavableObjectsPanel
import com.synngate.synnframe.presentation.ui.taskx.components.CompactTaskInfoCard
import com.synngate.synnframe.presentation.ui.taskx.components.FactActionsView
import com.synngate.synnframe.presentation.ui.taskx.components.FilterIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.InitialActionsRequiredDialog
import com.synngate.synnframe.presentation.ui.taskx.components.PlannedActionsView
import com.synngate.synnframe.presentation.ui.taskx.components.SearchResultsIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.TaskXActionsDialog
import com.synngate.synnframe.presentation.ui.taskx.components.TaskXVerificationDialog
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailView
import kotlinx.coroutines.delay
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
    val task = state.task
    val nextActionId = state.nextActionId
    val coroutineScope = rememberCoroutineScope()

    // Scanner listener setup
    val scannerService = LocalScannerService.current
    if (state.showSearchField && scannerService?.hasRealScanner() == true) {
        ScannerListener(onBarcodeScanned = { barcode ->
            viewModel.searchByScanner(barcode)
        })
    }

    // Back button handling
    BackHandler(enabled = !state.showActionsDialog) {
        viewModel.handleBackNavigation()
    }

    // Events handling
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
                is TaskXDetailEvent.TaskActionCompleted -> {
                    navigateBack()
                    coroutineScope.launch {
                        delay(100)
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    HandleDialogs(state, viewModel, navigateBack)

    // Main screen content
    AppScaffold(
        showTopBar = false,
        title = task?.taskTypeId?.let { viewModel.formatTaskType() } ?: "Unknown",
        onNavigateBack = { viewModel.handleBackNavigation() },
        snackbarHostState = snackbarHostState,
        notification = state.error?.let { Pair(it, StatusType.ERROR) },
        isLoading = state.isLoading,
        floatingActionButton = {
            // Compact Next Action button as FAB (only shows when applicable)
            if (task?.status == TaskXStatus.IN_PROGRESS &&
                state.taskType?.strictActionOrder != true &&
                state.activeView == TaskXDetailView.PLANNED_ACTIONS) {
                CompactNextActionButton(
                    onClick = {
                        task.getNextAction()?.let { action ->
                            viewModel.startActionExecution(action.id)
                        }
                    }
                )
            }
        }
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
                .padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            // Header area with compact task info
            CompactTaskInfoCard(
                task = task,
                taskTypeName = viewModel.formatTaskType(),
                formatDate = viewModel::formatDate,
                initiallyExpanded = false,
                onShowPlannedActions = viewModel::showPlannedActions,
                onShowFactActions = viewModel::showFactActions,
                activeView = state.activeView
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Compact filters and search area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                // Display mode switcher (occupies most of the width)
                CompactActionDisplayModeSwitcher(
                    currentMode = state.actionsDisplayMode,
                    onModeChange = { mode -> viewModel.setActionsDisplayMode(mode) },
                    hasFinalActions = task.plannedActions.any { it.isFinalAction },
                    hasInitialActions = task.plannedActions.any { it.isInitialAction },
                    modifier = Modifier.weight(1f)
                )
            }

            // Compact search bar
            if (state.activeView == TaskXDetailView.PLANNED_ACTIONS) {
                CompactActionSearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onSearch = viewModel::searchActions,
                    onClear = viewModel::clearSearch,
                    onScannerClick = viewModel::toggleCameraScannerForSearch,
                    isSearching = state.isSearching,
                    error = state.searchError,
                    visible = state.showSearchField,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Compact savable objects panel
            if (state.activeView == TaskXDetailView.PLANNED_ACTIONS) {
                CompactSavableObjectsPanel(
                    savableObjects = state.savableObjects,
                    onRemoveObject = viewModel::removeSavableObject,
                    onFilterByObjects = viewModel::enableSavableObjectsFiltering,
                    visible = state.showSavableObjectsPanel && state.supportsSavableObjects,
                    modifier = Modifier.fillMaxWidth()
                )

                // Filter indicators and search results
                SearchResultsIndicator(
                    resultsCount = state.filteredActions.size,
                    visible = state.searchInfo.isNotEmpty() && !state.isFilteredBySavableObjects,
                    modifier = Modifier.fillMaxWidth()
                )

                FilterIndicator(
                    message = state.filterMessage,
                    onClearFilter = viewModel::clearAllFilters,
                    visible = state.isFilteredBySavableObjects && state.filterMessage.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Main content area - action lists
            Box(modifier = Modifier.weight(1f)) {
                when (state.activeView) {
                    TaskXDetailView.PLANNED_ACTIONS -> {
                        PlannedActionsView(
                            plannedActions = state.filteredActions,
                            factActions = task.factActions,
                            nextActionId = nextActionId,
                            onActionClick = { action ->
                                if (task.status == TaskXStatus.IN_PROGRESS && !action.isSkipped) {
                                    val isCompleted = action.isActionCompleted(task.factActions)
                                    if (isCompleted) {
                                        if (viewModel.canReopenAction(action, isCompleted)) {
                                            viewModel.startActionExecution(action.id)
                                        }
                                    } else {
                                        viewModel.tryExecuteAction(action.id)
                                    }
                                }
                            },
                            onToggleCompletion = { action, completed ->
                                if (viewModel.canManageCompletionStatus(action)) {
                                    viewModel.toggleActionCompletion(action.id, completed)
                                }
                            }
                        )
                    }
                    TaskXDetailView.FACT_ACTIONS -> {
                        FactActionsView(
                            factActions = task.factActions,
                            formatDate = viewModel::formatDate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HandleDialogs(
    state: TaskXDetailState,
    viewModel: TaskXDetailViewModel,
    navigateBack: () -> Unit
) {
    // Task actions dialog
    if (state.showActionsDialog) {
        TaskXActionsDialog(
            onDismiss = { viewModel.hideActionsDialog() },
            onNavigateBack = { navigateBack() },
            statusActions = state.statusActions,
            isProcessing = state.isProcessingDialogAction
        )
    }

    // Completion confirmation dialog
    if (state.showCompletionDialog) {
        ConfirmationDialog(
            title = "Завершить задание?",
            message = "Вы уверены, что хотите завершить задание?",
            onConfirm = { viewModel.completeTask() },
            onDismiss = { viewModel.hideCompletionDialog() }
        )
    }

    // Verification dialog
    if (state.showVerificationDialog) {
        TaskXVerificationDialog(
            onBarcodeScan = { barcode -> viewModel.verifyTask(barcode) },
            onDismiss = { viewModel.hideVerificationDialog() }
        )
    }

    // Order required message dialog
    if (state.showOrderRequiredMessage) {
        AlertDialog(
            onDismissRequest = { viewModel.hideOrderRequiredMessage() },
            title = { Text("Соблюдение порядка") },
            text = { Text("Действия необходимо выполнять в указанном порядке. Пожалуйста, выполните первое не завершенное действие.") },
            confirmButton = {
                Button(onClick = { viewModel.hideOrderRequiredMessage() }) {
                    Text("ОК")
                }
            }
        )
    }

    // Initial actions required dialog
    if (state.showInitialActionsRequiredDialog) {
        InitialActionsRequiredDialog(
            onDismiss = { viewModel.hideInitialActionsRequiredDialog() },
            onGoToInitialActions = {
                viewModel.hideInitialActionsRequiredDialog()
                viewModel.goToInitialActions()
            },
            completedCount = state.completedInitialActionsCount,
            totalCount = state.totalInitialActionsCount
        )
    }

    // Camera scanner dialog
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
}