package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.TaskFactLine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskFactLineDto(
    @SerialName("id")
    val id: String,

    @SerialName("taskId")
    val taskId: String,

    @SerialName("productId")
    val productId: String,

    @SerialName("quantity")
    val quantity: Float
) {

    fun toDomainModel(): TaskFactLine {
        return TaskFactLine(
            id = id,
            taskId = taskId,
            productId = productId,
            quantity = quantity
        )
    }

    companion object {
        fun fromDomainModel(factLine: TaskFactLine): TaskFactLineDto {
            return TaskFactLineDto(
                id = factLine.id,
                taskId = factLine.taskId,
                productId = factLine.productId,
                quantity = factLine.quantity
            )
        }
    }
}