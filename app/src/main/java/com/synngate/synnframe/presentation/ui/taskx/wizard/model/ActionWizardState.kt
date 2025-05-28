package com.synngate.synnframe.presentation.ui.taskx.wizard.model

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
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
    val selectedObjects: Map<String, Any> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSummary: Boolean = false,
    val showExitDialog: Boolean = false,
    val sendingFailed: Boolean = false,

    val classifierProductInfo: Product? = null,
    val isLoadingProductInfo: Boolean = false,
    val productInfoError: String? = null
) {

    fun getCurrentStep(): ActionStepTemplate? {
        return steps.getOrNull(currentStepIndex)
    }

    fun shouldShowExpirationDate(): Boolean {
        return classifierProductInfo?.accountingModel == AccountingModel.BATCH
    }

    fun getTaskProductFromClassifier(stepId: String): TaskProduct {
        val selectedProduct = selectedObjects[stepId] as? TaskProduct
        if (selectedProduct != null) {
            return selectedProduct
        }

        val classifierProduct = plannedAction?.storageProductClassifier
        if (classifierProduct != null) {
            return TaskProduct(
                id = java.util.UUID.randomUUID().toString(),
                product = classifierProduct,
                expirationDate = null,
                status = ProductStatus.STANDARD
            )
        }

        return TaskProduct(
            id = java.util.UUID.randomUUID().toString(),
            product = Product(id = "", name = ""),
            status = ProductStatus.STANDARD
        )
    }

    fun shouldShowAdditionalProps(step: ActionStepTemplate): Boolean {
        return plannedAction?.storageProductClassifier != null &&
                plannedAction.storageProduct == null &&
                step.inputAdditionalProps &&
                step.factActionField == FactActionField.STORAGE_PRODUCT
    }
}