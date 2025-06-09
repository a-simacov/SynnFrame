package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.CreationResult
import timber.log.Timber

class QuantityFieldHandler(
    validationService: ValidationService
) : BaseFieldHandler<Float>(validationService) {

    override fun getPlannedObject(state: ActionWizardState, step: ActionStepTemplate): Float? {
        return state.plannedAction?.quantity?.takeIf { it > 0 }
    }

    override fun matchesPlannedObject(barcode: String, plannedObject: Float): Boolean {
        val parsedValue = barcode.toFloatOrNull()
        return parsedValue != null && parsedValue == plannedObject
    }

    override suspend fun createFromString(value: String): CreationResult<Float> {
        if (value.isBlank()) {
            return CreationResult.error("Value cannot be empty")
        }

        try {
            val parsedValue = value.toFloatOrNull()
            if (parsedValue != null) {
                if (parsedValue <= 0) {
                    return CreationResult.error("Quantity must be greater than zero")
                }
                return CreationResult.success(parsedValue)
            }
            return CreationResult.error("Invalid number format: $value")
        } catch (e: Exception) {
            Timber.e(e, "Error parsing quantity: $value")
            return CreationResult.error("Error processing quantity: ${e.message}")
        }
    }

    override fun supportsType(obj: Any): Boolean {
        return obj is Float || obj is Number
    }

    companion object {
        fun isApplicableField(field: FactActionField): Boolean {
            return field == FactActionField.QUANTITY
        }
    }
}