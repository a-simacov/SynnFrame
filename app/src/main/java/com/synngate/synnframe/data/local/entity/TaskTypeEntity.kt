package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synngate.synnframe.domain.entity.TaskAction
import com.synngate.synnframe.domain.entity.TaskType

@Entity(tableName = "task_types")
data class TaskTypeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val action: String, // Enum as string
    val canBeCreatedInApp: Boolean,
    val allowExceedPlanQuantity: Boolean
) {
    fun toDomainModel(factActions: List<FactLineActionEntity>): TaskType {
        return TaskType(
            id = id,
            name = name,
            action = TaskAction.valueOf(action),
            canBeCreatedInApp = canBeCreatedInApp,
            allowExceedPlanQuantity = allowExceedPlanQuantity,
            factLineActions = factActions.map { it.toDomainModel() }
        )
    }

    companion object {
        fun fromDomainModel(taskType: TaskType): TaskTypeEntity {
            return TaskTypeEntity(
                id = taskType.id,
                name = taskType.name,
                action = taskType.action.name,
                canBeCreatedInApp = taskType.canBeCreatedInApp,
                allowExceedPlanQuantity = taskType.allowExceedPlanQuantity
            )
        }
    }
}