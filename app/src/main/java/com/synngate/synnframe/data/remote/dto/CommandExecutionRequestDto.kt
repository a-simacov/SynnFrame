package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommandExecutionRequestDto(
    val commandId: String,
    val stepId: String,
    val factAction: FactActionRequestDto,
    val parameters: Map<String, String> = emptyMap(),
    val additionalContext: Map<String, String> = emptyMap()
)

@Serializable
data class CommandExecutionResponseDto(
    val success: Boolean,
    val message: String? = null,
    val resultData: Map<String, String>? = null,
    val nextAction: CommandNextAction? = null,
    val updatedFactAction: FactActionRequestDto? = null
)

@Serializable
enum class CommandNextAction {
    NONE,
    REFRESH_STEP,
    GO_TO_NEXT_STEP,
    GO_TO_PREVIOUS_STEP,
    COMPLETE_ACTION,
    SET_OBJECT,// Установить объект в шаг (если есть resultData)
    SHOW_DIALOG
}