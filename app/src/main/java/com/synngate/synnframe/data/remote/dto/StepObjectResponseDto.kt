package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StepObjectResponseDto(
    @SerialName("objectType")
    val objectType: String,

    // Поля для ячеек
    @SerialName("binCode")
    val binCode: String? = null,

    @SerialName("binZone")
    val binZone: String? = null,

    // Поля для паллет
    @SerialName("palletCode")
    val palletCode: String? = null,

    // Поля для товара из классификатора
    @SerialName("productId")
    val productId: String? = null,

    // Поля для товара задания
    @SerialName("taskProductId")
    val taskProductId: String? = null,

    @SerialName("expirationDate")
    val expirationDate: String? = null,

    @SerialName("productStatus")
    val productStatus: String? = null,

    // Поле для количества
    @SerialName("quantity")
    val quantity: Float? = null,

    // Дополнительные поля для потенциальных ошибок
    @SerialName("success")
    val success: Boolean = true,

    @SerialName("errorMessage")
    val errorMessage: String? = null
)