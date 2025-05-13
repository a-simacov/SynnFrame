package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.service.ActionExecutionService
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.presentation.di.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Улучшенная машина состояний для управления визардом действий.
 * Предоставляет единый интерфейс для работы с визардом без необходимости адаптеров.
 * Все методы безопасны в отношении потоков и исключений.
 */
class WizardStateMachine(
    private val taskContextManager: TaskContextManager,
    private val actionExecutionService: ActionExecutionService
) : Disposable {

    // Состояние визарда (доступное для UI)
    private val _state = MutableStateFlow<ActionWizardState?>(null)
    val state: StateFlow<ActionWizardState?> = _state.asStateFlow()

    /**
     * Инициализирует визард для указанного задания и действия
     * @return результат инициализации
     */
    suspend fun initialize(taskId: String, actionId: String): Result<Boolean> {
        Timber.d("Initializing wizard for task $taskId, action $actionId")

        try {
            // Получаем задание из контекста
            val task = taskContextManager.lastStartedTaskX.value
                ?: return Result.failure(IllegalStateException("Task not found in context"))

            if (task.id != taskId) {
                return Result.failure(IllegalStateException("Task ID mismatch: expected $taskId, got ${task.id}"))
            }

            // Находим действие в задании
            val action = task.plannedActions.find { it.id == actionId }
                ?: return Result.failure(IllegalStateException("Action not found: $actionId"))

            // Получаем тип задания
            val taskType = taskContextManager.lastTaskTypeX.value
                ?: return Result.failure(IllegalStateException("Task type not found in context"))

            // Создаем шаги из шаблона действия
            val steps = createStepsFromAction(action)
            if (steps.isEmpty()) {
                return Result.failure(IllegalStateException("No steps found for action"))
            }

            // Создаем базовое состояние визарда
            val initialState = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                action = action,
                steps = steps,
                currentStepIndex = 0,
                results = mapOf("taskType" to taskType),
                startedAt = LocalDateTime.now(),
                isInitialized = true
            )

            // Устанавливаем начальное состояние
            _state.value = initialState

            return Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing wizard")
            return Result.failure(e)
        }
    }

    /**
     * Обрабатывает результат шага
     * @param result результат шага или null для навигации назад
     */
    suspend fun processStepResult(result: Any?) {
        val currentState = _state.value ?: return

        try {
            if (result == null) {
                // Навигация назад
                navigateBack(currentState)
            } else {
                // Сохраняем результат и переходим к следующему шагу
                saveStepResultAndMoveForward(currentState, result)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing step result")
            // Обновляем состояние с ошибкой
            _state.update { currentState ->
                currentState?.let {
                    val updatedErrors = it.errors.toMutableMap()
                    updatedErrors[it.currentStep?.id ?: ""] = e.message ?: "Unknown error"
                    it.copy(errors = updatedErrors)
                }
            }
        }
    }

    /**
     * Обрабатывает штрих-код от сканера
     */
    fun processBarcodeFromScanner(barcode: String) {
        _state.update { it?.copy(lastScannedBarcode = barcode) }
    }

    /**
     * Отменяет визард
     */
    fun cancel() {
        reset()
    }

    /**
     * Завершает визард с выполнением действия
     */
    suspend fun complete(): Result<TaskX> {
        try {
            val currentState = _state.value ?:
            return Result.failure(IllegalStateException("Wizard not initialized"))

            // Устанавливаем флаг отправки
            _state.update { it?.copy(isSending = true) }

            // Выполняем действие через ActionExecutionService
            val result = actionExecutionService.executeAction(
                taskId = currentState.taskId,
                actionId = currentState.actionId,
                stepResults = currentState.results,
                completeAction = true
            )

            // Обновляем состояние в зависимости от результата
            if (result.isSuccess) {
                _state.update { it?.copy(isSending = false, sendError = null) }
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                _state.update { it?.copy(isSending = false, sendError = errorMessage) }
            }

            return result
        } catch (e: Exception) {
            Timber.e(e, "Error completing wizard")
            _state.update { it?.copy(isSending = false, sendError = e.message) }
            return Result.failure(e)
        }
    }

    /**
     * Сбрасывает машину состояний
     */
    override fun dispose() {
        reset()
    }

    /**
     * Сбрасывает состояние машины
     */
    private fun reset() {
        _state.value = null
    }

    /**
     * Навигация назад
     */
    private fun navigateBack(currentState: ActionWizardState) {
        if (currentState.currentStepIndex > 0) {
            // Если мы не на первом шаге, переходим к предыдущему
            _state.update { it?.copy(
                currentStepIndex = currentState.currentStepIndex - 1,
                lastScannedBarcode = null
            ) }
        } else if (currentState.isCompleted) {
            // Если мы на экране завершения, возвращаемся к последнему шагу
            _state.update { it?.copy(
                currentStepIndex = currentState.steps.size - 1,
                lastScannedBarcode = null
            ) }
        }
    }

    /**
     * Сохраняет результат шага и переходит к следующему
     */
    private fun saveStepResultAndMoveForward(currentState: ActionWizardState, result: Any) {
        val currentStep = currentState.currentStep ?: return
        val updatedResults = currentState.results.toMutableMap()

        // Сохраняем результат текущего шага
        updatedResults[currentStep.id] = result

        // Также сохраняем отдельные индексы для определенных типов данных
        // для упрощения доступа в следующих шагах
        when (result) {
            is TaskProduct -> {
                updatedResults["lastTaskProduct"] = result
                updatedResults["lastProduct"] = result.product
            }
            is Product -> {
                updatedResults["lastProduct"] = result
            }
        }

        // Определяем следующий шаг
        val nextStepIndex = if (currentState.currentStepIndex < currentState.steps.size - 1) {
            currentState.currentStepIndex + 1
        } else {
            currentState.steps.size // Указывает на завершение визарда
        }

        // Обновляем состояние
        _state.update { it?.copy(
            currentStepIndex = nextStepIndex,
            results = updatedResults,
            lastScannedBarcode = null
        ) }
    }

    /**
     * Создает шаги из шаблона действия
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
}