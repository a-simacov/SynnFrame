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
    private var selectedProduct: Product? = null
    private var selectedTaskProduct: TaskProduct? = null

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
    private fun findPreviousStepResult(): Pair<Product?, TaskProduct?> {
        try {
            var foundProduct: Product? = null
            var foundTaskProduct: TaskProduct? = null

            // Более безопасный поиск результатов предыдущих шагов

            // Сначала пытаемся найти TaskProduct в результатах
            for ((stepId, value) in context.results) {
                if (stepId != context.stepId) {
                    when (value) {
                        is TaskProduct -> {
                            Timber.d("Found TaskProduct from previous step: $stepId")
                            foundTaskProduct = value
                            foundProduct = value.product
                            break
                        }
                        is Product -> {
                            Timber.d("Found Product from previous step: $stepId")
                            foundProduct = value
                            // Не прерываем поиск, потому что может найтись TaskProduct
                        }
                    }
                }
            }

            // Если нашли только Product, создаем из него TaskProduct
            if (foundProduct != null && foundTaskProduct == null) {
                foundTaskProduct = TaskProduct(product = foundProduct, quantity = 0f)
            }

            return Pair(foundProduct, foundTaskProduct)
        } catch (e: Exception) {
            Timber.e(e, "Error finding previous step result")
            return Pair(null, null)
        }
    }

    /**
     * Инициализация из предыдущего шага
     */
    private fun initFromPreviousStep() {
        viewModelScope.launch {
            try {
                setLoading(true)

                // Находим продукт из предыдущего шага
                val (product, taskProduct) = findPreviousStepResult()
                selectedProduct = product
                selectedTaskProduct = taskProduct

                if (product != null) {
                    // Если у нас уже есть результат в контексте, используем его количество
                    if (context.hasStepResult) {
                        val currentResult = context.getCurrentStepResult()
                        if (currentResult is TaskProduct && currentResult.quantity > 0) {
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
                Timber.e(e, "Ошибка при инициализации данных из предыдущего шага: ${e.message}")
                setError("Ошибка загрузки данных: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * Получает связанные фактические действия из контекста
     */
    @Suppress("UNCHECKED_CAST")
    private fun getRelatedFactActions(): List<FactAction> {
        try {
            val factActionsInfo = context.results["factActions"] as? Map<*, *> ?: emptyMap<String, Any>()
            return (factActionsInfo[action.id] as? List<FactAction>) ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении связанных фактических действий: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Обновляет расчетные данные
     */
    private fun updateCalculatedValues() {
        if (selectedProduct == null) return

        // Получаем запланированное количество
        val plannedProduct = action.storageProduct
        plannedQuantity = if (plannedProduct?.product?.id == selectedProduct?.id) {
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
        val updatedTaskProduct = selectedTaskProduct?.copy(quantity = quantityValue)
            ?: TaskProduct(product = selectedProduct!!, quantity = quantityValue)

        selectedTaskProduct = updatedTaskProduct
        setData(updatedTaskProduct)

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

        val updatedTaskProduct = selectedTaskProduct?.copy(quantity = quantityValue)
            ?: TaskProduct(product = selectedProduct!!, quantity = quantityValue)

        completeStep(updatedTaskProduct)
    }

    /**
     * Проверяет, выбран ли продукт
     */
    fun hasSelectedProduct(): Boolean {
        return selectedProduct != null
    }

    /**
     * Возвращает выбранный продукт
     */
    fun getSelectedProduct(): Product? {
        return selectedProduct
    }

    /**
     * Возвращает выбранный TaskProduct
     */
    fun getSelectedTaskProduct(): TaskProduct? {
        return selectedTaskProduct
    }
}