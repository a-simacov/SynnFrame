package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.service.ValidationResult
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.wizard.handler.FieldHandlerFactory
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import timber.log.Timber

class WizardValidator(
    private val validationService: ValidationService,
    private val handlerFactory: FieldHandlerFactory? = null
) {

    suspend fun validateCurrentStep(state: ActionWizardState): Boolean {
        val currentStep = state.steps.getOrNull(state.currentStepIndex) ?: return false

        if (!currentStep.isRequired) {
            return true
        }

        val stepObject = state.selectedObjects[currentStep.id]
        if (stepObject == null) {
            Timber.d("Шаг ${currentStep.name} не прошел валидацию: не выбран объект")
            return false
        }

        if (handlerFactory != null) {
            val handler = handlerFactory.createHandlerForObject(stepObject)
            if (handler != null) {
                val validationResult = runCatching {
                    @Suppress("UNCHECKED_CAST")
                    val result = handler.validateObject(stepObject as Any, state, currentStep)
                    if (!result.isSuccess()) {
                        Timber.d("Шаг ${currentStep.name} не прошел валидацию обработчика: ${result.getErrorMessage()}")
                        return false
                    }
                    true
                }.getOrElse { e ->
                    Timber.e(e, "Ошибка при валидации объекта через обработчик")
                    false
                }

                return validationResult
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
        val allRequiredStepsCompleted = state.steps
            .filter { it.isRequired }
            .all { step -> state.selectedObjects.containsKey(step.id) }

        if (!allRequiredStepsCompleted) {
            Timber.d("Не все обязательные шаги выполнены")
            return false
        }

        return true
    }
}