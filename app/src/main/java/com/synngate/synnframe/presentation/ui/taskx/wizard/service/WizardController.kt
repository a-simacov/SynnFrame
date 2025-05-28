package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * Контроллер для управления жизненным циклом визарда
 */
class WizardController(
    private val validator: WizardValidator
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

            return ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                plannedAction = plannedAction,
                steps = sortedSteps,
                factAction = newFactAction
            )

        } catch (e: Exception) {
            Timber.e(e, "Ошибка инициализации визарда: ${e.message}")
            return ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                error = "Ошибка: ${e.message}"
            )
        }
    }

    /**
     * Обрабатывает нажатие на кнопку "Далее" для текущего шага
     */
    fun confirmCurrentStep(state: ActionWizardState, validateStep: () -> Boolean): ActionWizardState {
        val currentStepIndex = state.currentStepIndex
        val steps = state.steps

        if (currentStepIndex >= steps.size) {
            return state
        }

        if (!validateStep()) {
            return state
        }

        return if (currentStepIndex == steps.size - 1) {
            state.copy(showSummary = true)
        } else {
            state.copy(currentStepIndex = state.currentStepIndex + 1)
        }
    }

    /**
     * Обрабатывает нажатие на кнопку "Назад"
     */
    fun previousStep(state: ActionWizardState): ActionWizardState {
        if (state.showSummary) {
            return state.copy(showSummary = false)
        }

        if (state.currentStepIndex == 0) {
            return state.copy(showExitDialog = true)
        }

        return state.copy(currentStepIndex = state.currentStepIndex - 1)
    }

    /**
     * Обновляет факт действия с новым объектом
     */
    fun updateFactActionWithObject(factAction: FactAction?, field: FactActionField, obj: Any): FactAction? {
        if (factAction == null) return null

        return when {
            field == FactActionField.STORAGE_PRODUCT && obj is TaskProduct ->
                factAction.copy(storageProduct = obj)

            field == FactActionField.STORAGE_PRODUCT_CLASSIFIER && obj is Product ->
                factAction.copy(storageProductClassifier = obj)

            field == FactActionField.STORAGE_BIN && obj is BinX ->
                factAction.copy(storageBin = obj)

            field == FactActionField.STORAGE_PALLET && obj is Pallet ->
                factAction.copy(storagePallet = obj)

            field == FactActionField.ALLOCATION_BIN && obj is BinX ->
                factAction.copy(placementBin = obj)

            field == FactActionField.ALLOCATION_PALLET && obj is Pallet ->
                factAction.copy(placementPallet = obj)

            field == FactActionField.QUANTITY && obj is Number ->
                factAction.copy(quantity = obj.toFloat())

            else -> factAction
        }
    }

    /**
     * Устанавливает объект для текущего шага и обновляет состояние
     */
    fun setObjectForCurrentStep(state: ActionWizardState, obj: Any): ActionWizardState {
        val currentStep = state.steps.getOrNull(state.currentStepIndex) ?: return state

        val updatedSelectedObjects = state.selectedObjects.toMutableMap()
        updatedSelectedObjects[currentStep.id] = obj

        val updatedFactAction = updateFactActionWithObject(state.factAction, currentStep.factActionField, obj)

        return state.copy(
            selectedObjects = updatedSelectedObjects,
            factAction = updatedFactAction,
            error = null
        )
    }

    /**
     * Пытается выполнить автоматический переход к следующему шагу
     */
    fun tryAutoAdvance(state: ActionWizardState, validateStep: () -> Boolean): Pair<Boolean, ActionWizardState> {
        val currentStep = state.getCurrentStep() ?: return Pair(false, state)

        Timber.d("Пробуем выполнить автопереход с шага ${currentStep.name} (${currentStep.factActionField})")

        val isAdditionalPropsStep = state.shouldShowAdditionalProps(currentStep)

        if (isAdditionalPropsStep) {
            val taskProduct = state.selectedObjects[currentStep.id] as? TaskProduct
            if (taskProduct == null) {
                Timber.d("Автопереход отменен: не выбран TaskProduct для шага с доп. свойствами")
                return Pair(false, state)
            }

            if (state.shouldShowExpirationDate() && taskProduct.expirationDate == null) {
                Timber.d("Автопереход отменен: не указан срок годности")
                return Pair(false, state)
            }
        }

        if (!validateStep()) {
            Timber.d("Автопереход отменен: ошибка валидации")
            return Pair(false, state)
        }

        val newState = if (state.currentStepIndex == state.steps.size - 1) {
            Timber.d("Автопереход: переходим к сводной информации")
            state.copy(showSummary = true, isLoading = false)
        } else {
            Timber.d("Автопереход: переходим к следующему шагу ${state.currentStepIndex + 1}")
            state.copy(currentStepIndex = state.currentStepIndex + 1, isLoading = false)
        }

        return Pair(true, newState)
    }

    /**
     * Показывает диалог выхода
     */
    fun showExitDialog(state: ActionWizardState): ActionWizardState {
        return state.copy(showExitDialog = true)
    }

    /**
     * Скрывает диалог выхода
     */
    fun dismissExitDialog(state: ActionWizardState): ActionWizardState {
        return state.copy(showExitDialog = false)
    }

    /**
     * Очищает ошибку в состоянии
     */
    fun clearError(state: ActionWizardState): ActionWizardState {
        return state.copy(error = null)
    }

    /**
     * Устанавливает состояние загрузки
     */
    fun setLoading(state: ActionWizardState, isLoading: Boolean): ActionWizardState {
        return state.copy(isLoading = isLoading)
    }

    /**
     * Устанавливает сообщение об ошибке
     */
    fun setError(state: ActionWizardState, message: String?): ActionWizardState {
        return state.copy(error = message)
    }
}