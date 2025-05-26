package com.synngate.synnframe.presentation.ui.taskx

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.common.status.TaskXStatusIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.ExpandableTaskInfoCard
import com.synngate.synnframe.presentation.ui.taskx.components.FilterIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.InitialActionsRequiredDialog
import com.synngate.synnframe.presentation.ui.taskx.components.SearchResultsIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.TaskProgressIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.TaskXActionsDialog
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
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

    val scannerService = LocalScannerService.current
    if (state.showSearchField && scannerService?.hasRealScanner() == true) {
        ScannerListener(onBarcodeScanned = { barcode ->
            viewModel.searchByScanner(barcode)
        })
    }

    // Перехватываем нажатие кнопки Назад
    BackHandler(enabled = !state.showActionsDialog) {
        viewModel.handleBackNavigation()
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
                is TaskXDetailEvent.TaskActionCompleted -> {
                    // Для комбинированного события сразу выполняем навигацию
                    navigateBack()

                    // Показываем снекбар с небольшой задержкой,
                    // чтобы он появился уже на экране списка
                    coroutineScope.launch {
                        delay(100) // Небольшая задержка
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    // Отображаем диалог действий
    if (state.showActionsDialog) {
        TaskXActionsDialog(
            onDismiss = { viewModel.hideActionsDialog() },
            onNavigateBack = navigateBack,
            statusActions = state.statusActions,
            isProcessing = state.isProcessingDialogAction
        )
    }

    if (state.showCompletionDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.complete_task_confirmation),
            message = stringResource(R.string.complete_task_confirmation),
            onConfirm = { viewModel.completeTask() },
            onDismiss = { viewModel.hideCompletionDialog() }
        )
    }

    if (state.showOrderRequiredMessage) {
        AlertDialog(
            onDismissRequest = {  },
            title = { Text("Соблюдение порядка") },
            text = { Text("Действия необходимо выполнять в указанном порядке. Пожалуйста, выполните первое не завершенное действие.") },
            confirmButton = {
                Button(onClick = {  }) {
                    Text("ОК")
                }
            }
        )
    }

    if (state.showInitialActionsRequiredDialog) {
        InitialActionsRequiredDialog(
            onDismiss = {  },
            onGoToInitialActions = {

            },
            completedCount = state.completedInitialActionsCount,
            totalCount = state.totalInitialActionsCount
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

    AppScaffold(
        showTopBar = false,
        title = task?.taskTypeId ?: "Unknown",
        onNavigateBack = { viewModel.handleBackNavigation() },
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        isLoading = state.isLoading
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
                initiallyExpanded = false, // По умолчанию свернуто
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
                        text = "Начато: $it}",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(
                        onClick = {  },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (state.activeView == TaskXDetailView.PLANNED_ACTIONS)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("План")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = {  },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (state.activeView == TaskXDetailView.FACT_ACTIONS)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Факт")
                    }
                }
            }

            TaskProgressIndicator(
                task = task,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (state.activeView == TaskXDetailView.PLANNED_ACTIONS) {
                FilterIndicator(
                    message = state.filterMessage,
                    onClearFilter = {  },
                    visible = state.isFilteredBySavableObjects && state.filterMessage.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                SearchResultsIndicator(
                    resultsCount = state.filteredActions.size,
                    visible = state.searchInfo.isNotEmpty() && !state.isFilteredBySavableObjects,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            if (state.showSavableObjectsPanel && state.supportsSavableObjects && state.savableObjects.isNotEmpty() &&
                !state.isFilteredBySavableObjects && state.activeView == TaskXDetailView.PLANNED_ACTIONS) {

                Button(
                    onClick = {  },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "Фильтровать по объектам",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Фильтровать действия по объектам")
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}