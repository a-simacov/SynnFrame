package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.CreationPlace
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskPlanLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class TaskDto(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("type")
    val type: String,

    @SerialName("barcode")
    val barcode: String,

    @SerialName("createdAt")
    val createdAt: String,

    @SerialName("viewedAt")
    val viewedAt: String? = null,

    @SerialName("startedAt")
    val startedAt: String? = null,

    @SerialName("completedAt")
    val completedAt: String? = null,

    @SerialName("creationPlace")
    val creationPlace: String,

    @SerialName("executorId")
    val executorId: String? = null,

    @SerialName("status")
    val status: String,

    @SerialName("uploaded")
    val uploaded: Boolean = false,

    @SerialName("uploadedAt")
    val uploadedAt: String? = null,

    @SerialName("planLines")
    val planLines: List<TaskPlanLineDto> = emptyList(),

    @SerialName("factLines")
    val factLines: List<TaskFactLineDto> = emptyList()
) {
    fun toDomainModel(): Task {
        return Task(
            id = id,
            name = name,
            type = TaskType.fromString(type),
            barcode = barcode,
            createdAt = LocalDateTime.parse(createdAt),
            viewedAt = viewedAt?.let { LocalDateTime.parse(it) },
            startedAt = startedAt?.let { LocalDateTime.parse(it) },
            completedAt = completedAt?.let { LocalDateTime.parse(it) },
            creationPlace = CreationPlace.fromString(creationPlace),
            executorId = executorId,
            status = TaskStatus.fromString(status),
            uploaded = uploaded,
            uploadedAt = uploadedAt?.let { LocalDateTime.parse(it) },
            planLines = planLines.map { it.toDomainModel() },
            factLines = factLines.map { it.toDomainModel() }
        )
    }

    companion object {
        fun fromDomainModel(task: Task): TaskDto {
            return TaskDto(
                id = task.id,
                name = task.name,
                type = task.type.name,
                barcode = task.barcode,
                createdAt = task.createdAt.toString(),
                viewedAt = task.viewedAt?.toString(),
                startedAt = task.startedAt?.toString(),
                completedAt = task.completedAt?.toString(),
                creationPlace = task.creationPlace.name,
                executorId = task.executorId,
                status = task.status.name,
                uploaded = task.uploaded,
                uploadedAt = task.uploadedAt?.toString(),
                planLines = task.planLines.map { TaskPlanLineDto.fromDomainModel(it) },
                factLines = task.factLines.map { TaskFactLineDto.fromDomainModel(it) }
            )
        }
    }
}

/**
 * DTO для строки плана задания
 */
@Serializable
data class TaskPlanLineDto(
    /**
     * Идентификатор строки плана
     */
    @SerialName("id")
    val id: String,

    /**
     * Идентификатор задания
     */
    @SerialName("taskId")
    val taskId: String,

    /**
     * Идентификатор товара
     */
    @SerialName("productId")
    val productId: String,

    /**
     * Количество по плану
     */
    @SerialName("quantity")
    val quantity: Float
) {
    /**
     * Преобразование DTO в доменную модель
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
         * Создание DTO из доменной модели
         */
        fun fromDomainModel(planLine: TaskPlanLine): TaskPlanLineDto {
            return TaskPlanLineDto(
                id = planLine.id,
                taskId = planLine.taskId,
                productId = planLine.productId,
                quantity = planLine.quantity
            )
        }
    }
}

/**
 * DTO для строки факта задания
 */
@Serializable
data class TaskFactLineDto(
    /**
     * Идентификатор строки факта
     */
    @SerialName("id")
    val id: String,

    /**
     * Идентификатор задания
     */
    @SerialName("taskId")
    val taskId: String,

    /**
     * Идентификатор товара
     */
    @SerialName("productId")
    val productId: String,

    /**
     * Количество по факту
     */
    @SerialName("quantity")
    val quantity: Float
) {
    /**
     * Преобразование DTO в доменную модель
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
         * Создание DTO из доменной модели
         */
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

/**
 * DTO для проверки доступности задания
 */
@Serializable
data class TaskAvailabilityResponseDto(
    /**
     * Признак доступности задания
     */
    @SerialName("available")
    val available: Boolean
)