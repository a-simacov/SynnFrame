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

/**
 * Контроллер для управления жизненным циклом визарда
 */
class WizardController(
    private val validator: WizardValidator,
    private val stateMachine: WizardStateMachine = WizardStateMachine()
) {
    /**
     * Инициализирует визард для указанного задания и действия
     */
    fun initializeWizard(taskId: String, actionId: String): StateTransitionResult<ActionWizardState> {
        try {
            val task = TaskXDataHolderSingleton.currentTask.value
            if (task == null) {
                Timber.e("Задание не найдено в TaskXDataHolderSingleton")
                val errorState = ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    error = "Задание не найдено"
                )
                return StateTransitionResult.error(errorState, "Задание не найдено")
            }

            val plannedAction = task.plannedActions.find { it.id == actionId }
            if (plannedAction == null) {
                Timber.e("Действие $actionId не найдено в задании ${task.id}")
                val errorState = ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    error = "Действие не найдено"
                )
                return StateTransitionResult.error(errorState, "Действие не найдено")
            }

            val actionTemplate = plannedAction.actionTemplate
            if (actionTemplate == null) {
                Timber.e("Шаблон действия не найден для действия $actionId")
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

            // Используем машину состояний для перехода к состоянию загрузки завершена
            val updatedState = stateMachine.transition(initialState, WizardEvent.LoadSuccess)
            return StateTransitionResult.success(updatedState)

        } catch (e: Exception) {
            Timber.e(e, "Ошибка инициализации визарда: ${e.message}")

            val initialState = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                error = "Ошибка: ${e.message}"
            )

            // Используем машину состояний для перехода к состоянию ошибки загрузки
            val errorState = stateMachine.transition(initialState, WizardEvent.LoadFailure(e.message ?: "Неизвестная ошибка"))
            return StateTransitionResult.error(errorState, "Ошибка инициализации визарда: ${e.message}")
        }
    }

    /**
     * Обрабатывает нажатие на кнопку "Далее" для текущего шага
     */
    suspend fun confirmCurrentStep(state: ActionWizardState, validateStep: suspend () -> Boolean): StateTransitionResult<ActionWizardState> {
        val currentStepIndex = state.currentStepIndex
        val steps = state.steps

        if (currentStepIndex >= steps.size) {
            return StateTransitionResult.error(state, "Индекс шага вне диапазона")
        }

        if (!validateStep()) {
            return StateTransitionResult.error(state, "Валидация шага не пройдена")
        }

        // Используем машину состояний для перехода к следующему шагу
        val newState = stateMachine.transition(state, WizardEvent.NextStep)
        return StateTransitionResult.success(newState)
    }

    /**
     * Обрабатывает нажатие на кнопку "Назад"
     */
    fun previousStep(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        // Используем машину состояний для перехода к предыдущему шагу
        val newState = stateMachine.transition(state, WizardEvent.PreviousStep)
        return StateTransitionResult.success(newState)
    }

    /**
     * Устанавливает объект для текущего шага и обновляет состояние
     */
    fun setObjectForCurrentStep(state: ActionWizardState, obj: Any): StateTransitionResult<ActionWizardState> {
        val currentStep = state.getCurrentStep() ?: return StateTransitionResult.error(state, "Текущий шаг не найден")

        // Сначала очищаем ошибку, если она есть
        var updatedState = state
        if (state.error != null) {
            updatedState = stateMachine.transition(state, WizardEvent.ClearError)
        }

        // Затем используем машину состояний для установки объекта
        val newState = stateMachine.transition(updatedState, WizardEvent.SetObject(obj, currentStep.id))
        return StateTransitionResult.success(newState)
    }

    /**
     * Пытается выполнить автоматический переход к следующему шагу
     */
    suspend fun tryAutoAdvance(state: ActionWizardState, validateStep: suspend () -> Boolean): StateTransitionResult<ActionWizardState> {
        // Если есть ошибка, не выполняем автопереход
        if (state.error != null) {
            Timber.d("Автопереход отменен: есть ошибка в состоянии")
            return StateTransitionResult.error(state, "Автопереход отменен: есть ошибка в состоянии")
        }

        val currentStep = state.getCurrentStep() ?: return StateTransitionResult.error(state, "Текущий шаг не найден")

        Timber.d("Пробуем выполнить автопереход с шага ${currentStep.name} (${currentStep.factActionField})")

        // Проверка для шагов товара - нужно проверить, требуются ли дополнительные свойства
        if (currentStep.factActionField == FactActionField.STORAGE_PRODUCT) {
            // Проверяем, требуются ли дополнительные свойства для товара
            val needsAdditionalProps = state.shouldShowAdditionalProps(currentStep)

            if (needsAdditionalProps) {
                // Получаем выбранный объект для текущего шага
                val selectedObj = state.selectedObjects[currentStep.id]
                if (selectedObj == null) {
                    Timber.d("Автопереход отменен: не выбран объект для шага товара")
                    return StateTransitionResult.error(state, "Автопереход отменен: не выбран объект для шага товара")
                }

                // Если это товар, проверяем заполнены ли его свойства
                val taskProduct = selectedObj as? TaskProduct
                if (taskProduct != null) {
                    val needsExpDate = state.shouldShowExpirationDate()

                    // Проверяем, заполнен ли срок годности, если он требуется
                    if (needsExpDate && taskProduct.expirationDate == null) {
                        Timber.d("Автопереход отменен: товар требует заполнения срока годности")
                        return StateTransitionResult.error(state, "Автопереход отменен: товар требует заполнения срока годности")
                    }

                    // Для товаров с дополнительными свойствами автопереход не выполняем
                    Timber.d("Автопереход отменен: для товаров с дополнительными свойствами автопереход запрещен")
                    return StateTransitionResult.error(state, "Автопереход отменен: для товаров с дополнительными свойствами автопереход запрещен")
                }
            }
        }

        if (!validateStep()) {
            Timber.d("Автопереход отменен: ошибка валидации")
            return StateTransitionResult.error(state, "Автопереход отменен: ошибка валидации")
        }

        // Используем машину состояний для перехода к следующему шагу
        val newState = stateMachine.transition(state, WizardEvent.NextStep)

        // Если состояние изменилось, значит переход был успешным
        val success = newState != state
        Timber.d("Результат автоперехода: success=$success")

        return if (success) {
            StateTransitionResult.success(newState)
        } else {
            StateTransitionResult.error(state, "Автопереход не выполнен: состояние не изменилось")
        }
    }

    /**
     * Показывает диалог выхода
     */
    fun showExitDialog(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        // Используем машину состояний для показа диалога выхода
        val newState = stateMachine.transition(state, WizardEvent.ShowExitDialog)
        return StateTransitionResult.success(newState)
    }

    /**
     * Скрывает диалог выхода
     */
    fun dismissExitDialog(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        // Используем машину состояний для скрытия диалога выхода
        val newState = stateMachine.transition(state, WizardEvent.DismissExitDialog)
        return StateTransitionResult.success(newState)
    }

    /**
     * Очищает ошибку в состоянии
     */
    fun clearError(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        // Используем машину состояний для очистки ошибки
        val newState = stateMachine.transition(state, WizardEvent.ClearError)
        return StateTransitionResult.success(newState)
    }

    /**
     * Устанавливает состояние загрузки
     */
    fun setLoading(state: ActionWizardState, isLoading: Boolean): StateTransitionResult<ActionWizardState> {
        // Используем машину состояний для установки состояния загрузки
        val newState = if (isLoading) {
            stateMachine.transition(state, WizardEvent.StartLoading)
        } else {
            stateMachine.transition(state, WizardEvent.StopLoading)
        }
        return StateTransitionResult.success(newState)
    }

    /**
     * Устанавливает сообщение об ошибке
     */
    fun setError(state: ActionWizardState, message: String?): StateTransitionResult<ActionWizardState> {
        // Используем машину состояний для установки ошибки
        val newState = if (message != null) {
            stateMachine.transition(state, WizardEvent.SetError(message))
        } else {
            stateMachine.transition(state, WizardEvent.ClearError)
        }
        return StateTransitionResult.success(newState)
    }

    /**
     * Отправляет форму на сервер
     */
    fun submitForm(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        // Используем машину состояний для отправки формы
        val newState = stateMachine.transition(state, WizardEvent.SubmitForm)
        return StateTransitionResult.success(newState)
    }

    /**
     * Обрабатывает успешную отправку данных
     */
    fun handleSendSuccess(state: ActionWizardState): StateTransitionResult<ActionWizardState> {
        // Используем машину состояний для обработки успешной отправки
        val newState = stateMachine.transition(state, WizardEvent.SendSuccess)
        return StateTransitionResult.success(newState)
    }

    /**
     * Обрабатывает ошибку отправки данных
     */
    fun handleSendFailure(state: ActionWizardState, error: String): StateTransitionResult<ActionWizardState> {
        // Используем машину состояний для обработки ошибки отправки
        val newState = stateMachine.transition(state, WizardEvent.SendFailure(error))
        return StateTransitionResult.error(newState, error)
    }
}