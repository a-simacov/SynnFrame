package com.synngate.synnframe.presentation.ui.wizard.action.quantity

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardLogger
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardUtils
import timber.log.Timber

/**
 * Оптимизированная ViewModel для шага ввода количества продукта
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
    var quantityInput = ""
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
        Timber.d("$TAG: Barcode processing not supported in quantity step")
    }

    /**
     * Инициализация из предыдущего шага
     */
    private fun initFromPreviousStep() {
        executeWithErrorHandling("загрузки данных из предыдущего шага") {
            // Находим продукт/TaskProduct в контексте
            selectedProduct = WizardUtils.findProduct(context.results)
            selectedTaskProduct = WizardUtils.findTaskProduct(context.results)

            // Логируем найденные объекты
            WizardLogger.logProduct(TAG, selectedProduct)
            WizardLogger.logTaskProduct(TAG, selectedTaskProduct)

            // Если не нашли ничего в контексте, используем экстренное восстановление
            if (selectedProduct == null) {
                Timber.w("$TAG: Product not found in context, using emergency recovery from action")
                // Экстренное восстановление из запланированного действия
                val actionProduct = action.storageProduct
                if (actionProduct != null) {
                    Timber.d("$TAG: Found product in action: ${actionProduct.product.name}")
                    selectedProduct = actionProduct.product
                    selectedTaskProduct = actionProduct
                }
            }

            if (selectedProduct != null) {
                // Если у нас уже есть результат в контексте, используем его количество
                if (context.hasStepResult) {
                    val currentResult = context.getCurrentStepResult()
                    Timber.d("$TAG: Current step has result: $currentResult")
                    if (currentResult is TaskProduct && currentResult.quantity > 0) {
                        quantityInput = WizardUtils.formatQuantity(currentResult.quantity)
                        Timber.d("$TAG: Using quantity from context: ${currentResult.quantity}")
                    }
                }

                // Обновляем расчетные данные
                updateCalculatedValues()

                // Обновляем состояние с выбранным продуктом и текущим количеством
                updateStateFromQuantity()
                Timber.d("$TAG: View state updated with product")
            } else {
                setError("Не удалось найти товар для ввода количества")
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
            Timber.e(e, "$TAG: Error getting related fact actions: ${e.message}")
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
        if (plannedProduct != null) {
            plannedQuantity = if (plannedProduct.product.id == selectedProduct?.id) {
                WizardUtils.roundToThreeDecimals(plannedProduct.quantity ?: 0f)
            } else {
                0f
            }
        }

        // Получаем выполненное количество
        val relatedFactActions = getRelatedFactActions()
        completedQuantity = WizardUtils.roundToThreeDecimals(
            relatedFactActions.sumOf {
                it.storageProduct?.quantity?.toDouble() ?: 0.0
            }.toFloat()
        )

        // Рассчитываем текущее введенное количество
        currentInputQuantity = WizardUtils.roundToThreeDecimals(WizardUtils.parseQuantityInput(quantityInput))

        // Рассчитываем прогнозируемый итог
        projectedTotalQuantity = WizardUtils.roundToThreeDecimals(completedQuantity + currentInputQuantity)

        // Рассчитываем оставшееся количество
        remainingQuantity = WizardUtils.roundToThreeDecimals((plannedQuantity - projectedTotalQuantity).coerceAtLeast(0f))

        // Проверяем, будет ли превышение плана
        willExceedPlan = plannedQuantity > 0f && projectedTotalQuantity > plannedQuantity
    }

    /**
     * Обновляет состояние на основе введенного количества
     */
    private fun updateStateFromQuantity() {
        if (selectedProduct == null) return

        // ИСПРАВЛЕНИЕ: Используем значение из пользовательского ввода, а не из плана
        val quantityValue = WizardUtils.parseQuantityInput(quantityInput)

        // Выводим отладочную информацию о вводимом количестве
        Timber.d("updateStateFromQuantity: parsed quantity value = $quantityValue")

        // Создаем обновленный продукт с текущим количеством
        val updatedTaskProduct = selectedTaskProduct?.copy(quantity = WizardUtils.roundToThreeDecimals(quantityValue))
            ?: WizardUtils.createTaskProductFromProduct(
                product = selectedProduct!!,
                quantity = WizardUtils.roundToThreeDecimals(quantityValue)
            )

        selectedTaskProduct = updatedTaskProduct

        // Обновляем данные в состоянии
        setData(updatedTaskProduct)

        // Выводим отладочную информацию о новом TaskProduct
        Timber.d("updateStateFromQuantity: updated TaskProduct with quantity = ${updatedTaskProduct.quantity}")

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
        val processedValue = WizardUtils.processQuantityInput(input)
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
        val currentValue = WizardUtils.parseQuantityInput(quantityInput)
        val newValue = currentValue + 1f
        updateQuantityInput(WizardUtils.formatQuantity(newValue))
    }

    /**
     * Уменьшение количества
     */
    fun decrementQuantity() {
        val currentValue = WizardUtils.parseQuantityInput(quantityInput)
        val newValue = (currentValue - 1f).coerceAtLeast(0f)
        updateQuantityInput(WizardUtils.formatQuantity(newValue))
    }

    /**
     * Очистка поля (установка значения "0")
     */
    fun clearQuantity() {
        updateQuantityInput("0")
    }

    /**
     * Сохранение результата
     */
    fun saveResult() {
        executeWithErrorHandling("сохранения результата") {
            if (selectedProduct == null) {
                setError("Сначала необходимо выбрать товар")
                return@executeWithErrorHandling
            }

            // ВАЖНОЕ ИСПРАВЛЕНИЕ: Используем значение из quantityInput, а не из других источников
            val quantityValue = WizardUtils.parseQuantityInput(quantityInput)

            // Подробный лог для отладки
            Timber.d("saveResult: Using quantity from input field: $quantityInput (parsed as $quantityValue)")

            if (quantityValue <= 0f) {
                setError("Количество должно быть больше нуля")
                return@executeWithErrorHandling
            }

            // Создаем новый объект с четко заданным количеством
            val finalQuantity = WizardUtils.roundToThreeDecimals(quantityValue)

            // Получаем статус и срок годности из имеющегося TaskProduct, если доступны
            val currentStatus = selectedTaskProduct?.status ?: ProductStatus.STANDARD
            val currentExpirationDate = selectedTaskProduct?.expirationDate

            val updatedTaskProduct = WizardUtils.createTaskProductFromProduct(
                product = selectedProduct!!,
                quantity = finalQuantity,
                status = currentStatus,
                expirationDate = currentExpirationDate
            )

            // Убеждаемся, что selectedTaskProduct тоже обновлен
            selectedTaskProduct = updatedTaskProduct

            // Для отладки отображаем итоговый объект
            Timber.d("saveResult: Created TaskProduct with quantity = $finalQuantity for product ${updatedTaskProduct.product.name}")

            // Завершаем шаг с новым TaskProduct
            completeStep(updatedTaskProduct)
        }
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

    override fun initStateFromContext() {
        try {
            // Находим продукт/TaskProduct в контексте
            selectedProduct = WizardUtils.findProduct(context.results)
            selectedTaskProduct = WizardUtils.findTaskProduct(context.results)

            // Логируем найденные объекты
            WizardLogger.logProduct(TAG, selectedProduct)
            WizardLogger.logTaskProduct(TAG, selectedTaskProduct)

            // Если не нашли ничего в контексте, используем экстренное восстановление
            if (selectedProduct == null) {
                Timber.w("$TAG: Product not found in context, using emergency recovery from action")
                // Экстренное восстановление из запланированного действия
                val actionProduct = action.storageProduct
                if (actionProduct != null) {
                    Timber.d("$TAG: Found product in action: ${actionProduct.product.name}")
                    selectedProduct = actionProduct.product
                    selectedTaskProduct = actionProduct
                }
            }

            if (selectedProduct != null) {
                // ИСПРАВЛЕНИЕ: Проверяем сначала текущий шаг, а затем "lastTaskProduct"
                // Если у нас уже есть результат в контексте для текущего шага, используем его количество
                if (context.hasStepResult) {
                    val currentResult = context.getCurrentStepResult()

                    if (currentResult is TaskProduct) {
                        Timber.d("$TAG: Current step has result: $currentResult")

                        // Проверяем, что это тот же продукт
                        if (currentResult.product.id == selectedProduct?.id) {
                            // Используем количество из результата этого же шага
                            Timber.d("$TAG: Using quantity from this step result: ${currentResult.quantity}")
                            quantityInput = WizardUtils.formatQuantity(currentResult.quantity)
                            selectedTaskProduct = currentResult
                        } else {
                            // Если продукты не совпадают, используем плановое количество
                            Timber.d("$TAG: Product ID mismatch, using planned quantity")
                            val plannedQuantity = action.storageProduct?.quantity ?: 1f
                            quantityInput = WizardUtils.formatQuantity(plannedQuantity)
                        }
                    } else {
                        Timber.d("$TAG: Current step result is not TaskProduct: ${currentResult?.javaClass?.simpleName}")
                        // Используем quantity из selectedTaskProduct или 1 по умолчанию
                        quantityInput = WizardUtils.formatQuantity(selectedTaskProduct?.quantity ?: 1f)
                    }
                } else {
                    // Если нет результата для текущего шага, проверяем, есть ли lastTaskProduct
                    val lastTaskProduct = context.results["lastTaskProduct"] as? TaskProduct

                    if (lastTaskProduct != null && lastTaskProduct.product.id == selectedProduct?.id) {
                        // Используем quantity из lastTaskProduct
                        Timber.d("$TAG: Using quantity from lastTaskProduct: ${lastTaskProduct.quantity}")
                        quantityInput = WizardUtils.formatQuantity(lastTaskProduct.quantity)
                    } else {
                        // Используем плановое количество
                        val plannedQuantity = action.storageProduct?.quantity ?: 1f
                        Timber.d("$TAG: Using planned quantity: $plannedQuantity")
                        quantityInput = WizardUtils.formatQuantity(plannedQuantity)
                    }
                }

                // Обновляем расчетные данные
                updateCalculatedValues()

                // Обновляем состояние с выбранным продуктом и текущим количеством
                updateStateFromQuantity()
                Timber.d("$TAG: View state updated with product")
            } else {
                setError("Не удалось найти товар для ввода количества")
            }
        } catch (e: Exception) {
            handleException(e, "инициализации из контекста")
        }
    }
}