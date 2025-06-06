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
                    error = "Задание не найдено"
                )
                return StateTransitionResult.error(errorState, "Задание не найдено")
            }

            val plannedAction = task.plannedActions.find { it.id == actionId }
            if (plannedAction == null) {
                val errorState = ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    error = "Действие не найдено"
                )
                return StateTransitionResult.error(errorState, "Действие не найдено")
            }

            val actionTemplate = plannedAction.actionTemplate
            if (actionTemplate == null) {
                val errorState = ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    error = "Шаблон действия не найден"
                )
                return StateTransitionResult.error(errorState, "Шаблон действия не найден")
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

            Timber.d("Визард успешно инициализирован для задания ${task.id}, действие $actionId")

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
            Timber.e(e, "Ошибка инициализации визарда: ${e.message}")

            val initialState = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                error = "Ошибка: ${e.message}"
            )

            val errorState = stateMachine.transition(initialState, WizardEvent.LoadFailure(e.message ?: "Неизвестная ошибка"))
            return StateTransitionResult.error(errorState, "Ошибка инициализации визарда: ${e.message}")
        }
    }

    suspend fun confirmCurrentStep(state: ActionWizardState, validateStep: suspend () -> Boolean): StateTransitionResult<ActionWizardState> {
        val currentStepIndex = state.currentStepIndex
        val steps = state.steps

        if (currentStepIndex >= steps.size) {
            return StateTransitionResult.error(state, "Индекс шага вне диапазона")
        }

        if (!validateStep()) {
            return StateTransitionResult.error(state, "Валидация шага не пройдена")
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
        val currentStep = state.getCurrentStep() ?: return StateTransitionResult.error(state, "Текущий шаг не найден")

        // Если шаг заблокирован буфером (режим ALWAYS), не позволяем изменять значение
        if (state.lockedObjectSteps.contains(currentStep.id)) {
            return StateTransitionResult.error(state, "Шаг заблокирован буфером (режим ALWAYS)")
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
            val errorMessage = networkResult.getErrorMessage() ?: "Неизвестная ошибка"
            val stateWithError = stateWithoutLoading.copy(error = errorMessage)
            return StateTransitionResult.error(stateWithError, errorMessage)
        }

        val serverObject = networkResult.getResponseData() ?: return StateTransitionResult.error(
            stateWithoutLoading,
            "Нет данных в ответе сервера"
        )

        // Устанавливаем объект в текущий шаг
        val currentStep = state.getCurrentStep() ?: return StateTransitionResult.error(
            stateWithoutLoading,
            "Текущий шаг не найден"
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
            return StateTransitionResult.error(state, "Автопереход отменен: есть ошибка в состоянии")
        }

        val currentStep = state.getCurrentStep() ?:
        return StateTransitionResult.error(state, "Текущий шаг не найден")

        // Проверяем настройку автоперехода для текущего шага
        if (!currentStep.autoAdvance) {
            Timber.d("Автопереход отключен в настройках шага ${currentStep.id}: ${currentStep.name}")
            return StateTransitionResult.error(state, "Автопереход отключен в настройках")
        }

        // Далее идет существующая логика с учетом особенностей различных типов полей...

        if (currentStep.factActionField == FactActionField.STORAGE_PRODUCT) {
            val needsAdditionalProps = state.shouldShowAdditionalProps(currentStep)

            if (needsAdditionalProps) {
                val selectedObj = state.selectedObjects[currentStep.id]
                if (selectedObj == null) {
                    Timber.d("Автопереход отменен: не выбран объект для шага товара")
                    return StateTransitionResult.error(state, "Автопереход отменен: не выбран объект для шага товара")
                }

                val taskProduct = selectedObj as? TaskProduct
                if (taskProduct != null) {
                    val needsExpDate = state.shouldShowExpirationDate()

                    if (needsExpDate && taskProduct.expirationDate == null) {
                        Timber.d("Автопереход отменен: товар требует заполнения срока годности")
                        return StateTransitionResult.error(state, "Автопереход отменен: товар требует заполнения срока годности")
                    }

                    Timber.d("Автопереход отменен: для товаров с дополнительными свойствами автопереход запрещен")
                    return StateTransitionResult.error(state, "Автопереход отменен: для товаров с дополнительными свойствами автопереход запрещен")
                }
            }
        }

        // Валидация шага
        if (!validateStep()) {
            Timber.d("Автопереход отменен: ошибка валидации")
            return StateTransitionResult.error(state, "Автопереход отменен: ошибка валидации")
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
            Timber.d("Буфер не используется для шага ${currentStep.id} (режим NEVER)")
            return StateTransitionResult.success(state)
        }

        // Если уже выбран объект для этого шага, не применяем буфер
        if (state.selectedObjects.containsKey(currentStep.id)) {
            Timber.d("Объект уже выбран для шага ${currentStep.id}, буфер не применяется")
            return StateTransitionResult.success(state)
        }

        // Получаем объект из буфера
        val taskBuffer = TaskXDataHolderSingleton.taskBuffer
        val bufferValue = taskBuffer.getObjectForField(currentStep.factActionField)

        if (bufferValue == null) {
            Timber.d("Объект не найден в буфере для поля ${currentStep.factActionField}")
            return StateTransitionResult.success(state)
        }

        val (obj, source) = bufferValue
        val isBufferObjectLocked = bufferUsage == BufferUsage.ALWAYS

        Timber.d("Применяем объект из буфера для шага ${currentStep.id}: $obj, source: $source, locked: $isBufferObjectLocked")

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

        Timber.d("Автопереход из буфера для шага ${currentStep.id}")

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

            Timber.d("Сохранение объекта ${selectedObject.javaClass.simpleName} в буфер из шага ${currentStep.name}")

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
            Timber.d("Очистка поля ${currentStep.factActionField} в буфере (режим CLEAR)")
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
            Timber.w("isLoading все еще true после SendFailure, принудительно сбрасываем")
            newState.copy(isLoading = false, sendingFailed = true)
        } else {
            newState
        }

        return StateTransitionResult.error(finalState, error)
    }
}