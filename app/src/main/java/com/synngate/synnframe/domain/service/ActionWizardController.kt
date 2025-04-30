package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.model.wizard.WizardStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Контроллер для управления визардом действий
 * Упрощенная версия, работающая только с TaskContextManager
 */
class ActionWizardController(
    private val actionExecutionService: ActionExecutionService,
    private val actionStepExecutionService: ActionStepExecutionService,
    private val taskContextManager: TaskContextManager
) {
    private val _wizardState = MutableStateFlow<ActionWizardState?>(null)
    val wizardState: StateFlow<ActionWizardState?> = _wizardState.asStateFlow()

    suspend fun initialize(taskId: String, actionId: String): Result<Boolean> {
        return try {
            _wizardState.value = null

            // Получаем данные только из TaskContextManager
            val task = taskContextManager.lastStartedTaskX.value
                ?: return Result.failure(IllegalArgumentException("Task not found in context: $taskId"))

            if (task.id != taskId) {
                return Result.failure(IllegalArgumentException("Task ID mismatch: expected $taskId, got ${task.id}"))
            }

            val action = task.plannedActions.find { it.id == actionId }
                ?: return Result.failure(IllegalArgumentException("Planned action not found: $actionId"))

            val steps = createStepsFromAction(action)

            if (steps.isEmpty()) {
                return Result.failure(IllegalStateException("No steps created for this action"))
            }

            // Создаем начальное состояние
            _wizardState.value = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                action = action,
                steps = steps,
                results = mapOf(),
                startedAt = LocalDateTime.now(),
                isInitialized = true,
                lastScannedBarcode = null,
                isProcessingStep = false
            )

            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing wizard: ${e.message}")
            Result.failure(e)
        }
    }

    private fun findActionStepForWizardStep(action: PlannedAction, stepId: String): ActionStep? {
        action.actionTemplate.storageSteps.find { it.id == stepId }?.let {
            return it
        }

        action.actionTemplate.placementSteps.find { it.id == stepId }?.let {
            return it
        }

        return null
    }

    private fun createStepsFromAction(action: PlannedAction): List<WizardStep> {
        val steps = mutableListOf<WizardStep>()
        val template = action.actionTemplate

        if (template.storageSteps.isEmpty() && template.placementSteps.isEmpty()) {
            return emptyList()
        }

        template.storageSteps.sortedBy { it.order }.forEach { actionStep ->
            steps.add(createWizardStep(actionStep))
        }

        template.placementSteps.sortedBy { it.order }.forEach { actionStep ->
            steps.add(createWizardStep(actionStep))
        }

        return steps
    }

    private fun createWizardStep(actionStep: ActionStep): WizardStep {
        return WizardStep(
            id = actionStep.id,
            title = actionStep.name,
            // Пустая функция для содержимого, будет заполнена в UI
            content = { _ -> /* Заполняется в ActionWizardScreen */ },
            canNavigateBack = true,
            isAutoComplete = false,
            shouldShow = { true }
        )
    }

    suspend fun processStepResult(result: Any?) {
        val state = _wizardState.value ?: return

        try {
            if (state.isProcessingStep) {
                return
            }

            _wizardState.value = state.copy(isProcessingStep = true)

            if (result == null) {
                handleBackStep(state)
            } else {
                handleStepResult(state, result)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing step result")
            _wizardState.value = _wizardState.value?.copy(isProcessingStep = false)
        }
    }

    private fun handleStepResult(state: ActionWizardState, result: Any) {
        val currentStep = state.currentStep ?: return
        val stepId = currentStep.id

        val updatedResults = state.results.toMutableMap()
        updatedResults[stepId] = result

        _wizardState.value = state.copy(
            currentStepIndex = state.currentStepIndex + 1,
            results = updatedResults,
            lastScannedBarcode = null,
            isProcessingStep = false
        )
    }

    suspend fun processForwardStep() {
        val state = _wizardState.value ?: return

        if (state.isProcessingStep) {
            return
        }

        _wizardState.value = state.copy(isProcessingStep = true)

        try {
            handleForwardStep(state)
        } catch (e: Exception) {
            Timber.e(e, "Error processing forward step")
        } finally {
            _wizardState.value = _wizardState.value?.copy(isProcessingStep = false)
        }
    }

    private fun handleForwardStep(state: ActionWizardState) {
        val currentStep = state.currentStep
        if (!state.isCompleted && currentStep != null && state.results.containsKey(currentStep.id)) {
            _wizardState.value = state.copy(
                currentStepIndex = state.currentStepIndex + 1,
                lastScannedBarcode = null,
                isProcessingStep = false
            )
        } else {
            _wizardState.value = state.copy(isProcessingStep = false)
        }
    }

    private fun handleBackStep(state: ActionWizardState) {
        if (state.isCompleted) {
            if (state.steps.isNotEmpty()) {
                _wizardState.value = state.copy(
                    currentStepIndex = state.steps.size - 1,
                    lastScannedBarcode = null,
                    isProcessingStep = false
                )
            }
        } else if (state.canGoBack && state.currentStepIndex > 0) {
            _wizardState.value = state.copy(
                currentStepIndex = state.currentStepIndex - 1,
                lastScannedBarcode = null,
                isProcessingStep = false
            )
        } else {
            Timber.d("Cannot go back from first step or step doesn't allow back navigation")
        }
    }

    suspend fun processBarcodeFromScanner(barcode: String) {
        val currentState = _wizardState.value ?: return

        try {
            if (barcode == currentState.lastScannedBarcode || currentState.isProcessingStep) {
                Timber.d("Игнорирование повторного штрихкода или визард в процессе обработки")
                return
            }

            _wizardState.value = currentState.copy(
                lastScannedBarcode = barcode,
                isProcessingStep = true
            )

            // Никакой логики взаимодействия с репозиторием здесь нет,
            // просто обновляем состояние штрихкода для обработки в UI

            _wizardState.value = _wizardState.value?.copy(isProcessingStep = false)
        } catch (e: Exception) {
            Timber.e(e, "Error processing barcode: ${e.message}")
            _wizardState.value = _wizardState.value?.copy(isProcessingStep = false)
        }
    }

    fun cancel() {
        _wizardState.value = null
    }

    suspend fun complete(): Result<TaskX> {
        val state = _wizardState.value
            ?: return Result.failure(IllegalStateException("Wizard is not initialized"))

        if (!state.isCompleted) {
            return Result.failure(IllegalStateException("Wizard is not completed"))
        }

        try {
            val result = actionExecutionService.executeAction(
                state.taskId,
                state.actionId,
                state.results
            )

            _wizardState.value = null

            return result
        } catch (e: Exception) {
            Timber.e(e, "Error completing action")
            return Result.failure(e)
        }
    }
}