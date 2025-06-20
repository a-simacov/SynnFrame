package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для ответа валидации ключа поиска
 */
@Serializable
data class SearchKeyValidationResponseDto(
    @SerialName("isValid")
    val isValid: Boolean,

    @SerialName("message")
    val message: String? = null,

    @SerialName("validatedValue")
    val validatedValue: String? = null
)