package com.synngate.synnframe.presentation.ui.taskx

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.AvailableTaskAction
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.taskx.model.StatusActionData
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailView
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
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
    val actionWizardController: ActionWizardController,
    val actionWizardContextFactory: ActionWizardContextFactory,
    val actionStepFactoryRegistry: ActionStepFactoryRegistry,
    private val preloadedTask: TaskX? = null,
    private val preloadedTaskType: TaskTypeX? = null
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private var taskObserverJob: Job? = null

    init {
        // Если есть предзагруженные данные из TaskContextManager, используем их
        if (preloadedTask != null && preloadedTaskType != null) {
            updateState {
                it.copy(
                    task = preloadedTask,
                    taskType = preloadedTaskType,
                    isLoading = false,
                    error = null
                )
            }
            // Инициализируем все зависимые данные
            updateDependentState(preloadedTask, preloadedTaskType)
            startObservingTaskChanges()
        } else {
            // Иначе загружаем стандартным способом
            loadTask()
        }
    }

    fun startActionExecution(actionId: String) {
        launchIO {
            try {
                Timber.d("Starting action execution for actionId: $actionId")

                val task = uiState.value.task ?: return@launchIO

                // Проверяем, что задание в статусе "Выполняется"
                if (task.status != TaskXStatus.IN_PROGRESS) {
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание не в статусе 'Выполняется'"))
                    return@launchIO
                }

                // Проверяем строгий порядок выполнения
                val isStrictOrder = uiState.value.taskType?.strictActionOrder == true
                val action = task.plannedActions.find { it.id == actionId }

                if (isStrictOrder && action != null) {
                    // Если порядок строгий, проверяем, что это первое не выполненное действие
                    val firstNotCompletedAction = task.plannedActions
                        .sortedBy { it.order }
                        .firstOrNull { !it.isCompleted && !it.isSkipped }

                    if (firstNotCompletedAction?.id != actionId) {
                        showOrderRequiredMessage()
                        return@launchIO
                    }
                }

                val result = actionWizardController.initialize(taskId, actionId)
                if (result.isSuccess) {
                    Timber.d("Action wizard initialized successfully")
                    // Обновляем состояние, чтобы показать визард
                    updateState { it.copy(showActionWizard = true) }
                } else {
                    Timber.e("Failed to initialize action wizard: ${result.exceptionOrNull()?.message}")
                    updateState { it.copy(error = "Ошибка при инициализации действия: ${result.exceptionOrNull()?.message}") }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Не удалось начать выполнение действия"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing action wizard")
                updateState { it.copy(error = "Ошибка при инициализации действия: ${e.message}") }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Не удалось начать выполнение действия"))
            }
        }
    }

    // Определение следующего действия
    private fun calculateNextActionId(task: TaskX, taskType: TaskTypeX?): String? {
        val isStrictOrder = uiState.value.taskType?.strictActionOrder == true

        return if (isStrictOrder) {
            task.plannedActions
                .sortedBy { it.order }
                .firstOrNull { !it.isCompleted && !it.isSkipped }
                ?.id
        } else {
            null
        }
    }

    // Проверка, можно ли выполнить действие
    fun canExecuteAction(actionId: String): Boolean {
        val task = uiState.value.task ?: return false
        val isStrictOrder = uiState.value.taskType?.strictActionOrder == true

        // Для задания без строгого порядка - можно выполнять любое действие
        if (!isStrictOrder) return true

        // Для задания со строгим порядком - только первое невыполненное
        val nextActionId = uiState.value.nextActionId
        return nextActionId == actionId
    }

    // Попытка выполнить действие с проверкой порядка
    fun tryExecuteAction(actionId: String) {
        if (canExecuteAction(actionId)) {
            startActionExecution(actionId)
        } else {
            showOrderRequiredMessage()
        }
    }

    // Проверка наличия дополнительных действий
    private fun checkHasAdditionalActions(task: TaskX): Boolean {
        return !task.isVerified && isActionAvailable(AvailableTaskAction.VERIFY_TASK) ||
                isActionAvailable(AvailableTaskAction.PRINT_TASK_LABEL)
    }

    // Формирование списка действий для статуса
    private fun createStatusActions(task: TaskX, taskType: TaskTypeX?): List<StatusActionData> {
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
                    text = "Пауза",  // Добавлен текст кнопки
                    description = "Приостановить",
                    onClick = ::pauseTask
                ),
                StatusActionData(
                    id = "finish",
                    iconName = "check_circle",
                    text = "Финиш",  // Добавлен текст кнопки
                    description = "Завершить",
                    onClick = ::showCompletionDialog,
                )
            )
            TaskXStatus.PAUSED -> listOf(
                StatusActionData(
                    id = "resume",
                    iconName = "play_arrow",
                    description = "Продолжить",
                    text = "Старт",  // Добавлен текст кнопки
                    onClick = ::resumeTask
                )
            )
            else -> emptyList()
        }
    }

    // Метод для показа сообщения о необходимости соблюдения порядка выполнения
    fun showOrderRequiredMessage() {
        updateState { it.copy(showOrderRequiredMessage = true) }
        sendEvent(TaskXDetailEvent.ShowSnackbar("Необходимо выполнять действия в указанном порядке"))
    }

    // Метод для скрытия сообщения о необходимости соблюдения порядка
    fun hideOrderRequiredMessage() {
        updateState { it.copy(showOrderRequiredMessage = false) }
    }

    // Метод для скрытия визарда
    fun hideActionWizard() {
        updateState { it.copy(showActionWizard = false) }
        // Отменяем визард при скрытии
        actionWizardController.cancel()
    }

    // Метод для завершения визарда
    fun completeActionWizard() {
        launchIO {
            try {
                val result = actionWizardController.complete()
                if (result.isSuccess) {
                    // Получаем обновленное задание из результата
                    val updatedTask = result.getOrNull()

                    if (updatedTask != null) {
                        // Сразу обновляем состояние с новым заданием
                        updateState { it.copy(
                            task = updatedTask,
                            showActionWizard = false
                        ) }
                        // Обновляем зависимые данные (nextActionId, statusActions и т.д.)
                        updateDependentState(updatedTask, uiState.value.taskType)
                    } else {
                        // Если обновленное задание не вернулось, перезагружаем его
                        loadTask()
                        updateState { it.copy(showActionWizard = false) }
                    }

                    sendEvent(TaskXDetailEvent.ShowSnackbar("Действие успешно выполнено"))
                } else {
                    updateState { it.copy(error = "Ошибка при выполнении действия: ${result.exceptionOrNull()?.message}") }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Не удалось выполнить действие"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error completing action wizard")
                updateState { it.copy(error = "Ошибка при завершении действия: ${e.message}") }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Не удалось выполнить действие"))
            }
        }
    }

    fun loadTask() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                // Если у нас уже есть предзагруженные данные, просто используем их
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

                // Иначе загружаем задание из репозитория
                val task = taskXUseCases.getTaskById(taskId)

                if (task != null) {
                    val taskType = taskXUseCases.getTaskType(task.taskTypeId)
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
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on starting task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error on starting task: ${e.message}"
                    )
                }
            }
        }
    }

    fun completeTask() {
        launchIO {
            val currentState = uiState.value
            val task = currentState.task ?: return@launchIO
            val taskType = currentState.taskType ?: return@launchIO

            // Проверяем, что все необходимые действия выполнены
            val allActionsCompleted = task.plannedActions.all { it.isCompleted || it.isSkipped }

            // Если не все действия выполнены и не установлен флаг allowCompletionWithoutFactActions,
            // то показываем предупреждение и не завершаем задание
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
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on pausing task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error on pausing task: ${e.message}"
                    )
                }
            }
        }
    }

    fun resumeTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.resumeTask(task.id)

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
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on resuming task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error on resuming task: ${e.message}"
                    )
                }
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

    /**
     * Метод для начала наблюдения за изменениями статуса задания
     * Автоматически обновляет зависимые данные при изменении статуса
     */
    private fun startObservingTaskChanges() {
        // Отменяем предыдущее наблюдение, если оно было
        taskObserverJob?.cancel()

        // Запускаем новое наблюдение
        taskObserverJob = viewModelScope.launch {
            uiState
                .map { it.task }
                .distinctUntilChanged { old, new ->
                    // Расширенное сравнение - теперь учитываем количество фактических действий
                    // и количество выполненных запланированных действий
                    if (old == null || new == null) false
                    else old.id == new.id &&
                            old.status == new.status &&
                            old.factActions.size == new.factActions.size &&
                            old.plannedActions.count { it.isCompleted } == new.plannedActions.count { it.isCompleted }
                }
                .collect { task ->
                    if (task != null) {
                        val taskType = uiState.value.taskType
                        Timber.d("Задание изменилось, обновляем UI: ${task.status}, факт. действий: ${task.factActions.size}, завершено плановых: ${task.plannedActions.count { it.isCompleted }}")
                        updateDependentState(task, taskType)
                    }
                }
        }
    }

    /**
     * Метод для остановки наблюдения
     */
    private fun stopObservingTaskChanges() {
        taskObserverJob?.cancel()
        taskObserverJob = null
    }

    private fun updateDependentState(task: TaskX, taskType: TaskTypeX?) {
        val nextActionId = calculateNextActionId(task, taskType)
        val hasAdditionalActions = checkHasAdditionalActions(task)
        val statusActions = createStatusActions(task, taskType)

        updateState { currentState ->
            currentState.copy(
                nextActionId = nextActionId,
                hasAdditionalActions = hasAdditionalActions,
                statusActions = statusActions
            )
        }
    }

    fun showCompletionDialog() {
        updateState { it.copy(showCompletionDialog = true) }
    }

    fun hideCompletionDialog() {
        updateState { it.copy(showCompletionDialog = false) }
    }

    fun showVerificationDialog() {
        updateState { it.copy(showVerificationDialog = true) }
    }

    fun hideVerificationDialog() {
        updateState { it.copy(showVerificationDialog = false) }
    }

    fun formatDate(dateTime: LocalDateTime?): String {
        return dateTime?.format(dateFormatter) ?: "Не указано"
    }

    fun formatTaskStatus(status: TaskXStatus): String {
        return when (status) {
            TaskXStatus.TO_DO -> "К выполнению"
            TaskXStatus.IN_PROGRESS -> "Выполняется"
            TaskXStatus.PAUSED -> "Приостановлено"
            TaskXStatus.COMPLETED -> "Завершено"
            TaskXStatus.CANCELLED -> "Отменено"
        }
    }

    fun formatTaskType(taskTypeId: String): String {
        return uiState.value.taskType?.name ?: "Неизвестный тип"
    }

    // Методы для переключения видов
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

    override fun dispose() {
        super.dispose()
        stopObservingTaskChanges()
    }
}