package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.dto.StepObjectResponseDto
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Сервис для преобразования DTO в объекты доменной модели
 */
class StepObjectMapperService(
    private val productUseCases: ProductUseCases
) {
    // Константы для типов объектов
    companion object {
        const val OBJECT_TYPE_BIN = "BIN"
        const val OBJECT_TYPE_PALLET = "PALLET"
        const val OBJECT_TYPE_PRODUCT = "PRODUCT"
        const val OBJECT_TYPE_TASK_PRODUCT = "TASK_PRODUCT"
        const val OBJECT_TYPE_QUANTITY = "QUANTITY"
    }

    /**
     * Преобразует DTO из ответа сервера в объект доменной модели
     * с учетом типа поля и objectType из ответа
     *
     * @param response DTO из ответа сервера
     * @param fieldType Тип поля, для которого запрашивался объект
     * @return Объект доменной модели или null при ошибке
     */
    suspend fun mapResponseToObject(
        response: StepObjectResponseDto,
        fieldType: FactActionField
    ): Any? {
        // Сначала проверяем objectType (если указан)
        if (!response.objectType.isNullOrBlank()) {
            when (response.objectType.uppercase()) {
                OBJECT_TYPE_BIN -> {
                    if (response.binCode != null) {
                        return BinX(
                            code = response.binCode,
                            zone = response.binZone ?: ""
                        )
                    }
                }

                OBJECT_TYPE_PALLET -> {
                    if (response.palletCode != null) {
                        return Pallet(
                            code = response.palletCode,
                            isClosed = response.palletIsClosed ?: false
                        )
                    }
                }

                OBJECT_TYPE_PRODUCT -> {
                    if (response.productId != null) {
                        return productUseCases.getProductById(response.productId)
                    }
                }

                OBJECT_TYPE_TASK_PRODUCT -> {
                    if (response.taskProductId != null) {
                        val product = productUseCases.getProductById(response.taskProductId)
                        if (product != null) {
                            val status = response.productStatus?.let {
                                ProductStatus.fromString(it)
                            } ?: ProductStatus.STANDARD

                            val expDate = response.expirationDate?.let {
                                try {
                                    LocalDateTime.parse(it)
                                } catch (e: Exception) {
                                    Timber.e(e, "Ошибка парсинга даты: $it")
                                    null
                                }
                            }

                            return TaskProduct(
                                id = response.taskProductId,
                                product = product,
                                expirationDate = expDate,
                                status = status
                            )
                        }
                    }
                }

                OBJECT_TYPE_QUANTITY -> {
                    if (response.quantity != null) {
                        return response.quantity
                    }
                }
            }
        }

        // Если objectType не указан или не удалось создать объект по нему,
        // используем запасной вариант определения типа по наличию полей и fieldType
        return when {
            response.binCode != null && (fieldType == FactActionField.STORAGE_BIN ||
                    fieldType == FactActionField.ALLOCATION_BIN) -> {
                BinX(
                    code = response.binCode,
                    zone = response.binZone ?: ""
                )
            }

            response.palletCode != null && (fieldType == FactActionField.STORAGE_PALLET ||
                    fieldType == FactActionField.ALLOCATION_PALLET) -> {
                Pallet(
                    code = response.palletCode,
                    isClosed = response.palletIsClosed ?: false
                )
            }

            response.productId != null && fieldType == FactActionField.STORAGE_PRODUCT_CLASSIFIER -> {
                productUseCases.getProductById(response.productId)
            }

            response.taskProductId != null && fieldType == FactActionField.STORAGE_PRODUCT -> {
                val product = productUseCases.getProductById(response.taskProductId)
                if (product != null) {
                    val status = response.productStatus?.let {
                        ProductStatus.fromString(it)
                    } ?: ProductStatus.STANDARD

                    val expDate = response.expirationDate?.let {
                        try {
                            LocalDateTime.parse(it)
                        } catch (e: Exception) {
                            Timber.e(e, "Ошибка парсинга даты: $it")
                            null
                        }
                    }

                    TaskProduct(
                        id = response.taskProductId,
                        product = product,
                        expirationDate = expDate,
                        status = status
                    )
                } else {
                    null
                }
            }

            response.quantity != null && fieldType == FactActionField.QUANTITY -> {
                response.quantity
            }

            else -> null
        }
    }
}