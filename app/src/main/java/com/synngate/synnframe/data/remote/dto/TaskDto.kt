package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.CreationPlace
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskStatus
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

    @SerialName("allowProductsNotInPlan")
    val allowProductsNotInPlan: Boolean = false,

    @SerialName("planLines")
    val planLines: List<TaskPlanLineDto> = emptyList(),

    @SerialName("factLines")
    val factLines: List<TaskFactLineDto> = emptyList()
) {
    fun toDomainModel(): Task {
        return Task(
            id = id,
            name = name,
            taskTypeId = type, // Используем поле type из DTO как taskTypeId
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
            factLines = factLines.map { it.toDomainModel() },
            allowProductsNotInPlan = allowProductsNotInPlan
        )
    }

    companion object {
        fun fromDomainModel(task: Task): TaskDto {
            return TaskDto(
                id = task.id,
                name = task.name,
                type = task.taskTypeId, // Используем taskTypeId вместо task.type.name
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
                allowProductsNotInPlan = task.allowProductsNotInPlan,
                planLines = task.planLines.map { TaskPlanLineDto.fromDomainModel(it) },
                factLines = task.factLines.map { TaskFactLineDto.fromDomainModel(it) }
            )
        }
    }
}

