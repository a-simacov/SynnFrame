package com.synngate.synnframe.domain.entity

import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDateTime

@Serializable
data class Task(
    val id: String,

    val name: String,

    val taskTypeId: String, // Идентификатор типа задания вместо enum

    @Transient
    val taskType: TaskType? = null, // Полный объект типа (transient - не сериализуется)

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

    fun canDelete(): Boolean = status == TaskStatus.COMPLETED && uploaded

    // Получение действия WMS из типа задания
    fun getTaskAction(): TaskAction? = taskType?.action

    // Получение названия типа задания
    fun getTaskTypeName(): String = taskType?.name ?: taskTypeId
}