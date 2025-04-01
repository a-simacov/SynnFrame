package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.synngate.synnframe.domain.entity.FactLineAction
import com.synngate.synnframe.domain.entity.FactLineActionType

@Entity(
    tableName = "fact_line_actions",
    foreignKeys = [
        ForeignKey(
            entity = TaskTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskTypeId")]
)
data class FactLineActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskTypeId: String,
    val type: String, // Enum as string
    val order: Int,
    val promptText: String
) {
    fun toDomainModel(): FactLineAction {
        return FactLineAction(
            type = FactLineActionType.valueOf(type),
            order = order,
            promptText = promptText
        )
    }

    companion object {
        fun fromDomainModel(taskTypeId: String, factAction: FactLineAction): FactLineActionEntity {
            return FactLineActionEntity(
                taskTypeId = taskTypeId,
                type = factAction.type.name,
                order = factAction.order,
                promptText = factAction.promptText
            )
        }
    }
}