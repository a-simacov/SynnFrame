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

/**
 * ViewModel для шага выбора TaskProduct (товара с количеством и другими характеристиками)
 */
class TaskProductSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val productLookupService: ProductLookupService,
    validationService: ValidationService
) : BaseStepViewModel<TaskProduct>(step, action, context, validationService) {

    // Данные о продуктах из плана
    private val plannedTaskProduct = action.storageProduct
    private val planProducts = listOfNotNull(plannedTaskProduct)
    private val planProductIds = planProducts.mapNotNull { it.product.id }.toSet()

    // Состояние выбора продукта
    private var selectedProduct: Product? = null
    var selectedStatus = ProductStatus.STANDARD
        private set
    var expirationDate: LocalDateTime? = null
        private set

    // Состояние поля ввода штрих-кода
    var productCodeInput = ""
        private set

    // Состояние диалогов
    var showCameraScannerDialog = false
        private set
    var showProductSelectionDialog = false
        private set

    init {
        // Если есть запланированный продукт, устанавливаем его в состояние
        if (plannedTaskProduct != null) {
            selectedProduct = plannedTaskProduct.product
            selectedStatus = plannedTaskProduct.status
            if (plannedTaskProduct.hasExpirationDate()) {
                expirationDate = plannedTaskProduct.expirationDate
            }
        }

        // Обновляем состояние с данными продукта из плана
        updateStateFromSelectedProduct()
    }

    /**
     * Проверка типа результата
     */
    override fun isValidType(result: Any): Boolean {
        return result is TaskProduct
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
                                setSelectedProduct(data)
                                // Очищаем поле ввода
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

    /**
     * Создаем расширенный контекст для валидации API
     */
    override fun createValidationContext(): Map<String, Any> {
        val baseContext = super.createValidationContext().toMutableMap()

        // Добавляем планируемый продукт и список продуктов плана для валидации
        plannedTaskProduct?.let { baseContext["plannedTaskProduct"] = it }
        if (planProducts.isNotEmpty()) {
            baseContext["planProducts"] = planProducts
        }

        return baseContext
    }

    /**
     * Валидация данных
     */
    override fun validateBasicRules(data: TaskProduct?): Boolean {
        if (data == null) return false

        // Если есть ограничение по плану, проверяем соответствие
        if (planProductIds.isNotEmpty() && !planProductIds.contains(data.product.id)) {
            setError("Продукт не соответствует плану")
            return false
        }

        // Проверяем наличие срока годности для продуктов с учетом по партиям
        if (data.product.accountingModel == AccountingModel.BATCH && !data.hasExpirationDate()) {
            setError("Необходимо указать срок годности для данного товара")
            return false
        }

        return true
    }

    /**
     * Формирует объект TaskProduct из текущего выбранного продукта
     */
    fun createTaskProductFromState(): TaskProduct? {
        val product = selectedProduct ?: return null

        // Определяем статус на основе срока годности
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

    /**
     * Сохраняем результат шага (TaskProduct)
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
        selectedProduct = product

        // Если это продукт из плана, устанавливаем данные из планового TaskProduct
        val plannedTaskProduct = planProducts.find { it.product.id == product.id }
        if (plannedTaskProduct != null) {
            selectedStatus = plannedTaskProduct.status
            expirationDate = if (plannedTaskProduct.hasExpirationDate()) {
                plannedTaskProduct.expirationDate
            } else null
        } else {
            // По умолчанию статус "Стандарт" и нет срока годности
            selectedStatus = ProductStatus.STANDARD
            expirationDate = null
        }

        // Обновляем состояние с данными выбранного продукта
        updateStateFromSelectedProduct()
    }

    /**
     * Устанавливает статус продукта
     */
    fun setSelectedStatus(status: ProductStatus) {
        selectedStatus = status

        // Если срок годности истек, автоматически устанавливаем статус "Просрочен"
        if (expirationDate != null && expirationDate!!.isBefore(LocalDateTime.now())
            && status != ProductStatus.EXPIRED) {
            selectedStatus = ProductStatus.EXPIRED
        }

        // Обновляем состояние с новым статусом
        updateStateFromSelectedProduct()
    }

    /**
     * Устанавливает срок годности
     */
    fun setExpirationDate(date: LocalDateTime?) {
        expirationDate = date

        // Если срок годности истек, автоматически устанавливаем статус "Просрочен"
        if (date != null && date.isBefore(LocalDateTime.now())) {
            selectedStatus = ProductStatus.EXPIRED
        }

        // Обновляем состояние с новой датой
        updateStateFromSelectedProduct()
    }

    /**
     * Выбирает продукт задания из плана
     */
    fun selectTaskProductFromPlan(taskProduct: TaskProduct) {
        selectedProduct = taskProduct.product
        selectedStatus = taskProduct.status
        expirationDate = if (taskProduct.hasExpirationDate()) {
            taskProduct.expirationDate
        } else null

        // Обновляем состояние
        updateStateFromSelectedProduct()
    }

    /**
     * Обновление состояния на основе выбранного продукта
     */
    private fun updateStateFromSelectedProduct() {
        val taskProduct = createTaskProductFromState()
        if (taskProduct != null) {
            setData(taskProduct)
        }
    }

    /**
     * Обновление ввода кода продукта
     */
    fun updateProductCodeInput(input: String) {
        productCodeInput = input
    }

    /**
     * Выполнение поиска по коду продукта
     */
    fun searchByProductCode() {
        if (productCodeInput.isNotEmpty()) {
            processBarcode(productCodeInput)
            // Не очищаем поле ввода здесь, это сделает processBarcode при успешном поиске
        }
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
     * Проверяет, выбран ли продукт и указан ли срок годности (если требуется)
     */
    fun isFormValid(): Boolean {
        val product = selectedProduct ?: return false

        // Проверяем наличие срока годности для продуктов с учетом по партиям
        return !(product.accountingModel == AccountingModel.BATCH && expirationDate == null)
    }

    /**
     * Проверяет, истек ли срок годности
     */
    fun isExpirationDateExpired(): Boolean {
        return expirationDate != null && expirationDate!!.isBefore(LocalDateTime.now())
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