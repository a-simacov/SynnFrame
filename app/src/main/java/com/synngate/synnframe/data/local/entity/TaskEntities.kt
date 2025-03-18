package com.synngate.synnframe.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.synngate.synnframe.domain.entity.CreationPlace
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskPlanLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import java.time.LocalDateTime

/**
 * Entity класс для хранения заданий в Room
 */
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
    val uploadedAt: LocalDateTime?
) {
    /**
     * Преобразование в доменную модель (без строк плана и факта)
     */
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
            planLines = planLines,
            factLines = factLines
        )
    }

    companion object {
        /**
         * Создание Entity из доменной модели
         */
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
                uploadedAt = task.uploadedAt
            )
        }
    }
}

/**
 * Entity класс для хранения строк плана заданий в Room
 */
@Entity(
    tableName = "task_plan_lines",
    primaryKeys = ["id", "taskId"]
)
data class TaskPlanLineEntity(
    val id: String,
    val taskId: String,
    val productId: String,
    val quantity: Float
) {
    /**
     * Преобразование в доменную модель
     */
    fun toDomainModel(): TaskPlanLine {
        return TaskPlanLine(
            id = id,
            taskId = taskId,
            productId = productId,
            quantity = quantity
        )
    }

    companion object {
        /**
         * Создание Entity из доменной модели
         */
        fun fromDomainModel(planLine: TaskPlanLine): TaskPlanLineEntity {
            return TaskPlanLineEntity(
                id = planLine.id,
                taskId = planLine.taskId,
                productId = planLine.productId,
                quantity = planLine.quantity
            )
        }
    }
}

/**
 * Entity класс для хранения строк факта заданий в Room
 */
@Entity(
    tableName = "task_fact_lines",
    primaryKeys = ["id", "taskId"]
)
data class TaskFactLineEntity(
    val id: String,
    val taskId: String,
    val productId: String,
    val quantity: Float
) {
    /**
     * Преобразование в доменную модель
     */
    fun toDomainModel(): TaskFactLine {
        return TaskFactLine(
            id = id,
            taskId = taskId,
            productId = productId,
            quantity = quantity
        )
    }

    companion object {
        /**
         * Создание Entity из доменной модели
         */
        fun fromDomainModel(factLine: TaskFactLine): TaskFactLineEntity {
            return TaskFactLineEntity(
                id = factLine.id,
                taskId = factLine.taskId,
                productId = factLine.productId,
                quantity = factLine.quantity
            )
        }
    }
}

/**
 * Класс для связывания Task с его строками плана и факта
 */
data class TaskWithDetails(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val planLines: List<TaskPlanLineEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val factLines: List<TaskFactLineEntity>
) {
    /**
     * Преобразование в доменную модель
     */
    fun toDomainModel(): Task {
        val domainPlanLines = planLines.map { it.toDomainModel() }
        val domainFactLines = factLines.map { it.toDomainModel() }
        return task.toDomainModel(domainPlanLines, domainFactLines)
    }
}