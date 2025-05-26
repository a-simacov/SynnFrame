package com.synngate.synnframe.presentation.ui.taskx

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolder
import com.synngate.synnframe.presentation.ui.taskx.model.StatusActionData
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class TaskXDetailViewModel(
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases,
    private val taskXDataHolder: TaskXDataHolder
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    init {
        // Инициализация состояния из холдера или переданных данных
        val task = taskXDataHolder.currentTask.value
        val taskType = taskXDataHolder.currentTaskType.value

        if (task != null && taskType != null) {
            updateState {
                it.copy(
                    task = task,
                    taskType = taskType
                )
            }

            // Подписываемся на изменения в холдере
            observeTaskChanges()

            // Загружаем текущего пользователя
            loadCurrentUser()

            // Обновляем доступные действия
            updateAvailableActions()
        } else {
            Timber.e("Task or TaskType not found in TaskXDataHolder")
            sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка загрузки данных задания"))
            sendEvent(TaskXDetailEvent.NavigateBack)
        }
    }

    private fun observeTaskChanges() {
        // Подписываемся на изменения задания в холдере
        taskXDataHolder.currentTask
            .onEach { task ->
                task?.let {
                    updateState { state ->
                        state.copy(
                            task = it,
                            // Обновляем другие связанные поля
                            hasInitialActions = it.getInitialActions().isNotEmpty(),
                            areInitialActionsCompleted = it.areInitialActionsCompleted(),
                            completedInitialActionsCount = it.getCompletedInitialActionsCount(),
                            totalInitialActionsCount = it.getTotalInitialActionsCount()
                        )
                    }
                    updateAvailableActions()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadCurrentUser() {
        launchIO {
            userUseCases.getCurrentUser().collect { user ->
                updateState { it.copy(currentUserId = user?.id) }
            }
        }
    }

    private fun updateAvailableActions() {
        val task = uiState.value.task ?: return
        val taskType = uiState.value.taskType ?: return

        // Определяем доступные действия со статусом задания
        val statusActions = when (task.status) {
            TaskXStatus.TO_DO -> listOf(
                StatusActionData(
                    id = "start",
                    iconName = "play_arrow",
                    text = "Начать выполнение",
                    description = "Начать выполнение задания",
                    onClick = { startTask() }
                )
            )
            TaskXStatus.IN_PROGRESS -> listOf(
                StatusActionData(
                    id = "pause",
                    iconName = "pause",
                    text = "Приостановить",
                    description = "Приостановить выполнение",
                    onClick = { pauseTask() }
                ),
                StatusActionData(
                    id = "finish",
                    iconName = "check_circle",
                    text = "Завершить",
                    description = "Завершить задание",
                    onClick = { showCompletionDialog() }
                )
            )
            TaskXStatus.PAUSED -> listOf(
                StatusActionData(
                    id = "resume",
                    iconName = "play_arrow",
                    text = "Возобновить",
                    description = "Возобновить выполнение",
                    onClick = { resumeTask() }
                )
            )
            else -> emptyList()
        }

        updateState { it.copy(statusActions = statusActions) }
    }

    fun startTask() {
        launchIO {
            val currentState = uiState.value
            val task = currentState.task ?: return@launchIO
            val currentUserId = currentState.currentUserId ?: return@launchIO
            val endpoint = taskXDataHolder.endpoint ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.startTask(task.id, currentUserId, endpoint)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    if (updatedTask != null) {
                        // Обновляем задание в холдере
                        taskXDataHolder.updateTask(updatedTask)
                        sendEvent(TaskXDetailEvent.ShowSnackbar("Задание начато"))
                    }
                } else {
                    updateState { it.copy(isProcessing = false) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при запуске задания"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on starting task")
                updateState { it.copy(isProcessing = false) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при запуске задания"))
            }
        }
    }

    fun completeTask() {
        launchIO {
            val currentState = uiState.value
            val task = currentState.task ?: return@launchIO
            val taskType = currentState.taskType ?: return@launchIO
            val endpoint = taskXDataHolder.endpoint ?: return@launchIO

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
                val result = taskXUseCases.completeTask(task.id, endpoint)

                if (result.isSuccess) {
                    // Очищаем данные в холдере при завершении
                    taskXDataHolder.clear()
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
            val endpoint = taskXDataHolder.endpoint ?: return@launchIO

            updateState { it.copy(isProcessingDialogAction = true) }

            try {
                val result = taskXUseCases.pauseTask(task.id, endpoint)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    if (updatedTask != null) {
                        taskXDataHolder.updateTask(updatedTask)
                    }
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
            val endpoint = taskXDataHolder.endpoint ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.startTask(task.id, currentUserId, endpoint)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    if (updatedTask != null) {
                        taskXDataHolder.updateTask(updatedTask)
                        sendEvent(TaskXDetailEvent.ShowSnackbar("Задание возобновлено"))
                    }
                } else {
                    updateState { it.copy(isProcessing = false) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при возобновлении задания"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on resuming task")
                updateState { it.copy(isProcessing = false) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при возобновлении задания"))
            }
        }
    }

    fun showCompletionDialog() {
        updateState { it.copy(showCompletionDialog = true) }
    }

    fun hideCompletionDialog() {
        updateState { it.copy(showCompletionDialog = false) }
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

    fun toggleCameraScannerForSearch() {
        updateState { it.copy(showCameraScannerForSearch = !it.showCameraScannerForSearch) }
    }

    fun hideCameraScannerForSearch() {
        updateState { it.copy(showCameraScannerForSearch = false) }
    }

    fun searchByScanner(barcode: String) {

    }
}