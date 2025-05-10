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
import java.util.Locale
import kotlin.math.round

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
    var quantityInput = "1.0"
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
    }

    /**
     * Находит предыдущий результат шага с продуктом
     */
    private fun findPreviousStepResult(): Pair<Product?, TaskProduct?> {
        try {
            var foundProduct: Product? = null
            var foundTaskProduct: TaskProduct? = null

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
                            quantityInput = formatQuantityForDisplay(currentResult.quantity)
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
            roundToThreeDecimals(plannedProduct?.quantity ?: 0f)
        } else {
            0f
        }

        // Получаем выполненное количество
        val relatedFactActions = getRelatedFactActions()
        completedQuantity = roundToThreeDecimals(
            relatedFactActions.sumOf {
                it.storageProduct?.quantity?.toDouble() ?: 0.0
            }.toFloat()
        )

        // Рассчитываем текущее введенное количество
        currentInputQuantity = roundToThreeDecimals(parseQuantityInput(quantityInput))

        // Рассчитываем прогнозируемый итог
        projectedTotalQuantity = roundToThreeDecimals(completedQuantity + currentInputQuantity)

        remainingQuantity = roundToThreeDecimals((plannedQuantity - projectedTotalQuantity).coerceAtLeast(0f))

        // Проверяем, будет ли превышение плана
        willExceedPlan = plannedQuantity > 0f && projectedTotalQuantity > plannedQuantity
    }

    /**
     * Обновляет состояние на основе введенного количества
     */
    private fun updateStateFromQuantity() {
        if (selectedProduct == null) return

        val quantityValue = parseQuantityInput(quantityInput)

        // Создаем обновленный продукт с текущим количеством
        val updatedTaskProduct = selectedTaskProduct?.copy(quantity = roundToThreeDecimals(quantityValue))
            ?: TaskProduct(product = selectedProduct!!, quantity = roundToThreeDecimals(quantityValue))

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
        val processedValue = when {
            // Если поле пустое, устанавливаем "0.0"
            input.isEmpty() -> "0.0"
            // Если только точка, преобразуем в "0."
            input == "." -> "0."
            // Если начинается с точки, добавляем ведущий ноль
            input.startsWith(".") -> "0$input"
            // Остальные значения оставляем как есть
            else -> input
        }

        quantityInput = processedValue

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
        val currentValue = parseQuantityInput(quantityInput)
        val newValue = currentValue + 1f
        updateQuantityInput(formatQuantityForDisplay(newValue))
    }

    /**
     * Уменьшение количества
     */
    fun decrementQuantity() {
        val currentValue = parseQuantityInput(quantityInput)
        val newValue = (currentValue - 1f).coerceAtLeast(0f)
        updateQuantityInput(formatQuantityForDisplay(newValue))
    }

    /**
     * Очистка поля (установка значения "0.0")
     */
    fun clearQuantity() {
        updateQuantityInput("0.0")
    }

    /**
     * Сохранение результата
     */
    fun saveResult() {
        if (selectedProduct == null) {
            setError("Сначала необходимо выбрать товар")
            return
        }

        val quantityValue = parseQuantityInput(quantityInput)
        if (quantityValue <= 0f) {
            setError("Количество должно быть больше нуля")
            return
        }

        val updatedTaskProduct = selectedTaskProduct?.copy(quantity = roundToThreeDecimals(quantityValue))
            ?: TaskProduct(product = selectedProduct!!, quantity = roundToThreeDecimals(quantityValue))

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

    /**
     * Округляет число до 3 знаков после запятой
     */
    private fun roundToThreeDecimals(value: Float): Float {
        return (round(value * 1000) / 1000).toFloat()
    }

    /**
     * Форматирует количество для отображения - всегда с точкой
     */
    private fun formatQuantityForDisplay(value: Float): String {
        return if (value % 1f == 0f) {
            // Целое число - показываем с .0
            String.format(Locale.US, "%.1f", value)
        } else {
            // Дробное число - удаляем лишние нули
            String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Парсит строку ввода в Float
     */
    private fun parseQuantityInput(input: String): Float {
        return try {
            // Заменяем запятую на точку перед парсингом
            input.replace(",", ".").toFloatOrNull() ?: 0f
        } catch (e: Exception) {
            Timber.w("Error parsing quantity input: $input")
            0f
        }
    }
}