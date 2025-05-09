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

    // Изменяемый список продуктов плана
    private var _planProducts = listOfNotNull(plannedTaskProduct)
    val planProducts: List<TaskProduct>
        get() = _planProducts

    // Изменяемый набор ID продуктов плана
    private var _planProductIds = _planProducts.mapNotNull { it.product.id }.toSet()
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
        // Инициализация из контекста
        initFromContext()

        // Обновление товаров из локальной БД
        updateProductsFromLocalDb()
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

    /**
     * Обновляет данные товаров из локальной БД
     */
    private fun updateProductsFromLocalDb() {
        if (_planProducts.isEmpty()) return

        viewModelScope.launch {
            try {
                setLoading(true)

                // Собираем ID товаров для запроса
                val productIds = _planProducts.mapNotNull { it.product.id }.toSet()

                if (productIds.isEmpty()) {
                    setLoading(false)
                    return@launch
                }

                // Запрашиваем полные данные о товарах из БД
                val productsFromDb = productLookupService.getProductsByIds(productIds)

                if (productsFromDb.isEmpty()) {
                    Timber.w("Не найдено товаров в локальной БД")
                    setLoading(false)
                    return@launch
                }

                // Создаем мапу для быстрого доступа к продуктам
                val productsMap = productsFromDb.associateBy { it.id }

                // Создаем обновленные TaskProduct с полными данными о товарах
                val updatedPlanProducts = _planProducts.map { taskProduct ->
                    val productId = taskProduct.product.id
                    val fullProduct = productsMap[productId]

                    if (fullProduct != null) {
                        // Если нашли полную информацию, создаем новый TaskProduct
                        TaskProduct(
                            product = fullProduct,
                            status = taskProduct.status,
                            expirationDate = taskProduct.expirationDate,
                            quantity = taskProduct.quantity
                        )
                    } else {
                        // Иначе оставляем как есть
                        taskProduct
                    }
                }

                // Обновляем списки
                _planProducts = updatedPlanProducts
                _planProductIds = updatedPlanProducts.mapNotNull { it.product.id }.toSet()

                // Если уже выбран продукт, обновляем и его
                if (selectedProduct != null && selectedTaskProduct != null) {
                    val selectedId = selectedProduct?.id
                    val updatedProduct = selectedId?.let { productsMap[it] }

                    if (updatedProduct != null) {
                        selectedProduct = updatedProduct
                        selectedTaskProduct = selectedTaskProduct?.copy(
                            product = updatedProduct
                        )

                        // Обновляем состояние
                        updateStateFromSelectedProduct()
                    }
                }

                Timber.d("Обновлены данные ${updatedPlanProducts.size} товаров из локальной БД")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обновлении данных товаров из локальной БД: ${e.message}")
            } finally {
                setLoading(false)
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
                            if (_planProductIds.isEmpty() || _planProductIds.contains(data.id)) {
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

        val plannedTaskProduct = _planProducts.find { it.product.id == product.id }
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

                // Проверяем, есть ли уже полная информация о продукте
                val existingFullProduct = planProducts.find { it.product.id == productId }?.product

                // Если в планируемом товаре уже есть полная информация о продукте
                if (existingFullProduct != null && existingFullProduct.articleNumber.isNotEmpty()) {
                    // Используем её
                    selectedProduct = existingFullProduct
                    selectedTaskProduct = taskProduct.copy(product = existingFullProduct)
                } else {
                    // Иначе, запрашиваем полную информацию из БД
                    val fullProduct = productLookupService.getProductById(productId)

                    if (fullProduct != null) {
                        // Если нашли, обновляем продукт в текущей позиции товара задания
                        selectedProduct = fullProduct
                        selectedTaskProduct = taskProduct.copy(product = fullProduct)
                    } else {
                        // Используем то, что есть
                        selectedProduct = taskProduct.product
                        selectedTaskProduct = taskProduct
                    }
                }

                // Обновляем состояние UI
                selectedStatus = taskProduct.status
                expirationDate = if (taskProduct.hasExpirationDate()) {
                    taskProduct.expirationDate
                } else null

                // Обновляем состояние данных
                updateStateFromSelectedProduct()
                setLoading(false)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при выборе товара из плана: ${e.message}")

                // В случае ошибки используем имеющиеся данные
                selectedProduct = taskProduct.product
                selectedTaskProduct = taskProduct
                selectedStatus = taskProduct.status
                expirationDate = if (taskProduct.hasExpirationDate()) {
                    taskProduct.expirationDate
                } else null

                updateStateFromSelectedProduct()
                setLoading(false)
                setError("Ошибка при загрузке полных данных о товаре")
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

    fun hasPlanProducts(): Boolean {
        return _planProducts.isNotEmpty()
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
        return _planProductIds.contains(product.id)
    }
}