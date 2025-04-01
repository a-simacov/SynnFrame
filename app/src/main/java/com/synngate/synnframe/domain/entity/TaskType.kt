package com.synngate.synnframe.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class TaskType(
    val id: String,
    val name: String,
    val action: TaskAction,
    val canBeCreatedInApp: Boolean,
    val allowExceedPlanQuantity: Boolean,
    val factLineActions: List<FactLineAction>
)