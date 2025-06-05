package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.service.ValidationResult
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.handler.FieldHandlerFactory
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import timber.log.Timber

class WizardValidator(
    private val validationService: ValidationService,
    private val handlerFactory: FieldHandlerFactory? = null,
    private val expressionEvaluator: ExpressionEvaluator = ExpressionEvaluator()
) {

    suspend fun validateCurrentStep(state: ActionWizardState): Boolean {
        val currentStep = state.steps.getOrNull(state.currentStepIndex) ?: return false

        // Проверяем видимость шага - если шаг невидим, считаем его валидным
        if (!expressionEvaluator.evaluateVisibilityCondition(currentStep.visibilityCondition, state)) {
            return true
        }

        if (currentStep.factActionField == FactActionField.NONE) {
            return true
        }

        if (!currentStep.isRequired) {
            return true
        }

        val stepObject = state.selectedObjects[currentStep.id]
        if (stepObject == null) {
            Timber.d("Шаг ${currentStep.name} не прошел валидацию: не выбран объект")
            return false
        }

        if (handlerFactory != null) {
            // Используем createHandlerForObject вместо createHandlerForStep для правильной типизации
            val handler = handlerFactory.createHandlerForObject(stepObject)
            if (handler != null) {
                try {
                    // Теперь handler имеет правильный тип для работы с stepObject
                    val validationResult = handler.validateObject(stepObject, state, currentStep)

                    if (!validationResult.isSuccess()) {
                        Timber.d("Шаг ${currentStep.name} не прошел валидацию обработчика: ${validationResult.getErrorMessage()}")
                        return false
                    }
                    return true
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при валидации объекта через обработчик")
                    return false
                }
            }
        }

        if (currentStep.validationRules != null) {
            val validationResult = validationService.validate(
                rule = currentStep.validationRules,
                value = stepObject,
                context = emptyMap()
            )

            if (validationResult !is ValidationResult.Success) {
                Timber.d("Шаг ${currentStep.name} не прошел базовую валидацию правил")
                return false
            }
        }

        return true
    }

    fun canComplete(state: ActionWizardState): Boolean {
        // Фильтруем только видимые обязательные шаги
        val visibleRequiredSteps = state.steps
            .filter { step ->
                step.isRequired &&
                        expressionEvaluator.evaluateVisibilityCondition(step.visibilityCondition, state)
            }

        val allRequiredStepsCompleted = visibleRequiredSteps
            .all { step -> state.selectedObjects.containsKey(step.id) }

        if (!allRequiredStepsCompleted) {
            Timber.d("Не все обязательные шаги выполнены")
            return false
        }

        return true
    }
}