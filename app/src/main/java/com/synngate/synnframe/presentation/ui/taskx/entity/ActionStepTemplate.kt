package com.synngate.synnframe.presentation.ui.taskx.entity

import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import com.synngate.synnframe.presentation.ui.taskx.enums.BufferUsage
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import kotlinx.serialization.Serializable

@Serializable
data class ActionStepTemplate(
    val id: String,
    val order: Int,
    val name: String,
    val promptText: String,
    val factActionField: FactActionField,
    val isRequired: Boolean = true,
    val serverSelectionEndpoint: String = "",
    val inputAdditionalProps: Boolean = false,
    val bufferUsage: BufferUsage = BufferUsage.NEVER,
    val saveToTaskBuffer: Boolean = false,
    val validationRules: ValidationRule? = null
)