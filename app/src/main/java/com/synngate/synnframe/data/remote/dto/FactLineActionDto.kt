package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.FactLineAction
import com.synngate.synnframe.domain.entity.FactLineActionType
import com.synngate.synnframe.domain.entity.TaskAction
import com.synngate.synnframe.domain.entity.TaskType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FactLineActionDto(
    @SerialName("type")
    val type: String,

    @SerialName("order")
    val order: Int,

    @SerialName("promptText")
    val promptText: String
) {
    fun toDomainModel(): FactLineAction {
        return FactLineAction(
            type = FactLineActionType.valueOf(type),
            order = order,
            promptText = promptText
        )
    }
}

@Serializable
data class TaskTypeDto(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("action")
    val action: String,

    @SerialName("canBeCreatedInApp")
    val canBeCreatedInApp: Boolean,

    @SerialName("allowExceedPlanQuantity")
    val allowExceedPlanQuantity: Boolean,

    @SerialName("factLineActions")
    val factLineActions: List<FactLineActionDto>
) {
    fun toDomainModel(): TaskType {
        return TaskType(
            id = id,
            name = name,
            action = TaskAction.valueOf(action),
            canBeCreatedInApp = canBeCreatedInApp,
            allowExceedPlanQuantity = allowExceedPlanQuantity,
            factLineActions = factLineActions.map { it.toDomainModel() }
        )
    }
}