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

class StepObjectMapperService(
    private val productUseCases: ProductUseCases
) {
    suspend fun mapResponseToObject(
        response: StepObjectResponseDto,
        fieldType: FactActionField
    ): Any? {
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
                    isClosed = false
                )
            }

            response.productId != null && fieldType == FactActionField.STORAGE_PRODUCT_CLASSIFIER -> {
                productUseCases.getProductById(response.productId)
            }

            response.taskProductId != null && fieldType == FactActionField.STORAGE_PRODUCT -> {
                // Создаем TaskProduct из полученных данных
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