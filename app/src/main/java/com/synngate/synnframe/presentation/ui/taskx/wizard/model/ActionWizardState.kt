package com.synngate.synnframe.presentation.ui.taskx.wizard.model

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField

data class ActionWizardState(
    val taskId: String = "",
    val actionId: String = "",
    val currentStepIndex: Int = 0,
    val steps: List<ActionStepTemplate> = emptyList(),
    val plannedAction: PlannedAction? = null,
    val factAction: FactAction? = null,
    val selectedObjects: Map<String, Any> = emptyMap(), // key: stepId, value: выбранный объект
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSummary: Boolean = false,
    val showExitDialog: Boolean = false,
    val sendingFailed: Boolean = false, // Флаг ошибки отправки на сервер

    // Новые поля для хранения полной информации о товаре из классификатора
    val classifierProductInfo: Product? = null,
    val isLoadingProductInfo: Boolean = false,
    val productInfoError: String? = null
) {
    /**
     * Возвращает текущий шаг
     */
    fun getCurrentStep(): ActionStepTemplate? {
        return steps.getOrNull(currentStepIndex)
    }

    /**
     * Проверяет, нужно ли отображать форму ввода дополнительных свойств товара
     */
    fun shouldShowAdditionalProductProps(step: ActionStepTemplate): Boolean {
        // Условия:
        // 1. В плане есть товар из классификатора
        // 2. В плане нет товара задания
        // 3. Включен признак inputAdditionalProps
        // 4. Поле фактического действия - STORAGE_PRODUCT
        return plannedAction?.storageProductClassifier != null &&
                plannedAction?.storageProduct == null &&
                step.inputAdditionalProps &&
                step.factActionField == FactActionField.STORAGE_PRODUCT
    }

    /**
     * Проверяет, нужно ли отображать поле срока годности
     */
    fun shouldShowExpirationDate(): Boolean {
        return classifierProductInfo?.accountingModel == AccountingModel.BATCH
    }

    /**
     * Получает или создает TaskProduct на основе товара из классификатора
     */
    fun getTaskProductFromClassifier(stepId: String): TaskProduct {
        // Пытаемся получить уже выбранный объект
        val selectedProduct = selectedObjects[stepId] as? TaskProduct

        // Если объект уже выбран, возвращаем его
        if (selectedProduct != null) {
            return selectedProduct
        }

        // Иначе создаем новый на основе товара из классификатора
        val classifierProduct = plannedAction?.storageProductClassifier
        if (classifierProduct != null) {
            return TaskProduct(
                id = java.util.UUID.randomUUID().toString(),
                product = classifierProduct,
                expirationDate = null,
                status = com.synngate.synnframe.domain.entity.taskx.ProductStatus.STANDARD
            )
        }

        // Если не нашли товар из классификатора, возвращаем заглушку
        return TaskProduct(
            id = java.util.UUID.randomUUID().toString(),
            product = Product(id = "", name = ""),
            status = com.synngate.synnframe.domain.entity.taskx.ProductStatus.STANDARD
        )
    }
}