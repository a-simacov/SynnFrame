package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.service.ValidationResult
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import java.util.UUID

/**
 * Класс для валидации данных в визарде
 */
class WizardValidator(
    private val validationService: ValidationService
) {
    /**
     * Проверяет текущий шаг
     * @return true, если шаг прошел валидацию
     */
    fun validateCurrentStep(state: ActionWizardState): Boolean {
        val currentStep = state.steps.getOrNull(state.currentStepIndex) ?: return false

        if (!currentStep.isRequired) {
            return true
        }

        val stepObject = state.selectedObjects[currentStep.id]
        if (stepObject == null) {
            return false
        }

        if (currentStep.validationRules != null) {
            val validationResult = validationService.validate(
                rule = currentStep.validationRules,
                value = stepObject,
                context = mapOf("planItems" to listOfNotNull(getPlannedObjectForField(state, currentStep.factActionField)))
            )

            if (validationResult !is ValidationResult.Success) {
                return false
            }
        }

        return true
    }

    /**
     * Проверяет найденный объект по правилам валидации
     * @return Пара (результат валидации, сообщение об ошибке)
     */
    fun validateFoundObject(state: ActionWizardState, obj: Any, step: ActionStepTemplate): Pair<Boolean, String?> {
        if (step.validationRules == null) {
            return Pair(true, null)
        }

        val planItem = getPlannedObjectForField(state, step.factActionField)
        val context = if (planItem != null) {
            mapOf("planItems" to listOf(planItem))
        } else {
            emptyMap()
        }

        val validationResult = validationService.validate(
            rule = step.validationRules,
            value = obj,
            context = context
        )

        return when (validationResult) {
            is ValidationResult.Success -> Pair(true, null)
            is ValidationResult.Error -> Pair(false, validationResult.message)
        }
    }

    /**
     * Получает плановый объект для указанного типа поля
     */
    fun getPlannedObjectForField(state: ActionWizardState, field: FactActionField): Any? {
        val plannedAction = state.plannedAction ?: return null
        val currentStep = state.steps.getOrNull(state.currentStepIndex)

        return when (field) {
            FactActionField.STORAGE_PRODUCT -> {
                // Если storageProduct есть, возвращаем его
                plannedAction.storageProduct ?: run {
                    // Если storageProduct нет, но есть storageProductClassifier и включен
                    // признак inputAdditionalProps, создаем временный TaskProduct
                    if (plannedAction.storageProductClassifier != null &&
                        currentStep?.inputAdditionalProps == true) {

                        // Создаем временный TaskProduct на основе storageProductClassifier
                        // Значения expirationDate и status не важны, так как при валидации
                        // сравнивается только product.id, а не другие поля
                        TaskProduct(
                            id = UUID.randomUUID().toString(),
                            product = plannedAction.storageProductClassifier,
                            expirationDate = null,
                            status = ProductStatus.STANDARD
                        )
                    } else {
                        null
                    }
                }
            }
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> plannedAction.storageProductClassifier
            FactActionField.STORAGE_BIN -> plannedAction.storageBin
            FactActionField.STORAGE_PALLET -> plannedAction.storagePallet
            FactActionField.ALLOCATION_BIN -> plannedAction.placementBin
            FactActionField.ALLOCATION_PALLET -> plannedAction.placementPallet
            FactActionField.QUANTITY -> plannedAction.quantity
            else -> null
        }
    }
}