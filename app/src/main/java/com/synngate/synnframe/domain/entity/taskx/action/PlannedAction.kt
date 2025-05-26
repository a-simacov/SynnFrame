package com.synngate.synnframe.domain.entity.taskx.action

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.presentation.ui.taskx.enums.ActionCompletionCondition
import com.synngate.synnframe.presentation.ui.taskx.enums.CompletionOrderType
import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class PlannedAction(
    val id: String,
    val order: Int,
    val actionTemplateId: String,
    val actionTemplate: ActionTemplate? = null,
    val completionOrderType: CompletionOrderType,
    val storageProductClassifier: Product? = null,
    val storageProduct: TaskProduct? = null,
    val storagePallet: Pallet? = null,
    val storageBin: BinX? = null,
    val quantity: Float = 0f,
    val placementPallet: Pallet? = null,
    val placementBin: BinX? = null,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val manuallyCompleted: Boolean = false,
    @Serializable(with = LocalDateTimeSerializer::class)
    val manuallyCompletedAt: LocalDateTime? = null
) {
    fun isInitialAction(): Boolean = completionOrderType == CompletionOrderType.INITIAL
    fun isFinalAction(): Boolean = completionOrderType == CompletionOrderType.FINAL
    fun isRegularAction(): Boolean = completionOrderType == CompletionOrderType.REGULAR

    fun getWmsAction(): WmsAction = actionTemplate?.wmsAction ?: WmsAction.PUT_INTO

    fun canHaveMultipleFactActions(): Boolean =
        actionTemplate?.allowMultipleFactActions == true && quantity > 0f

    fun getPlannedQuantity(): Float = quantity

    fun getCompletedQuantity(factActions: List<FactAction>): Float {
        val relatedFacts = factActions.filter { it.plannedActionId == id }
        return relatedFacts.sumOf { it.quantity.toDouble() }.toFloat()
    }

    fun getRemainingQuantity(factActions: List<FactAction>): Float {
        if (quantity <= 0f || manuallyCompleted) return 0f
        val completedQuantity = getCompletedQuantity(factActions)
        return (quantity - completedQuantity).coerceAtLeast(0f)
    }

    fun isQuantityFulfilled(factActions: List<FactAction>): Boolean {
        if (quantity <= 0f) return true
        val completedQuantity = getCompletedQuantity(factActions)
        return completedQuantity >= quantity
    }

    fun isActionCompleted(factActions: List<FactAction>): Boolean {
        if (manuallyCompleted) return true

        return when {
            actionTemplate?.actionCompletionCondition == ActionCompletionCondition.PLAN_ACHIEVED && quantity > 0f -> {
                isQuantityFulfilled(factActions)
            }
            else -> factActions.any { it.plannedActionId == id }
        }
    }
}