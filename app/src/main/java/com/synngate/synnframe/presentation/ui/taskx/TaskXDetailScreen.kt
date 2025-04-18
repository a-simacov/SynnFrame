package com.synngate.synnframe.presentation.ui.taskx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.AvailableTaskAction
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.taskx.components.FactActionsView
import com.synngate.synnframe.presentation.ui.taskx.components.PlannedActionsView
import com.synngate.synnframe.presentation.ui.taskx.components.TaskXStatusIndicator
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

    // Флаг для показа дополнительных действий
    var showAdditionalActions by remember { mutableStateOf(false) }

    // Проверка наличия доступных дополнительных действий
    val hasAdditionalActions = task?.let {
        !it.isVerified && viewModel.isActionAvailable(AvailableTaskAction.VERIFY_TASK) ||
                viewModel.isActionAvailable(AvailableTaskAction.PRINT_TASK_LABEL)
    } ?: false

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
        title = task?.name ?: stringResource(R.string.task_details),
        // Убран subtitle со штрихкодом задания
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        isLoading = state.isLoading,
        floatingActionButton = {
            // Кнопка добавления действия показывается только для задания в статусе "Выполняется"
            if (task?.status == TaskXStatus.IN_PROGRESS) {
                val isStrictOrder = state.taskType?.strictActionOrder == true

                if (isStrictOrder) {
                    // Для заданий со строгим порядком выполнения - кнопка с текстом
                    ExtendedFloatingActionButton(
                        onClick = {
                            // Получаем первое невыполненное действие
                            task.getNextAction()?.let { action ->
                                viewModel.startActionExecution(action.id)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                        },
                        text = { Text("Выполнить действие") }
                    )
                } else {
                    // Для заданий с произвольным порядком - обычная кнопка с плюсом
                    FloatingActionButton(
                        onClick = {
                            // Получаем первое невыполненное действие и запускаем его
                            task.getNextAction()?.let { action ->
                                viewModel.startActionExecution(action.id)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить действие")
                    }
                }
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
                .padding(16.dp)
        ) {
            // Информация о задании
            Card(
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

                            // Добавлен штрихкод задания
                            Text(
                                text = "Штрихкод: ${task.barcode}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

                    // Только даты начала и изменения
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        task.startedAt?.let {
                            Text(
                                text = "Начато: ${viewModel.formatDate(it)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        task.lastModifiedAt?.let {
                            Text(
                                text = "Изменено: ${viewModel.formatDate(it)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    Text("Запланировано")
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
                    Text("Выполнено")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Содержимое в зависимости от выбранного вида
            Box(modifier = Modifier.weight(1f)) {
                when (state.activeView) {
                    TaskXDetailView.PLANNED_ACTIONS -> {
                        PlannedActionsView(
                            plannedActions = task.plannedActions,
                            onActionClick = { action ->
                                // Проверяем, что задание в статусе "Выполняется"
                                if (task.status == TaskXStatus.IN_PROGRESS &&
                                    !action.isCompleted &&
                                    !action.isSkipped) {

                                    // Проверяем строгий порядок выполнения
                                    val isStrictOrder = state.taskType?.strictActionOrder == true

                                    if (isStrictOrder) {
                                        // Если порядок строгий, проверяем, что это первое не выполненное действие
                                        val firstNotCompletedAction = task.plannedActions
                                            .sortedBy { it.order }
                                            .firstOrNull { !it.isCompleted && !it.isSkipped }

                                        if (firstNotCompletedAction?.id == action.id) {
                                            viewModel.startActionExecution(action.id)
                                        } else {
                                            // Показываем сообщение, что нужно выполнять действия по порядку
                                            viewModel.showOrderRequiredMessage()
                                        }
                                    } else {
                                        // Если порядок не строгий, разрешаем выполнять любое действие
                                        viewModel.startActionExecution(action.id)
                                    }
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

            Spacer(modifier = Modifier.height(8.dp))

            // Уменьшенный блок с кнопками управления статусом с иконками и кратким текстом
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (task.status) {
                        TaskXStatus.TO_DO -> {
                            Button(
                                onClick = { viewModel.startTask() },
                                enabled = task.canStart() && !state.isProcessing,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Начать выполнение",
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Старт")
                            }
                        }
                        TaskXStatus.IN_PROGRESS -> {
                            Button(
                                onClick = { viewModel.showCompletionDialog() },
                                enabled = task.canComplete() && !state.isProcessing,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Завершить",
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Финиш")
                            }

                            if (viewModel.isActionAvailable(AvailableTaskAction.PAUSE)) {
                                Button(
                                    onClick = { viewModel.pauseTask() },
                                    enabled = !state.isProcessing,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = "Приостановить",
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Пауза")
                                }
                            }
                        }
                        TaskXStatus.PAUSED -> {
                            Button(
                                onClick = { viewModel.resumeTask() },
                                enabled = task.canResume() && !state.isProcessing,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Продолжить",
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Старт")
                            }
                        }
                        else -> {}
                    }
                }
            }

            // Группа с дополнительными действиями - показывается только при наличии действий
            if (hasAdditionalActions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Дополнительные действия",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = 4.dp)
                            )

                            IconButton(onClick = { showAdditionalActions = !showAdditionalActions }) {
                                Icon(
                                    imageVector = if (showAdditionalActions)
                                        Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (showAdditionalActions)
                                        "Скрыть" else "Показать"
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showAdditionalActions,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!task.isVerified && viewModel.isActionAvailable(AvailableTaskAction.VERIFY_TASK)) {
                                    IconButton(
                                        onClick = { viewModel.showVerificationDialog() },
                                        enabled = !state.isProcessing
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = "Верифицировать",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Другие дополнительные действия...
                            }
                        }
                    }
                }
            }
        }
    }
}