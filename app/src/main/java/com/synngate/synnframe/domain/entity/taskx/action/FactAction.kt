package com.synngate.synnframe.domain.entity.taskx.action

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class FactAction(
    val id: String,
    val taskId: String,
    val plannedActionId: String? = null,
    val actionTemplateId: String? = null,
    val storageProductClassifier: Product? = null,
    val storageProduct: TaskProduct? = null,
    val storagePallet: Pallet? = null,
    val storageBin: BinX? = null,
    val wmsAction: WmsAction,
    val quantity: Float = 0f,
    val placementPallet: Pallet? = null,
    val placementBin: BinX? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startedAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val completedAt: LocalDateTime
)