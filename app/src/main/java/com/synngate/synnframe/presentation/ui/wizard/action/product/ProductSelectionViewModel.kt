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

class ProductSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val productLookupService: ProductLookupService,
    validationService: ValidationService
) : BaseStepViewModel<Product>(step, action, context, validationService) {

    private val plannedProduct = action.storageProduct?.product
    private val planProducts = listOfNotNull(action.storageProduct)
    private val planProductIds = planProducts.mapNotNull { it.product.id }.toSet()

    private var selectedProduct: Product? = null

    var barcodeInput = ""
        private set

    private var filteredProducts = emptyList<Product>()

    var showCameraScannerDialog = false
        private set
    var showProductSelectionDialog = false
        private set

    init {
        if (plannedProduct != null) {
            filteredProducts = listOf(plannedProduct)
        }

        initFromContext()
    }

    private fun initFromContext() {
        if (context.hasStepResult) {
            try {
                val result = context.getCurrentStepResult()
                if (result is Product) {
                    selectedProduct = result
                    setData(result)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка инициализации из контекста: ${e.message}")
            }
        }
    }

    override fun isValidType(result: Any): Boolean {
        return result is Product
    }

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
                                selectedProduct = data
                                setData(data)
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

    override fun createValidationContext(): Map<String, Any> {
        val baseContext = super.createValidationContext().toMutableMap()

        plannedProduct?.let { baseContext["plannedProduct"] = it }
        if (planProducts.isNotEmpty()) {
            baseContext["planProducts"] = planProducts
        }

        return baseContext
    }

    override fun validateBasicRules(data: Product?): Boolean {
        if (data == null) return false

        if (planProductIds.isNotEmpty() && !planProductIds.contains(data.id)) {
            setError("Продукт не соответствует плану")
            return false
        }

        return true
    }

    fun updateBarcodeInput(input: String) {
        barcodeInput = input
        updateAdditionalData("barcodeInput", input)
    }

    fun searchByBarcode() {
        if (barcodeInput.isNotEmpty()) {
            processBarcode(barcodeInput)
            // Не очищаем поле ввода здесь, это сделает processBarcode при успешном поиске
        }
    }

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

    fun selectProduct(product: Product) {
        selectedProduct = product
        setData(product)
        hideProductSelectionDialog()
    }

    fun selectProductFromPlan(taskProduct: TaskProduct) {
        selectedProduct = taskProduct.product
        setData(taskProduct.product)
    }

    fun toggleCameraScannerDialog(show: Boolean) {
        showCameraScannerDialog = show
        updateAdditionalData("showCameraScannerDialog", show)
    }

    fun toggleProductSelectionDialog(show: Boolean) {
        showProductSelectionDialog = show
        updateAdditionalData("showProductSelectionDialog", show)
    }

    fun hideCameraScannerDialog() {
        toggleCameraScannerDialog(false)
    }

    fun hideProductSelectionDialog() {
        toggleProductSelectionDialog(false)
    }

    fun getFilteredProducts(): List<Product> {
        return filteredProducts
    }

    fun getPlanProducts(): List<TaskProduct> {
        return planProducts
    }

    fun hasPlanProducts(): Boolean {
        return planProducts.isNotEmpty()
    }

    fun getSelectedProduct(): Product? {
        return selectedProduct
    }

    fun hasSelectedProduct(): Boolean {
        return selectedProduct != null
    }

    fun isSelectedProductMatchingPlan(): Boolean {
        val selected = selectedProduct ?: return false
        return plannedProduct != null && selected.id == plannedProduct.id
    }
}