package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTO для запроса выполнения команды
 */
@Serializable
data class CommandExecutionRequestDto(
    val commandId: String,
    val stepId: String,
    val factAction: FactActionRequestDto,
    val parameters: Map<String, String> = emptyMap(),
    val additionalContext: Map<String, String> = emptyMap()
)

/**
 * DTO для ответа на выполнение команды
 */
@Serializable
data class CommandExecutionResponseDto(
    val success: Boolean,
    val message: String? = null,
    val resultData: Map<String, String>? = null,
    val nextAction: CommandNextAction? = null,
    val updatedFactAction: FactActionRequestDto? = null
)

/**
 * Следующее действие после выполнения команды
 */
@Serializable
enum class CommandNextAction {
    NONE,               // Ничего не делать
    REFRESH_STEP,       // Обновить шаг
    GO_TO_NEXT_STEP,    // Перейти к следующему шагу
    GO_TO_PREVIOUS_STEP, // Вернуться к предыдущему шагу
    COMPLETE_ACTION,    // Завершить действие
    SET_OBJECT,         // Установить объект в шаг (если есть resultData)
    SHOW_DIALOG         // Показать диалог с результатом
}