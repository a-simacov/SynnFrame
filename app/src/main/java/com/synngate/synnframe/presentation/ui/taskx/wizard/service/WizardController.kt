package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
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
    fun initializeWizard(taskId: String, actionId: String): ActionWizardState {
        try {
            val task = TaskXDataHolderSingleton.currentTask.value
            if (task == null) {
                Timber.e("Задание не найдено в TaskXDataHolderSingleton")
                return ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    error = "Задание не найдено"
                )
            }

            val plannedAction = task.plannedActions.find { it.id == actionId }
            if (plannedAction == null) {
                Timber.e("Действие $actionId не найдено в задании ${task.id}")
                return ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    error = "Действие не найдено"
                )
            }

            val actionTemplate = plannedAction.actionTemplate
            if (actionTemplate == null) {
                Timber.e("Шаблон действия не найден для действия $actionId")
                return ActionWizardState(
                    taskId = taskId,
                    actionId = actionId,
                    error = "Шаблон действия не найден"
                )
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
            return stateMachine.transition(initialState, WizardEvent.LoadSuccess)

        } catch (e: Exception) {
            Timber.e(e, "Ошибка инициализации визарда: ${e.message}")

            val initialState = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                error = "Ошибка: ${e.message}"
            )

            // Используем машину состояний для перехода к состоянию ошибки загрузки
            return stateMachine.transition(initialState, WizardEvent.LoadFailure(e.message ?: "Неизвестная ошибка"))
        }
    }

    /**
     * Обрабатывает нажатие на кнопку "Далее" для текущего шага
     */
    suspend fun confirmCurrentStep(state: ActionWizardState, validateStep: suspend () -> Boolean): ActionWizardState {
        val currentStepIndex = state.currentStepIndex
        val steps = state.steps

        if (currentStepIndex >= steps.size) {
            return state
        }

        if (!validateStep()) {
            return state
        }

        // Используем машину состояний для перехода к следующему шагу
        return stateMachine.transition(state, WizardEvent.NextStep)
    }

    /**
     * Обрабатывает нажатие на кнопку "Назад"
     */
    fun previousStep(state: ActionWizardState): ActionWizardState {
        // Используем машину состояний для перехода к предыдущему шагу
        return stateMachine.transition(state, WizardEvent.PreviousStep)
    }

    /**
     * Устанавливает объект для текущего шага и обновляет состояние
     */
    fun setObjectForCurrentStep(state: ActionWizardState, obj: Any): ActionWizardState {
        val currentStep = state.getCurrentStep() ?: return state

        // Сначала очищаем ошибку, если она есть
        var updatedState = state
        if (state.error != null) {
            updatedState = stateMachine.transition(state, WizardEvent.ClearError)
        }

        // Затем используем машину состояний для установки объекта
        return stateMachine.transition(updatedState, WizardEvent.SetObject(obj, currentStep.id))
    }

    /**
     * Пытается выполнить автоматический переход к следующему шагу
     */
    suspend fun tryAutoAdvance(state: ActionWizardState, validateStep: suspend () -> Boolean): Pair<Boolean, ActionWizardState> {
        // Если есть ошибка, не выполняем автопереход
        if (state.error != null) {
            Timber.d("Автопереход отменен: есть ошибка в состоянии")
            return Pair(false, state)
        }

        val currentStep = state.getCurrentStep() ?: return Pair(false, state)

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
                    return Pair(false, state)
                }

                // Если это товар, проверяем заполнены ли его свойства
                val taskProduct = selectedObj as? TaskProduct
                if (taskProduct != null) {
                    val needsExpDate = state.shouldShowExpirationDate()

                    // Проверяем, заполнен ли срок годности, если он требуется
                    if (needsExpDate && taskProduct.expirationDate == null) {
                        Timber.d("Автопереход отменен: товар требует заполнения срока годности")
                        return Pair(false, state)
                    }

                    // Для товаров с дополнительными свойствами автопереход не выполняем - пользователь должен явно нажать кнопку "Далее"
                    // Это предотвращает случайные переходы без заполнения всех свойств
                    Timber.d("Автопереход отменен: для товаров с дополнительными свойствами автопереход запрещен")
                    return Pair(false, state)
                }
            }
        }

        if (!validateStep()) {
            Timber.d("Автопереход отменен: ошибка валидации")
            return Pair(false, state)
        }

        // Используем машину состояний для перехода к следующему шагу
        val newState = stateMachine.transition(state, WizardEvent.NextStep)

        // Если состояние изменилось, значит переход был успешным
        val success = newState != state
        Timber.d("Результат автоперехода: success=$success")

        return Pair(success, newState)
    }

    /**
     * Показывает диалог выхода
     */
    fun showExitDialog(state: ActionWizardState): ActionWizardState {
        // Используем машину состояний для показа диалога выхода
        return stateMachine.transition(state, WizardEvent.ShowExitDialog)
    }

    /**
     * Скрывает диалог выхода
     */
    fun dismissExitDialog(state: ActionWizardState): ActionWizardState {
        // Используем машину состояний для скрытия диалога выхода
        return stateMachine.transition(state, WizardEvent.DismissExitDialog)
    }

    /**
     * Очищает ошибку в состоянии
     */
    fun clearError(state: ActionWizardState): ActionWizardState {
        // Используем машину состояний для очистки ошибки
        return stateMachine.transition(state, WizardEvent.ClearError)
    }

    /**
     * Устанавливает состояние загрузки
     */
    fun setLoading(state: ActionWizardState, isLoading: Boolean): ActionWizardState {
        // Используем машину состояний для установки состояния загрузки
        return if (isLoading) {
            stateMachine.transition(state, WizardEvent.StartLoading)
        } else {
            stateMachine.transition(state, WizardEvent.StopLoading)
        }
    }

    /**
     * Устанавливает сообщение об ошибке
     */
    fun setError(state: ActionWizardState, message: String?): ActionWizardState {
        // Используем машину состояний для установки ошибки
        return if (message != null) {
            stateMachine.transition(state, WizardEvent.SetError(message))
        } else {
            stateMachine.transition(state, WizardEvent.ClearError)
        }
    }

    /**
     * Отправляет форму на сервер
     */
    fun submitForm(state: ActionWizardState): ActionWizardState {
        // Используем машину состояний для отправки формы
        return stateMachine.transition(state, WizardEvent.SubmitForm)
    }

    /**
     * Обрабатывает успешную отправку данных
     */
    fun handleSendSuccess(state: ActionWizardState): ActionWizardState {
        // Используем машину состояний для обработки успешной отправки
        return stateMachine.transition(state, WizardEvent.SendSuccess)
    }

    /**
     * Обрабатывает ошибку отправки данных
     */
    fun handleSendFailure(state: ActionWizardState, error: String): ActionWizardState {
        // Используем машину состояний для обработки ошибки отправки
        return stateMachine.transition(state, WizardEvent.SendFailure(error))
    }
}