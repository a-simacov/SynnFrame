// app/src/main/java/com/synngate/synnframe/presentation/ui/wizard/action/taskproduct/TaskProductSelectionViewModel.kt
package com.synngate.synnframe.presentation.ui.wizard.action.taskproduct

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime

class TaskProductSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val productLookupService: ProductLookupService,
    validationService: ValidationService
) : BaseStepViewModel<TaskProduct>(step, action, context, validationService) {

    private val plannedTaskProduct = action.storageProduct
    private val planProducts = listOfNotNull(plannedTaskProduct)
    private val planProductIds = planProducts.mapNotNull { it.product.id }.toSet()

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
        initFromContext()
    }

    private fun initFromContext() {
        if (context.hasStepResult) {
            try {
                val result = context.getCurrentStepResult()
                if (result is TaskProduct) {
                    selectedProduct = result.product
                    selectedTaskProduct = result
                    selectedStatus = result.status
                    expirationDate = if (result.hasExpirationDate()) result.expirationDate else null

                    setData(result)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка инициализации из контекста: ${e.message}")
            }
        }
    }

    override fun isValidType(result: Any): Boolean {
        return result is TaskProduct
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
                                setSelectedProduct(data)
                                updateProductCodeInput("")
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

    // Создаем расширенный контекст для валидации API
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

        if (planProductIds.isNotEmpty() && !planProductIds.contains(data.product.id)) {
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

        return TaskProduct(
            product = product,
            status = finalStatus,
            expirationDate = expirationDate ?: LocalDateTime.of(1970, 1, 1, 0, 0)
        )
    }

    fun saveResult() {
        val taskProduct = createTaskProductFromState()
        if (taskProduct != null) {
            completeStep(taskProduct)
        } else {
            setError("Необходимо выбрать товар")
        }
    }

    fun setSelectedProduct(product: Product) {
        selectedProduct = product

        val plannedTaskProduct = planProducts.find { it.product.id == product.id }
        if (plannedTaskProduct != null) {
            selectedTaskProduct = plannedTaskProduct
            selectedStatus = plannedTaskProduct.status
            expirationDate = if (plannedTaskProduct.hasExpirationDate()) {
                plannedTaskProduct.expirationDate
            } else null
        } else {
            selectedTaskProduct = TaskProduct(product = product)
            selectedStatus = ProductStatus.STANDARD
            expirationDate = null
        }

        updateStateFromSelectedProduct()
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
        setLoading(true)
        setError(null)

        viewModelScope.launch {
            try {
                // Получаем ID продукта из TaskProduct плана
                val productId = taskProduct.product.id

                // Запрашиваем полную информацию о продукте из базы данных
                val fullProduct = productLookupService.getProductById(productId)

                if (fullProduct != null) {
                    // Если нашли полную информацию, используем её
                    selectedProduct = fullProduct

                    // Создаем TaskProduct с полными данными о товаре, но сохраняем
                    // статус, срок годности и т.д. из планового TaskProduct
                    selectedTaskProduct = TaskProduct(
                        product = fullProduct,
                        status = taskProduct.status,
                        expirationDate = if (taskProduct.hasExpirationDate()) {
                            taskProduct.expirationDate
                        } else {
                            LocalDateTime.of(1970, 1, 1, 0, 0)
                        },
                        quantity = taskProduct.quantity
                    )

                    // Обновляем состояние UI
                    selectedStatus = taskProduct.status
                    expirationDate = if (taskProduct.hasExpirationDate()) {
                        taskProduct.expirationDate
                    } else null

                    Timber.d("Продукт из плана загружен с полными данными из БД: ${fullProduct.id}")
                } else {
                    // Если не нашли полную информацию, используем данные из планового TaskProduct
                    // как резервный вариант
                    Timber.w("Не удалось найти полную информацию о продукте: $productId, используем данные из плана")
                    selectedProduct = taskProduct.product
                    selectedTaskProduct = taskProduct
                    selectedStatus = taskProduct.status
                    expirationDate = if (taskProduct.hasExpirationDate()) {
                        taskProduct.expirationDate
                    } else null
                }

                // Обновляем состояние с данными выбранного продукта
                updateStateFromSelectedProduct()
                setLoading(false)
            } catch (e: Exception) {
                // В случае ошибки используем данные из планового TaskProduct
                Timber.e(e, "Ошибка при загрузке полной информации о продукте: ${e.message}")
                selectedProduct = taskProduct.product
                selectedTaskProduct = taskProduct
                selectedStatus = taskProduct.status
                expirationDate = if (taskProduct.hasExpirationDate()) {
                    taskProduct.expirationDate
                } else null

                updateStateFromSelectedProduct()
                setLoading(false)
                setError("Ошибка при загрузке данных о товаре: ${e.message}")
            }
        }
    }

    private fun updateStateFromSelectedProduct() {
        val taskProduct = createTaskProductFromState()
        if (taskProduct != null) {
            setData(taskProduct)
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

    fun isExpirationDateExpired(): Boolean {
        return expirationDate != null && expirationDate!!.isBefore(LocalDateTime.now())
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

    fun getSelectedTaskProduct(): TaskProduct? {
        return selectedTaskProduct
    }

    fun hasSelectedProduct(): Boolean {
        return selectedProduct != null
    }

    fun isSelectedProductMatchingPlan(): Boolean {
        val product = selectedProduct ?: return false
        return planProductIds.contains(product.id)
    }
}