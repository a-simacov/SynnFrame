package com.synngate.synnframe.presentation.ui.taskx

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.synngate.synnframe.presentation.ui.taskx.components.TaskXVerificationDialog
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailView
import com.synngate.synnframe.presentation.ui.wizard.action.ActionWizardScreen

@Composable
fun TaskXDetailScreen(
    viewModel: TaskXDetailViewModel,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val wizardState by viewModel.actionWizardController.wizardState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val task = state.task

    val nextActionId = state.nextActionId

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskXDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is TaskXDetailEvent.ShowActionWizard -> {
                    // Обработка будет через state.showActionWizard
                }
                is TaskXDetailEvent.HideActionWizard -> {
                    // Обработка будет через state.showActionWizard
                }
            }
        }
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

    // Показываем визард действий, если он активен
    if (state.showActionWizard && wizardState != null) {
        Dialog(
            onDismissRequest = { viewModel.hideActionWizard() },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ActionWizardScreen(
                    actionWizardController = viewModel.actionWizardController,
                    actionWizardContextFactory = viewModel.actionWizardContextFactory,
                    actionStepFactoryRegistry = viewModel.actionStepFactoryRegistry,
                    onComplete = { viewModel.completeActionWizard() },
                    onCancel = { viewModel.hideActionWizard() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    AppScaffold(
        title = task?.taskTypeId?.let { viewModel.formatTaskType(it) } ?: "Unknown",
        onNavigateBack = navigateBack,
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
            }

            TaskProgressIndicator(
                task = task,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

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

            when (state.activeView) {
                TaskXDetailView.PLANNED_ACTIONS -> {
                    ActionDisplayModeSwitcher(
                        currentMode = state.actionsDisplayMode,
                        onModeChange = { mode -> viewModel.setActionsDisplayMode(mode) },
                        hasFinalActions = task.plannedActions.any { it.isFinalAction },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        PlannedActionsView(
                            plannedActions = state.filteredActions,
                            nextActionId = nextActionId,
                            onActionClick = { action ->
                                // Проверяем, что задание в статусе "Выполняется"
                                if (task.status == TaskXStatus.IN_PROGRESS &&
                                    !action.isCompleted &&
                                    !action.isSkipped
                                ) {
                                    viewModel.tryExecuteAction(action.id)
                                }
                            }
                        )
                    }

                    if (task.status == TaskXStatus.IN_PROGRESS &&
                        state.taskType?.strictActionOrder != true) {
                        Button(
                            onClick = {
                                task.getNextAction()?.let { action ->
                                    viewModel.startActionExecution(action.id)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Добавить действие",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Добавить действие")
                        }
                    }

                    if (state.statusActions.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            state.statusActions.forEach { actionData ->
                                Button(
                                    onClick = actionData.onClick,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when (actionData.id) {
                                            "start" -> MaterialTheme.colorScheme.primary
                                            "finish" -> MaterialTheme.colorScheme.secondary
                                            "pause" -> MaterialTheme.colorScheme.tertiary
                                            "resume" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                ) {
                                    Icon(
                                        imageVector = getIconByName(actionData.iconName),
                                        contentDescription = actionData.description,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(actionData.text)
                                }
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