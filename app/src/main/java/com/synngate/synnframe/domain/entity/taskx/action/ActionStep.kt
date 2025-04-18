package com.synngate.synnframe.domain.entity.taskx.action

import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import kotlinx.serialization.Serializable

@Serializable
data class ActionStep(
    val id: String,
    val order: Int,
    val name: String,
    val promptText: String,          // Сообщение для пользователя
    val objectType: ActionObjectType,
    val validationRules: ValidationRule,
    val isRequired: Boolean = true,
    val canSkip: Boolean = false
)