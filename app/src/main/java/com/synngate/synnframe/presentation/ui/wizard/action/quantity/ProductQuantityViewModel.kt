package com.synngate.synnframe.presentation.ui.wizard.action.quantity

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardUtils

class ProductQuantityViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    validationService: ValidationService,
    stepFactory: ActionStepFactory? = null
) : BaseStepViewModel<TaskProduct>(step, action, context, validationService, stepFactory) {

    private var selectedProduct: Product? = null
    private var selectedTaskProduct: TaskProduct? = null

    var quantityInput = ""
        private set

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
        initFromPreviousStep()
    }

    override fun isValidType(result: Any): Boolean {
        return result is TaskProduct
    }

    override fun processBarcode(barcode: String) {
        // Для этого типа шага обработка штрих-кода не требуется
    }

    private fun initFromPreviousStep() {
        executeWithErrorHandling("загрузки данных из предыдущего шага") {
            selectedProduct = WizardUtils.findProduct(context.results)
            selectedTaskProduct = WizardUtils.findTaskProduct(context.results)

            if (selectedProduct == null) {
                val actionProduct = action.storageProduct
                if (actionProduct != null) {
                    selectedProduct = actionProduct.product
                    selectedTaskProduct = actionProduct
                }
            }

            if (selectedProduct != null) {
                if (context.hasStepResult) {
                    val currentResult = context.getCurrentStepResult()
                    if (currentResult is TaskProduct && currentResult.quantity > 0) {
                        quantityInput = WizardUtils.formatQuantity(currentResult.quantity)
                    }
                }

                updateCalculatedValues()

                updateStateFromQuantity()
            } else {
                setError("Не удалось найти товар для ввода количества")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getRelatedFactActions(): List<FactAction> {
        try {
            val factActionsInfo = context.results["factActions"] as? Map<*, *> ?: emptyMap<String, Any>()
            return (factActionsInfo[action.id] as? List<FactAction>) ?: emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun updateCalculatedValues() {
        if (selectedProduct == null) return

        val plannedProduct = action.storageProduct
        if (plannedProduct != null) {
            plannedQuantity = if (plannedProduct.product.id == selectedProduct?.id) {
                WizardUtils.roundToThreeDecimals(plannedProduct.quantity ?: 0f)
            } else {
                0f
            }
        }

        val relatedFactActions = getRelatedFactActions()
        completedQuantity = WizardUtils.roundToThreeDecimals(
            relatedFactActions.sumOf {
                it.storageProduct?.quantity?.toDouble() ?: 0.0
            }.toFloat()
        )

        currentInputQuantity = WizardUtils.roundToThreeDecimals(WizardUtils.parseQuantityInput(quantityInput))

        projectedTotalQuantity = WizardUtils.roundToThreeDecimals(completedQuantity + currentInputQuantity)

        remainingQuantity = WizardUtils.roundToThreeDecimals((plannedQuantity - projectedTotalQuantity).coerceAtLeast(0f))

        willExceedPlan = plannedQuantity > 0f && projectedTotalQuantity > plannedQuantity
    }

    private fun updateStateFromQuantity() {
        if (selectedProduct == null) return

        val quantityValue = WizardUtils.parseQuantityInput(quantityInput)

        val updatedTaskProduct = selectedTaskProduct?.copy(quantity = WizardUtils.roundToThreeDecimals(quantityValue))
            ?: WizardUtils.createTaskProductFromProduct(
                product = selectedProduct!!,
                quantity = WizardUtils.roundToThreeDecimals(quantityValue)
            )

        selectedTaskProduct = updatedTaskProduct

        setData(updatedTaskProduct)

        updateCalculatedValues()
    }

    override fun validateBasicRules(data: TaskProduct?): Boolean {
        if (data == null) return false

        if (data.quantity <= 0f) {
            setError("Количество должно быть больше нуля")
            return false
        }

        return true
    }

    fun updateQuantityInput(input: String) {
        val processedValue = WizardUtils.processQuantityInput(input)
        quantityInput = processedValue

        updateCalculatedValues()
        updateStateFromQuantity()

        setError(null)
    }

    fun incrementQuantity() {
        val currentValue = WizardUtils.parseQuantityInput(quantityInput)
        val newValue = currentValue + 1f
        updateQuantityInput(WizardUtils.formatQuantity(newValue))
    }

    fun decrementQuantity() {
        val currentValue = WizardUtils.parseQuantityInput(quantityInput)
        val newValue = (currentValue - 1f).coerceAtLeast(0f)
        updateQuantityInput(WizardUtils.formatQuantity(newValue))
    }

    fun saveResult() {
        executeWithErrorHandling("сохранения результата") {
            if (selectedProduct == null) {
                setError("Сначала необходимо выбрать товар")
                return@executeWithErrorHandling
            }

            val quantityValue = WizardUtils.parseQuantityInput(quantityInput)

            if (quantityValue <= 0f) {
                setError("Количество должно быть больше нуля")
                return@executeWithErrorHandling
            }

            val finalQuantity = WizardUtils.roundToThreeDecimals(quantityValue)

            val currentStatus = selectedTaskProduct?.status ?: ProductStatus.STANDARD
            val currentExpirationDate = selectedTaskProduct?.expirationDate

            val updatedTaskProduct = WizardUtils.createTaskProductFromProduct(
                product = selectedProduct!!,
                quantity = finalQuantity,
                status = currentStatus,
                expirationDate = currentExpirationDate
            )

            selectedTaskProduct = updatedTaskProduct

            completeStep(updatedTaskProduct)
        }
    }

    fun hasSelectedProduct(): Boolean {
        return selectedProduct != null
    }

    fun getSelectedProduct(): Product? {
        return selectedProduct
    }

    fun getSelectedTaskProduct(): TaskProduct? {
        return selectedTaskProduct
    }

    override fun initStateFromContext() {
        try {
            selectedProduct = WizardUtils.findProduct(context.results)
            selectedTaskProduct = WizardUtils.findTaskProduct(context.results)

            if (selectedProduct == null) {
                val actionProduct = action.storageProduct
                if (actionProduct != null) {
                    selectedProduct = actionProduct.product
                    selectedTaskProduct = actionProduct
                }
            }

            if (selectedProduct != null) {
                if (context.hasStepResult) {
                    val currentResult = context.getCurrentStepResult()

                    if (currentResult is TaskProduct) {
                        if (currentResult.product.id == selectedProduct?.id) {
                            quantityInput = WizardUtils.formatQuantity(currentResult.quantity)
                            selectedTaskProduct = currentResult
                        } else {
                            val plannedQuantity = action.storageProduct?.quantity ?: 1f
                            quantityInput = WizardUtils.formatQuantity(plannedQuantity)
                        }
                    } else {
                        quantityInput = WizardUtils.formatQuantity(selectedTaskProduct?.quantity ?: 1f)
                    }
                } else {
                    val lastTaskProduct = context.results["lastTaskProduct"] as? TaskProduct

                    if (lastTaskProduct != null && lastTaskProduct.product.id == selectedProduct?.id) {
                        quantityInput = WizardUtils.formatQuantity(lastTaskProduct.quantity)
                    } else {
                        val plannedQuantity = action.storageProduct?.quantity ?: 1f
                        quantityInput = WizardUtils.formatQuantity(plannedQuantity)
                    }
                }

                updateCalculatedValues()

                updateStateFromQuantity()
            } else {
                setError("Не удалось найти товар для ввода количества")
            }
        } catch (e: Exception) {
            handleException(e, "инициализации из контекста")
        }
    }
}