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
     * @return Результат инициализации
     */
    suspend fun initialize(taskId: String, actionId: String): Result<Boolean> {
        return try {
            Timber.d("Initializing wizard for task $taskId, action $actionId")
            // Сначала сбрасываем текущее состояние
            _wizardState.value = null

            // Получаем задание и запланированное действие
            val task = taskXRepository.getTaskById(taskId)
                ?: return Result.failure(IllegalArgumentException("Task not found: $taskId"))

            val action = task.plannedActions.find { it.id == actionId }
                ?: return Result.failure(IllegalArgumentException("Action not found: $actionId"))

            // Создаем шаги для визарда на основе шаблона действия
            val steps = createStepsFromAction(action)

            // Создаем начальное состояние
            _wizardState.value = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                action = action,
                steps = steps,
                results = mapOf(),
                startedAt = LocalDateTime.now(),
                isInitialized = true
            )

            Timber.d("Wizard initialized successfully with ${steps.size} steps")
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing wizard")
            Result.failure(e)
        }
    }

    /**
     * Обрабатывает результат выполнения шага
     * @param result Результат выполнения шага или null для шага назад
     */
    suspend fun processStepResult(result: Any?) {
        val state = _wizardState.value ?: return
        val currentStep = state.currentStep

        try {
            // Если результат null, это означает шаг назад
            if (result == null) {
                handleBackStep(state)
                return
            }

            // Обработка результата шага
            if (currentStep != null && !state.isCompleted) {
                // Обновляем результаты
                val updatedResults = state.results.toMutableMap()
                updatedResults[currentStep.id] = result

                // Переходим к следующему шагу
                _wizardState.value = state.copy(
                    currentStepIndex = state.currentStepIndex + 1,
                    results = updatedResults
                )

                Timber.d("Step completed, moving to next step (${state.currentStepIndex + 1})")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing step result")
        }
    }

    /**
     * Обрабатывает шаг назад
     */
    private fun handleBackStep(state: ActionWizardState) {
        if (state.isCompleted) {
            // Возврат из итогового экрана к последнему шагу
            if (state.steps.isNotEmpty()) {
                _wizardState.value = state.copy(
                    currentStepIndex = state.steps.size - 1
                )
                Timber.d("Returning from summary to last step")
            }
        } else if (state.canGoBack) {
            // Возврат к предыдущему шагу
            _wizardState.value = state.copy(
                currentStepIndex = state.currentStepIndex - 1
            )
            Timber.d("Going back to previous step")
        }
    }

    /**
     * Отменяет выполнение визарда
     */
    fun cancel() {
        Timber.d("Cancelling wizard")
        _wizardState.value = null
    }

    /**
     * Завершает выполнение действия и создает фактическое действие
     * @return Результат с обновленным заданием
     */
    suspend fun complete(): Result<TaskX> {
        val state = _wizardState.value
            ?: return Result.failure(IllegalStateException("Wizard is not initialized"))

        if (!state.isCompleted) {
            return Result.failure(IllegalStateException("Wizard is not completed"))
        }

        try {
            Timber.d("Completing action ${state.actionId} for task ${state.taskId}")

            // Выполняем действие через сервис
            val result = actionExecutionService.executeAction(
                state.taskId,
                state.actionId,
                state.results
            )

            // Сбрасываем состояние визарда
            _wizardState.value = null

            return result
        } catch (e: Exception) {
            Timber.e(e, "Error completing action")
            return Result.failure(e)
        }
    }

    /**
     * Создает шаги визарда на основе шаблона действия
     */
    private fun createStepsFromAction(action: PlannedAction): List<WizardStep> {
        val steps = mutableListOf<WizardStep>()
        val template = action.actionTemplate

        // Добавляем шаги для объекта хранения
        template.storageSteps.sortedBy { it.order }.forEach { actionStep ->
            steps.add(createWizardStep(actionStep))
        }

        // Добавляем шаги для объекта размещения
        template.placementSteps.sortedBy { it.order }.forEach { actionStep ->
            steps.add(createWizardStep(actionStep))
        }

        return steps
    }

    /**
     * Создает шаг визарда на основе шага действия
     */
    private fun createWizardStep(actionStep: ActionStep): WizardStep {
        return WizardStep(
            id = actionStep.id,
            title = actionStep.name,
            content = { /* Будет заполнено в UI */ },
            canNavigateBack = true,
            isAutoComplete = false,
            shouldShow = { true }
        )
    }
}