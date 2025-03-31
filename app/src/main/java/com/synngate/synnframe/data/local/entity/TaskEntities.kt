package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synngate.synnframe.domain.entity.CreationPlace
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskPlanLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val barcode: String,
    val createdAt: LocalDateTime,
    val viewedAt: LocalDateTime?,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val creationPlace: String,
    val executorId: String?,
    val status: String,
    val uploaded: Boolean,
    val uploadedAt: LocalDateTime?,
    val allowProductsNotInPlan: Boolean = false
) {
    fun toDomainModel(
        planLines: List<TaskPlanLine> = emptyList(),
        factLines: List<TaskFactLine> = emptyList()
    ): Task {
        return Task(
            id = id,
            name = name,
            type = TaskType.fromString(type),
            barcode = barcode,
            createdAt = createdAt,
            viewedAt = viewedAt,
            startedAt = startedAt,
            completedAt = completedAt,
            creationPlace = CreationPlace.fromString(creationPlace),
            executorId = executorId,
            status = TaskStatus.fromString(status),
            uploaded = uploaded,
            uploadedAt = uploadedAt,
            allowProductsNotInPlan = allowProductsNotInPlan,
            planLines = planLines,
            factLines = factLines
        )
    }

    companion object {
        fun fromDomainModel(task: Task): TaskEntity {
            return TaskEntity(
                id = task.id,
                name = task.name,
                type = task.type.name,
                barcode = task.barcode,
                createdAt = task.createdAt,
                viewedAt = task.viewedAt,
                startedAt = task.startedAt,
                completedAt = task.completedAt,
                creationPlace = task.creationPlace.name,
                executorId = task.executorId,
                status = task.status.name,
                uploaded = task.uploaded,
                uploadedAt = task.uploadedAt,
                allowProductsNotInPlan = task.allowProductsNotInPlan
            )
        }
    }
}

@Entity(
    tableName = "task_plan_lines",
    primaryKeys = ["id", "taskId"]
)
data class TaskPlanLineEntity(
    val id: String,
    val taskId: String,
    val productId: String,
    val quantity: Float,
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
        fun fromDomainModel(planLine: TaskPlanLine): TaskPlanLineEntity {
            return TaskPlanLineEntity(
                id = planLine.id,
                taskId = planLine.taskId,
                productId = planLine.productId,
                quantity = planLine.quantity,
                binCode = planLine.binCode
            )
        }
    }
}

@Entity(
    tableName = "task_fact_lines",
    primaryKeys = ["id", "taskId"]
)
data class TaskFactLineEntity(
    val id: String,
    val taskId: String,
    val productId: String,
    val quantity: Float,
    val binCode: String? = null
) {

    fun toDomainModel(): TaskFactLine {
        return TaskFactLine(
            id = id,
            taskId = taskId,
            productId = productId,
            quantity = quantity,
            binCode = binCode
        )
    }

    companion object {
        fun fromDomainModel(factLine: TaskFactLine): TaskFactLineEntity {
            return TaskFactLineEntity(
                id = factLine.id,
                taskId = factLine.taskId,
                productId = factLine.productId,
                quantity = factLine.quantity,
                binCode = factLine.binCode
            )
        }
    }
}