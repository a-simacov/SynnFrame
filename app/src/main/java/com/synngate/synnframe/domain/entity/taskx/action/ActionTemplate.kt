package com.synngate.synnframe.domain.entity.taskx.action

import com.synngate.synnframe.domain.entity.taskx.WmsAction
import kotlinx.serialization.Serializable

@Serializable
data class ActionTemplate(
    val id: String,
    val name: String,
    val wmsAction: WmsAction,
    val storageObjectType: ActionObjectType?,
    val placementObjectType: ActionObjectType?,
    val storageSteps: List<ActionStep> = emptyList(),
    val placementSteps: List<ActionStep> = emptyList()
)