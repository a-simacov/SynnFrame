package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.CreationResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.SearchResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.ValidationResult
import timber.log.Timber
import java.util.UUID

class TaskProductHandler(
    validationService: ValidationService,
    private val productUseCases: ProductUseCases
) : BaseFieldHandler<TaskProduct>(validationService) {

    override fun getPlannedObject(state: ActionWizardState, step: ActionStepTemplate): TaskProduct? {
        val plannedTaskProduct = state.plannedAction?.storageProduct
        if (plannedTaskProduct != null) {
            return plannedTaskProduct
        }

        val plannedClassifierProduct = state.plannedAction?.storageProductClassifier
        if (plannedClassifierProduct != null && step.inputAdditionalProps) {
            return TaskProduct(
                id = "temp_${UUID.randomUUID()}",
                product = plannedClassifierProduct,
                status = ProductStatus.STANDARD
            )
        }

        return null
    }

    override fun matchesPlannedObject(barcode: String, plannedObject: TaskProduct): Boolean {
        if (plannedObject.id == barcode) {
            return true
        }

        val product = plannedObject.product
        if (product.id == barcode || product.articleNumber == barcode) {
            return true
        }

        return product.units.any { unit ->
            unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
        }
    }

    override suspend fun handleBarcode(barcode: String, state: ActionWizardState, step: ActionStepTemplate): SearchResult<TaskProduct> {
        val classifierProduct = state.plannedAction?.storageProductClassifier

        if (step.inputAdditionalProps && classifierProduct != null) {
            return createFromClassifier(barcode, classifierProduct, state, step)
        }

        return super.handleBarcode(barcode, state, step)
    }

    override suspend fun createFromString(value: String): CreationResult<TaskProduct> {
        if (value.isBlank()) {
            return CreationResult.error("Значение не может быть пустым")
        }

        try {
            val product = productUseCases.findProductByBarcode(value)
                ?: productUseCases.getProductById(value)

            if (product != null) {
                val taskProduct = TaskProduct(
                    id = UUID.randomUUID().toString(),
                    product = product,
                    status = ProductStatus.STANDARD
                )
                return CreationResult.success(taskProduct)
            }

            return CreationResult.error("Товар не найден по штрихкоду или ID: $value")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске товара задания: $value")
            return CreationResult.error("Ошибка при поиске товара: ${e.message}")
        }
    }

    override fun supportsType(obj: Any): Boolean {
        return obj is TaskProduct
    }

    override suspend fun validateObject(obj: TaskProduct, state: ActionWizardState, step: ActionStepTemplate): ValidationResult<TaskProduct> {
        val baseValidationResult = super.validateObject(obj, state, step)
        if (!baseValidationResult.isSuccess()) {
            return baseValidationResult
        }

        val plannedObject = getPlannedObject(state, step)
        if (plannedObject != null) {
            if (obj.product.id != plannedObject.product.id) {
                return ValidationResult.error("Товар не соответствует плану. Ожидается: ${plannedObject.product.name} (${plannedObject.product.id})")
            }
        }

        return ValidationResult.success(obj)
    }

    suspend fun createFromClassifier(
        value: String,
        classifierProduct: Product,
        state: ActionWizardState,
        step: ActionStepTemplate
    ): SearchResult<TaskProduct> {
        if (value.isBlank()) {
            return SearchResult.error("Значение не может быть пустым")
        }

        if (matchesProduct(value, classifierProduct)) {
            Timber.d("Штрихкод $value соответствует товару классификатора ${classifierProduct.id}")

            val taskProduct = TaskProduct(
                id = UUID.randomUUID().toString(),
                product = classifierProduct,
                status = ProductStatus.STANDARD
            )

            val validationResult = validateObject(taskProduct, state, step)
            if (!validationResult.isSuccess()) {
                return SearchResult.error(validationResult.getErrorMessage() ?: "Ошибка валидации")
            }

            return SearchResult.success(taskProduct)
        }

        Timber.d("Штрихкод $value не соответствует товару классификатора ${classifierProduct.id}, выполняем стандартный поиск")

        val creationResult = createFromString(value)
        if (!creationResult.isSuccess()) {
            return SearchResult.error(creationResult.getErrorMessage() ?: "Не удалось создать объект")
        }

        val product = creationResult.getCreatedData() ?: return SearchResult.error("Не удалось создать объект")

        val validationResult = validateObject(product, state, step)
        if (!validationResult.isSuccess()) {
            return SearchResult.error(validationResult.getErrorMessage() ?: "Ошибка валидации")
        }

        return SearchResult.success(product)
    }

    private fun matchesProduct(barcode: String, product: Product): Boolean {
        if (product.id == barcode || product.articleNumber == barcode) {
            return true
        }

        return product.units.any { unit ->
            unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
        }
    }

    companion object {
        fun isApplicableField(field: FactActionField): Boolean {
            return field == FactActionField.STORAGE_PRODUCT
        }
    }
}