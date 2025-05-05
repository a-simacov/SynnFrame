package com.synngate.synnframe.domain.entity.taskx

import kotlinx.serialization.Serializable

@Serializable
data class TaskTypeX(
    val id: String,
    val name: String,                       // Имя типа задания
    val wmsOperation: WmsOperation,         // Операция WMS
    val canBeCreatedInApp: Boolean = false, // Можно ли создать в приложении
    val allowCompletionWithoutFactActions: Boolean = false, // Разрешено ли завершение без факт. действий
    val allowExceedPlanQuantity: Boolean = false, // Разрешено ли превышение планового количества
    val strictActionOrder: Boolean = true,  // Требовать строгий порядок выполнения действий
    val availableActions: List<AvailableTaskAction> = emptyList(), // Доступные действия

    // Новое поле для поддержки множественных фактических действий
    val allowMultipleFactActions: Boolean = false // Разрешены ли множественные фактические действия
)