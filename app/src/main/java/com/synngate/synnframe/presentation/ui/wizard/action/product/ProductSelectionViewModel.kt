package com.synngate.synnframe.presentation.ui.wizard.action.product

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Обновленная ViewModel для шага выбора продукта
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

    // Состояние выбранного продукта
    private var selectedProduct: Product? = null

    // Состояние поля ввода
    var barcodeInput = ""
        private set

    // Состояние списка продуктов
    private var filteredProducts = emptyList<Product>()

    // Состояние диалогов
    var showCameraScannerDialog = false
        private set
    var showProductSelectionDialog = false
        private set

    init {
        // Инициализация списка продуктов
        if (plannedProduct != null) {
            filteredProducts = listOf(plannedProduct)
        }

        // Инициализация из контекста
        initFromContext()
    }

    /**
     * Инициализация из данных контекста
     */
    private fun initFromContext() {
        if (context.hasStepResult) {
            try {
                val result = context.getCurrentStepResult()
                if (result is Product) {
                    selectedProduct = result
                    setData(result)
                    Timber.d("Initialized from context: product=${result.name}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing from context: ${e.message}")
            }
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
                                selectProduct(data)
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
                Timber.e(e, "Error processing barcode: $barcode")
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
        updateAdditionalData("barcodeInput", input)
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
     * Поиск продуктов по запросу
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
                Timber.e(e, "Error searching products: ${e.message}")
                setError("Ошибка поиска: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * Выбор продукта с поддержкой автоперехода
     */
    fun selectProduct(product: Product) {
        selectedProduct = product

        // Обновляем состояние и проверяем автопереход
        if (stepFactory is AutoCompleteCapableFactory) {
            handleFieldUpdate("selectedProduct", product)
        } else {
            setData(product)
        }

        hideProductSelectionDialog()
    }

    /**
     * Выбор продукта из плана
     */
    fun selectProductFromPlan(taskProduct: TaskProduct) {
        selectProduct(taskProduct.product)
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
     * Получение продуктов из плана
     */
    fun getPlanProducts(): List<TaskProduct> {
        return planProducts
    }

    /**
     * Проверка наличия продуктов в плане
     */
    fun hasPlanProducts(): Boolean {
        return planProducts.isNotEmpty()
    }

    /**
     * Получение выбранного продукта
     */
    fun getSelectedProduct(): Product? {
        return selectedProduct
    }

    /**
     * Проверка, выбран ли продукт
     */
    fun hasSelectedProduct(): Boolean {
        return selectedProduct != null
    }

    /**
     * Проверяет, соответствует ли выбранный продукт плану
     */
    fun isSelectedProductMatchingPlan(): Boolean {
        val selected = selectedProduct ?: return false
        return plannedProduct != null && selected.id == plannedProduct.id
    }
}