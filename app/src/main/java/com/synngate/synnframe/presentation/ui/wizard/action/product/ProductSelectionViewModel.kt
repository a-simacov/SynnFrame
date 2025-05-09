package com.synngate.synnframe.presentation.ui.wizard.action.product

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel для шага выбора продукта
 */
class ProductSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val productLookupService: ProductLookupService,
    validationService: ValidationService
) : BaseStepViewModel<Product>(step, action, context, validationService) {

    // Данные о продуктах из плана
    private val plannedProduct = action.storageProduct?.product
    private val planProducts = listOfNotNull(action.storageProduct)
    private val planProductIds = planProducts.mapNotNull { it.product.id }.toSet()

    // Состояние поля ввода штрих-кода
    var barcodeInput = ""
        private set

    // Состояние списка продуктов для выбора
    private var filteredProducts = emptyList<Product>()

    // Состояние диалогов
    var showCameraScannerDialog = false
        private set
    var showProductSelectionDialog = false
        private set

    init {
        if (plannedProduct != null) {
            // Если есть запланированный продукт, добавляем его в filteredProducts
            filteredProducts = listOf(plannedProduct)
        }
    }

    /**
     * Проверка типа результата
     */
    override fun isValidType(result: Any): Boolean {
        return result is Product
    }

    /**
     * Обработка штрих-кода
     */
    override fun processBarcode(barcode: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                setError(null)

                productLookupService.processBarcode(
                    barcode = barcode,
                    onResult = { found, data ->
                        if (found && data is Product) {
                            if (planProductIds.isEmpty() || planProductIds.contains(data.id)) {
                                setData(data)
                                // Очищаем поле ввода
                                updateBarcodeInput("")
                            } else {
                                setError("Продукт не соответствует плану")
                            }
                        } else {
                            setError("Продукт со штрихкодом '$barcode' не найден")
                        }
                        setLoading(false)
                    },
                    onError = { message ->
                        setError(message)
                        setLoading(false)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке штрихкода: $barcode")
                setError("Ошибка: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * Создаем расширенный контекст для валидации API
     */
    override fun createValidationContext(): Map<String, Any> {
        val baseContext = super.createValidationContext().toMutableMap()

        // Добавляем планируемый продукт и список продуктов плана для валидации
        // Используем safe call и проверку списка, чтобы избежать добавления null-значений
        plannedProduct?.let { baseContext["plannedProduct"] = it }
        if (planProducts.isNotEmpty()) {
            baseContext["planProducts"] = planProducts
        }

        return baseContext
    }

    /**
     * Валидация данных
     */
    override fun validateBasicRules(data: Product?): Boolean {
        if (data == null) return false

        // Если есть ограничение по плану, проверяем соответствие
        if (planProductIds.isNotEmpty() && !planProductIds.contains(data.id)) {
            setError("Продукт не соответствует плану")
            return false
        }

        return true
    }

    /**
     * Обновление ввода штрих-кода
     */
    fun updateBarcodeInput(input: String) {
        barcodeInput = input
    }

    /**
     * Выполнение поиска по штрих-коду
     */
    fun searchByBarcode() {
        if (barcodeInput.isNotEmpty()) {
            processBarcode(barcodeInput)
            // Не очищаем поле ввода здесь, это сделает processBarcode при успешном поиске
        }
    }

    /**
     * Поиск продуктов по тексту
     */
    fun searchProducts(query: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val products = productLookupService.searchProducts(query)

                // Если есть ограничения по плану, фильтруем результаты
                filteredProducts = if (planProductIds.isNotEmpty()) {
                    products.filter { planProductIds.contains(it.id) }
                } else {
                    products
                }

                setLoading(false)
                // Обновляем список в состоянии
                updateAdditionalData("filteredProducts", filteredProducts)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске продуктов: ${e.message}")
                setError("Ошибка поиска: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * Выбор продукта из списка
     */
    fun selectProduct(product: Product) {
        setData(product)
        hideProductSelectionDialog()
    }

    /**
     * Выбор продукта из плана
     */
    fun selectProductFromPlan(taskProduct: TaskProduct) {
        setData(taskProduct.product)
    }

    /**
     * Управление видимостью диалога сканера
     */
    fun toggleCameraScannerDialog(show: Boolean) {
        showCameraScannerDialog = show
        updateAdditionalData("showCameraScannerDialog", show)
    }

    /**
     * Управление видимостью диалога выбора продукта
     */
    fun toggleProductSelectionDialog(show: Boolean) {
        showProductSelectionDialog = show
        updateAdditionalData("showProductSelectionDialog", show)
    }

    /**
     * Скрытие диалога сканера
     */
    fun hideCameraScannerDialog() {
        toggleCameraScannerDialog(false)
    }

    /**
     * Скрытие диалога выбора продукта
     */
    fun hideProductSelectionDialog() {
        toggleProductSelectionDialog(false)
    }

    /**
     * Получение отфильтрованных продуктов
     */
    fun getFilteredProducts(): List<Product> {
        return filteredProducts
    }

    /**
     * Получение запланированных продуктов
     */
    fun getPlanProducts(): List<TaskProduct> {
        return planProducts
    }

    /**
     * Проверка наличия запланированных продуктов
     */
    fun hasPlanProducts(): Boolean {
        return planProducts.isNotEmpty()
    }
}