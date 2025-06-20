package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для запроса валидации ключа поиска
 */
@Serializable
data class SearchKeyValidationRequestDto(
    @SerialName("key")
    val key: String
)