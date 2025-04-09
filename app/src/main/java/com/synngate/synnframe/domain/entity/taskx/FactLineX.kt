package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class FactLineX(
    val id: String,
    val taskId: String,                 // ID задания
    val storageProduct: TaskProduct? = null,  // Товар хранения
    val storagePallet: Pallet? = null,        // Паллета хранения
    val wmsAction: WmsAction,                 // Действие WMS
    val placementPallet: Pallet? = null,      // Паллета размещения
    val placementBin: BinX? = null,           // Ячейка размещения
    @Serializable(with = LocalDateTimeSerializer::class)
    val startedAt: LocalDateTime,            // Дата и время начала
    @Serializable(with = LocalDateTimeSerializer::class)
    val completedAt: LocalDateTime           // Дата и время завершения
)