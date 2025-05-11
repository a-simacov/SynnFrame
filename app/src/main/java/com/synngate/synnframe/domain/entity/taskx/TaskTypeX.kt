package com.synngate.synnframe.domain.entity.taskx

import kotlinx.serialization.Serializable

@Serializable
data class TaskTypeX(
    val id: String,
    val name: String,
    val wmsOperation: WmsOperation,
    val canBeCreatedInApp: Boolean = false,
    val allowCompletionWithoutFactActions: Boolean = false,
    val allowExceedPlanQuantity: Boolean = false,
    val strictActionOrder: Boolean = true,
    val availableActions: List<AvailableTaskAction> = emptyList(),
    val allowMultipleFactActions: Boolean = false
)