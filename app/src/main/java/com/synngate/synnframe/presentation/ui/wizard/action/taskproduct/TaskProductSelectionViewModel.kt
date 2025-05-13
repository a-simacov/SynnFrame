package com.synngate.synnframe.presentation.ui.wizard.action.taskproduct

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardLogger
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardUtils
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService
import java.time.LocalDateTime

/**
 * Оптимизированная ViewModel для шага выбора товара с учетными характеристиками
 */
class TaskProductSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val productLookupService: ProductLookupService,
    validationService: ValidationService,
) : BaseStepViewModel<TaskProduct>(step, action, context, validationService) {

    private val plannedTaskProduct = action.storageProduct

    // Данные для списка товаров из плана
    private var _planProducts = listOfNotNull(plannedTaskProduct)
    val planProducts: List<TaskProduct>
        get() = _planProducts

    // Набор ID товаров для быстрого поиска
    private var _planProductIds = _planProducts.mapNotNull { it.product.id }.toSet()
    val planProductIds: Set<String>
        get() = _planProductIds

    // Состояние выбранного товара
    private var selectedProduct: Product? = null
    private var selectedTaskProduct: TaskProduct? = null
    var selectedStatus = ProductStatus.STANDARD
        private set
    var expirationDate: LocalDateTime? = null
        private set

    // Состояние поля ввода
    var productCodeInput = ""
        private set

    // Состояние диалогов
    var showCameraScannerDialog = false
        private set
    var showProductSelectionDialog = false
        private set

    init {
        // Обновление товаров из локальной БД
        updateProductsFromLocalDb()

        // Логирование планового товара
        WizardLogger.logTaskProduct(TAG, plannedTaskProduct)
    }

    /**
     * Переопределяем для загрузки TaskProduct из контекста
     */
    override fun onResultLoadedFromContext(result: TaskProduct) {
        selectedProduct = result.product
        selectedTaskProduct = result
        selectedStatus = result.status
        expirationDate = if (result.hasExpirationDate()) result.expirationDate else null

        WizardLogger.logTaskProduct(TAG, result)
    }

    /**
     * Проверка типа результата
     */
    override fun isValidType(result: Any): Boolean {
        return result is TaskProduct
    }

    /**
     * Обновляет данные товаров из локальной БД
     */
    private fun updateProductsFromLocalDb() {
        if (_planProducts.isEmpty()) return

        executeWithErrorHandling("обновления данных товаров", showLoading = false) {
            // Собираем ID товаров для запроса
            val productIds = _planProducts.mapNotNull { it.product.id }.toSet()
            if (productIds.isEmpty()) {
                return@executeWithErrorHandling
            }

            // Запрашиваем полные данные о товарах из БД
            val productsFromDb = productLookupService.getProductsByIds(productIds)
            if (productsFromDb.isEmpty()) {
                WizardLogger.logStep(TAG, step.id, "Не найдены товары в локальной БД", WizardLogger.LogLevel.MINIMAL)
                return@executeWithErrorHandling
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

            WizardLogger.logStep(TAG, step.id, "Обновлено ${updatedPlanProducts.size} товаров из локальной БД")
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

    /**
     * Создает TaskProduct из текущего состояния
     */
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

    /**
     * Сохраняет результат и переходит к следующему шагу
     */
    fun saveResult() {
        val taskProduct = createTaskProductFromState()
        if (taskProduct != null) {
            completeStep(taskProduct)
        } else {
            setError("Необходимо выбрать товар")
        }
    }

    /**
     * Устанавливает выбранный продукт
     */
    fun setSelectedProduct(product: Product) {
        executeWithErrorHandling("выбора продукта", showLoading = false) {
            selectedProduct = product
            WizardLogger.logProduct(TAG, product)

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

            // Обновляем состояние и проверяем автопереход
            updateStateFromSelectedProduct(true)
        }
    }

    /**
     * Устанавливает статус продукта
     */
    fun setSelectedStatus(status: ProductStatus) {
        selectedStatus = status

        if (expirationDate != null && expirationDate!!.isBefore(LocalDateTime.now())
            && status != ProductStatus.EXPIRED) {
            selectedStatus = ProductStatus.EXPIRED
        }

        updateStateFromSelectedProduct()
    }

    /**
     * Устанавливает срок годности
     */
    fun setExpirationDate(date: LocalDateTime?) {
        expirationDate = date

        if (date != null && date.isBefore(LocalDateTime.now())) {
            selectedStatus = ProductStatus.EXPIRED
        }

        updateStateFromSelectedProduct()
    }

    /**
     * Выбирает товар из плана
     */
    fun selectTaskProductFromPlan(taskProduct: TaskProduct) {
        executeWithErrorHandling("выбора товара из плана") {
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

            // Обновляем состояние данных с проверкой автоперехода
            updateStateFromSelectedProduct(true)
        }
    }

    /**
     * Обновляет состояние на основе выбранного продукта
     * @param checkAutoTransition Проверять автопереход
     */
    private fun updateStateFromSelectedProduct(checkAutoTransition: Boolean = false) {
        val taskProduct = createTaskProductFromState()
        if (taskProduct != null) {
            if (checkAutoTransition && stepFactory is AutoCompleteCapableFactory) {
                // Проверяем автопереход если указано
                handleFieldUpdate("selectedTaskProduct", taskProduct)
            } else {
                // Просто обновляем состояние
                setData(taskProduct)
            }
        }
    }

    /**
     * Обновляет поле ввода кода продукта
     */
    fun updateProductCodeInput(input: String) {
        productCodeInput = input
        updateAdditionalData("productCodeInput", input)
    }

    /**
     * Выполняет поиск по коду продукта
     */
    fun searchByProductCode() {
        if (productCodeInput.isNotEmpty()) {
            processBarcode(productCodeInput)
        }
    }

    /**
     * Управляет видимостью диалога сканера
     */
    fun toggleCameraScannerDialog(show: Boolean) {
        showCameraScannerDialog = show
        updateAdditionalData("showCameraScannerDialog", show)
    }

    /**
     * Управляет видимостью диалога выбора продукта
     */
    fun toggleProductSelectionDialog(show: Boolean) {
        showProductSelectionDialog = show
        updateAdditionalData("showProductSelectionDialog", show)
    }

    /**
     * Скрывает диалог сканера
     */
    fun hideCameraScannerDialog() {
        toggleCameraScannerDialog(false)
    }

    /**
     * Скрывает диалог выбора продукта
     */
    fun hideProductSelectionDialog() {
        toggleProductSelectionDialog(false)
    }

    /**
     * Проверяет валидность формы
     */
    fun isFormValid(): Boolean {
        val product = selectedProduct ?: return false

        return !(product.accountingModel == AccountingModel.BATCH && expirationDate == null)
    }

    /**
     * Проверяет, просрочен ли товар
     */
    fun isExpirationDateExpired(): Boolean {
        return expirationDate != null && expirationDate!!.isBefore(LocalDateTime.now())
    }

    /**
     * Проверяет наличие товаров в плане
     */
    fun hasPlanProducts(): Boolean {
        return _planProducts.isNotEmpty()
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
     * Проверяет, выбран ли продукт
     */
    fun hasSelectedProduct(): Boolean {
        return selectedProduct != null
    }

    /**
     * Проверяет, соответствует ли выбранный продукт плану
     */
    fun isSelectedProductMatchingPlan(): Boolean {
        val product = selectedProduct ?: return false
        return _planProductIds.contains(product.id)
    }
}