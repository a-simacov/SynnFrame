package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.enums.BufferUsage
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.NetworkResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.StateTransitionResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardEvent
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardStateMachine
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

// Контроллер для управления жизненным циклом визарда
class WizardController(
    private val stateMachine: WizardStateMachine = WizardStateMachine(),
    private val expressionEvaluator: ExpressionEvaluator = ExpressionEvaluator()
) {

    fun initializeWizard(taskId: String, actionId: String): StateTransitionResult<ActionWizardState> {
        try {
            val task = TaskXDataHolderSingleton.currentTask.value
            if (task == null) {
                val errorState = ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    error = "Task not found"
                )
                return StateTransitionResult.error(errorState, "Task not found")
            }

            val plannedAction = task.plannedActions.find { it.id == actionId }
            if (plannedAction == null) {
                val errorState = ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    error = "Action not found"
                )
                return StateTransitionResult.error(errorState, "Action not found")
            }

            val actionTemplate = plannedAction.actionTemplate
            if (actionTemplate == null) {
                val errorState = ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    error = "Action template not found"
                )
                return StateTransitionResult.error(errorState, "Action template not found")
            }

            val sortedSteps = actionTemplate.actionSteps.sortedBy { it.order }

            val hasQuantityStep = sortedSteps.any { it.factActionField == FactActionField.QUANTITY }

            val newFactAction = FactAction(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                plannedActionId = plannedAction.id,
                actionTemplateId = actionTemplate.id,
                wmsAction = actionTemplate.wmsAction,
                quantity = if (!hasQuantityStep && plannedAction.quantity > 0) plannedAction.quantity else 0f,
                startedAt = LocalDateTime.now(),
                completedAt = LocalDateTime.now()
            )

            Timber.d("Wizard successfully initialized for task ${task.id}, action $actionId")

            val initialState = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                plannedAction = plannedAction,
                steps = sortedSteps,
                factAction = newFactAction
            )

            // Находим первый видимый шаг
            val firstVisibleStepIndex = expressionEvaluator.findFirstVisibleStepIndex(initialState)
            val stateWithVisibleStep = if (firstVisibleStepIndex != null) {
                initialState.copy(currentStepIndex = firstVisibleStepIndex)
            } else {
                // Если нет видимых шагов, сразу переходим к итоговому экрану
                initialState.copy(showSummary = true)
            }

            val updatedState = stateMachine.transition(stateWithVisibleStep, WizardEvent.LoadSuccess)
            return StateTransitionResult.success(updatedState)

        } catch (e: Exception) {
            Timber.e(e, "Error initializing wizard: ${e.message}")

            val initialState = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                error = "Error: ${e.message}"
            )

            val errorState = stateMachine.transition(initialState, WizardEvent.LoadFailure(e.message ?: "Unknown error"))
            return StateTransitionResult.error(errorState, "Error initializing wizard: ${e.message}")
        }
    }

    suspend fun confirmCurrentStep(state: ActionWizardState, validateStep: suspend () -> Boolean): StateTransitionResult<ActionWizardState> {
        val currentStepIndex = state.currentStepIndex
        val steps = state.steps

        if (currentStepIndex >= steps.size) {
            return StateTransitionResult.error(state, "Step index out of range")
        }

        if (!validateStep()) {
            return StateTransitionResult.error(state, "Step validation failed")
        }

        // Перед переходом сохраняем объект в буфер, если нужно
        val stateAfterBuffer = saveToBufferIfNeeded(state).getNewState()

        // Используем nextVisibleStep вместо обычного перехода
        return nextVisibleStep(stateAfterBuffer)
    }

    /**
     * Переходит к следующему видимому шагу с учетом условий видимости
     */
    fun nextVisibleStep(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val nextIndex = expressionEvaluator.findNextVisibleStepIndex(state)

        // Если нет видимых шагов впереди, переходим к экрану итогов
        if (nextIndex == null) {
            val stateWithSummary = state.copy(showSummary = true)
            return StateTransitionResult.success(stateWithSummary)
        }

        // Переходим к найденному видимому шагу
        return StateTransitionResult.success(state.copy(currentStepIndex = nextIndex))
    }

    fun previousStep(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        return previousVisibleStep(state)
    }

    /**
     * Переходит к предыдущему видимому шагу с учетом условий видимости
     */
    fun previousVisibleStep(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val prevIndex = expressionEvaluator.findPreviousVisibleStepIndex(state)

        // Если нет видимых шагов позади, показываем диалог выхода
        if (prevIndex == null) {
            val newState = stateMachine.transition(state, WizardEvent.ShowExitDialog)
            return StateTransitionResult.success(newState)
        }

        // Переходим к найденному видимому шагу
        return StateTransitionResult.success(state.copy(currentStepIndex = prevIndex))
    }

    /**
     * Проверяет, виден ли текущий шаг
     */
    fun isCurrentStepVisible(state: ActionWizardState): Boolean {
        val currentStep = state.getCurrentStep() ?: return false
        return expressionEvaluator.evaluateVisibilityCondition(currentStep.visibilityCondition, state)
    }

    fun setObjectForCurrentStep(state: ActionWizardState, obj: Any): StateTransitionResult<ActionWizardState> {
        val currentStep = state.getCurrentStep() ?: return StateTransitionResult.error(state, "Current step not found")

        // Если шаг заблокирован буфером (режим ALWAYS), не позволяем изменять значение
        if (state.lockedObjectSteps.contains(currentStep.id)) {
            return StateTransitionResult.error(state, "Step is locked by buffer (ALWAYS mode)")
        }

        var updatedState = state
        if (state.error != null) {
            updatedState = stateMachine.transition(state, WizardEvent.ClearError)
        }

        val newState = stateMachine.transition(updatedState, WizardEvent.SetObject(obj, currentStep.id))
        return StateTransitionResult.success(newState)
    }

    /**
     * Обрабатывает результат запроса объекта с сервера
     *
     * @param state Текущее состояние визарда
     * @param networkResult Результат сетевого запроса
     * @return Результат перехода состояния с новым объектом или ошибкой
     */
    suspend fun handleServerObjectRequest(
        state: ActionWizardState,
        networkResult: NetworkResult<Any>
    ): StateTransitionResult<ActionWizardState> {
        // Сбрасываем флаг запроса
        val stateWithoutLoading = state.copy(
            isRequestingServerObject = false,
            serverRequestCancellationToken = null
        )

        if (!networkResult.isSuccess()) {
            val errorMessage = networkResult.getErrorMessage() ?: "Unknown error"
            val stateWithError = stateWithoutLoading.copy(error = errorMessage)
            return StateTransitionResult.error(stateWithError, errorMessage)
        }

        val serverObject = networkResult.getResponseData() ?: return StateTransitionResult.error(
            stateWithoutLoading,
            "No data in server response"
        )

        // Устанавливаем объект в текущий шаг
        val currentStep = state.getCurrentStep() ?: return StateTransitionResult.error(
            stateWithoutLoading,
            "Current step not found"
        )

        // Используем существующий метод для установки объекта
        val stateWithObject = setObjectForCurrentStep(stateWithoutLoading, serverObject).getNewState()

        // Если autoAdvance включен, пробуем перейти к следующему шагу
        if (currentStep.autoAdvance) {
            return tryAutoAdvance(stateWithObject) {
                // Объекты с сервера не требуют дополнительной валидации
                true
            }
        }

        return StateTransitionResult.success(stateWithObject)
    }

    suspend fun tryAutoAdvance(state: ActionWizardState, validateStep: suspend () -> Boolean): StateTransitionResult<ActionWizardState> {
        if (state.error != null) {
            return StateTransitionResult.error(state, "Auto-advance canceled: there is an error in the state")
        }

        val currentStep = state.getCurrentStep() ?:
        return StateTransitionResult.error(state, "Current step not found")

        // Проверяем настройку автоперехода для текущего шага
        if (!currentStep.autoAdvance) {
            Timber.d("Auto-advance disabled in step settings ${currentStep.id}: ${currentStep.name}")
            return StateTransitionResult.error(state, "Auto-advance disabled in settings")
        }

        // Далее идет существующая логика с учетом особенностей различных типов полей...

        if (currentStep.factActionField == FactActionField.STORAGE_PRODUCT) {
            val needsAdditionalProps = state.shouldShowAdditionalProps(currentStep)

            if (needsAdditionalProps) {
                val selectedObj = state.selectedObjects[currentStep.id]
                if (selectedObj == null) {
                    Timber.d("Auto-advance canceled: no object selected for product step")
                    return StateTransitionResult.error(state, "Auto-advance canceled: no object selected for product step")
                }

                val taskProduct = selectedObj as? TaskProduct
                if (taskProduct != null) {
                    val needsExpDate = state.shouldShowExpirationDate()

                    if (needsExpDate && taskProduct.expirationDate == null) {
                        Timber.d("Auto-advance canceled: product requires expiration date")
                        return StateTransitionResult.error(state, "Auto-advance canceled: product requires expiration date")
                    }

                    Timber.d("Auto-advance canceled: auto-advance is forbidden for products with additional properties")
                    return StateTransitionResult.error(state, "Auto-advance canceled: auto-advance is forbidden for products with additional properties")
                }
            }
        }

        // Валидация шага
        if (!validateStep()) {
            Timber.d("Auto-advance canceled: validation error")
            return StateTransitionResult.error(state, "Auto-advance canceled: validation error")
        }

        // Сохраняем в буфер и переходим к следующему видимому шагу
        val stateAfterBuffer = saveToBufferIfNeeded(state).getNewState()
        return nextVisibleStep(stateAfterBuffer)
    }

    /**
     * Метод для проверки и применения значения из буфера для текущего шага
     */
    fun applyBufferValueIfNeeded(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val currentStep = state.getCurrentStep() ?:
        return StateTransitionResult.success(state)

        // Проверяем, можно ли использовать буфер для текущего шага
        val bufferUsage = currentStep.bufferUsage
        if (bufferUsage == BufferUsage.NEVER) {
            Timber.d("Buffer not used for step ${currentStep.id} (NEVER mode)")
            return StateTransitionResult.success(state)
        }

        // Если уже выбран объект для этого шага, не применяем буфер
        if (state.selectedObjects.containsKey(currentStep.id)) {
            Timber.d("Object already selected for step ${currentStep.id}, buffer not applied")
            return StateTransitionResult.success(state)
        }

        // Получаем объект из буфера
        val taskBuffer = TaskXDataHolderSingleton.taskBuffer
        val bufferValue = taskBuffer.getObjectForField(currentStep.factActionField)

        if (bufferValue == null) {
            Timber.d("Object not found in buffer for field ${currentStep.factActionField}")
            return StateTransitionResult.success(state)
        }

        val (obj, source) = bufferValue
        val isBufferObjectLocked = bufferUsage == BufferUsage.ALWAYS

        Timber.d("Applying object from buffer for step ${currentStep.id}: $obj, source: $source, locked: $isBufferObjectLocked")

        // Применяем значение из буфера
        val updatedState = stateMachine.transition(
            state,
            WizardEvent.SetObjectFromBuffer(obj, currentStep.id, source, isBufferObjectLocked)
        )

        return StateTransitionResult.success(updatedState)
    }

    /**
     * Метод для автоматического перехода при применении объекта из буфера
     */
    fun tryAutoAdvanceFromBuffer(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val currentStep = state.getCurrentStep()
        if (currentStep == null || !state.isCurrentStepLockedByBuffer()) {
            return StateTransitionResult.success(state)
        }

        Timber.d("Auto-advance from buffer for step ${currentStep.id}")

        // Для заблокированных полей (режим ALWAYS) автоматически переходим к следующему шагу
        val updatedState = stateMachine.transition(state, WizardEvent.AutoAdvanceFromBuffer)
        return StateTransitionResult.success(updatedState)
    }

    /**
     * Метод для сохранения объекта в буфер, если это необходимо
     */
    fun saveToBufferIfNeeded(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val currentStep = state.getCurrentStep() ?:
        return StateTransitionResult.success(state)

        // Если шаг требует сохранения в буфер
        if (currentStep.saveToTaskBuffer) {
            val selectedObject = state.selectedObjects[currentStep.id] ?:
            return StateTransitionResult.success(state)

            val taskBuffer = TaskXDataHolderSingleton.taskBuffer

            Timber.d("Saving object ${selectedObject.javaClass.simpleName} to buffer from step ${currentStep.name}")

            // Сохраняем объект в буфер с указанием источника "wizard"
            when (currentStep.factActionField) {
                FactActionField.STORAGE_BIN ->
                    taskBuffer.addBin(selectedObject as BinX, true, "wizard")
                FactActionField.ALLOCATION_BIN ->
                    taskBuffer.addBin(selectedObject as BinX, false, "wizard")
                FactActionField.STORAGE_PALLET ->
                    taskBuffer.addPallet(selectedObject as Pallet, true, "wizard")
                FactActionField.ALLOCATION_PALLET ->
                    taskBuffer.addPallet(selectedObject as Pallet, false, "wizard")
                FactActionField.STORAGE_PRODUCT_CLASSIFIER ->
                    taskBuffer.addProduct(selectedObject as Product, "wizard")
                FactActionField.STORAGE_PRODUCT ->
                    taskBuffer.addTaskProduct(selectedObject as TaskProduct, "wizard")
                else -> {} // Другие типы не сохраняем
            }
        }

        // Если для шага установлен режим CLEAR, очищаем соответствующее поле в буфере
        if (currentStep.bufferUsage == BufferUsage.CLEAR) {
            Timber.d("Clearing field ${currentStep.factActionField} in buffer (CLEAR mode)")
            TaskXDataHolderSingleton.taskBuffer.clearField(currentStep.factActionField)
        }

        return StateTransitionResult.success(state)
    }

    fun showExitDialog(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val newState = stateMachine.transition(state, WizardEvent.ShowExitDialog)
        return StateTransitionResult.success(newState)
    }

    fun dismissExitDialog(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val newState = stateMachine.transition(state, WizardEvent.DismissExitDialog)
        return StateTransitionResult.success(newState)
    }

    fun clearError(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val newState = stateMachine.transition(state, WizardEvent.ClearError)
        return StateTransitionResult.success(newState)
    }

    fun setLoading(state: ActionWizardState, isLoading: Boolean): StateTransitionResult<ActionWizardState> {
        val newState = if (isLoading) {
            stateMachine.transition(state, WizardEvent.StartLoading)
        } else {
            stateMachine.transition(state, WizardEvent.StopLoading)
        }
        return StateTransitionResult.success(newState)
    }

    fun setError(state: ActionWizardState, message: String?): StateTransitionResult<ActionWizardState> {
        val newState = if (message != null) {
            stateMachine.transition(state, WizardEvent.SetError(message))
        } else {
            stateMachine.transition(state, WizardEvent.ClearError)
        }
        return StateTransitionResult.success(newState)
    }

    fun submitForm(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val newState = stateMachine.transition(state, WizardEvent.SubmitForm)
        return StateTransitionResult.success(newState)
    }

    fun handleSendSuccess(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val newState = stateMachine.transition(state, WizardEvent.SendSuccess)
        return StateTransitionResult.success(newState)
    }

    fun handleSendFailure(state: ActionWizardState, error: String): StateTransitionResult<ActionWizardState> {
        // Сначала явно сбрасываем флаг загрузки
        val stateWithoutLoading = state.copy(isLoading = false)

        // Затем обрабатываем событие ошибки
        val newState = stateMachine.transition(stateWithoutLoading, WizardEvent.SendFailure(error))

        // Дополнительная проверка, что isLoading точно сброшен
        val finalState = if (newState.isLoading) {
            Timber.w("isLoading is still true after SendFailure, forcefully resetting")
            newState.copy(isLoading = false, sendingFailed = true)
        } else {
            newState
        }

        return StateTransitionResult.error(finalState, error)
    }
}