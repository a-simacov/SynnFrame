package com.synngate.synnframe.domain.entity.taskx

import kotlinx.serialization.Serializable

@Serializable
data class PlanLineX(
    val id: String,
    val taskId: String,                 // ID задания
    val executionOrder: Int = 0,        // Порядок выполнения
    val storageProduct: TaskProduct? = null,  // Товар хранения
    val storagePallet: Pallet? = null,        // Паллета хранения
    val wmsAction: WmsAction,                 // Действие WMS
    val placementPallet: Pallet? = null,      // Паллета размещения
    val placementBin: BinX? = null            // Ячейка размещения
)