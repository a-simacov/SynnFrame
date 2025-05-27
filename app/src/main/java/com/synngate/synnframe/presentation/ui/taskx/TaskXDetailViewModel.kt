package com.synngate.synnframe.presentation.ui.taskx

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolder
import com.synngate.synnframe.presentation.ui.taskx.model.ActionFilter
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

/**
 * ViewModel для экрана детального просмотра задания
 */
class TaskXDetailViewModel(
    private val taskId: String,
    private val endpoint: String,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases,
    private val taskXDataHolder: TaskXDataHolder? = null // Опциональный параметр для обратной совместимости
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    init {
        loadTask()
        loadCurrentUser()
    }

    /**
     * Загрузка данных задания с сервера
     */
    private fun loadTask() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val taskResult = dynamicMenuUseCases.startDynamicTask(endpoint, taskId)

                if (taskResult.isSuccess()) {
                    val task = taskResult.getOrNull()
                    if (task != null && task.taskType != null) {
                        // Опционально сохраняем в холдер для использования другими компонентами
                        taskXDataHolder?.setTaskData(task, task.taskType, endpoint)

                        updateState {
                            it.copy(
                                task = task,
                                taskType = task.taskType,
                                isLoading = false,
                                filteredActions = it.copy(task = task).getDisplayActions()
                            )
                        }
                    } else {
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
                    updateState { it.copy(isLoading = false, error = error) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка загрузки: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading task data")
                updateState { it.copy(isLoading = false, error = e.message) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
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

            updateState { it.copy(isProcessingAction = true, showExitDialog = false) }

            try {
                val result = taskXUseCases.pauseTask(task.id, endpoint)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    if (updatedTask != null) {
                        // Обновляем состояние
                        updateState {
                            it.copy(
                                task = updatedTask,
                                filteredActions = it.copy(task = updatedTask).getDisplayActions()
                            )
                        }

                        // Опционально обновляем холдер
                        taskXDataHolder?.updateTask(updatedTask)
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
                    taskXDataHolder?.clear()
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
}