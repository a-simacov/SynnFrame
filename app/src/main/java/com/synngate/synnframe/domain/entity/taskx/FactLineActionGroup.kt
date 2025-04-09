package com.synngate.synnframe.domain.entity.taskx

import kotlinx.serialization.Serializable

@Serializable
data class FactLineActionGroup(
    val id: String,
    val name: String,                          // Имя группы действий
    val order: Int,                           // Порядок выполнения
    val targetFieldType: TaskXLineFieldType,   // Целевое поле для заполнения
    val wmsAction: WmsAction,                 // Тип действия WMS
    val resultType: String,                   // Тип результата (TaskProduct, BinX, Pallet)
    val actions: List<FactLineXAction>         // Список действий в группе
)