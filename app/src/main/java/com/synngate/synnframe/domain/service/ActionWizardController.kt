package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.model.wizard.WizardStep
import com.synngate.synnframe.domain.repository.TaskXRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Контроллер для управления визардом действий
 */
class ActionWizardController(
    private val taskXRepository: TaskXRepository,
    private val actionExecutionService: ActionExecutionService,
    private val actionStepExecutionService: ActionStepExecutionService
) {
    private val _wizardState = MutableStateFlow<ActionWizardState?>(null)
    val wizardState: StateFlow<ActionWizardState?> = _wizardState.asStateFlow()

    /**
     * Инициализирует визард для выполнения действия
     * @param taskId Идентификатор задания
     * @param actionId Идентификатор действия
     */
    suspend fun initialize(taskId: String, actionId: String): Result<Unit> {
        try {
            Timber.d("Initializing wizard for task: $taskId, action: $actionId")

            // Получение задания и действия
            val task = taskXRepository.getTaskById(taskId)
                ?: return Result.failure(IllegalArgumentException("Task not found: $taskId"))

            val action = task.plannedActions.find { it.id == actionId }
                ?: return Result.failure(IllegalArgumentException("Action not found: $actionId"))

            // Если действие уже выполнено или пропущено, возвращаем ошибку
            if (action.isCompleted || action.isSkipped) {
                return Result.failure(IllegalStateException("Action is already completed or skipped"))
            }

            // Создание шагов для визарда
            val steps = createWizardSteps(action)

            // Создание состояния визарда
            val initialState = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                action = action,
                steps = steps,
                startedAt = LocalDateTime.now(),
                isInitialized = true
            )

            _wizardState.value = initialState
            Timber.d("Wizard initialized with ${steps.size} steps")

            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing wizard")
            return Result.failure(e)
        }
    }

    /**
     * Обрабатывает результат шага визарда
     * @param stepResult Результат выполнения шага
     */
    suspend fun processStepResult(stepResult: Any?) {
        val currentState = _wizardState.value ?: return
        val currentStepIndex = currentState.currentStepIndex
        val currentStep = currentState.currentStep

        if (currentStep == null) {
            Timber.w("No current step to process result")
            return
        }

        // Если результат null, это может означать навигацию назад
        if (stepResult == null) {
            processBackNavigation(currentState)
            return
        }

        try {
            // Выполнение шага
            val action = currentState.action ?: throw IllegalStateException("No action in current state")
            val actionStep = getActionStepForWizardStep(action, currentStep)

            val executionResult = actionStepExecutionService.executeStep(
                taskId = currentState.taskId,
                action = action,
                step = actionStep,
                value = stepResult,
                contextData = currentState.results
            )

            // Обработка результата выполнения шага
            when (executionResult) {
                is StepExecutionResult.Success -> {
                    // Добавляем результат шага в общие результаты
                    val updatedResults = currentState.results.toMutableMap()
                    updatedResults[executionResult.stepId] = executionResult.value

                    // Переходим к следующему шагу
                    _wizardState.value = currentState.copy(
                        currentStepIndex = currentStepIndex + 1,
                        results = updatedResults,
                        errors = emptyMap()
                    )
                }
                is StepExecutionResult.Error -> {
                    // Добавляем ошибку в состояние
                    val updatedErrors = currentState.errors.toMutableMap()
                    updatedErrors[currentStep.id] = executionResult.message

                    _wizardState.value = currentState.copy(
                        errors = updatedErrors
                    )
                }
                is StepExecutionResult.Skipped -> {
                    // Переходим к следующему шагу
                    _wizardState.value = currentState.copy(
                        currentStepIndex = currentStepIndex + 1,
                        errors = emptyMap()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing step result")

            // Добавляем ошибку в состояние
            val updatedErrors = currentState.errors.toMutableMap()
            updatedErrors[currentStep.id] = e.message ?: "Unknown error"

            _wizardState.value = currentState.copy(
                errors = updatedErrors
            )
        }
    }

    /**
     * Обрабатывает навигацию назад
     */
    private fun processBackNavigation(currentState: ActionWizardState) {
        if (currentState.currentStepIndex > 0) {
            // Переходим к предыдущему шагу
            _wizardState.value = currentState.copy(
                currentStepIndex = currentState.currentStepIndex - 1,
                errors = emptyMap()
            )
        } else if (currentState.isCompleted) {
            // Если визард уже завершен, возвращаемся к последнему шагу
            val lastStepIndex = currentState.steps.size - 1
            if (lastStepIndex >= 0) {
                _wizardState.value = currentState.copy(
                    currentStepIndex = lastStepIndex,
                    errors = emptyMap()
                )
            }
        } else {
            // Если мы на первом шаге и не можем вернуться назад,
            // отменяем визард
            cancel()
        }
    }

    /**
     * Завершает визард и выполняет действие
     */
    suspend fun complete(): Result<TaskX> {
        val currentState = _wizardState.value
            ?: return Result.failure(IllegalStateException("Wizard not initialized"))

        if (!currentState.isCompleted) {
            return Result.failure(IllegalStateException("Wizard is not completed"))
        }

        try {
            // Выполнение действия
            val result = actionExecutionService.executeAction(
                taskId = currentState.taskId,
                actionId = currentState.actionId,
                stepResults = currentState.results
            )

            if (result.isSuccess) {
                // Сбрасываем состояние визарда
                _wizardState.value = null
            }

            return result
        } catch (e: Exception) {
            Timber.e(e, "Error completing wizard")
            return Result.failure(e)
        }
    }

    /**
     * Пропускает текущее действие
     */
    suspend fun skip(): Result<TaskX> {
        val currentState = _wizardState.value
            ?: return Result.failure(IllegalStateException("Wizard not initialized"))

        try {
            // Пропуск действия
            val result = actionExecutionService.skipAction(
                taskId = currentState.taskId,
                actionId = currentState.actionId
            )

            if (result.isSuccess) {
                // Сбрасываем состояние визарда
                _wizardState.value = null
            }

            return result
        } catch (e: Exception) {
            Timber.e(e, "Error skipping action")
            return Result.failure(e)
        }
    }

    /**
     * Отменяет визард
     */
    fun cancel() {
        Timber.d("Cancelling wizard")
        _wizardState.value = null
    }

    /**
     * Создает шаги визарда из действия
     */
    private fun createWizardSteps(action: PlannedAction): List<WizardStep> {
        val steps = mutableListOf<WizardStep>()

        // Добавляем шаги хранения
        action.actionTemplate.storageSteps.forEach { actionStep ->
            steps.add(createWizardStep(actionStep, action, true))
        }

        // Добавляем шаги размещения
        action.actionTemplate.placementSteps.forEach { actionStep ->
            steps.add(createWizardStep(actionStep, action, false))
        }

        // Сортируем шаги по порядку
        return steps.sortedBy { step ->
            // Получаем оригинальный ActionStep и его order
            val originalStep = getActionStepForWizardStep(action, step)
            originalStep.order
        }
    }

    /**
     * Создает шаг визарда из шага действия
     */
    private fun createWizardStep(
        actionStep: ActionStep,
        action: PlannedAction,
        isStorage: Boolean
    ): WizardStep {
        return WizardStep(
            id = actionStep.id,
            title = actionStep.name,
            content = { context ->
                // Фактическое содержимое будет определено в UI
                // здесь мы просто создаем шаг-заглушку
            },
            validator = { results ->
                // Проверка валидности результата будет определена в UI
                true
            },
            canNavigateBack = true,
            isAutoComplete = false,
            shouldShow = { true }
        )
    }

    /**
     * Находит шаг действия для шага визарда
     */
    private fun getActionStepForWizardStep(action: PlannedAction, step: WizardStep): ActionStep {
        // Ищем в шагах хранения
        action.actionTemplate.storageSteps.find { it.id == step.id }?.let { return it }

        // Ищем в шагах размещения
        action.actionTemplate.placementSteps.find { it.id == step.id }?.let { return it }

        throw IllegalArgumentException("Step not found: ${step.id}")
    }
}