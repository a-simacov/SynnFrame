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
    val productInfoError: String? = null,

    // Поля для работы с буфером
    val bufferObjectSources: Map<String, String> = emptyMap(), // stepId -> source
    val lockedObjectSteps: Set<String> = emptySet(),  // stepId заблокированных объектов (ALWAYS)

    // Новые поля для запроса объекта с сервера
    val isRequestingServerObject: Boolean = false,
    val serverRequestCancellationToken: String? = null,

    val lastCommandResultData: Map<String, String> = emptyMap(),
    val showResultDialog: Boolean = false,
    val resultDialogTitle: String = "",
    val resultDialogContent: List<Pair<String, String>> = emptyList()
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

    // Вспомогательные функции для буфера

    /**
     * Проверяет, заблокирован ли текущий шаг буфером (режим ALWAYS)
     */
    fun isCurrentStepLockedByBuffer(): Boolean {
        val currentStep = getCurrentStep() ?: return false
        return lockedObjectSteps.contains(currentStep.id)
    }

    /**
     * Возвращает источник данных из буфера для текущего шага
     */
    fun getBufferSourceForCurrentStep(): String? {
        val currentStep = getCurrentStep() ?: return null
        return bufferObjectSources[currentStep.id]
    }

    /**
     * Проверяет, нужно ли использовать серверный запрос для текущего шага
     */
    fun shouldUseServerRequest(): Boolean {
        val currentStep = getCurrentStep() ?: return false
        return currentStep.serverSelectionEndpoint.isNotEmpty()
    }
}