package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import timber.log.Timber
import java.util.UUID

/**
 * Обработчик для полей типа товар задания (TaskProduct)
 *
 * @param validationService Сервис валидации
 * @param productUseCases Сценарии использования для работы с товарами
 */
class TaskProductHandler(
    validationService: ValidationService,
    private val productUseCases: ProductUseCases
) : BaseFieldHandler<TaskProduct>(validationService) {

    /**
     * ИСПРАВЛЕНИЕ: Метод учитывает как storageProduct, так и storageProductClassifier
     * Если есть storageProduct, возвращаем его
     * Если нет storageProduct, но есть storageProductClassifier, создаем временный TaskProduct
     */
    override fun getPlannedObject(state: ActionWizardState, step: ActionStepTemplate): TaskProduct? {
        // Первый приоритет - товар задания, если он указан в плане
        val plannedTaskProduct = state.plannedAction?.storageProduct
        if (plannedTaskProduct != null) {
            return plannedTaskProduct
        }

        // Второй приоритет - товар классификатора, если он указан в плане и шаг поддерживает доп. свойства
        val plannedClassifierProduct = state.plannedAction?.storageProductClassifier
        if (plannedClassifierProduct != null && step.inputAdditionalProps) {
            // Создаем временный TaskProduct на основе товара классификатора
            return TaskProduct(
                id = "temp_${UUID.randomUUID()}",
                product = plannedClassifierProduct,
                status = ProductStatus.STANDARD
            )
        }

        return null
    }

    override fun matchesPlannedObject(barcode: String, plannedObject: TaskProduct): Boolean {
        // Проверяем по ID товара задания
        if (plannedObject.id == barcode) {
            return true
        }

        // Проверяем по ID и артикулу товара
        val product = plannedObject.product
        if (product.id == barcode || product.articleNumber == barcode) {
            return true
        }

        // Проверяем по штрихкодам единиц измерения
        return product.units.any { unit ->
            unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
        }
    }

    override suspend fun createFromString(value: String): Pair<TaskProduct?, String?> {
        if (value.isBlank()) {
            return Pair(null, "Значение не может быть пустым")
        }

        try {
            // Ищем товар по штрихкоду
            val product = productUseCases.findProductByBarcode(value)
                ?: productUseCases.getProductById(value)

            if (product != null) {
                // Создаем новый товар задания из найденного товара
                val taskProduct = TaskProduct(
                    id = UUID.randomUUID().toString(),
                    product = product,
                    status = ProductStatus.STANDARD
                )
                return Pair(taskProduct, null)
            }

            return Pair(null, "Товар не найден по штрихкоду или ID: $value")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске товара задания: $value")
            return Pair(null, "Ошибка при поиске товара: ${e.message}")
        }
    }

    override fun supportsType(obj: Any): Boolean {
        return obj is TaskProduct
    }

    /**
     * ИСПРАВЛЕНИЕ: Добавлена валидация на соответствие плановому товару
     */
    override suspend fun validateObject(obj: TaskProduct, state: ActionWizardState, step: ActionStepTemplate): Pair<Boolean, String?> {
        // Сначала проверяем с помощью стандартной валидации правил
        val (baseValidationResult, baseErrorMessage) = super.validateObject(obj, state, step)
        if (!baseValidationResult) {
            return Pair(false, baseErrorMessage)
        }

        // Дополнительная проверка: если есть плановый объект, проверяем соответствие
        val plannedObject = getPlannedObject(state, step)
        if (plannedObject != null) {
            // Проверяем, совпадает ли ID продукта
            if (obj.product.id != plannedObject.product.id) {
                return Pair(false, "Товар не соответствует плану. Ожидается: ${plannedObject.product.name} (${plannedObject.product.id})")
            }
        }

        return Pair(true, null)
    }

    /**
     * Проверка и создание TaskProduct на основе товара классификатора и штрихкода
     */
    suspend fun createFromClassifier(
        value: String,
        classifierProduct: Product?,
        state: ActionWizardState,
        step: ActionStepTemplate
    ): Pair<TaskProduct?, String?> {
        if (classifierProduct == null) {
            return createFromString(value)
        }

        // Проверяем, совпадает ли штрихкод с товаром классификатора
        if (matchesProduct(value, classifierProduct)) {
            // Создаем TaskProduct на основе товара классификатора
            val taskProduct = TaskProduct(
                id = UUID.randomUUID().toString(),
                product = classifierProduct,
                status = ProductStatus.STANDARD
            )

            // Валидируем созданный объект
            val (isValid, validationError) = validateObject(taskProduct, state, step)
            if (!isValid) {
                return Pair(null, validationError)
            }

            return Pair(taskProduct, null)
        }

        // Если штрихкод не совпадает с товаром классификатора, создаем обычным способом
        // а затем проверяем на соответствие плану
        val (product, error) = createFromString(value)
        if (product == null) {
            return Pair(null, error)
        }

        // Проверяем, соответствует ли созданный товар плану
        val (isValid, validationError) = validateObject(product, state, step)
        if (!isValid) {
            return Pair(null, validationError)
        }

        return Pair(product, null)
    }

    /**
     * Проверяет, соответствует ли штрихкод товару
     */
    private fun matchesProduct(barcode: String, product: Product): Boolean {
        // Проверяем по ID и артикулу
        if (product.id == barcode || product.articleNumber == barcode) {
            return true
        }

        // Проверяем по штрихкодам единиц измерения
        return product.units.any { unit ->
            unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
        }
    }

    companion object {
        /**
         * Проверяет, соответствует ли поле типу обработчика
         */
        fun isApplicableField(field: FactActionField): Boolean {
            return field == FactActionField.STORAGE_PRODUCT
        }
    }
}