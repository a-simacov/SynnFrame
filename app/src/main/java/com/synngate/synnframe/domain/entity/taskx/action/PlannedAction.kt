package com.synngate.synnframe.domain.entity.taskx.action

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class PlannedAction(
    val id: String,
    val order: Int,
    val actionTemplate: ActionTemplate,
    val storageProduct: TaskProduct? = null,
    val storagePallet: Pallet? = null,
    val wmsAction: WmsAction,
    val placementPallet: Pallet? = null,
    val placementBin: BinX? = null,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val isFinalAction: Boolean = false,

    val manuallyCompleted: Boolean = false,
    @Serializable(with = LocalDateTimeSerializer::class)
    val manuallyCompletedAt: LocalDateTime? = null,

    // progressType не передается с сервера и всегда расчитывается в приложении
    // это поле задано как опциональное, так как используется только для расчетов в приложении
    private val progressType: ProgressType? = null
) {

    fun getProgressType(): ProgressType {
        return if (storageProduct != null && storageProduct.quantity > 0f) {
            ProgressType.QUANTITY
        } else {
            ProgressType.SIMPLE
        }
    }

    fun isClickable(): Boolean = !isSkipped

    fun calculateProgress(factActions: List<FactAction>): Float {
        if (getProgressType() == ProgressType.SIMPLE) {
            return if (isCompleted || manuallyCompleted || factActions.any { it.plannedActionId == id }) 1f else 0f
        }

        if (getProgressType() == ProgressType.QUANTITY && storageProduct != null) {
            val completedQuantity = getCompletedQuantity(factActions)
            val plannedQuantity = storageProduct.quantity

            if (plannedQuantity <= 0f || manuallyCompleted) {
                return 1f
            }

            return (completedQuantity / plannedQuantity).coerceIn(0f, 1f)
        }

        return if (isCompleted || manuallyCompleted) 1f else 0f
    }

    fun isActionCompleted(factActions: List<FactAction>): Boolean {
        if (manuallyCompleted) {
            return true;
        }

        if (getProgressType() == ProgressType.QUANTITY && storageProduct != null) {
            val plannedQuantity = storageProduct.quantity
            if (plannedQuantity <= 0f) return false;

            val completedQuantity = factActions
                .filter { it.plannedActionId == id }
                .sumOf { it.storageProduct?.quantity?.toDouble() ?: 0.0 }
                .toFloat()

            return completedQuantity >= plannedQuantity;
        }

        return factActions.any { it.plannedActionId == id };
    }

    fun canHaveMultipleFactActions(): Boolean {
        return getProgressType() == ProgressType.QUANTITY
    }

    fun getPlannedQuantity(): Float {
        return storageProduct?.quantity ?: 0f
    }

    fun getCompletedQuantity(factActions: List<FactAction>): Float {
        val relatedFacts = factActions.filter { it.plannedActionId == id }
        return relatedFacts.sumOf {
            it.storageProduct?.quantity?.toDouble() ?: 0.0
        }.toFloat()
    }

    fun getRemainingQuantity(factActions: List<FactAction>): Float {
        if (getProgressType() != ProgressType.QUANTITY || storageProduct == null) {
            return 0f
        }

        val plannedQuantity = getPlannedQuantity()
        val completedQuantity = getCompletedQuantity(factActions)

        if (manuallyCompleted) {
            return 0f
        }

        return (plannedQuantity - completedQuantity).coerceAtLeast(0f)
    }

    fun isQuantityFulfilled(factActions: List<FactAction>): Boolean {
        if (getProgressType() != ProgressType.QUANTITY || storageProduct == null) {
            return false
        }

        val plannedQuantity = getPlannedQuantity()
        val completedQuantity = getCompletedQuantity(factActions)

        return completedQuantity >= plannedQuantity
    }
}