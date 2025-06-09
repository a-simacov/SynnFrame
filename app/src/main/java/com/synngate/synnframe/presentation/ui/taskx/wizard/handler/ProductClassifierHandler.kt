package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.CreationResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.ValidationResult
import timber.log.Timber

class ProductClassifierHandler(
    validationService: ValidationService,
    private val productUseCases: ProductUseCases
) : BaseFieldHandler<Product>(validationService) {

    override fun getPlannedObject(state: ActionWizardState, step: ActionStepTemplate): Product? {
        return state.plannedAction?.storageProductClassifier
    }

    override fun matchesPlannedObject(barcode: String, plannedObject: Product): Boolean {
        if (plannedObject.id == barcode || plannedObject.articleNumber == barcode) {
            return true
        }

        return plannedObject.units.any { unit ->
            unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
        }
    }

    override suspend fun createFromString(value: String): CreationResult<Product> {
        if (value.isBlank()) {
            return CreationResult.error("Value cannot be empty")
        }

        try {
            var product = productUseCases.findProductByBarcode(value)

            if (product == null) {
                product = productUseCases.getProductById(value)
            }

            if (product != null) {
                return CreationResult.success(product)
            }

            return CreationResult.error("Product not found by barcode or ID: $value")
        } catch (e: Exception) {
            Timber.e(e, "Error searching for product: $value")
            return CreationResult.error("Error searching for product: ${e.message}")
        }
    }

    override suspend fun validateObject(obj: Product, state: ActionWizardState, step: ActionStepTemplate): ValidationResult<Product> {
        val baseValidationResult = super.validateObject(obj, state, step)
        if (!baseValidationResult.isSuccess()) {
            return baseValidationResult
        }

        val plannedObject = getPlannedObject(state, step)
        if (plannedObject != null) {
            if (obj.id != plannedObject.id) {
                return ValidationResult.error("Product does not match plan. Expected: ${plannedObject.name} (${plannedObject.id})")
            }
        }

        return ValidationResult.success(obj)
    }

    override fun supportsType(obj: Any): Boolean {
        return obj is Product
    }

    companion object {
        fun isApplicableField(field: FactActionField): Boolean {
            return field == FactActionField.STORAGE_PRODUCT_CLASSIFIER
        }
    }
}