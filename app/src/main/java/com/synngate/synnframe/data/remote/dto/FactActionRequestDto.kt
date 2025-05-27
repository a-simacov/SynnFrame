package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import kotlinx.serialization.Serializable

@Serializable
data class FactActionRequestDto(
    val id: String,
    val taskId: String,
    val storageProduct: TaskProduct? = null,
    val storageProductClassifier: Product? = null,
    val storagePallet: Pallet? = null,
    val storageBin: BinX? = null,
    val wmsAction: WmsAction,
    val quantity: Float = 0f,
    val placementPallet: Pallet? = null,
    val placementBin: BinX? = null,
    val startedAt: String, // ISO формат даты-времени
    val completedAt: String, // ISO формат даты-времени
    val plannedActionId: String? = null,
    val actionTemplateId: String? = null
) {
    companion object {
        fun fromDomain(factAction: FactAction): FactActionRequestDto {
            return FactActionRequestDto(
                id = factAction.id,
                taskId = factAction.taskId,
                storageProduct = factAction.storageProduct,
                storageProductClassifier = factAction.storageProductClassifier,
                storagePallet = factAction.storagePallet,
                storageBin = factAction.storageBin,
                wmsAction = factAction.wmsAction,
                quantity = factAction.quantity,
                placementPallet = factAction.placementPallet,
                placementBin = factAction.placementBin,
                startedAt = factAction.startedAt.toString(),
                completedAt = factAction.completedAt.toString(),
                plannedActionId = factAction.plannedActionId,
                actionTemplateId = factAction.actionTemplateId
            )
        }
    }
}