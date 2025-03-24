package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.ProductUnit
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для товара
 */
@Serializable
data class ProductDto(
    /**
     * Идентификатор товара
     */
    @SerialName("id")
    val id: String,

    /**
     * Наименование товара
     */
    @SerialName("name")
    val name: String,

    /**
     * Модель учета товара
     */
    @SerialName("accountingModel")
    val accountingModel: String,

    /**
     * Артикул товара
     */
    @SerialName("articleNumber")
    val articleNumber: String,

    /**
     * Идентификатор основной единицы измерения
     */
    @SerialName("mainUnitId")
    val mainUnitId: String,

    /**
     * Единицы измерения товара
     */
    @SerialName("units")
    val units: List<ProductUnitDto>
) {
    /**
     * Преобразование DTO в доменную модель
     */
    fun toDomainModel(): Product {
        return Product(
            id = id,
            name = name,
            accountingModel = AccountingModel.fromString(accountingModel),
            articleNumber = articleNumber,
            mainUnitId = mainUnitId,
            units = units.map { it.toDomainModel() }
        )
    }

    companion object {
        /**
         * Создание DTO из доменной модели
         */
        fun fromDomainModel(product: Product): ProductDto {
            return ProductDto(
                id = product.id,
                name = product.name,
                accountingModel = product.accountingModel.name,
                articleNumber = product.articleNumber,
                mainUnitId = product.mainUnitId,
                units = product.units.map { ProductUnitDto.fromDomainModel(it) }
            )
        }
    }
}

/**
 * DTO для единицы измерения товара
 */
@Serializable
data class ProductUnitDto(
    /**
     * Идентификатор единицы измерения
     */
    @SerialName("id")
    val id: String,

    /**
     * Идентификатор товара
     */
    @SerialName("productId")
    val productId: String,

    /**
     * Наименование единицы измерения
     */
    @SerialName("name")
    val name: String,

    /**
     * Коэффициент пересчета
     */
    @SerialName("quantity")
    val quantity: Float,

    /**
     * Основной штрихкод
     */
    @SerialName("mainBarcode")
    val mainBarcode: String,

    /**
     * Дополнительные штрихкоды
     */
    @SerialName("barcodes")
    val barcodes: List<String> = emptyList()
) {
    /**
     * Преобразование DTO в доменную модель
     */
    fun toDomainModel(): ProductUnit {
        return ProductUnit(
            id = id,
            productId = productId,
            name = name,
            quantity = quantity,
            mainBarcode = mainBarcode,
            barcodes = barcodes
        )
    }

    companion object {
        /**
         * Создание DTO из доменной модели
         */
        fun fromDomainModel(productUnit: ProductUnit): ProductUnitDto {
            return ProductUnitDto(
                id = productUnit.id,
                productId = productUnit.productId,
                name = productUnit.name,
                quantity = productUnit.quantity,
                mainBarcode = productUnit.mainBarcode,
                barcodes = productUnit.barcodes
            )
        }
    }
}