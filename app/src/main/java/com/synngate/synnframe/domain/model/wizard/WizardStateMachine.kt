package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.service.ActionExecutionService
import com.synngate.synnframe.domain.service.ActionStepExecutionService
import com.synngate.synnframe.domain.service.TaskContextManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Машина состояний для управления визардом действий.
 * Отвечает за переходы между состояниями на основе внешних событий.
 */
class WizardStateMachine(
    private val taskContextManager: TaskContextManager,
    private val actionExecutionService: ActionExecutionService,
    private val actionStepExecutionService: ActionStepExecutionService
) {
    // Текущее состояние машины
    private val _currentState = MutableStateFlow<WizardState?>(null)
    val currentState: StateFlow<WizardState?> = _currentState.asStateFlow()

    // Текущий контекст
    private var context: WizardContext = WizardContext()

    // Публичный доступ к контексту (только для чтения)
    val wizardContext: WizardContext get() = context

    init {
        // Начальное состояние - null
        _currentState.value = null
    }

    /**
     * Обрабатывает событие и переходит в новое состояние, если это необходимо.
     * @param event Событие для обработки
     * @return true, если переход состоялся, false - если состояние не изменилось
     */
    fun handleEvent(event: WizardEvent): Boolean {
        val currentState = _currentState.value
        Timber.d("WizardStateMachine: handling event $event in state ${currentState?.id}")

        // Если текущее состояние не установлено и событие не инициализация,
        // то сначала нужно инициализировать
        if (currentState == null && event !is WizardEvent.Initialize) {
            Timber.e("Cannot handle event $event: machine not initialized")
            return false
        }

        // Запрашиваем у текущего состояния новое состояние для данного события
        val nextState = currentState?.handleEvent(event)

        // Если получили новое состояние, обновляем текущее
        if (nextState != null && nextState != currentState) {
            Timber.d("Transitioning from ${currentState?.id} to ${nextState.id}")
            _currentState.value = nextState
            return true
        }

        if (nextState == currentState) {
            Timber.d("State unchanged after handling event $event")
        } else if (nextState == null) {
            Timber.d("No state transition for event $event in state ${currentState?.id}")
        }

        return false
    }

    /**
     * Инициализация визарда с данными из TaskContextManager
     */
    suspend fun initialize(taskId: String, actionId: String): Boolean {
        try {
            val task = taskContextManager.lastStartedTaskX.value
                ?: return false

            if (task.id != taskId) {
                Timber.e("Task ID mismatch: expected $taskId, got ${task.id}")
                return false
            }

            val action = task.plannedActions.find { it.id == actionId }
                ?: return false

            val taskType = taskContextManager.lastTaskTypeX.value

            // Создаем шаги из шаблона действия
            val steps = createStepsFromAction(action)

            if (steps.isEmpty()) {
                Timber.e("No steps created for action $actionId")
                return false
            }

            // Обновляем контекст
            if (taskType != null)
                context = WizardContext(
                    taskId = taskId,
                    actionId = actionId,
                    task = task,
                    action = action,
                    steps = steps,
                    // Здесь можно добавить дополнительные данные
                    results = mapOf("taskType" to taskType)
                )

            // Создаем начальное состояние и устанавливаем его
            val initialState = context.createInitializingState()
            _currentState.value = initialState

            // Обрабатываем событие инициализации
            return handleEvent(WizardEvent.Initialize(taskId, actionId))
        } catch (e: Exception) {
            Timber.e(e, "Error initializing wizard for task $taskId, action $actionId")
            return false
        }
    }

    /**
     * Создает список шагов из шаблона действия
     */
    private fun createStepsFromAction(action: PlannedAction): List<WizardStep> {
        val steps = mutableListOf<WizardStep>()
        val template = action.actionTemplate

        // Добавляем шаги хранения
        template.storageSteps.sortedBy { it.order }.forEach { actionStep ->
            steps.add(
                WizardStep(
                    id = actionStep.id,
                    title = actionStep.name,
                    content = { /* Заполняется в UI */ },
                    canNavigateBack = true,
                    isAutoComplete = false,
                    shouldShow = { true }
                )
            )
        }

        // Добавляем шаги размещения
        template.placementSteps.sortedBy { it.order }.forEach { actionStep ->
            steps.add(
                WizardStep(
                    id = actionStep.id,
                    title = actionStep.name,
                    content = { /* Заполняется в UI */ },
                    canNavigateBack = true,
                    isAutoComplete = false,
                    shouldShow = { true }
                )
            )
        }

        return steps
    }

    /**
     * Сброс машины состояний в начальное состояние
     */
    fun reset() {
        Timber.d("Resetting state machine")
        _currentState.value = null
        context = WizardContext()
    }

    /**
     * Выполнение действия с текущими данными контекста
     */
    suspend fun executeAction(): Result<TaskX> {
        val taskId = context.taskId
        val actionId = context.actionId

        if (taskId.isEmpty() || actionId.isEmpty()) {
            return Result.failure(IllegalStateException("Task ID or Action ID is empty"))
        }

        return actionExecutionService.executeAction(
            taskId = taskId,
            actionId = actionId,
            stepResults = context.results,
            completeAction = false
        )
    }
}