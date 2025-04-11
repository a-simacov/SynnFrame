package com.synngate.synnframe.domain.model.wizard

import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction

/**
 * Модель шага визарда
 */
data class WizardStep(
    val id: String,
    val title: String,
    val action: FactLineXAction? = null,
    val content: @Composable (WizardContext) -> Unit,
    val validator: (Map<String, Any?>) -> Boolean = { true },
    val canNavigateBack: Boolean = true,
    val isAutoComplete: Boolean = false
)

/**
 * Контекст для компонента шага
 */
data class WizardContext(
    val results: WizardResultModel,
    val stepResults: Map<String, Any?>, // Для обратной совместимости
    // Методы обработки результатов
    val onComplete: (Any?) -> Unit,
    val onBack: () -> Unit,
    val onSkip: (Any?) -> Unit,
    val onCancel: () -> Unit
) {
    // Удобные методы для обновления конкретных полей
    fun completeWithStorageProduct(product: TaskProduct) {
        onComplete(product)
    }

    fun completeWithStoragePallet(pallet: Pallet) {
        onComplete(pallet)
    }

    fun completeWithPlacementPallet(pallet: Pallet) {
        onComplete(pallet)
    }

    fun completeWithPlacementBin(bin: BinX) {
        onComplete(bin)
    }

    fun completeWithWmsAction(action: WmsAction) {
        onComplete(action)
    }

    // Общий метод для обратной совместимости
    fun completeWithResult(result: Any?) {
        onComplete(result)
    }
}