package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.TaskPlanLine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskPlanLineDto(
    @SerialName("id")
    val id: String,

    @SerialName("taskId")
    val taskId: String,

    @SerialName("productId")
    val productId: String,

    @SerialName("quantity")
    val quantity: Float,

    @SerialName("binCode")
    val binCode: String? = null
) {
    fun toDomainModel(): TaskPlanLine {
        return TaskPlanLine(
            id = id,
            taskId = taskId,
            productId = productId,
            quantity = quantity,
            binCode = binCode
        )
    }

    companion object {
        fun fromDomainModel(planLine: TaskPlanLine): TaskPlanLineDto {
            return TaskPlanLineDto(
                id = planLine.id,
                taskId = planLine.taskId,
                productId = planLine.productId,
                quantity = planLine.quantity,
                binCode = planLine.binCode
            )
        }
    }
}