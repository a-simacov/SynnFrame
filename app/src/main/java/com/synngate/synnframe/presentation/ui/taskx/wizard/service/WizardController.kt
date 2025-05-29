package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.StateTransitionResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardEvent
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardStateMachine
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

// Контроллер для управления жизненным циклом визарда
class WizardController(
    private val stateMachine: WizardStateMachine = WizardStateMachine()
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

            val updatedState = stateMachine.transition(initialState, WizardEvent.LoadSuccess)
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

        val newState = stateMachine.transition(state, WizardEvent.NextStep)
        return StateTransitionResult.success(newState)
    }

    fun previousStep(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        val newState = stateMachine.transition(state, WizardEvent.PreviousStep)
        return StateTransitionResult.success(newState)
    }

    fun setObjectForCurrentStep(state: ActionWizardState, obj: Any): StateTransitionResult<ActionWizardState> {
        val currentStep = state.getCurrentStep() ?: return StateTransitionResult.error(state, "Текущий шаг не найден")

        var updatedState = state
        if (state.error != null) {
            updatedState = stateMachine.transition(state, WizardEvent.ClearError)
        }

        val newState = stateMachine.transition(updatedState, WizardEvent.SetObject(obj, currentStep.id))
        return StateTransitionResult.success(newState)
    }

    suspend fun tryAutoAdvance(state: ActionWizardState, validateStep: suspend () -> Boolean): StateTransitionResult<ActionWizardState> {
        if (state.error != null) {
            return StateTransitionResult.error(state, "Автопереход отменен: есть ошибка в состоянии")
        }

        val currentStep = state.getCurrentStep() ?: return StateTransitionResult.error(state, "Текущий шаг не найден")

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

        if (!validateStep()) {
            Timber.d("Автопереход отменен: ошибка валидации")
            return StateTransitionResult.error(state, "Автопереход отменен: ошибка валидации")
        }

        val newState = stateMachine.transition(state, WizardEvent.NextStep)

        val success = newState != state
        Timber.d("Результат автоперехода: success=$success")

        return if (success) {
            StateTransitionResult.success(newState)
        } else {
            StateTransitionResult.error(state, "Автопереход не выполнен: состояние не изменилось")
        }
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
        val newState = stateMachine.transition(state, WizardEvent.SendFailure(error))
        return StateTransitionResult.error(newState, error)
    }
}