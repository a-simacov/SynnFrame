package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для запроса создания нового задания
 *
 * @property searchKey Сохраненный ключ поиска (опциональный)
 */
@Serializable
data class TaskCreateRequestDto(
    @SerialName("searchKey")
    val searchKey: String? = null
)