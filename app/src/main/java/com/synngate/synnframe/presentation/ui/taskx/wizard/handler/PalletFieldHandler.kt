package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.CreationResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.ValidationResult
import timber.log.Timber

class PalletFieldHandler(
    validationService: ValidationService,
    private val isStorage: Boolean
) : BaseFieldHandler<Pallet>(validationService) {

    override fun getPlannedObject(state: ActionWizardState, step: ActionStepTemplate): Pallet? {
        return if (isStorage) {
            state.plannedAction?.storagePallet
        } else {
            state.plannedAction?.placementPallet
        }
    }

    override fun matchesPlannedObject(barcode: String, plannedObject: Pallet): Boolean {
        return plannedObject.code == barcode
    }

    override suspend fun createFromString(value: String): CreationResult<Pallet> {
        if (value.isBlank()) {
            return CreationResult.error("Pallet code cannot be empty")
        }

        try {
            // В реальном приложении здесь может быть логика получения информации о паллете из БД или API
            val pallet = Pallet(code = value, isClosed = false)
            return CreationResult.success(pallet)
        } catch (e: Exception) {
            Timber.e(e, "Error creating pallet from string: $value")
            return CreationResult.error("Error creating pallet: ${e.message}")
        }
    }

    override suspend fun validateObject(obj: Pallet, state: ActionWizardState, step: ActionStepTemplate): ValidationResult<Pallet> {
        val baseValidationResult = super.validateObject(obj, state, step)
        if (!baseValidationResult.isSuccess()) {
            return baseValidationResult
        }

        val plannedObject = getPlannedObject(state, step)
        if (plannedObject != null) {
            if (obj.code != plannedObject.code) {
                val palletType = if (isStorage) "storage" else "placement"
                return ValidationResult.error("Pallet $palletType does not match plan. Expected: ${plannedObject.code}")
            }
        }

        return ValidationResult.success(obj)
    }

    override fun supportsType(obj: Any): Boolean {
        return obj is Pallet
    }

    companion object {
        fun isApplicableField(field: FactActionField, isStorage: Boolean): Boolean {
            return (isStorage && field == FactActionField.STORAGE_PALLET) ||
                    (!isStorage && field == FactActionField.ALLOCATION_PALLET)
        }
    }
}