package com.synngate.synnframe.presentation.ui.taskx

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolder
import com.synngate.synnframe.presentation.ui.taskx.model.ActionFilter
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * ViewModel для экрана детального просмотра задания
 */
class TaskXDetailViewModel(
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases,
    private val taskXDataHolder: TaskXDataHolder
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    init {
        initializeFromDataHolder()
        observeTaskChanges()
        loadCurrentUser()
    }

    /**
     * Инициализация из холдера данных
     */
    private fun initializeFromDataHolder() {
        val task = taskXDataHolder.currentTask.value
        val taskType = taskXDataHolder.currentTaskType

        if (task != null && taskType != null) {
            updateState {
                it.copy(
                    task = task,
                    taskType = taskType,
                    filteredActions = it.getDisplayActions()
                )
            }
        } else {
            Timber.e("Task or TaskType not found in TaskXDataHolder")
            sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка загрузки данных задания"))
            sendEvent(TaskXDetailEvent.NavigateBack)
        }
    }

    /**
     * Наблюдение за изменениями задания
     */
    private fun observeTaskChanges() {
        taskXDataHolder.currentTask
            .onEach { task ->
                task?.let {
                    updateState { state ->
                        state.copy(
                            task = it,
                            filteredActions = state.getDisplayActions()
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Загрузка текущего пользователя
     */
    private fun loadCurrentUser() {
        launchIO {
            userUseCases.getCurrentUser().collect { user ->
                updateState { it.copy(currentUserId = user?.id) }
            }
        }
    }

    /**
     * Обработка нажатия на действие
     */
    fun onActionClick(actionId: String) {
        val task = uiState.value.task ?: return

        // Проверяем, может ли пользователь выполнять действие
        if (task.status != TaskXStatus.IN_PROGRESS) {
            sendEvent(TaskXDetailEvent.ShowSnackbar("Задание должно быть в статусе 'Выполняется'"))
            return
        }

        // Переход к визарду действия
        sendEvent(TaskXDetailEvent.NavigateToActionWizard(task.id, actionId))
    }

    /**
     * Изменение фильтра действий
     */
    fun onFilterChange(filter: ActionFilter) {
        updateState {
            it.copy(
                actionFilter = filter,
                filteredActions = it.copy(actionFilter = filter).getDisplayActions()
            )
        }
    }

    /**
     * Обработка кнопки "Назад"
     */
    fun onBackPressed() {
        val task = uiState.value.task

        // Если задание выполняется или на паузе, показываем диалог
        if (task?.status == TaskXStatus.IN_PROGRESS || task?.status == TaskXStatus.PAUSED) {
            updateState { it.copy(showExitDialog = true) }
        } else {
            sendEvent(TaskXDetailEvent.NavigateBack)
        }
    }

    /**
     * Закрытие диалога выхода
     */
    fun dismissExitDialog() {
        updateState { it.copy(showExitDialog = false) }
    }

    /**
     * Выход без сохранения
     */
    fun exitWithoutSaving() {
        dismissExitDialog()
        sendEvent(TaskXDetailEvent.NavigateBack)
    }

    /**
     * Продолжение работы
     */
    fun continueWork() {
        dismissExitDialog()
    }

    /**
     * Приостановка задания
     */
    fun pauseTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO
            val endpoint = taskXDataHolder.endpoint ?: return@launchIO

            updateState { it.copy(isProcessingAction = true, showExitDialog = false) }

            try {
                val result = taskXUseCases.pauseTask(task.id, endpoint)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    if (updatedTask != null) {
                        taskXDataHolder.updateTask(updatedTask)
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

    /**
     * Завершение задания
     */
    fun completeTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO
            val taskType = uiState.value.taskType ?: return@launchIO
            val endpoint = taskXDataHolder.endpoint ?: return@launchIO

            // Проверяем, можно ли завершить задание
            val pendingActions = task.plannedActions.filter {
                !it.isCompleted && !it.manuallyCompleted && !it.isSkipped
            }

            if (pendingActions.isNotEmpty() && !taskType.allowCompletionWithoutFactActions) {
                sendEvent(TaskXDetailEvent.ShowSnackbar("Необходимо выполнить все запланированные действия"))
                return@launchIO
            }

            updateState { it.copy(isProcessingAction = true, showExitDialog = false) }

            try {
                val result = taskXUseCases.completeTask(task.id, endpoint)

                if (result.isSuccess) {
                    // Очищаем данные в холдере
                    taskXDataHolder.clear()
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

    }

    fun hideCameraScannerForSearch() {

    }
}