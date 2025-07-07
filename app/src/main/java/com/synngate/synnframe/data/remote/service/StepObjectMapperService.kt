package com.synngate.synnframe.data.remote.service

import com.synngate.synnframe.data.remote.dto.StepObjectResponseDto
import com.synngate.synnframe.domain.entity.AccountingModel
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
        Timber.d("Mapping response to object: objectType=${response.objectType}, fieldType=$fieldType")
        Timber.d("Response fields: taskProductId=${response.taskProductId}, productWeight=${response.productWeight}")
        
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
                        val baseProduct = productUseCases.getProductById(response.productId)
                        if (baseProduct != null) {
                            // Создаем обновленный продукт с полями из ответа сервера
                            return baseProduct.copy(
                                name = response.productName ?: baseProduct.name,
                                articleNumber = response.productArticleNumber ?: baseProduct.articleNumber,
                                weight = response.productWeight ?: baseProduct.weight,
                                maxQtyPerPallet = response.productMaxQtyPerPallet ?: baseProduct.maxQtyPerPallet,
                                accountingModel = response.productAccountingModel?.let { 
                                    try { 
                                        AccountingModel.valueOf(it) 
                                    } catch (e: Exception) { 
                                        baseProduct.accountingModel 
                                    } 
                                } ?: baseProduct.accountingModel,
                                mainUnitId = response.productMainUnitId ?: baseProduct.mainUnitId
                            )
                        }
                    }
                }

                OBJECT_TYPE_TASK_PRODUCT -> {
                    if (response.taskProductId != null) {
                        Timber.d("Creating TASK_PRODUCT with ID: ${response.taskProductId}")
                        
                        // Для обновления TaskProduct нужен ID продукта, а не TaskProduct
                        // Если есть productId в ответе, используем его
                        val productId = response.productId ?: run {
                            Timber.w("No productId in response, using taskProductId as fallback")
                            response.taskProductId
                        }
                        
                        val baseProduct = productUseCases.getProductById(productId)
                        if (baseProduct != null) {
                            Timber.d("Found base product: ${baseProduct.id}, updating with weight: ${response.productWeight}")
                            // Создаем обновленный продукт с полями из ответа сервера
                            val updatedProduct = baseProduct.copy(
                                name = response.productName ?: baseProduct.name,
                                articleNumber = response.productArticleNumber ?: baseProduct.articleNumber,
                                weight = response.productWeight ?: baseProduct.weight,
                                maxQtyPerPallet = response.productMaxQtyPerPallet ?: baseProduct.maxQtyPerPallet,
                                accountingModel = response.productAccountingModel?.let { 
                                    try { 
                                        AccountingModel.valueOf(it) 
                                    } catch (e: Exception) { 
                                        baseProduct.accountingModel 
                                    } 
                                } ?: baseProduct.accountingModel,
                                mainUnitId = response.productMainUnitId ?: baseProduct.mainUnitId
                            )

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

                            val resultTaskProduct = TaskProduct(
                                id = response.taskProductId,
                                product = updatedProduct,
                                expirationDate = expDate,
                                status = status
                            )
                            
                            Timber.d("Successfully created updated TaskProduct with weight: ${updatedProduct.weight}")
                            return resultTaskProduct
                        } else {
                            Timber.e("Failed to find base product with ID: $productId")
                        }
                    } else {
                        Timber.e("No taskProductId in response")
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
                val baseProduct = productUseCases.getProductById(response.productId)
                baseProduct?.copy(
                    name = response.productName ?: baseProduct.name,
                    articleNumber = response.productArticleNumber ?: baseProduct.articleNumber,
                    weight = response.productWeight ?: baseProduct.weight,
                    maxQtyPerPallet = response.productMaxQtyPerPallet ?: baseProduct.maxQtyPerPallet,
                    accountingModel = response.productAccountingModel?.let { 
                        try { 
                            AccountingModel.valueOf(it) 
                        } catch (e: Exception) { 
                            baseProduct.accountingModel 
                        } 
                    } ?: baseProduct.accountingModel,
                    mainUnitId = response.productMainUnitId ?: baseProduct.mainUnitId
                )
            }

            response.taskProductId != null && fieldType == FactActionField.STORAGE_PRODUCT -> {
                val baseProduct = productUseCases.getProductById(response.taskProductId)
                if (baseProduct != null) {
                    // Создаем обновленный продукт с полями из ответа сервера
                    val updatedProduct = baseProduct.copy(
                        name = response.productName ?: baseProduct.name,
                        articleNumber = response.productArticleNumber ?: baseProduct.articleNumber,
                        weight = response.productWeight ?: baseProduct.weight,
                        maxQtyPerPallet = response.productMaxQtyPerPallet ?: baseProduct.maxQtyPerPallet,
                        accountingModel = response.productAccountingModel?.let { 
                            try { 
                                AccountingModel.valueOf(it) 
                            } catch (e: Exception) { 
                                baseProduct.accountingModel 
                            } 
                        } ?: baseProduct.accountingModel,
                        mainUnitId = response.productMainUnitId ?: baseProduct.mainUnitId
                    )

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
                        product = updatedProduct,
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