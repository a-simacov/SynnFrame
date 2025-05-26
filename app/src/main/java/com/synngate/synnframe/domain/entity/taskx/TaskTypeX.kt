package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.domain.entity.taskx.action.ActionTemplate
import com.synngate.synnframe.presentation.ui.taskx.entity.SearchActionFieldType
import com.synngate.synnframe.presentation.ui.taskx.enums.RegularActionsExecutionOrder
import kotlinx.serialization.Serializable

@Serializable
data class TaskTypeX(
    val id: String,
    val name: String,
    val wmsOperation: WmsOperation,
    val canBeCreatedInApp: Boolean = false,
    val allowCompletionWithoutFactActions: Boolean = false,
    val allowExceedPlanQuantity: Boolean = false,
    val regularActionsExecutionOrder: RegularActionsExecutionOrder = RegularActionsExecutionOrder.STRICT,
    val allowMultipleFactActions: Boolean = false,
    val searchActionFieldsTypes: List<SearchActionFieldType> = emptyList(),
    val availableActionsTemplates: List<ActionTemplate> = emptyList()
) {
    fun isStrictActionOrder(): Boolean =
        regularActionsExecutionOrder == RegularActionsExecutionOrder.STRICT

    fun isActionSearchEnabled(): Boolean =
        searchActionFieldsTypes.isNotEmpty()

    fun getSavableFieldTypes(): List<SearchActionFieldType> =
        searchActionFieldsTypes.filter { it.saveToTaskBuffer }
}