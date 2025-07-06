package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.operation.CustomListItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для ответа API со списком пользовательских элементов.
 *
 * @property list Список элементов с id и description (HTML)
 */
@Serializable
data class CustomListResponseDto(
    @SerialName("list")
    val list: List<CustomListItem> = emptyList()
)