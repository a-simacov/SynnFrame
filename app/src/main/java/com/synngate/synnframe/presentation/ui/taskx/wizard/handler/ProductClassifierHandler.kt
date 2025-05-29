package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import timber.log.Timber

/**
 * Обработчик для полей типа товар классификатора (Product)
 *
 * @param validationService Сервис валидации
 * @param productUseCases Сценарии использования для работы с товарами
 */
class ProductClassifierHandler(
    validationService: ValidationService,
    private val productUseCases: ProductUseCases
) : BaseFieldHandler<Product>(validationService) {

    override fun getPlannedObject(state: ActionWizardState, step: ActionStepTemplate): Product? {
        return state.plannedAction?.storageProductClassifier
    }

    override fun matchesPlannedObject(barcode: String, plannedObject: Product): Boolean {
        // Проверяем по ID и артикулу
        if (plannedObject.id == barcode || plannedObject.articleNumber == barcode) {
            return true
        }

        // Проверяем по штрихкодам единиц измерения
        return plannedObject.units.any { unit ->
            unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
        }
    }

    override suspend fun createFromString(value: String): Pair<Product?, String?> {
        if (value.isBlank()) {
            return Pair(null, "Значение не может быть пустым")
        }

        try {
            // Сначала ищем по штрихкоду
            var product = productUseCases.findProductByBarcode(value)

            // Если не нашли, пробуем найти по ID
            if (product == null) {
                product = productUseCases.getProductById(value)
            }

            if (product != null) {
                return Pair(product, null)
            }

            return Pair(null, "Товар не найден по штрихкоду или ID: $value")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске товара: $value")
            return Pair(null, "Ошибка при поиске товара: ${e.message}")
        }
    }

    /**
     * ИСПРАВЛЕНИЕ: Добавлена дополнительная проверка соответствия плану
     */
    override suspend fun validateObject(obj: Product, state: ActionWizardState, step: ActionStepTemplate): Pair<Boolean, String?> {
        // Сначала проверяем с помощью стандартной валидации правил
        val (baseValidationResult, baseErrorMessage) = super.validateObject(obj, state, step)
        if (!baseValidationResult) {
            return Pair(false, baseErrorMessage)
        }

        // Дополнительная проверка: если есть плановый объект, проверяем точное соответствие
        val plannedObject = getPlannedObject(state, step)
        if (plannedObject != null) {
            // Проверяем, совпадает ли ID товара
            if (obj.id != plannedObject.id) {
                return Pair(false, "Товар не соответствует плану. Ожидается: ${plannedObject.name} (${plannedObject.id})")
            }
        }

        return Pair(true, null)
    }

    override fun supportsType(obj: Any): Boolean {
        return obj is Product
    }

    companion object {
        /**
         * Проверяет, соответствует ли поле типу обработчика
         */
        fun isApplicableField(field: FactActionField): Boolean {
            return field == FactActionField.STORAGE_PRODUCT_CLASSIFIER
        }
    }
}