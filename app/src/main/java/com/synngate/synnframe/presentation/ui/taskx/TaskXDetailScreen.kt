package com.synngate.synnframe.presentation.ui.taskx

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.common.status.TaskXStatusIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.ActionDisplayModeSwitcher
import com.synngate.synnframe.presentation.ui.taskx.components.ExpandableTaskInfoCard
import com.synngate.synnframe.presentation.ui.taskx.components.FactActionsView
import com.synngate.synnframe.presentation.ui.taskx.components.PlannedActionsView
import com.synngate.synnframe.presentation.ui.taskx.components.TaskProgressIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.TaskXActionsDialog
import com.synngate.synnframe.presentation.ui.taskx.components.TaskXVerificationDialog
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailView
import timber.log.Timber

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

    // Перехватываем нажатие кнопки Назад
    BackHandler {
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
                    // Навигация к экрану визарда
                    Timber.d("Навигация к экрану визарда: ${event.taskId}, ${event.actionId}")
                    navigateToActionWizard(event.taskId, event.actionId)
                }
            }
        }
    }

    // Отображаем диалог действий
    if (state.showActionsDialog) {
        TaskXActionsDialog(
            onDismiss = { viewModel.hideActionsDialog() },
            onNavigateBack = navigateBack,
            statusActions = state.statusActions
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

    if (state.showVerificationDialog) {
        TaskXVerificationDialog(
            onBarcodeScan = { barcode -> viewModel.verifyTask(barcode) },
            onDismiss = { viewModel.hideVerificationDialog() }
        )
    }

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

    AppScaffold(
        title = task?.taskTypeId?.let { viewModel.formatTaskType(it) } ?: "Unknown",
        // Перехватываем нажатие на кнопку "Назад" в заголовке
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

                // Статус задания
                TaskXStatusIndicator(
                    status = task.status,
                )

                // Можно добавить дополнительную информацию
                task.startedAt?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Начато: ${viewModel.formatDate(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                task.lastModifiedAt?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Изменено: ${viewModel.formatDate(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Панель переключения вида
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.showPlannedActions() },
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
                        onClick = { viewModel.showFactActions() },
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

            when (state.activeView) {
                TaskXDetailView.PLANNED_ACTIONS -> {
                    ActionDisplayModeSwitcher(
                        currentMode = state.actionsDisplayMode,
                        onModeChange = { mode -> viewModel.setActionsDisplayMode(mode) },
                        hasFinalActions = task.plannedActions.any { it.isFinalAction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        // Обновляем компонент для отображения действий
                        PlannedActionsView(
                            plannedActions = state.filteredActions,
                            factActions = task.factActions, // Передаем фактические действия
                            nextActionId = nextActionId,
                            onActionClick = { action ->
                                // Проверяем, что задание в статусе "Выполняется"
                                if (task.status == TaskXStatus.IN_PROGRESS &&
                                    !action.isSkipped
                                ) {
                                    // Если действие уже отмечено как выполненное, но разрешено
                                    // множественное выполнение - запускаем выполнение
                                    if (action.isActionCompleted(task.factActions) &&
                                        viewModel.supportsMultipleFactActions() &&
                                        viewModel.isQuantityBasedAction(action)
                                    ) {
                                        // Запускаем выполнение действия без изменения статуса
                                        viewModel.startActionExecution(action.id)
                                    } else if (!action.isActionCompleted(task.factActions)) {
                                        // Обычный запуск действия
                                        viewModel.tryExecuteAction(action.id)
                                    }
                                }
                            },
                            onToggleCompletion = { action, completed ->
                                // Проверяем, можно ли управлять статусом выполнения
                                if (viewModel.canManageCompletionStatus(action)) {
                                    viewModel.toggleActionCompletion(action.id, completed)
                                }
                            }
                        )
                    }

                    // Кнопка для запуска следующего действия, если задание в статусе "Выполняется"
                    // и не требуется строгий порядок
                    if (task.status == TaskXStatus.IN_PROGRESS &&
                        state.taskType?.strictActionOrder != true
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    task.getNextAction()?.let { action ->
                                        viewModel.startActionExecution(action.id)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.PendingActions,
                                    contentDescription = "Выполнить следующее действие",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Выполнить следующее")
                            }
                        }
                    }
                }

                TaskXDetailView.FACT_ACTIONS -> {
                    FactActionsView(
                        factActions = task.factActions,
                        formatDate = viewModel::formatDate,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun getIconByName(name: String): ImageVector {
    return when (name) {
        "play_arrow" -> Icons.Default.PlayArrow
        "pause" -> Icons.Default.Pause
        "check_circle" -> Icons.Default.CheckCircle
        // другие иконки...
        else -> Icons.Default.Info
    }
}