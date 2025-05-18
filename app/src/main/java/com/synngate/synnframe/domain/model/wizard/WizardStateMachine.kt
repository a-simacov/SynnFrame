package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.service.ActionExecutionService
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

class WizardStateMachine(
    private val taskContextManager: TaskContextManager,
    private val actionExecutionService: ActionExecutionService
) : Disposable {
    private val TAG = "WizardStateMachine"

    private val _state = MutableStateFlow(ActionWizardState())
    val state: StateFlow<ActionWizardState> = _state.asStateFlow()

    // Флаг, указывающий, что инициализация была запущена
    private val initializationStarted = AtomicBoolean(false)

    // Флаг, указывающий, что инициализация завершена успешно
    private val initializationCompleted = AtomicBoolean(false)

    private var _lastSavedState: ActionWizardState? = null

    private var isExplicitlyCancelled = false

    fun initialize(taskId: String, actionId: String): Result<Boolean> {
        Timber.d("$TAG: Начало инициализации визарда, taskId=$taskId, actionId=$actionId")
        isExplicitlyCancelled = false

        // Устанавливаем флаг начала инициализации
        initializationStarted.set(true)

        try {
            val task = taskContextManager.lastStartedTaskX.value
            if (task == null) {
                Timber.e("$TAG: Задача не найдена в контексте")
                return Result.failure(IllegalStateException("Task not found in context"))
            }

            if (task.id != taskId) {
                Timber.e("$TAG: Несоответствие ID задачи: ожидался $taskId, получен ${task.id}")
                return Result.failure(IllegalStateException("Task ID mismatch: expected $taskId, got ${task.id}"))
            }

            val action = task.plannedActions.find { it.id == actionId }
            if (action == null) {
                Timber.e("$TAG: Действие не найдено: $actionId")
                return Result.failure(IllegalStateException("Action not found: $actionId"))
            }

            val taskType = taskContextManager.lastTaskTypeX.value
            if (taskType == null) {
                Timber.e("$TAG: Тип задачи не найден в контексте")
                return Result.failure(IllegalStateException("Task type not found in context"))
            }

            val steps = createStepsFromAction(action)
            if (steps.isEmpty()) {
                Timber.e("$TAG: Для действия не найдено шагов")
                return Result.failure(IllegalStateException("No steps found for action"))
            }

            Timber.d("$TAG: Найдено ${steps.size} шагов для действия")

            val factActionsMap = task.factActions
                .groupBy { it.plannedActionId }
                .mapValues { (_, factActions) -> factActions }

            val initialData = mutableMapOf(
                "taskType" to taskType,
                "factActions" to factActionsMap
            )

            action.storageProduct?.let {
                initialData["actionTaskProduct"] = it
                initialData["actionProduct"] = it.product

                initialData["lastTaskProduct"] = it
                initialData["lastProduct"] = it.product
            }

            val initialState = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                action = action,
                steps = steps,
                currentStepIndex = 0,
                results = initialData,
                startedAt = LocalDateTime.now(),
                isInitialized = true
            )

            saveStateForRecovery(initialState)

            _state.update { initialState }

            // Устанавливаем флаг завершения инициализации
            initializationCompleted.set(true)
            Timber.d("$TAG: Инициализация визарда успешно завершена")

            return Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Ошибка инициализации визарда")

            _state.update {
                ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    isInitialized = false,
                    errors = mapOf("initialization" to (e.message ?: "Unknown error"))
                )
            }

            return Result.failure(e)
        }
    }

    fun processStepResult(result: Any?) {
        if (isExplicitlyCancelled) {
            return
        }

        val currentState = _state.value

        if (!currentState.isInitialized) {
            return
        }

        try {
            if (result == null) {
                navigateBack(currentState)
            } else {
                saveStateForRecovery(currentState)
                saveStepResultAndMoveForward(currentState, result)
            }
        } catch (e: Exception) {
            val recoveredState = recoverFromError(e.message ?: "Unknown error")
            if (recoveredState != null) {
                _state.update { recoveredState }
            } else {
                _state.update { state ->
                    val updatedErrors = state.errors.toMutableMap()
                    updatedErrors[state.currentStep?.id ?: ""] = e.message ?: "Unknown error"
                    state.copy(errors = updatedErrors)
                }
            }
        }
    }

    fun processBarcodeFromScanner(barcode: String) {
        if (isExplicitlyCancelled) {
            return
        }

        if (!_state.value.isInitialized) {
            return
        }

        _state.update { state ->
            state.copy(lastScannedBarcode = barcode)
        }
    }

    /**
     * Отменяет визард
     */
    fun cancel() {
        Timber.d("$TAG: Wizard cancelled explicitly")
        // Устанавливаем флаг явной отмены
        isExplicitlyCancelled = true
        reset()
    }

    suspend fun complete(): Result<TaskX> {
        if (isExplicitlyCancelled) {
            return Result.failure(IllegalStateException("Wizard was cancelled"))
        }

        val currentState = _state.value
        if (!currentState.isInitialized) {
            return Result.failure(IllegalStateException("Wizard not initialized"))
        }

        try {
            saveStateForRecovery(currentState)

            _state.update { it.copy(isSending = true) }

            val enrichedResults = WizardUtils.enrichResultsData(currentState.results)

            val result = actionExecutionService.executeAction(
                taskId = currentState.taskId,
                actionId = currentState.actionId,
                stepResults = enrichedResults,
                completeAction = true
            )

            if (result.isSuccess) {
                _state.update { it.copy(isSending = false, sendError = null) }
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"

                val recoveredState = recoverFromError(errorMessage)
                if (recoveredState != null) {
                    _state.update { recoveredState }
                } else {
                    _state.update { it.copy(isSending = false, sendError = errorMessage) }
                }
            }

            return result
        } catch (e: Exception) {
            val recoveredState = recoverFromError(e.message ?: "Unknown error")
            if (recoveredState != null) {
                _state.update { recoveredState }
            } else {
                _state.update { it.copy(isSending = false, sendError = e.message) }
            }

            return Result.failure(e)
        }
    }

    override fun dispose() {
        reset()
    }

    fun reset() {
        _state.update { ActionWizardState() }
        _lastSavedState = null
    }

    fun isInitialized(): Boolean {
        return _state.value.isInitialized
    }

    private fun saveStateForRecovery(state: ActionWizardState) {
        _lastSavedState = state.copy()
    }

    private fun recoverFromError(errorMessage: String): ActionWizardState? {
        val savedState = _lastSavedState ?: return null

        return savedState.copy(
            sendError = errorMessage,
            isSending = false
        )
    }

    private fun navigateBack(currentState: ActionWizardState) {
        if (currentState.currentStepIndex > 0) {
            _state.update { it.copy(
                currentStepIndex = currentState.currentStepIndex - 1,
                lastScannedBarcode = null
            ) }
        } else if (currentState.isCompleted) {
            _state.update { it.copy(
                currentStepIndex = currentState.steps.size - 1,
                lastScannedBarcode = null
            ) }
        }
    }

    private fun saveStepResultAndMoveForward(currentState: ActionWizardState, result: Any) {
        val currentStep = currentState.currentStep ?: return
        val updatedResults = currentState.results.toMutableMap()

        updatedResults[currentStep.id] = result

        when (result) {
            is TaskProduct -> {
                val existingTaskProduct = updatedResults["lastTaskProduct"] as? TaskProduct

                if (existingTaskProduct == null ||
                    existingTaskProduct.product.id != result.product.id ||
                    existingTaskProduct.quantity != result.quantity) {

                    updatedResults["lastTaskProduct"] = result
                    updatedResults["lastProduct"] = result.product
                }
            }
            is Product -> {
                updatedResults["lastProduct"] = result
            }
            is Pallet -> {
                updatedResults["lastPallet"] = result
            }
            is BinX -> {
                updatedResults["lastBin"] = result
            }
        }

        val nextStepIndex = if (currentState.currentStepIndex < currentState.steps.size - 1) {
            currentState.currentStepIndex + 1
        } else {
            currentState.steps.size
        }

        _state.update { state ->
            state.copy(
                currentStepIndex = nextStepIndex,
                results = updatedResults,
                lastScannedBarcode = null
            )
        }
    }

    private fun createStepsFromAction(action: PlannedAction): List<WizardStep> {
        val steps = mutableListOf<WizardStep>()
        val template = action.actionTemplate

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