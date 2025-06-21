package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.service.ValidationResult
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.handler.FieldHandlerFactory
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import timber.log.Timber

/**
 * Результат валидации шага
 */
data class StepValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success() = StepValidationResult(true)
        fun error(message: String) = StepValidationResult(false, message)
    }
}

class WizardValidator(
    private val validationService: ValidationService,
    private val handlerFactory: FieldHandlerFactory? = null,
    private val expressionEvaluator: ExpressionEvaluator = ExpressionEvaluator()
) {

    suspend fun validateCurrentStep(state: ActionWizardState): StepValidationResult {
        val currentStep = state.steps.getOrNull(state.currentStepIndex)
            ?: return StepValidationResult.error("Current step not found")

        // Проверяем видимость шага - если шаг невидим, считаем его валидным
        if (!expressionEvaluator.evaluateVisibilityCondition(currentStep.visibilityCondition, state)) {
            return StepValidationResult.success()
        }

        if (currentStep.factActionField == FactActionField.NONE) {
            return StepValidationResult.success()
        }

        if (!currentStep.isRequired) {
            return StepValidationResult.success()
        }

        val stepObject = state.selectedObjects[currentStep.id]
        if (stepObject == null) {
            Timber.d("Шаг ${currentStep.name} не прошел валидацию: не выбран объект")
            return StepValidationResult.error("Required field is not filled")
        }

        if (handlerFactory != null) {
            // Используем createHandlerForObject вместо createHandlerForStep для правильной типизации
            val isStorage = currentStep.factActionField in listOf(FactActionField.STORAGE_BIN, FactActionField.STORAGE_PALLET)
            val handler = handlerFactory.createHandlerForObject(stepObject, isStorage)
            if (handler != null) {
                try {
                    // Теперь handler имеет правильный тип для работы с stepObject
                    val validationResult = handler.validateObject(stepObject, state, currentStep)

                    if (!validationResult.isSuccess()) {
                        val errorMessage = validationResult.getErrorMessage() ?: "Validation failed"
                        Timber.d("Шаг ${currentStep.name} не прошел валидацию обработчика: $errorMessage")
                        return StepValidationResult.error(errorMessage)
                    }
                    return StepValidationResult.success()
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при валидации объекта через обработчик")
                    return StepValidationResult.error("Validation error: ${e.message}")
                }
            }
        }

        if (currentStep.validationRules != null) {
            val context = buildValidationContext(state)

            // Сначала пробуем синхронную валидацию
            val syncValidationResult = validationService.validate(
                rule = currentStep.validationRules,
                value = stepObject,
                context = context
            )

            when (syncValidationResult) {
                is ValidationResult.Success -> {
                    // Синхронная валидация прошла успешно
                }
                is ValidationResult.Error -> {
                    Timber.d("Шаг ${currentStep.name} не прошел синхронную валидацию: ${syncValidationResult.message}")
                    return StepValidationResult.error(syncValidationResult.message)
                }
                is ValidationResult.ApiValidationRequired -> {
                    // Требуется API валидация, выполняем асинхронную валидацию
                    val asyncValidationResult = validationService.validateAsync(
                        rule = currentStep.validationRules,
                        value = stepObject,
                        context = context
                    )

                    if (asyncValidationResult !is ValidationResult.Success) {
                        val errorMessage = when (asyncValidationResult) {
                            is ValidationResult.Error -> asyncValidationResult.message
                            else -> "API validation failed"
                        }
                        Timber.d("Шаг ${currentStep.name} не прошел API валидацию: $errorMessage")
                        return StepValidationResult.error(errorMessage)
                    }
                }
            }
        }

        return StepValidationResult.success()
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