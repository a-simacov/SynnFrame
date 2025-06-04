package com.synngate.synnframe.presentation.ui.taskx.dto

import kotlinx.serialization.Serializable

/**
 * DTO для передачи опций булевого параметра
 */
@Serializable
data class BooleanParameterOptionsDto(
    val displayType: String = "CHECKBOX",
    val trueLabel: String = "Да",
    val falseLabel: String = "Нет"
)