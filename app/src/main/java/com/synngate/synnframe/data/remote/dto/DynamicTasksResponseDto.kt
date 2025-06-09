package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.operation.DynamicTask
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для ответа API со списком динамических заданий.
 *
 * @property taskTypeId Идентификатор типа задания, необходимый для создания новых заданий
 * @property list Список заданий
 */
@Serializable
data class DynamicTasksResponseDto(
    @SerialName("taskTypeId")
    val taskTypeId: String? = null,

    @SerialName("list")
    val list: List<DynamicTask> = emptyList()
)