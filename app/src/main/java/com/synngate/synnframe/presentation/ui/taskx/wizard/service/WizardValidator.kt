package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.wizard.handler.FieldHandlerFactory
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import timber.log.Timber

/**
 * Класс для высокоуровневой валидации шагов визарда
 */
class WizardValidator(
    private val validationService: ValidationService,
    private val handlerFactory: FieldHandlerFactory? = null
) {
    /**
     * Проверяет текущий шаг на валидность
     * @return true, если шаг прошел валидацию
     */
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

        // Если доступна фабрика обработчиков, используем ее для валидации
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

        // Если обработчик не доступен или не найден, используем базовую валидацию
        if (currentStep.validationRules != null) {
            val validationResult = validationService.validate(
                rule = currentStep.validationRules,
                value = stepObject,
                context = emptyMap()
            )

            if (validationResult !is com.synngate.synnframe.domain.service.ValidationResult.Success) {
                Timber.d("Шаг ${currentStep.name} не прошел базовую валидацию правил")
                return false
            }
        }

        return true
    }

    /**
     * Проверяет возможность завершения визарда
     * @return true, если все обязательные шаги выполнены
     */
    fun canComplete(state: ActionWizardState): Boolean {
        // Проверяем, что все обязательные шаги имеют выбранные объекты
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