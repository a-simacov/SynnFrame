package com.synngate.synnframe.presentation.ui.wizard.action.product

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService
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
    }

    override fun isValidType(result: Any): Boolean {
        return result is Product
    }

    override fun onResultLoadedFromContext(result: Product) {
        selectedProduct = result
    }

    override fun processBarcode(barcode: String) {
        executeWithErrorHandling("обработки штрих-кода") {
            Timber.d("ProductSelectionViewModel: начинаем обработку штрихкода: $barcode")
            productLookupService.processBarcode(
                barcode = barcode,
                onResult = { found, data ->
                    Timber.d("ProductSelectionViewModel: штрихкод $barcode обработан, found=$found, data=$data")
                    if (found && data is Product) {
                        if (planProductIds.isEmpty() || planProductIds.contains(data.id)) {
                            Timber.d("ProductSelectionViewModel: найден продукт ${data.id}")
                            selectProduct(data)
                            updateBarcodeInput("")
                        } else {
                            Timber.d("ProductSelectionViewModel: продукт не соответствует плану")
                            setError("Продукт не соответствует плану")
                        }
                    } else {
                        Timber.d("ProductSelectionViewModel: продукт не найден")
                        setError("Продукт со штрихкодом '$barcode' не найден")
                    }
                },
                onError = { message ->
                    Timber.e("ProductSelectionViewModel: ошибка при поиске продукта: $message")
                    setError(message)
                }
            )
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

    fun selectProduct(product: Product) {
        selectedProduct = product

        if (stepFactory is AutoCompleteCapableFactory) {
            handleFieldUpdate("selectedProduct", product)
        } else {
            setData(product)
        }

        hideProductSelectionDialog()
    }

    fun selectProductFromPlan(taskProduct: TaskProduct) {
        selectProduct(taskProduct.product)
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