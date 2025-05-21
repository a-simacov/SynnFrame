package com.synngate.synnframe.presentation.ui.taskx

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.AvailableTaskAction
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.SavableObject
import com.synngate.synnframe.domain.entity.taskx.SavableObjectData
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.action.ProgressType
import com.synngate.synnframe.domain.service.ActionExecutionService
import com.synngate.synnframe.domain.service.ActionSearchService
import com.synngate.synnframe.domain.service.FinalActionsValidator
import com.synngate.synnframe.domain.service.InitialActionsValidator
import com.synngate.synnframe.domain.service.TaskContextManager
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
    private val initialActionsValidator: InitialActionsValidator,
    private val actionExecutionService: ActionExecutionService,
    private val actionSearchService: ActionSearchService? = null,
    private val taskContextManager: TaskContextManager? = null,
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

    fun startActionExecution(actionId: String, skipOrderCheck: Boolean = false) {
        launchIO {
            try {
                val task = uiState.value.task ?: return@launchIO

                if (task.status != TaskXStatus.IN_PROGRESS) {
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание не в статусе 'Выполняется'"))
                    return@launchIO
                }

                val isStrictOrder = uiState.value.taskType?.strictActionOrder == true
                val action = task.plannedActions.find { it.id == actionId }

                if (action != null && !action.isInitialAction) {
                    if (!initialActionsValidator.canExecuteRegularAction(task, actionId)) {
                        showInitialActionsRequiredMessage()
                        return@launchIO
                    }
                }

                if (!skipOrderCheck && isStrictOrder && action != null) {
                    val nextActionId = finalActionsValidator.getNextActionIdInStrictOrder(task)
                    if (nextActionId != actionId) {
                        showOrderRequiredMessage()
                        return@launchIO
                    }
                }

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

    fun setActionsDisplayMode(mode: ActionDisplayMode) {
        updateState { it.copy(actionsDisplayMode = mode) }
        updateFilteredActions()
    }

    fun showFinalActionNotAvailableMessage() {
        sendEvent(TaskXDetailEvent.ShowSnackbar("Сначала выполните все обычные действия"))
    }

    fun showInitialActionsRequiredMessage() {
        updateState { it.copy(showOrderRequiredMessage = true) }

        val completedCount = uiState.value.completedInitialActionsCount
        val totalCount = uiState.value.totalInitialActionsCount

        val message = if (completedCount > 0) {
            "Выполнено $completedCount из $totalCount начальных действий"
        } else {
            "Необходимо выполнить все начальные действия"
        }

        sendEvent(TaskXDetailEvent.ShowSnackbar(message))
    }

    fun tryExecuteAction(actionId: String) {
        val task = uiState.value.task ?: return
        val isStrictOrder = uiState.value.taskType?.strictActionOrder == true
        val action = task.plannedActions.find { it.id == actionId }

        if (action?.isInitialAction == true) {
            if (finalActionsValidator.isInitialActionOutOfOrder(task, actionId)) {
                sendEvent(TaskXDetailEvent.ShowSnackbar("Сначала выполните предыдущие начальные действия"))
                return
            }

            startActionExecution(actionId)
            return
        }

        val blockReason = finalActionsValidator.getActionBlockReason(task, actionId)

        when (blockReason) {
            is FinalActionsValidator.ActionBlockReason.InitialActionsNotCompleted -> {
                showInitialActionsRequiredDialog()
            }
            is FinalActionsValidator.ActionBlockReason.RegularActionsNotCompleted -> {
                showFinalActionNotAvailableMessage()
            }
            is FinalActionsValidator.ActionBlockReason.OutOfOrder -> {
                if (isStrictOrder) {
                    showOrderRequiredMessage()
                } else {
                    startActionExecution(actionId, skipOrderCheck = true)
                }
            }
            is FinalActionsValidator.ActionBlockReason.None -> {
                startActionExecution(actionId, skipOrderCheck = true)
            }
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

                    initialActionsValidator.clearCache()

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

            updateState { it.copy(
                showCompletionDialog = false,
                isProcessingDialogAction = true
            ) }

            try {
                val result = taskXUseCases.completeTask(task.id)

                if (result.isSuccess) {
                    sendEvent(TaskXDetailEvent.TaskActionCompleted("Задание завершено"))
                } else {
                    updateState { it.copy(isProcessingDialogAction = false) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при завершении задания"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on completing task")
                updateState { it.copy(isProcessingDialogAction = false) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при завершении задания"))
            }
        }
    }

    fun pauseTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            updateState { it.copy(isProcessingDialogAction = true) }

            try {
                val result = taskXUseCases.pauseTask(task.id)

                if (result.isSuccess) {
                    sendEvent(TaskXDetailEvent.TaskActionCompleted("Задание приостановлено"))
                } else {
                    updateState { it.copy(isProcessingDialogAction = false) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при приостановке задания"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on pausing task")
                updateState { it.copy(isProcessingDialogAction = false) }
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

    private fun updateSavableObjects() {
        if (taskContextManager != null) {
            val taskType = uiState.value.taskType
            val supportsSavableObjects = taskType?.savableObjectTypes?.isNotEmpty() == true

            if (supportsSavableObjects) {
                val objects = taskContextManager.savableObjects.value

                // Добавляем немедленное логирование для отладки
                Timber.d("Обновление сохраняемых объектов: найдено ${objects.size} объектов")
                objects.forEach { obj ->
                    Timber.d("Сохраняемый объект: ${obj.objectType} - ${obj.getShortDescription()}")
                }

                updateState { state ->
                    state.copy(
                        savableObjects = objects,
                        showSavableObjectsPanel = objects.isNotEmpty() && supportsSavableObjects
                    )
                }
            }
        }
    }

    private fun startObservingTaskChanges() {
        taskObserverJob?.cancel()

        taskObserverJob = viewModelScope.launch {
            // Подписка на изменения задания
            launch {
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
                            updateDependentState(task, taskType)
                        }
                    }
            }

            // Подписка на изменения сохраняемых объектов
            if (taskContextManager != null) {
                launch {
                    taskContextManager.savableObjects.collect { objects ->
                        updateState { state ->
                            val taskType = state.taskType
                            val supportsSavableObjects = taskType?.savableObjectTypes?.isNotEmpty() == true

                            state.copy(
                                savableObjects = objects,
                                showSavableObjectsPanel = objects.isNotEmpty() && supportsSavableObjects
                            )
                        }
                    }
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

        val shouldShowSearch = taskType?.enableActionSearch == true
        val supportsSavableObjects = taskType?.savableObjectTypes?.isNotEmpty() == true

        // Получаем сохраняемые объекты, если функционал поддерживается
        val savableObjects = if (supportsSavableObjects && taskContextManager != null) {
            taskContextManager.savableObjects.value
        } else {
            emptyList()
        }

        Timber.d("updateDependentState: найдено ${savableObjects.size} сохраняемых объектов")

        val initialActions = task.plannedActions.filter { it.isInitialAction }
        val hasInitialActions = initialActions.isNotEmpty()
        val completedInitialActions = initialActions.count { it.isCompleted }
        val areInitialActionsCompleted = initialActions.all { it.isCompleted || it.isSkipped }

        val autoSwitchToInitials = hasInitialActions &&
                !areInitialActionsCompleted &&
                uiState.value.actionsDisplayMode == ActionDisplayMode.CURRENT

        val autoSwitchToCurrent = hasInitialActions &&
                areInitialActionsCompleted &&
                uiState.value.actionsDisplayMode == ActionDisplayMode.INITIALS

        val newDisplayMode = when {
            autoSwitchToInitials -> ActionDisplayMode.INITIALS
            autoSwitchToCurrent -> ActionDisplayMode.CURRENT
            else -> uiState.value.actionsDisplayMode
        }

        updateState { currentState ->
            currentState.copy(
                nextActionId = nextActionId,
                hasAdditionalActions = hasAdditionalActions,
                statusActions = statusActions,
                showSearchField = if (shouldShowSearch) {
                    true
                } else {
                    false
                },
                hasInitialActions = hasInitialActions,
                areInitialActionsCompleted = areInitialActionsCompleted,
                completedInitialActionsCount = completedInitialActions,
                totalInitialActionsCount = initialActions.size,
                initialActionsIds = initialActions.map { it.id },
                actionsDisplayMode = newDisplayMode,
                savableObjects = savableObjects,
                showSavableObjectsPanel = savableObjects.isNotEmpty() && supportsSavableObjects,
                supportsSavableObjects = supportsSavableObjects
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

    fun showInitialActionsRequiredDialog() {
        updateState { it.copy(showInitialActionsRequiredDialog = true) }

        val completedCount = uiState.value.completedInitialActionsCount
        val totalCount = uiState.value.totalInitialActionsCount

        val message = if (completedCount > 0) {
            "Выполнено $completedCount из $totalCount начальных действий"
        } else {
            "Необходимо выполнить все начальные действия"
        }

        sendEvent(TaskXDetailEvent.ShowSnackbar(message))
    }

    // Метод для скрытия диалога начальных действий
    fun hideInitialActionsRequiredDialog() {
        updateState { it.copy(showInitialActionsRequiredDialog = false) }
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
        val foundActionIds = uiState.value.filteredActionIds
        val savableObjects = uiState.value.savableObjects

        var filteredActions = filterActionsByMode(task, mode)

        if (foundActionIds.isNotEmpty()) {
            filteredActions = filteredActions.filter { it.id in foundActionIds }
        }

        else if (savableObjects.isNotEmpty() && uiState.value.supportsSavableObjects && uiState.value.showSavableObjectsPanel) {
            val savableObjectsActionIds = mutableSetOf<String>()

            savableObjects.forEach { savableObject ->
                val matchingIds = findActionsForSavableObject(task, savableObject)
                savableObjectsActionIds.addAll(matchingIds)
            }

            if (savableObjectsActionIds.isNotEmpty()) {
                filteredActions = filteredActions.filter { it.id in savableObjectsActionIds }
            }
        }

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

            ActionDisplayMode.INITIALS -> task.plannedActions
                .filter { it.isInitialAction }
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
            sendEvent(TaskXDetailEvent.NavigateBack)
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
                        initialActionsValidator.clearCache()
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
                // Явно вызываем обновление сохраняемых объектов
                updateSavableObjects()
                sendEvent(TaskXDetailEvent.ShowSnackbar("Действие успешно выполнено"))

                if (uiState.value.filteredActionIds.contains(actionId)) {
                    clearSearch()
                }

                // Для отладки - проверяем текущее состояние объектов
                Timber.d("После onActionCompleted: имеем ${uiState.value.savableObjects.size} сохраняемых объектов")
                Timber.d("showSavableObjectsPanel = ${uiState.value.showSavableObjectsPanel}")
                Timber.d("supportsSavableObjects = ${uiState.value.supportsSavableObjects}")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке завершения действия: ${e.message}")
                loadTask()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }

        if (query.isEmpty()) {
            clearSearch()
        }
    }

    fun searchActions() {
        val state = uiState.value
        val query = state.searchQuery.trim()
        val task = state.task
        val taskType = state.taskType

        if (query.isEmpty() || task == null || taskType == null || actionSearchService == null) {
            return
        }

        launchIO {
            updateState { it.copy(isSearching = true, searchError = null) }

            try {
                val currentActionId = state.nextActionId

                val result = actionSearchService.searchActions(
                    searchValue = query,
                    searchableObjects = taskType.searchableActionObjects,
                    plannedActions = task.plannedActions,
                    taskId = task.id,
                    currentActionId = currentActionId
                )

                if (result.isSuccess) {
                    val foundActionIds = result.getOrNull() ?: emptyList()
                    processSearchResults(foundActionIds)
                } else {
                    updateState {
                        it.copy(
                            isSearching = false,
                            searchError = result.exceptionOrNull()?.message
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error searching actions")
                updateState {
                    it.copy(
                        isSearching = false,
                        searchError = "Ошибка поиска: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearSearch() {
        updateState {
            it.copy(
                searchQuery = "",
                filteredActionIds = emptyList(),
                searchError = null,
                searchInfo = "",
                isFilteredBySavableObjects = false,
                filterMessage = ""
            )
        }
        updateFilteredActions()
    }

    fun clearAllFilters() {
        clearSearch()

        updateState {
            it.copy(
                isFilteredBySavableObjects = false,
                activeFiltersCount = 0,
                filterMessage = ""
            )
        }

        updateFilteredActions()
    }

    fun enableSavableObjectsFiltering() {
        val savableObjects = uiState.value.savableObjects
        if (savableObjects.isEmpty()) return

        // Формируем сообщение о фильтрации
        val objectTypesCount = savableObjects.map { it.objectType }.distinct().size
        val message = when {
            objectTypesCount == 1 -> "Фильтр по объекту: ${savableObjects.first().getShortDescription()}"
            else -> "Фильтр по ${savableObjects.size} объектам"
        }

        updateState {
            it.copy(
                isFilteredBySavableObjects = true,
                activeFiltersCount = savableObjects.size,
                filterMessage = message
            )
        }

        // Применяем фильтрацию
        filterActionsBySavableObjects()
    }

    fun searchByScanner(barcode: String) {
        updateState { it.copy(searchQuery = barcode) }
        tryIdentifyAndSaveObject(barcode)
        searchActions()
    }

    private fun tryIdentifyAndSaveObject(barcode: String) {
        val task = uiState.value.task ?: return
        val taskType = uiState.value.taskType ?: return

        if (!uiState.value.supportsSavableObjects || taskContextManager == null) {
            return
        }

        // Проверяем паллеты
        if (ActionObjectType.PALLET in taskType.savableObjectTypes) {
            task.plannedActions.forEach { action ->
                if (action.storagePallet?.code == barcode || action.placementPallet?.code == barcode) {
                    val pallet = action.storagePallet ?: action.placementPallet
                    if (pallet != null) {
                        taskContextManager.addSavableObject(
                            ActionObjectType.PALLET,
                            pallet,
                            "scan:pallet:$barcode"
                        )
                        return
                    }
                }
            }
        }

        // Проверяем ячейки
        if (ActionObjectType.BIN in taskType.savableObjectTypes) {
            task.plannedActions.forEach { action ->
                if (action.placementBin?.code == barcode) {
                    val bin = action.placementBin
                    taskContextManager.addSavableObject(
                        ActionObjectType.BIN,
                        bin,
                        "scan:bin:$barcode"
                    )
                    return
                }
            }
        }

        // Проверяем товары
        if (ActionObjectType.TASK_PRODUCT in taskType.savableObjectTypes ||
            ActionObjectType.CLASSIFIER_PRODUCT in taskType.savableObjectTypes) {

            // Здесь можно было бы использовать productRepository.findProductByBarcode
            // Но эта реализация выходит за рамки данного этапа

            task.plannedActions.forEach { action ->
                val product = action.storageProduct?.product
                if (product != null && product.getAllBarcodes().contains(barcode)) {
                    if (ActionObjectType.TASK_PRODUCT in taskType.savableObjectTypes) {
                        taskContextManager.addSavableObject(
                            ActionObjectType.TASK_PRODUCT,
                            action.storageProduct,
                            "scan:taskproduct:$barcode"
                        )
                    } else {
                        taskContextManager.addSavableObject(
                            ActionObjectType.CLASSIFIER_PRODUCT,
                            product,
                            "scan:product:$barcode"
                        )
                    }
                    return
                }
            }
        }
    }

    private fun processSearchResults(foundActionIds: List<String>) {
        val state = uiState.value
        val task = state.task ?: return
        val taskType = state.taskType ?: return

        updateState { it.copy(isSearching = false, filteredActionIds = foundActionIds) }

        if (foundActionIds.isEmpty()) {
            updateState { it.copy(searchError = "Действия не найдены") }
            return
        }

        if (taskType.strictActionOrder) {
            val nextAvailableAction = task.plannedActions
                .filter { it.id in foundActionIds }
                .sortedBy { it.order }
                .firstOrNull { !it.isCompleted && !it.isSkipped }

            if (nextAvailableAction != null) {
                startActionExecution(nextAvailableAction.id)
                clearSearch()
            } else {
                updateState { it.copy(searchError = "Найденные действия уже выполнены или недоступны") }
            }
        } else {
            updateFilteredActionsBySearch()

            val foundCount = foundActionIds.size
            val message = when {
                foundCount == 1 -> "Найдено 1 действие"
                foundCount in 2..4 -> "Найдено $foundCount действия"
                else -> "Найдено $foundCount действий"
            }

            updateState { it.copy(searchInfo = message) }

            if (foundActionIds.size == 1) {
                val actionId = foundActionIds.first()
                val action = task.plannedActions.find { it.id == actionId }
                if (action != null && !action.isCompleted && !action.isSkipped) {
                    startActionExecution(actionId)
                    clearSearch()
                }
            }
        }
    }

    private fun updateFilteredActionsBySearch() {
        val state = uiState.value
        val task = state.task ?: return
        val foundActionIds = state.filteredActionIds

        if (foundActionIds.isEmpty()) {
            updateFilteredActions()
            return
        }

        val filteredActions = task.plannedActions
            .filter { it.id in foundActionIds }
            .sortedBy { it.order }

        updateState { it.copy(filteredActions = filteredActions) }
    }

    fun toggleCameraScannerForSearch() {
        updateState { it.copy(showCameraScannerForSearch = !it.showCameraScannerForSearch) }
    }

    fun hideCameraScannerForSearch() {
        updateState { it.copy(showCameraScannerForSearch = false) }
    }

    fun goToInitialActions() {
        val task = uiState.value.task ?: return

        if (!uiState.value.areInitialActionsCompleted) {
            setActionsDisplayMode(ActionDisplayMode.INITIALS)

            val nextInitialActionId = initialActionsValidator.getNextInitialActionId(task)
            if (nextInitialActionId != null) {
                tryExecuteAction(nextInitialActionId)
            }
        }
    }

    fun canReopenAction(action: PlannedAction, isCompleted: Boolean): Boolean {
        if (!isCompleted) return true

        if (action.isInitialAction) return true

        return supportsMultipleFactActions() && isQuantityBasedAction(action)
    }

    fun removeSavableObject(id: String) {
        if (taskContextManager == null) return

        launchIO {
            try {
                val result = taskContextManager.removeSavableObject(id)

                if (result) {
                    // Если объект удален успешно, обновляем состояние
                    val newObjects = taskContextManager.savableObjects.value

                    updateState { currentState ->
                        currentState.copy(
                            savableObjects = newObjects,
                            showSavableObjectsPanel = newObjects.isNotEmpty()
                        )
                    }

                    // Обновляем фильтрацию действий
                    if (uiState.value.filteredActionIds.isNotEmpty()) {
                        clearSearch()
                    }

                    sendEvent(TaskXDetailEvent.ShowSnackbar("Объект удален"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при удалении сохраняемого объекта: ${e.message}")
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при удалении объекта"))
            }
        }
    }

    fun clearAllSavableObjects() {
        if (taskContextManager == null) return

        launchIO {
            try {
                taskContextManager.clearSavableObjects()

                updateState { currentState ->
                    currentState.copy(
                        savableObjects = emptyList(),
                        showSavableObjectsPanel = false
                    )
                }

                // Обновляем фильтрацию действий
                if (uiState.value.filteredActionIds.isNotEmpty()) {
                    clearSearch()
                }

                sendEvent(TaskXDetailEvent.ShowSnackbar("Все объекты очищены"))
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при очистке сохраняемых объектов: ${e.message}")
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при очистке объектов"))
            }
        }
    }

    fun filterActionsBySavableObjects() {
        val task = uiState.value.task ?: return
        val savableObjects = uiState.value.savableObjects

        if (savableObjects.isEmpty()) {
            updateFilteredActions()
            return
        }

        val matchingActionIds = mutableSetOf<String>()

        savableObjects.forEach { savableObject ->
            val ids = findActionsForSavableObject(task, savableObject)
            matchingActionIds.addAll(ids)
        }

        if (matchingActionIds.isNotEmpty()) {
            updateState { state ->
                state.copy(
                    filteredActionIds = matchingActionIds.toList(),
                    searchQuery = "Фильтр по объектам"
                )
            }
        }

        updateFilteredActions()
    }

    private fun findActionsForSavableObject(task: TaskX, savableObject: SavableObject): List<String> {
        return when (savableObject.objectType) {
            ActionObjectType.PALLET -> {
                val palletData = (savableObject.objectData as? SavableObjectData.PalletData)?.pallet
                if (palletData != null) {
                    findActionsByPallet(task, palletData)
                } else emptyList()
            }
            ActionObjectType.BIN -> {
                val binData = (savableObject.objectData as? SavableObjectData.BinData)?.bin
                if (binData != null) {
                    findActionsByBin(task, binData)
                } else emptyList()
            }
            ActionObjectType.TASK_PRODUCT -> {
                val productData = (savableObject.objectData as? SavableObjectData.TaskProductData)?.taskProduct
                if (productData != null) {
                    findActionsByTaskProduct(task, productData)
                } else emptyList()
            }
            ActionObjectType.CLASSIFIER_PRODUCT -> {
                val productData = (savableObject.objectData as? SavableObjectData.ProductData)?.product
                if (productData != null) {
                    findActionsByProduct(task, productData)
                } else emptyList()
            }
            else -> emptyList()
        }
    }

    private fun findActionsByPallet(task: TaskX, pallet: Pallet): List<String> {
        return task.plannedActions
            .filter { action ->
                action.storagePallet?.code == pallet.code ||
                        action.placementPallet?.code == pallet.code
            }
            .map { it.id }
    }

    private fun findActionsByBin(task: TaskX, bin: BinX): List<String> {
        return task.plannedActions
            .filter { action ->
                action.placementBin?.code == bin.code
            }
            .map { it.id }
    }

    private fun findActionsByTaskProduct(task: TaskX, taskProduct: TaskProduct): List<String> {
        return task.plannedActions
            .filter { action ->
                action.storageProduct?.product?.id == taskProduct.product.id
            }
            .map { it.id }
    }

    private fun findActionsByProduct(task: TaskX, product: Product): List<String> {
        return task.plannedActions
            .filter { action ->
                action.storageProduct?.product?.id == product.id
            }
            .map { it.id }
    }

    override fun dispose() {
        super.dispose()
        stopObservingTaskChanges()
    }
}