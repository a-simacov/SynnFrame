package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для ответа сервера на запрос объекта шага
 *
 * Возможные значения objectType:
 * - "BIN" - ячейка (используются поля binCode, binZone)
 * - "PALLET" - паллета (используются поля palletCode, palletIsClosed)
 * - "PRODUCT" - товар из классификатора (используется поле productId, дополнительно productName, productWeight и др.)
 * - "TASK_PRODUCT" - товар задания (используются поля taskProductId, expirationDate, productStatus, дополнительно productName, productWeight и др.)
 * - "QUANTITY" - количество (используется поле quantity)
 */
@Serializable
data class StepObjectResponseDto(
    // Общие поля
    @SerialName("objectType")
    val objectType: String? = null,

    @SerialName("success")
    val success: Boolean = true,

    @SerialName("errorMessage")
    val errorMessage: String? = null,

    // Поля для ячеек
    @SerialName("binCode")
    val binCode: String? = null,

    @SerialName("binZone")
    val binZone: String? = null,

    // Поля для паллет
    @SerialName("palletCode")
    val palletCode: String? = null,

    @SerialName("palletIsClosed")
    val palletIsClosed: Boolean? = null,

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

    // Поля продукта для обновления
    @SerialName("productName")
    val productName: String? = null,

    @SerialName("productArticleNumber")
    val productArticleNumber: String? = null,

    @SerialName("productWeight")
    val productWeight: Float? = null,

    @SerialName("productAccountingModel")
    val productAccountingModel: String? = null,

    @SerialName("productMainUnitId")
    val productMainUnitId: String? = null,

    // Поле для количества
    @SerialName("quantity")
    val quantity: Float? = null,

    // Дополнительные поля для расширения
    @SerialName("additionalInfo")
    val additionalInfo: Map<String, String>? = null
)