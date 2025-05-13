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
 * Обновленная ViewModel для шага ввода количества продукта
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
        Timber.d("Barcode processing not supported in quantity step")
    }

    /**
     * Расширенный метод поиска продукта из предыдущего шага
     */
    private fun findPreviousStepResultWithExtendedSearch(): Pair<Product?, TaskProduct?> {
        try {
            // Логируем весь контекст для отладки
            Timber.d("Context.results size: ${context.results.size}")
            context.results.forEach { (key, value) ->
                Timber.d("Context contains: key=$key, value=${value?.javaClass?.simpleName}")
            }

            // ПРЯМОЙ ПОИСК ПО КЛЮЧУ lastTaskProduct
            val directTaskProduct = context.results["lastTaskProduct"] as? TaskProduct
            if (directTaskProduct != null) {
                Timber.d("Found TaskProduct directly by lastTaskProduct key: $directTaskProduct")
                return Pair(directTaskProduct.product, directTaskProduct)
            }

            // ПОИСК ПО КЛЮЧУ lastProduct
            val directProduct = context.results["lastProduct"] as? Product
            if (directProduct != null) {
                Timber.d("Found Product directly by lastProduct key: $directProduct")
                val taskProduct = TaskProduct(product = directProduct, quantity = 0f)
                return Pair(directProduct, taskProduct)
            }

            // ПОИСК ПО ВСЕМ КЛЮЧАМ, ИСКЛЮЧАЯ ТЕКУЩИЙ ШАГ
            // Тщательно проверяем каждый элемент в контексте независимо от ключа
            var foundProduct: Product? = null
            var foundTaskProduct: TaskProduct? = null

            for ((stepId, value) in context.results) {
                if (stepId != context.stepId) {
                    when (value) {
                        is TaskProduct -> {
                            Timber.d("Found TaskProduct from previous step $stepId: $value")
                            foundTaskProduct = value
                            foundProduct = value.product
                            break
                        }
                        is Product -> {
                            Timber.d("Found Product from previous step $stepId: $value")
                            foundProduct = value
                        }
                    }
                }
            }

            // Если нашли только Product, создаем из него TaskProduct
            val finalProduct = foundProduct // создаем неизменяемую копию
            if (finalProduct != null && foundTaskProduct == null) {
                Timber.d("Creating TaskProduct from found Product")
                foundTaskProduct = TaskProduct(product = finalProduct, quantity = 0f)
            }

            // Если нашли что-то, возвращаем
            if (foundProduct != null) {
                Timber.d("Returning found Product and TaskProduct")
                return Pair(foundProduct, foundTaskProduct)
            }

            // ЗАПАСНОЙ МЕТОД: ПРОВЕРКА ВСЕХ ОБЪЕКТОВ В КОНТЕКСТЕ
            // В крайнем случае просмотрим все объекты, независимо от ключа
            Timber.d("No Product found by keys, checking all objects in context...")
            for (value in context.results.values) {
                when (value) {
                    is TaskProduct -> {
                        Timber.d("Found TaskProduct object in values: $value")
                        foundTaskProduct = value
                        foundProduct = value.product
                        break
                    }
                    is Product -> {
                        if (foundProduct == null) {
                            Timber.d("Found Product object in values: $value")
                            foundProduct = value
                        }
                    }
                }
            }

            // Если нашли только Product, создаем из него TaskProduct
            val finalFoundProduct = foundProduct // создаем неизменяемую копию
            if (finalFoundProduct != null && foundTaskProduct == null) {
                foundTaskProduct = TaskProduct(product = finalFoundProduct, quantity = 0f)
                Timber.d("Created TaskProduct from found Product in values")
            }

            Timber.d("Final search result: product=$foundProduct, taskProduct=$foundTaskProduct")
            return Pair(foundProduct, foundTaskProduct)
        } catch (e: Exception) {
            Timber.e(e, "Error in extended search for previous step result")
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

                // ДОБАВЛЯЕМ ПОЛНОЕ ЛОГИРОВАНИЕ КОНТЕКСТА
                Timber.d("initFromPreviousStep: context results count = ${context.results.size}")

                // Находим продукт из предыдущего шага с расширенным поиском
                val (product, taskProduct) = findPreviousStepResultWithExtendedSearch()

                // Если не нашли ничего в контексте, используем продукт из действия
                if (product == null) {
                    Timber.w("Product not found in context, using emergency recovery from action")
                    // ЭКСТРЕННОЕ ВОССТАНОВЛЕНИЕ из запланированного действия
                    val actionProduct = action.storageProduct
                    if (actionProduct != null) {
                        Timber.d("Found product in action: ${actionProduct.product.name}")
                        selectedProduct = actionProduct.product
                        selectedTaskProduct = actionProduct

                        // Искусственно обогащаем контекст для последующих вызовов
                        val contextResults = context.results.toMutableMap()
                        contextResults["emergency_lastTaskProduct"] = actionProduct
                        contextResults["emergency_lastProduct"] = actionProduct.product
                        context.onUpdate(contextResults)

                        Timber.d("Emergency recovery successful!")
                    } else {
                        Timber.e("Emergency recovery failed: no product in action")
                    }
                } else {
                    // Продукт найден в контексте, используем его
                    selectedProduct = product
                    selectedTaskProduct = taskProduct
                    Timber.d("Using product from context: ${product.name}")
                }

                Timber.d("After product resolution: product=${selectedProduct?.name}, taskProduct=${selectedTaskProduct?.product?.name}")

                if (selectedProduct != null) {
                    // Если у нас уже есть результат в контексте, используем его количество
                    if (context.hasStepResult) {
                        val currentResult = context.getCurrentStepResult()
                        Timber.d("Current step has result: $currentResult")
                        if (currentResult is TaskProduct && currentResult.quantity > 0) {
                            quantityInput = formatQuantityForDisplay(currentResult.quantity)
                            Timber.d("Using quantity from context: ${currentResult.quantity}")
                        }
                    }

                    // Обновляем расчетные данные
                    updateCalculatedValues()

                    // Обновляем состояние с выбранным продуктом и текущим количеством
                    updateStateFromQuantity()
                    Timber.d("View state updated with product")
                } else {
                    Timber.e("NO PRODUCT AVAILABLE AFTER ALL RECOVERY ATTEMPTS!")
                }

                setLoading(false)
            } catch (e: Exception) {
                Timber.e(e, "Error initializing data from previous step: ${e.message}")
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
            Timber.e(e, "Error getting related fact actions: ${e.message}")
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

        // Рассчитываем оставшееся количество
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