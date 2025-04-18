package com.synngate.synnframe.domain.entity.taskx.validation

import kotlinx.serialization.Serializable

@Serializable
data class ValidationRuleItem(
    val type: ValidationType,
    val parameter: String? = null,
    val errorMessage: String
)

@Serializable
data class ValidationRule(
    val name: String,
    val rules: List<ValidationRuleItem> = emptyList()
)