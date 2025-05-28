package com.synngate.synnframe.presentation.ui.taskx

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.model.ActionFilter
import com.synngate.synnframe.presentation.ui.taskx.model.PlannedActionUI
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.ui.taskx.validator.ActionValidator
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

class TaskXDetailViewModel(
    private val taskId: String,
    private val endpoint: String,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    private val actionValidator = ActionValidator()

    init {
        loadTask()
        loadCurrentUser()
    }

    private fun loadTask() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val startEndpoint = "$endpoint/$taskId/take"
                val taskResult = dynamicMenuUseCases.startDynamicTask(startEndpoint, taskId)

                if (taskResult.isSuccess()) {
                    val task = taskResult.getOrNull()
                    if (task != null && task.taskType != null) {
                        TaskXDataHolderSingleton.setTaskData(task, task.taskType, endpoint)

                        val actionUiModels = createActionUiModels(task)

                        updateState {
                            it.copy(
                                task = task,
                                taskType = task.taskType,
                                actionUiModels = actionUiModels,
                                isLoading = false
                            )
                        }
                    } else {
                        Timber.e("Задание или его тип не определены после загрузки")
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "Не удалось загрузить данные задания"
                            )
                        }
                        sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка загрузки данных задания"))
                    }
                } else {
                    val error = (taskResult as? ApiResult.Error)?.message ?: "Неизвестная ошибка"
                    Timber.e("Ошибка при загрузке задания: $error")
                    updateState { it.copy(isLoading = false, error = error) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка загрузки: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Исключение при загрузке задания $taskId")
                updateState { it.copy(isLoading = false, error = e.message) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    private fun createActionUiModels(task: TaskX): List<PlannedActionUI> {
        val isTaskInProgress = task.status == TaskXStatus.IN_PROGRESS

        return task.plannedActions.map { action ->
            PlannedActionUI.fromDomain(
                action = action,
                factActions = task.factActions,
                isTaskInProgress = isTaskInProgress
            )
        }
    }

    private fun loadCurrentUser() {
        launchIO {
            userUseCases.getCurrentUser().collect { user ->
                updateState { it.copy(currentUserId = user?.id) }
            }
        }
    }

    fun onActionClick(actionId: String) {
        val task = uiState.value.task ?: return
        val actionUiModel = uiState.value.actionUiModels.find { it.id == actionId } ?: return

        if (!actionUiModel.isClickable) {
            sendEvent(TaskXDetailEvent.ShowSnackbar("Действие уже выполнено"))
            return
        }

        if (task.status != TaskXStatus.IN_PROGRESS) {
            sendEvent(TaskXDetailEvent.ShowSnackbar("Задание должно быть в статусе 'Выполняется'"))
            return
        }

        val validationResult = actionValidator.canExecuteAction(task, actionId)
        if (!validationResult.isSuccess) {
            showValidationError(validationResult.errorMessage ?: "Невозможно выполнить действие")
            return
        }

        if (!TaskXDataHolderSingleton.hasData()) {
            TaskXDataHolderSingleton.setTaskData(task, task.taskType!!, endpoint)

            if (!TaskXDataHolderSingleton.hasData()) {
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка: невозможно запустить визард. Данные недоступны."))
                return
            }
        }

        sendEvent(TaskXDetailEvent.NavigateToActionWizard(task.id, actionId))
    }

    private fun showValidationError(message: String) {
        updateState {
            it.copy(
                showValidationErrorDialog = true,
                validationErrorMessage = message
            )
        }
    }

    fun dismissValidationErrorDialog() {
        updateState {
            it.copy(
                showValidationErrorDialog = false,
                validationErrorMessage = null
            )
        }
    }

    fun onFilterChange(filter: ActionFilter) {
        updateState {
            it.copy(actionFilter = filter)
        }
    }

    fun onBackPressed() {
        val task = uiState.value.task

        if (task?.status == TaskXStatus.IN_PROGRESS || task?.status == TaskXStatus.PAUSED) {
            updateState { it.copy(showExitDialog = true) }
        } else {
            sendEvent(TaskXDetailEvent.NavigateBack)
        }
    }

    fun dismissExitDialog() {
        updateState { it.copy(showExitDialog = false) }
    }

    fun exitWithoutSaving() {
        dismissExitDialog()
        sendEvent(TaskXDetailEvent.NavigateBack)
    }

    fun continueWork() {
        dismissExitDialog()
    }

    fun pauseTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            updateState { it.copy(isProcessingAction = true, showExitDialog = false) }

            try {
                val result = taskXUseCases.pauseTask(task.id, endpoint)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    if (updatedTask != null) {
                        updateState {
                            it.copy(task = updatedTask)
                        }

                        TaskXDataHolderSingleton.updateTask(updatedTask)
                    }
                    sendEvent(TaskXDetailEvent.NavigateBackWithMessage("Задание приостановлено"))
                } else {
                    updateState { it.copy(isProcessingAction = false) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при приостановке задания"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error pausing task")
                updateState { it.copy(isProcessingAction = false) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    fun completeTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO
            val taskType = uiState.value.taskType ?: return@launchIO

            val validationResult = actionValidator.canCompleteTask(task)
            if (!validationResult.isSuccess && !taskType.allowCompletionWithoutFactActions) {
                sendEvent(TaskXDetailEvent.ShowSnackbar(
                    validationResult.errorMessage ?: "Невозможно завершить задание"
                ))
                return@launchIO
            }

            updateState { it.copy(isProcessingAction = true, showExitDialog = false) }

            try {
                val result = taskXUseCases.completeTask(task.id, endpoint)

                if (result.isSuccess) {
                    TaskXDataHolderSingleton.forceClean()
                    sendEvent(TaskXDetailEvent.NavigateBackWithMessage("Задание завершено"))
                } else {
                    updateState { it.copy(isProcessingAction = false) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при завершении задания"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error completing task")
                updateState { it.copy(isProcessingAction = false) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    fun searchByScanner(barcode: String) {
        // Реализация поиска по штрих-коду
    }

    fun hideCameraScannerForSearch() {
        updateState { it.copy(showCameraScannerForSearch = false) }
    }

    fun checkTaskCompletion() {
        val task = uiState.value.task ?: return

        val allActionsCompleted = task.plannedActions.all { it.isFullyCompleted(task.factActions) }

        if (allActionsCompleted && !uiState.value.showCompletionDialog) {
            updateState { it.copy(showCompletionDialog = true) }
        }
    }

    fun dismissCompletionDialog() {
        updateState { it.copy(showCompletionDialog = false) }
    }
}