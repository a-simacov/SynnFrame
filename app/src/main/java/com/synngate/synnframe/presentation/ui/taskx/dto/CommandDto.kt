package com.synngate.synnframe.presentation.ui.taskx.dto

import kotlinx.serialization.Serializable

@Serializable
data class StepCommandDto(
    val id: String,
    val name: String,
    val description: String = "",
    val endpoint: String,
    val icon: String? = null,
    val buttonStyle: String = "SECONDARY",
    val displayCondition: String = "ALWAYS",
    val executionBehavior: String = "SHOW_RESULT",
    val parameters: List<CommandParameterDto> = emptyList(),
    val confirmationRequired: Boolean = false,
    val confirmationMessage: String? = null,
    val order: Int = 0
)

@Serializable
data class CommandParameterDto(
    val id: String,
    val name: String,
    val displayName: String,
    val type: String,
    val isRequired: Boolean = true,
    val defaultValue: String? = null,
    val placeholder: String? = null,
    val validation: ParameterValidationDto? = null,
    val options: List<ParameterOptionDto>? = null,
    val order: Int = 0
)

@Serializable
data class ParameterValidationDto(
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val pattern: String? = null,
    val errorMessage: String? = null
)

@Serializable
data class ParameterOptionDto(
    val value: String,
    val displayName: String
)