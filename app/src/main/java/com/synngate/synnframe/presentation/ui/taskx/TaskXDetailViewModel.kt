package com.synngate.synnframe.presentation.ui.taskx

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.AvailableTaskAction
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.action.ProgressType
import com.synngate.synnframe.domain.service.ActionExecutionService
import com.synngate.synnframe.domain.service.FinalActionsValidator
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.taskx.model.ActionDisplayMode
import com.synngate.synnframe.presentation.ui.taskx.model.StatusActionData
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailView
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskXDetailViewModel(
    private val taskId: String,
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases,
    private val finalActionsValidator: FinalActionsValidator,
    private val actionExecutionService: ActionExecutionService,
    private val preloadedTask: TaskX? = null,
    private val preloadedTaskType: TaskTypeX? = null
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private var taskObserverJob: Job? = null

    init {
        if (preloadedTask != null && preloadedTaskType != null) {
            launchIO {
                val currentUser = userUseCases.getCurrentUser().first()
                updateState {
                    it.copy(
                        task = preloadedTask,
                        taskType = preloadedTaskType,
                        currentUserId = currentUser?.id,
                        isLoading = false,
                        error = null
                    )
                }
                updateDependentState(preloadedTask, preloadedTaskType)
                startObservingTaskChanges()
            }
        } else {
            loadTask()
        }
    }

    fun startActionExecution(actionId: String) {
        launchIO {
            try {
                val task = uiState.value.task ?: return@launchIO

                if (task.status != TaskXStatus.IN_PROGRESS) {
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание не в статусе 'Выполняется'"))
                    return@launchIO
                }

                // Проверка на строгий порядок выполнения действий
                val isStrictOrder = uiState.value.taskType?.strictActionOrder == true
                val action = task.plannedActions.find { it.id == actionId }

                if (isStrictOrder && action != null) {
                    val nextActionId = finalActionsValidator.getNextActionIdInStrictOrder(task)
                    if (nextActionId != actionId) {
                        showOrderRequiredMessage()
                        return@launchIO
                    }
                }

                // Отправляем событие навигации к экрану визарда
                sendEvent(TaskXDetailEvent.NavigateToActionWizard(task.id, actionId))
            } catch (e: Exception) {
                Timber.e(e, "Error starting action execution")
                updateState { it.copy(error = "Ошибка при инициализации действия: ${e.message}") }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Не удалось начать выполнение действия"))
            }
        }
    }

    private fun calculateNextActionId(task: TaskX, taskType: TaskTypeX?): String? {
        val isStrictOrder = taskType?.strictActionOrder == true

        if (isStrictOrder) {
            return finalActionsValidator.getNextActionIdInStrictOrder(task)
        }

        return null
    }

    fun canExecuteFinalActions(task: TaskX): Boolean {
        return finalActionsValidator.canExecuteFinalActions(task)
    }

    fun canExecuteAction(actionId: String): Boolean {
        val task = uiState.value.task ?: return false
        val action = task.plannedActions.find { it.id == actionId } ?: return false

        if (action.isFinalAction && !finalActionsValidator.canExecuteFinalActions(task)) {
            return false
        }

        val isStrictOrder = uiState.value.taskType?.strictActionOrder == true
        if (!isStrictOrder) return true

        val nextActionId = uiState.value.nextActionId
        return nextActionId == actionId
    }

    fun setActionsDisplayMode(mode: ActionDisplayMode) {
        updateState { it.copy(actionsDisplayMode = mode) }
        updateFilteredActions()
    }

    fun showFinalActionNotAvailableMessage() {
        sendEvent(TaskXDetailEvent.ShowSnackbar("Сначала выполните все обычные действия"))
    }

    fun tryExecuteAction(actionId: String) {
        val task = uiState.value.task ?: return
        val action = task.plannedActions.find { it.id == actionId } ?: return

        if (action.isFinalAction && !finalActionsValidator.canExecuteFinalActions(task)) {
            showFinalActionNotAvailableMessage()
            return
        }

        if (canExecuteAction(actionId)) {
            startActionExecution(actionId)
        } else {
            showOrderRequiredMessage()
        }
    }

    fun showOrderRequiredMessage() {
        updateState { it.copy(showOrderRequiredMessage = true) }
        sendEvent(TaskXDetailEvent.ShowSnackbar("Необходимо выполнять действия в указанном порядке"))
    }

    fun hideOrderRequiredMessage() {
        updateState { it.copy(showOrderRequiredMessage = false) }
    }

    fun loadTask() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                if (preloadedTask != null && preloadedTaskType != null) {
                    updateState {
                        it.copy(
                            task = preloadedTask,
                            taskType = preloadedTaskType,
                            isLoading = false,
                            error = null
                        )
                    }
                    updateDependentState(preloadedTask, preloadedTaskType)
                    startObservingTaskChanges()
                    return@launchIO
                }

                val task = taskXUseCases.getTaskById(taskId)

                if (task != null) {
                    val taskType = taskXUseCases.getTaskType()
                    val currentUser = userUseCases.getCurrentUser().first()

                    updateState {
                        it.copy(
                            task = task,
                            taskType = taskType,
                            currentUserId = currentUser?.id,
                            isLoading = false,
                            error = null
                        )
                    }

                    updateDependentState(task, taskType)
                    startObservingTaskChanges()
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Task by ID $taskId was not found"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading task")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Error loading task: ${e.message}"
                    )
                }
            }
        }
    }

    fun startTask() {
        launchIO {
            val currentState = uiState.value
            val task = currentState.task ?: return@launchIO
            val currentUserId = currentState.currentUserId ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.startTask(task.id, currentUserId)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    updateState {
                        it.copy(
                            task = updatedTask,
                            isProcessing = false
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание начато"))
                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            error = result.exceptionOrNull()?.message ?: "Error on starting task"
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при запуске задания"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on starting task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error on starting task: ${e.message}"
                    )
                }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при запуске задания"))
            }
        }
    }

    fun completeTask() {
        launchIO {
            val currentState = uiState.value
            val task = currentState.task ?: return@launchIO
            val taskType = currentState.taskType ?: return@launchIO

            val allActionsCompleted = task.plannedActions.all { it.isCompleted || it.isSkipped }

            if (!allActionsCompleted && !taskType.allowCompletionWithoutFactActions) {
                sendEvent(TaskXDetailEvent.ShowSnackbar("Необходимо выполнить все запланированные действия"))
                updateState { it.copy(showCompletionDialog = false) }
                return@launchIO
            }

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.completeTask(task.id)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    updateState {
                        it.copy(
                            task = updatedTask,
                            isProcessing = false,
                            showCompletionDialog = false
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание завершено"))
                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            showCompletionDialog = false,
                            error = result.exceptionOrNull()?.message ?: "Error on completing task"
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при завершении задания"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on completing task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        showCompletionDialog = false,
                        error = "Error on completing task: ${e.message}"
                    )
                }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при завершении задания"))
            }
        }
    }

    fun pauseTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.pauseTask(task.id)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    updateState {
                        it.copy(
                            task = updatedTask,
                            isProcessing = false
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание приостановлено"))
                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            error = result.exceptionOrNull()?.message ?: "Error on pausing task"
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при приостановке задания"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on pausing task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error on pausing task: ${e.message}"
                    )
                }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при приостановке задания"))
            }
        }
    }

    fun resumeTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO
            val currentUserId = uiState.value.currentUserId ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.startTask(task.id, currentUserId)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    updateState {
                        it.copy(
                            task = updatedTask,
                            isProcessing = false
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание возобновлено"))
                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            error = result.exceptionOrNull()?.message ?: "Error on resuming task"
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при возобновлении задания"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on resuming task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error on resuming task: ${e.message}"
                    )
                }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при возобновлении задания"))
            }
        }
    }

    fun verifyTask(barcode: String) {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.verifyTask(task.id, barcode)

                if (result.isSuccess && result.getOrNull() == true) {
                    loadTask()
                    updateState { it.copy(showVerificationDialog = false) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание успешно верифицировано"))
                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            showVerificationDialog = false,
                            error = "Неверный штрихкод для верификации"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on verification task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        showVerificationDialog = false,
                        error = "Error on verification task: ${e.message}"
                    )
                }
            }
        }
    }

    private fun startObservingTaskChanges() {
        taskObserverJob?.cancel()

        taskObserverJob = viewModelScope.launch {
            uiState
                .map { it.task }
                .distinctUntilChanged { old, new ->
                    if (old == null || new == null) false
                    else old.id == new.id &&
                            old.status == new.status &&
                            old.factActions.size == new.factActions.size &&
                            old.plannedActions.count { it.isCompleted } == new.plannedActions.count { it.isCompleted }
                }
                .collect { task ->
                    if (task != null) {
                        val taskType = uiState.value.taskType
                        Timber.d("Задание изменилось, обновляем UI: ${task.status}, " +
                                "факт. действий: ${task.factActions.size}, " +
                                "завершено плановых: ${task.plannedActions.count { it.isCompleted }}")
                        updateDependentState(task, taskType)
                    }
                }
        }
    }

    private fun stopObservingTaskChanges() {
        taskObserverJob?.cancel()
        taskObserverJob = null
    }

    private fun updateDependentState(task: TaskX, taskType: TaskTypeX?) {
        val nextActionId = calculateNextActionId(task, taskType)
        val hasAdditionalActions = checkHasAdditionalActions(task)
        val statusActions = createStatusActions(task)

        updateState { currentState ->
            currentState.copy(
                nextActionId = nextActionId,
                hasAdditionalActions = hasAdditionalActions,
                statusActions = statusActions
            )
        }

        updateFilteredActions()
    }

    private fun checkHasAdditionalActions(task: TaskX): Boolean {
        return !task.isVerified && isActionAvailable(AvailableTaskAction.VERIFY_TASK) ||
                isActionAvailable(AvailableTaskAction.PRINT_TASK_LABEL)
    }

    private fun createStatusActions(task: TaskX): List<StatusActionData> {
        return when (task.status) {
            TaskXStatus.TO_DO -> listOf(
                StatusActionData(
                    id = "start",
                    iconName = "play_arrow",
                    text = "Старт",
                    description = "Начать выполнение",
                    onClick = ::startTask
                )
            )
            TaskXStatus.IN_PROGRESS -> listOf(
                StatusActionData(
                    id = "pause",
                    iconName = "pause",
                    text = "Приостановить",
                    description = "Приостановить",
                    onClick = ::pauseTask
                ),
                StatusActionData(
                    id = "finish",
                    iconName = "check_circle",
                    text = "Завершить задание",
                    description = "Завершить",
                    onClick = ::showCompletionDialog,
                )
            )
            TaskXStatus.PAUSED -> listOf(
                StatusActionData(
                    id = "resume",
                    iconName = "play_arrow",
                    description = "Продолжить",
                    text = "Старт",
                    onClick = ::resumeTask
                )
            )
            else -> emptyList()
        }
    }

    fun showCompletionDialog() {
        updateState { it.copy(showCompletionDialog = true) }
    }

    fun hideCompletionDialog() {
        updateState { it.copy(showCompletionDialog = false) }
    }

    fun hideVerificationDialog() {
        updateState { it.copy(showVerificationDialog = false) }
    }

    fun formatDate(dateTime: LocalDateTime?): String {
        return dateTime?.format(dateFormatter) ?: "Не указано"
    }

    fun formatTaskType(): String {
        return uiState.value.taskType?.name ?: "Неизвестный тип"
    }

    fun showPlannedActions() {
        updateState { it.copy(activeView = TaskXDetailView.PLANNED_ACTIONS) }
    }

    fun showFactActions() {
        updateState { it.copy(activeView = TaskXDetailView.FACT_ACTIONS) }
    }

    fun isActionAvailable(action: AvailableTaskAction): Boolean {
        val taskType = uiState.value.taskType ?: return false
        return action in taskType.availableActions
    }

    private fun updateFilteredActions() {
        val task = uiState.value.task ?: return
        val mode = uiState.value.actionsDisplayMode

        val filteredActions = filterActionsByMode(task, mode)

        updateState { it.copy(filteredActions = filteredActions) }
    }

    private fun filterActionsByMode(task: TaskX, mode: ActionDisplayMode): List<PlannedAction> {
        val canExecuteFinals = canExecuteFinalActions(task)
        val factActions = task.factActions

        val completionStatus = task.plannedActions.associateBy(
            { it.id },
            { it.isActionCompleted(factActions) }
        )

        return when (mode) {
            ActionDisplayMode.CURRENT -> task.plannedActions
                .filter {
                    !it.isSkipped &&
                            !completionStatus[it.id]!! &&
                            (!it.isFinalAction || canExecuteFinals)
                }
                .sortedBy { it.order }

            ActionDisplayMode.COMPLETED -> task.plannedActions
                .filter { completionStatus[it.id]!! }
                .sortedBy { it.order }

            ActionDisplayMode.ALL -> task.plannedActions
                .sortedBy { it.order }

            ActionDisplayMode.FINALS -> task.plannedActions
                .filter { it.isFinalAction }
                .sortedBy { it.order }
        }
    }

    fun toggleActionCompletion(actionId: String, completed: Boolean) {
        launchIO {
            try {
                val result = actionExecutionService.setActionCompletionStatus(
                    taskId = taskId,
                    actionId = actionId,
                    completed = completed
                )

                if (result.isSuccess) {
                    result.getOrNull()?.let { updatedTask ->
                        updateState { state ->
                            state.copy(
                                task = updatedTask,
                                error = null
                            )
                        }

                        updateDependentState(updatedTask, uiState.value.taskType)

                        val message = if (completed) {
                            "Действие отмечено как выполненное"
                        } else {
                            "Отметка о выполнении действия снята"
                        }

                        sendEvent(TaskXDetailEvent.ShowSnackbar(message))
                    }
                } else {
                    Timber.e(result.exceptionOrNull(), "Ошибка при изменении статуса действия")
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при изменении статуса действия"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при изменении статуса действия")
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при изменении статуса действия"))
            }
        }
    }

    fun supportsMultipleFactActions(): Boolean {
        return uiState.value.taskType?.allowMultipleFactActions == true
    }

    fun isQuantityBasedAction(action: PlannedAction): Boolean {
        return action.getProgressType() == ProgressType.QUANTITY
    }

    fun canManageCompletionStatus(action: PlannedAction): Boolean {
        val task = uiState.value.task ?: return false
        return task.status == TaskXStatus.IN_PROGRESS &&
                uiState.value.taskType?.allowMultipleFactActions == true &&
                action.getProgressType() == ProgressType.QUANTITY &&
                !action.isSkipped
    }

    fun showActionsDialog() {
        updateState { it.copy(showActionsDialog = true) }
    }

    fun hideActionsDialog() {
        updateState { it.copy(showActionsDialog = false) }
    }

    fun handleBackNavigation() {
        if (uiState.value.task?.status == TaskXStatus.IN_PROGRESS ||
            uiState.value.task?.status == TaskXStatus.PAUSED) {
            showActionsDialog()
        } else {
            showActionsDialog()
        }
    }

    private fun updateSingleAction(actionId: String) {
        val currentTask = uiState.value.task ?: return
        currentTask.plannedActions.find { it.id == actionId } ?: return

        launchIO {
            try {
                val stepResults = mapOf<String, Any>()
                val result = actionExecutionService.executeAction(
                    taskId = currentTask.id,
                    actionId = actionId,
                    stepResults = stepResults,
                    completeAction = false
                )

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()

                    if (updatedTask != null) {
                        updateState { it.copy(task = updatedTask) }
                        updateDependentState(updatedTask, uiState.value.taskType)

                        return@launchIO
                    }
                }

                val updatedTaskFromApi = taskXUseCases.getTaskById(currentTask.id)

                if (updatedTaskFromApi != null) {
                    updateState { it.copy(task = updatedTaskFromApi) }
                    updateDependentState(updatedTaskFromApi, uiState.value.taskType)
                } else {
                    Timber.w("Не удалось получить актуальные данные о задании")
                    loadTask()
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обновлении действия $actionId: ${e.message}")
                loadTask()
            }
        }
    }

    fun onActionCompleted(actionId: String) {
        launchIO {
            try {
                updateSingleAction(actionId)
                sendEvent(TaskXDetailEvent.ShowSnackbar("Действие успешно выполнено"))
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке завершения действия: ${e.message}")
                loadTask()
            }
        }
    }

    override fun dispose() {
        super.dispose()
        stopObservingTaskChanges()
    }
}