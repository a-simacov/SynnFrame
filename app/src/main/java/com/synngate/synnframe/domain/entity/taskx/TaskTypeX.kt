package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.SearchableActionObject
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
    val allowMultipleFactActions: Boolean = false,
    val enableActionSearch: Boolean = false,
    val searchableActionObjects: List<SearchableActionObject> = emptyList(),
    val savableObjectTypes: List<ActionObjectType> = emptyList()
)