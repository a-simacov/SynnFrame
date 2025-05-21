package com.synngate.synnframe.presentation.ui.wizard.action.taskproduct

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardUtils
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService
import timber.log.Timber
import java.time.LocalDateTime

class TaskProductSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val productLookupService: ProductLookupService,
    validationService: ValidationService,
) : BaseStepViewModel<TaskProduct>(step, action, context, validationService) {

    private val plannedTaskProduct = action.storageProduct

    private var _planProducts = listOfNotNull(plannedTaskProduct)
    val planProducts: List<TaskProduct>
        get() = _planProducts

    private var _planProductIds = _planProducts.map { it.product.id }.toSet()
    val planProductIds: Set<String>
        get() = _planProductIds

    private var selectedProduct: Product? = null
    private var selectedTaskProduct: TaskProduct? = null
    var selectedStatus = ProductStatus.STANDARD
        private set
    var expirationDate: LocalDateTime? = null
        private set

    var productCodeInput = ""
        private set

    var showCameraScannerDialog = false
        private set
    var showProductSelectionDialog = false
        private set

    init {
        updateProductsFromLocalDb()
    }

    override fun onResultLoadedFromContext(result: TaskProduct) {
        selectedProduct = result.product
        selectedTaskProduct = result
        selectedStatus = result.status
        expirationDate = if (result.hasExpirationDate()) result.expirationDate else null
    }

    override fun isValidType(result: Any): Boolean {
        return result is TaskProduct
    }

    // Переопределяем метод для поддержки автозаполнения
    override fun applyAutoFill(data: Any): Boolean {
        if (data is TaskProduct) {
            try {
                Timber.d("Автозаполнение товара задания: ${data.product.name}")
                selectedProduct = data.product
                selectedTaskProduct = data
                selectedStatus = data.status
                expirationDate = if (data.hasExpirationDate()) data.expirationDate else null
                updateStateFromSelectedProduct(true)
                return true
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при автозаполнении товара задания: ${e.message}")
                return false
            }
        }
        return super.applyAutoFill(data)
    }

    private fun updateProductsFromLocalDb() {
        if (_planProducts.isEmpty()) return

        executeWithErrorHandling("обновления данных товаров", showLoading = false) {
            val productIds = _planProducts.map { it.product.id }.toSet()
            if (productIds.isEmpty()) {
                return@executeWithErrorHandling
            }

            val productsFromDb = productLookupService.getProductsByIds(productIds)
            if (productsFromDb.isEmpty()) {
                return@executeWithErrorHandling
            }

            val productsMap = productsFromDb.associateBy { it.id }

            val updatedPlanProducts = _planProducts.map { taskProduct ->
                val productId = taskProduct.product.id
                val fullProduct = productsMap[productId]

                if (fullProduct != null) {
                    TaskProduct(
                        product = fullProduct,
                        status = taskProduct.status,
                        expirationDate = taskProduct.expirationDate,
                        quantity = taskProduct.quantity
                    )
                } else {
                    taskProduct
                }
            }

            _planProducts = updatedPlanProducts
            _planProductIds = updatedPlanProducts.mapNotNull { it.product.id }.toSet()

            if (selectedProduct != null && selectedTaskProduct != null) {
                val selectedId = selectedProduct?.id
                val updatedProduct = selectedId?.let { productsMap[it] }

                if (updatedProduct != null) {
                    selectedProduct = updatedProduct
                    selectedTaskProduct = selectedTaskProduct?.copy(
                        product = updatedProduct
                    )

                    updateStateFromSelectedProduct()
                }
            }
        }
    }

    override fun processBarcode(barcode: String) {
        executeWithErrorHandling("обработки штрих-кода") {
            productLookupService.processBarcode(
                barcode = barcode,
                onResult = { found, data ->
                    if (found && data is Product) {
                        if (_planProductIds.isEmpty() || _planProductIds.contains(data.id)) {
                            setSelectedProduct(data)
                            updateProductCodeInput("")
                        } else {
                            setError("Продукт не соответствует плану")
                        }
                    } else {
                        setError("Продукт со штрихкодом '$barcode' не найден")
                    }
                },
                onError = { message ->
                    setError(message)
                }
            )
        }
    }

    override fun createValidationContext(): Map<String, Any> {
        val baseContext = super.createValidationContext().toMutableMap()

        plannedTaskProduct?.let { baseContext["plannedTaskProduct"] = it }
        if (planProducts.isNotEmpty()) {
            baseContext["planProducts"] = planProducts
        }

        return baseContext
    }

    override fun validateBasicRules(data: TaskProduct?): Boolean {
        if (data == null) return false

        if (_planProductIds.isNotEmpty() && !_planProductIds.contains(data.product.id)) {
            setError("Продукт не соответствует плану")
            return false
        }

        if (data.product.accountingModel == AccountingModel.BATCH && !data.hasExpirationDate()) {
            setError("Необходимо указать срок годности для данного товара")
            return false
        }

        return true
    }

    fun createTaskProductFromState(): TaskProduct? {
        val product = selectedProduct ?: return null

        val finalStatus = if (expirationDate != null && expirationDate!!.isBefore(LocalDateTime.now())) {
            ProductStatus.EXPIRED
        } else {
            selectedStatus
        }

        return WizardUtils.createTaskProductFromProduct(
            product = product,
            status = finalStatus,
            expirationDate = expirationDate
        )
    }

    fun setSelectedProduct(product: Product) {
        executeWithErrorHandling("выбора продукта", showLoading = false) {
            selectedProduct = product

            val plannedTaskProduct = _planProducts.find { it.product.id == product.id }
            if (plannedTaskProduct != null) {
                selectedTaskProduct = plannedTaskProduct
                selectedStatus = plannedTaskProduct.status
                expirationDate = if (plannedTaskProduct.hasExpirationDate()) {
                    plannedTaskProduct.expirationDate
                } else null
            } else {
                selectedTaskProduct = WizardUtils.createTaskProductFromProduct(product = product)
                selectedStatus = ProductStatus.STANDARD
                expirationDate = null
            }

            updateStateFromSelectedProduct(true)
        }
    }

    fun setSelectedStatus(status: ProductStatus) {
        selectedStatus = status

        if (expirationDate != null && expirationDate!!.isBefore(LocalDateTime.now())
            && status != ProductStatus.EXPIRED) {
            selectedStatus = ProductStatus.EXPIRED
        }

        updateStateFromSelectedProduct()
    }

    fun setExpirationDate(date: LocalDateTime?) {
        expirationDate = date

        if (date != null && date.isBefore(LocalDateTime.now())) {
            selectedStatus = ProductStatus.EXPIRED
        }

        updateStateFromSelectedProduct()
    }

    fun selectTaskProductFromPlan(taskProduct: TaskProduct) {
        executeWithErrorHandling("выбора товара из плана") {
            val productId = taskProduct.product.id

            val existingFullProduct = planProducts.find { it.product.id == productId }?.product

            if (existingFullProduct != null && existingFullProduct.articleNumber.isNotEmpty()) {
                selectedProduct = existingFullProduct
                selectedTaskProduct = taskProduct.copy(product = existingFullProduct)
            } else {
                val fullProduct = productLookupService.getProductById(productId)

                if (fullProduct != null) {
                    selectedProduct = fullProduct
                    selectedTaskProduct = taskProduct.copy(product = fullProduct)
                } else {
                    selectedProduct = taskProduct.product
                    selectedTaskProduct = taskProduct
                }
            }

            selectedStatus = taskProduct.status
            expirationDate = if (taskProduct.hasExpirationDate()) {
                taskProduct.expirationDate
            } else null

            updateStateFromSelectedProduct(true)
        }
    }

    private fun updateStateFromSelectedProduct(checkAutoTransition: Boolean = false) {
        val taskProduct = createTaskProductFromState()
        if (taskProduct != null) {
            markObjectForSaving(ActionObjectType.TASK_PRODUCT, taskProduct)

            if (checkAutoTransition && stepFactory is AutoCompleteCapableFactory) {
                handleFieldUpdate("selectedTaskProduct", taskProduct)
            } else {
                setData(taskProduct)
            }
        }
    }

    fun updateProductCodeInput(input: String) {
        productCodeInput = input
        updateAdditionalData("productCodeInput", input)
    }

    fun searchByProductCode() {
        if (productCodeInput.isNotEmpty()) {
            processBarcode(productCodeInput)
        }
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

    fun isFormValid(): Boolean {
        val product = selectedProduct ?: return false

        return !(product.accountingModel == AccountingModel.BATCH && expirationDate == null)
    }

    fun hasPlanProducts(): Boolean {
        return _planProducts.isNotEmpty()
    }

    fun getSelectedProduct(): Product? {
        return selectedProduct
    }

    fun isSelectedProductMatchingPlan(): Boolean {
        val product = selectedProduct ?: return false
        return _planProductIds.contains(product.id)
    }
}