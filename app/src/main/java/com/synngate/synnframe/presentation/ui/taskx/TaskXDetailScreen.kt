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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.AvailableTaskAction
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.taskx.components.ComparedLinesView
import com.synngate.synnframe.presentation.ui.taskx.components.FactLinesView
import com.synngate.synnframe.presentation.ui.taskx.components.PlanLinesView
import com.synngate.synnframe.presentation.ui.taskx.components.TaskXStatusIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.TaskXVerificationDialog
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailView

@Composable
fun TaskXDetailScreen(
    viewModel: TaskXDetailViewModel,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val wizardState by viewModel.factLineWizardController.wizardState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val task = state.task
    val taskType = state.taskType

    // Обработка событий
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskXDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is TaskXDetailEvent.ShowFactLineWizard -> {
                    // Обрабатывается через wizardState
                }
                is TaskXDetailEvent.HideFactLineWizard -> {
                    viewModel.cancelWizard()
                }
            }
        }
    }

    // Диалог подтверждения завершения задания
    if (state.showCompletionDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.complete_task_confirmation),
            message = stringResource(R.string.complete_task_confirmation),
            onConfirm = { viewModel.completeTask() },
            onDismiss = { viewModel.hideCompletionDialog() }
        )
    }

    // Диалог верификации задания
    if (state.showVerificationDialog) {
        TaskXVerificationDialog(
            onBarcodeScan = { barcode -> viewModel.verifyTask(barcode) },
            onDismiss = { viewModel.hideVerificationDialog() }
        )
    }

    // Если открыт мастер добавления строки факта, отображаем его
    if (wizardState != null) {
        FactLineWizard(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
    }

    AppScaffold(
        title = task?.name ?: stringResource(R.string.task_details),
        subtitle = task?.barcode?.let { "Штрихкод: $it" },
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
                .padding(16.dp)
        ) {
            // Информация о задании
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Заголовок и статус
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.formatTaskType(task.taskTypeId),
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (task.isVerified) {
                                Text(
                                    text = "Верифицировано",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        TaskXStatusIndicator(
                            status = task.status,
                            formatStatus = viewModel::formatTaskStatus
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Дополнительная информация
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Создано: ${viewModel.formatDate(task.createdAt)}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            task.startedAt?.let {
                                Text(
                                    text = "Начато: ${viewModel.formatDate(it)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            task.lastModifiedAt?.let {
                                Text(
                                    text = "Изменено: ${viewModel.formatDate(it)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            task.completedAt?.let {
                                Text(
                                    text = "Завершено: ${viewModel.formatDate(it)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Исполнитель
                    task.executorId?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Исполнитель: ${it}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Панель переключения вида
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (viewModel.isActionAvailable(AvailableTaskAction.COMPARE_LINES)) {
                    FilledTonalButton(
                        onClick = { viewModel.showComparedLines() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("План/Факт")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (viewModel.isActionAvailable(AvailableTaskAction.SHOW_PLAN_LINES)) {
                    FilledTonalButton(
                        onClick = { viewModel.showPlanLines() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("План")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (viewModel.isActionAvailable(AvailableTaskAction.SHOW_FACT_LINES)) {
                    FilledTonalButton(
                        onClick = { viewModel.showFactLines() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Факт")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Содержимое в зависимости от выбранного вида
            Box(modifier = Modifier.weight(1f)) {
                when (state.activeView) {
                    TaskXDetailView.COMPARED_LINES -> {
                        ComparedLinesView(
                            task = task,
                            formatDate = viewModel::formatDate
                        )
                    }
                    TaskXDetailView.PLAN_LINES -> {
                        PlanLinesView(
                            planLines = task.planLines
                        )
                    }
                    TaskXDetailView.FACT_LINES -> {
                        FactLinesView(
                            factLines = task.factLines,
                            formatDate = viewModel::formatDate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (task.status) {
                    TaskXStatus.TO_DO -> {
                        Button(
                            onClick = { viewModel.startTask() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = task.canStart() && !state.isProcessing
                        ) {
                            Text(stringResource(R.string.start_task))
                        }
                    }
                    TaskXStatus.IN_PROGRESS -> {
                        Button(
                            onClick = { viewModel.showCompletionDialog() },
                            modifier = Modifier.weight(1f),
                            enabled = task.canComplete() && !state.isProcessing
                        ) {
                            Text(stringResource(R.string.complete_task))
                        }

                        Button(
                            onClick = { viewModel.startAddFactLineWizard() },
                            modifier = Modifier.weight(1f),
                            enabled = task.canAddFactLines() && !state.isProcessing
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Добавить факт")
                        }
                    }
                    TaskXStatus.PAUSED -> {
                        Button(
                            onClick = { viewModel.resumeTask() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = task.canResume() && !state.isProcessing
                        ) {
                            Text("Продолжить")
                        }
                    }
                    else -> {
                        // Для завершенных и отмененных заданий нет специфических действий
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Дополнительные действия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (task.status == TaskXStatus.IN_PROGRESS &&
                    viewModel.isActionAvailable(AvailableTaskAction.PAUSE)) {
                    FilledTonalButton(
                        onClick = { viewModel.pauseTask() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isProcessing
                    ) {
                        Text("Приостановить")
                    }
                }

                if (!task.isVerified &&
                    viewModel.isActionAvailable(AvailableTaskAction.VERIFY_TASK)) {
                    FilledTonalButton(
                        onClick = { viewModel.showVerificationDialog() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isProcessing
                    ) {
                        Text("Верифицировать")
                    }
                }
            }
        }
    }
}