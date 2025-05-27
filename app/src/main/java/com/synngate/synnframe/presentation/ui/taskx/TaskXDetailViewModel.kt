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

/**
 * ViewModel для экрана детального просмотра задания
 */
class TaskXDetailViewModel(
    private val taskId: String,
    private val endpoint: String,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    // Валидатор действий
    private val actionValidator = ActionValidator()

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
                // Логируем начало загрузки
                Timber.d("Начало загрузки задания $taskId с endpoint $endpoint")
                val startEndpoint = "$endpoint/$taskId/take"
                val taskResult = dynamicMenuUseCases.startDynamicTask(startEndpoint, taskId)

                if (taskResult.isSuccess()) {
                    val task = taskResult.getOrNull()
                    if (task != null && task.taskType != null) {
                        // Сохраняем в синглтон
                        TaskXDataHolderSingleton.setTaskData(task, task.taskType, endpoint)
                        Timber.d("Задание $taskId успешно загружено и сохранено в синглтоне")

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
        val actionUiModel = uiState.value.actionUiModels.find { it.id == actionId } ?: return

        if (!actionUiModel.isClickable) {
            sendEvent(TaskXDetailEvent.ShowSnackbar("Действие уже выполнено"))
            return
        }

        // Проверяем, может ли пользователь выполнять действие
        if (task.status != TaskXStatus.IN_PROGRESS) {
            sendEvent(TaskXDetailEvent.ShowSnackbar("Задание должно быть в статусе 'Выполняется'"))
            return
        }

        // Проверяем порядок выполнения действий
        val validationResult = actionValidator.canExecuteAction(task, actionId)
        if (!validationResult.isSuccess) {
            // Показываем диалог с ошибкой
            showValidationError(validationResult.errorMessage ?: "Невозможно выполнить действие")
            return
        }

        // Проверяем, что данные задания есть в синглтоне перед навигацией
        if (!TaskXDataHolderSingleton.hasData()) {
            Timber.e("Нет данных в TaskXDataHolderSingleton перед запуском визарда для действия $actionId")
            // Если данных нет, пытаемся сохранить их снова
            TaskXDataHolderSingleton.setTaskData(task, task.taskType!!, endpoint)

            // Двойная проверка
            if (!TaskXDataHolderSingleton.hasData()) {
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка: невозможно запустить визард. Данные недоступны."))
                return
            }
        }

        // Логируем переход к визарду
        Timber.d("Переход к визарду для действия $actionId задания ${task.id}")

        // Переход к визарду действия
        sendEvent(TaskXDetailEvent.NavigateToActionWizard(task.id, actionId))
    }

    /**
     * Показать ошибку валидации порядка выполнения
     */
    private fun showValidationError(message: String) {
        updateState {
            it.copy(
                showValidationErrorDialog = true,
                validationErrorMessage = message
            )
        }
    }

    /**
     * Закрыть диалог ошибки валидации
     */
    fun dismissValidationErrorDialog() {
        updateState {
            it.copy(
                showValidationErrorDialog = false,
                validationErrorMessage = null
            )
        }
    }

    /**
     * Изменение фильтра действий
     */
    fun onFilterChange(filter: ActionFilter) {
        updateState {
            it.copy(actionFilter = filter)
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
                            it.copy(task = updatedTask)
                        }

                        // Опционально обновляем холдер
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

    /**
     * Завершение задания
     */
    fun completeTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO
            val taskType = uiState.value.taskType ?: return@launchIO

            // Проверяем, можно ли завершить задание
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
                    // Принудительно очищаем данные в синглтоне при успешном завершении задания
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
}