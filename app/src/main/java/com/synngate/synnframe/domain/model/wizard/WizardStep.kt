package com.synngate.synnframe.domain.model.wizard

import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction

data class WizardStep(
    val id: String,
    val title: String,
    val action: FactLineXAction? = null,
    val content: @Composable (WizardContext) -> Unit,
    val validator: (WizardResultModel) -> Boolean = { true },
    val canNavigateBack: Boolean = true,
    val isAutoComplete: Boolean = false
)

/**
 * Контекст для компонента шага с типизированными методами
 */
data class WizardContext(
    val results: WizardResultModel,
    val onUpdate: (WizardResultModel) -> Unit,
    val onBack: () -> Unit,
    val onSkip: (Any?) -> Unit,
    val onCancel: () -> Unit
) {
    // Специализированные методы для завершения шага
    fun completeWithStorageProduct(product: TaskProduct) {
        onUpdate(results.withStorageProduct(product))
    }

    fun completeWithStoragePallet(pallet: Pallet) {
        onUpdate(results.withStoragePallet(pallet))
    }

    fun completeWithPlacementPallet(pallet: Pallet) {
        onUpdate(results.withPlacementPallet(pallet))
    }

    fun completeWithPlacementBin(bin: BinX) {
        onUpdate(results.withPlacementBin(bin))
    }

    fun completeWithWmsAction(action: WmsAction) {
        onUpdate(results.withWmsAction(action))
    }

    // Общий метод для обратной совместимости
    fun onComplete(result: Any?) {
        when (result) {
            is TaskProduct -> completeWithStorageProduct(result)
            is Pallet -> {
                // Попытка определить тип паллеты по текущим данным
                if (results.storagePallet == null) completeWithStoragePallet(result)
                else completeWithPlacementPallet(result)
            }
            is BinX -> completeWithPlacementBin(result)
            is WmsAction -> completeWithWmsAction(result)
            null -> onBack()
            else -> {
                // Для других типов данных используем общий механизм
                val updated = results.copy()
                updated.additionalData["generic_result"] = result
                onUpdate(updated)
            }
        }
    }
}