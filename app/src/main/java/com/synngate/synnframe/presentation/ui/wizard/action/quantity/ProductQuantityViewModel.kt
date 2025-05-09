package com.synngate.synnframe.presentation.ui.wizard.action.quantity

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel для шага ввода количества продукта
 */
class ProductQuantityViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    validationService: ValidationService
) : BaseStepViewModel<TaskProduct>(step, action, context, validationService) {

    // Данные выбранного продукта
    private var selectedProduct: TaskProduct? = null

    // Состояние поля ввода количества
    var quantityInput = "1"
        private set

    // Расчетные данные для отображения
    var plannedQuantity = 0f
        private set
    var completedQuantity = 0f
        private set
    var remainingQuantity = 0f
        private set
    var currentInputQuantity = 0f
        private set
    var projectedTotalQuantity = 0f
        private set
    var willExceedPlan = false
        private set

    init {
        // Инициализация из предыдущего шага
        initFromPreviousStep()
    }

    /**
     * Проверка типа результата
     */
    override fun isValidType(result: Any): Boolean {
        return result is TaskProduct
    }

    /**
     * Обработка штрих-кода - не используется в этом шаге
     */
    override fun processBarcode(barcode: String) {
        // Для этого типа шага обработка штрих-кода не требуется
        // Тем не менее реализуем заглушку, чтобы соответствовать интерфейсу
    }

    /**
     * Находит предыдущий результат шага с продуктом
     */
    private fun findPreviousStepResult(): TaskProduct? {
        // Ищем TaskProduct в результатах предыдущих шагов
        for ((stepId, value) in context.results) {
            if (stepId != context.stepId && value is TaskProduct) {
                return value
            }
        }

        // Если не нашли TaskProduct, ищем просто Product и создаем из него TaskProduct
        for ((stepId, value) in context.results) {
            if (stepId != context.stepId && value is Product) {
                return TaskProduct(product = value, quantity = 0f)
            }
        }

        return null
    }

    /**
     * Инициализация из предыдущего шага
     */
    private fun initFromPreviousStep() {
        viewModelScope.launch {
            try {
                setLoading(true)

                // Находим продукт из предыдущего шага
                selectedProduct = findPreviousStepResult()

                if (selectedProduct != null) {
                    // Если у нас уже есть результат в контексте, используем его количество
                    if (context.hasStepResult) {
                        val currentResult = context.getCurrentStepResult() as? TaskProduct
                        if (currentResult != null && currentResult.quantity > 0) {
                            quantityInput = currentResult.quantity.toString()
                        }
                    }

                    // Обновляем расчетные данные
                    updateCalculatedValues()

                    // Обновляем состояние с выбранным продуктом и текущим количеством
                    updateStateFromQuantity()
                }

                setLoading(false)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при инициализации данных из предыдущего шага")
                setError("Ошибка загрузки данных: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * Получает связанные фактические действия из контекста
     */
    private fun getRelatedFactActions(): List<FactAction> {
        val factActionsInfo = context.results["factActions"] as? Map<*, *> ?: emptyMap<String, Any>()

        @Suppress("UNCHECKED_CAST")
        return (factActionsInfo[action.id] as? List<FactAction>) ?: emptyList()
    }

    /**
     * Обновляет расчетные данные
     */
    private fun updateCalculatedValues() {
        if (selectedProduct == null) return

        // Получаем запланированное количество
        val plannedProduct = action.storageProduct
        plannedQuantity = if (plannedProduct?.product?.id == selectedProduct?.product?.id) {
            plannedProduct?.quantity ?: 0f
        } else {
            0f
        }

        // Получаем выполненное количество
        val relatedFactActions = getRelatedFactActions()
        completedQuantity = relatedFactActions.sumOf {
            it.storageProduct?.quantity?.toDouble() ?: 0.0
        }.toFloat()

        // Рассчитываем оставшееся количество
        remainingQuantity = (plannedQuantity - completedQuantity).coerceAtLeast(0f)

        // Рассчитываем текущее введенное количество
        currentInputQuantity = quantityInput.toFloatOrNull() ?: 0f

        // Рассчитываем прогнозируемый итог
        projectedTotalQuantity = completedQuantity + currentInputQuantity

        // Проверяем, будет ли превышение плана
        willExceedPlan = plannedQuantity > 0f && projectedTotalQuantity > plannedQuantity
    }

    /**
     * Обновляет состояние на основе введенного количества
     */
    private fun updateStateFromQuantity() {
        if (selectedProduct == null) return

        val quantityValue = quantityInput.toFloatOrNull() ?: 0f
        if (quantityValue <= 0f) {
            setError("Количество должно быть больше нуля")
            return
        }

        // Создаем обновленный продукт с текущим количеством
        val updatedProduct = selectedProduct!!.copy(quantity = quantityValue)
        setData(updatedProduct)

        // Обновляем расчетные данные
        updateCalculatedValues()
    }

    /**
     * Валидация данных
     */
    override fun validateBasicRules(data: TaskProduct?): Boolean {
        if (data == null) return false

        // Проверяем, что количество больше нуля
        if (data.quantity <= 0f) {
            setError("Количество должно быть больше нуля")
            return false
        }

        return true
    }

    /**
     * Обновление ввода количества
     */
    fun updateQuantityInput(input: String) {
        // Фильтруем ввод, чтобы были только цифры и точка/запятая
        val filteredValue = input.replace(",", ".").filter {
            it.isDigit() || it == '.'
        }

        quantityInput = filteredValue

        // Обновляем состояние и расчетные данные
        updateCalculatedValues()
        updateStateFromQuantity()

        // Сбрасываем ошибку
        setError(null)
    }

    /**
     * Увеличение количества
     */
    fun incrementQuantity() {
        val currentValue = quantityInput.toFloatOrNull() ?: 0f
        val newValue = currentValue + 1f
        updateQuantityInput(newValue.toString())
    }

    /**
     * Уменьшение количества
     */
    fun decrementQuantity() {
        val currentValue = quantityInput.toFloatOrNull() ?: 0f
        val newValue = (currentValue - 1f).coerceAtLeast(0.1f)
        updateQuantityInput(newValue.toString())
    }

    /**
     * Сохранение результата
     */
    fun saveResult() {
        if (selectedProduct == null) {
            setError("Сначала необходимо выбрать товар")
            return
        }

        val quantityValue = quantityInput.toFloatOrNull() ?: 0f
        if (quantityValue <= 0f) {
            setError("Количество должно быть больше нуля")
            return
        }

        val updatedProduct = selectedProduct!!.copy(quantity = quantityValue)
        completeStep(updatedProduct)
    }

    /**
     * Проверяет, выбран ли продукт
     */
    fun hasSelectedProduct(): Boolean {
        return selectedProduct != null
    }
}