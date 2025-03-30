package com.synngate.synnframe.domain.entity

import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Task(
    val id: String,

    val name: String,

    val type: TaskType,

    val barcode: String,

    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,

    @Serializable(with = LocalDateTimeSerializer::class)
    val viewedAt: LocalDateTime? = null,

    @Serializable(with = LocalDateTimeSerializer::class)
    val startedAt: LocalDateTime? = null,

    @Serializable(with = LocalDateTimeSerializer::class)
    val completedAt: LocalDateTime? = null,

    val creationPlace: CreationPlace,

    val executorId: String? = null,

    val status: TaskStatus = TaskStatus.TO_DO,

    val uploaded: Boolean = false,

    @Serializable(with = LocalDateTimeSerializer::class)
    val uploadedAt: LocalDateTime? = null,

    val allowProductsNotInPlan: Boolean = false,

    val planLines: List<TaskPlanLine> = emptyList(),

    val factLines: List<TaskFactLine> = emptyList()
) {
    fun canStart(): Boolean = status == TaskStatus.TO_DO

    fun canComplete(): Boolean = status == TaskStatus.IN_PROGRESS

    fun canUpload(): Boolean = status == TaskStatus.COMPLETED && !uploaded

    fun getTotalPlanQuantity(): Float = planLines.sumOf { it.quantity.toDouble() }.toFloat()

    fun getTotalFactQuantity(): Float = factLines.sumOf { it.quantity.toDouble() }.toFloat()

    fun getCompletionPercentage(): Float {
        val totalPlan = getTotalPlanQuantity()
        return if (totalPlan > 0) {
            (getTotalFactQuantity() / totalPlan) * 100f
        } else {
            0f
        }
    }

    fun getFactLineByProductId(productId: String): TaskFactLine? =
        factLines.find { it.productId == productId }

    fun isProductInPlan(productId: String): Boolean =
        planLines.any { it.productId == productId }
}

@Serializable
data class TaskPlanLine(
    val id: String,

    val taskId: String,

    val productId: String,

    val quantity: Float
)

@Serializable
data class TaskFactLine(
    val id: String,

    val taskId: String,

    val productId: String,

    val quantity: Float
)