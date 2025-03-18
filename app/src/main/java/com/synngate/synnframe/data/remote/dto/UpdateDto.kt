package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для информации о последней версии приложения
 */
@Serializable
data class AppVersionDto(
    /**
     * Версия приложения
     */
    @SerialName("lastVersion")
    val lastVersion: String,

    /**
     * Дата выпуска версии
     */
    @SerialName("releaseDate")
    val releaseDate: String
)