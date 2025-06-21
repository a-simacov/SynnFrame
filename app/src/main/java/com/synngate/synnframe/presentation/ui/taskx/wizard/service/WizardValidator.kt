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
    var lastValidationError: String? = null
        private set

    suspend fun validateCurrentStep(state: ActionWizardState): Boolean {
        lastValidationError = null // Сбрасываем предыдущую ошибку

        val currentStep = state.steps.getOrNull(state.currentStepIndex) ?: run {
            lastValidationError = "Current step not found"
            return false
        }

        if (!expressionEvaluator.evaluateVisibilityCondition(currentStep.visibilityCondition, state)) {
            return true
        }

        if (currentStep.factActionField == FactActionField.NONE || !currentStep.isRequired) {
            return true
        }

        val stepObject = state.selectedObjects[currentStep.id]
        if (stepObject == null) {
            lastValidationError = "Required field is not filled"
            return false
        }

        if (handlerFactory != null) {
            val isStorage = currentStep.factActionField in listOf(FactActionField.STORAGE_BIN, FactActionField.STORAGE_PALLET)
            val handler = handlerFactory.createHandlerForObject(stepObject, isStorage)
            if (handler != null) {
                try {
                    val validationResult = handler.validateObject(stepObject, state, currentStep)
                    if (!validationResult.isSuccess()) {
                        lastValidationError = validationResult.getErrorMessage() ?: "Validation failed"
                        return false
                    }
                    return true
                } catch (e: Exception) {
                    lastValidationError = "Validation error: ${e.message}"
                    return false
                }
            }
        }

        if (currentStep.validationRules != null) {
            val context = buildValidationContext(state)
            val validationResult = validationService.validate(
                rule = currentStep.validationRules,
                value = stepObject,
                context = context
            )

            if (validationResult !is ValidationResult.Success) {
                lastValidationError = (validationResult as? ValidationResult.Error)?.message ?: "Validation failed"
                return false
            }
        }

        return true
    }

    private fun buildValidationContext(state: ActionWizardState): Map<String, Any> {
        val context = mutableMapOf<String, Any>()

        if (state.taskId.isNotEmpty()) {
            context["taskId"] = state.taskId
        }

        return context
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