package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppVersionDto(
    @SerialName("lastVersion")
    val lastVersion: String,

    @SerialName("releaseDate")
    val releaseDate: String
)