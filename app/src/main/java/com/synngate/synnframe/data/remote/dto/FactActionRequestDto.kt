package com.synngate.synnframe.data.remote.dto

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
    val storagePallet: Pallet? = null,
    val wmsAction: WmsAction,
    val placementPallet: Pallet? = null,
    val placementBin: BinX? = null,
    val startedAt: String, // ISO формат даты-времени
    val completedAt: String, // ISO формат даты-времени
    val plannedActionId: String? = null
) {
    companion object {
        fun fromDomain(factAction: FactAction): FactActionRequestDto {
            return FactActionRequestDto(
                id = factAction.id,
                taskId = factAction.taskId,
                storageProduct = factAction.storageProduct,
                storagePallet = factAction.storagePallet,
                wmsAction = factAction.wmsAction,
                placementPallet = factAction.placementPallet,
                placementBin = factAction.placementBin,
                startedAt = factAction.startedAt.toString(),
                completedAt = factAction.completedAt.toString(),
                plannedActionId = factAction.plannedActionId
            )
        }
    }
}