// Тип задания X (TaskTypeX)
package com.synngate.synnframe.domain.entity.taskx

import kotlinx.serialization.Serializable

@Serializable
data class TaskTypeX(
    val id: String,
    val name: String,                         // Имя типа задания
    val wmsOperation: WmsOperation,           // Операция WMS
    val canBeCreatedInApp: Boolean = false,   // Можно ли создать в приложении
    val allowCompletionWithoutFactLines: Boolean = false, // Разрешено ли завершение без строк факта
    val allowExceedPlanQuantity: Boolean = false, // Разрешено ли превышение планового количества
    val availableActions: List<AvailableTaskAction> = emptyList(), // Доступные действия (список идентификаторов)
    val factLineActionGroups: List<FactLineActionGroup> = emptyList(), // Группы действий для строк факта
    val finalActions: List<FactLineXAction> = emptyList() // Финальные действия перед завершением
)