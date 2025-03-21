package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.ProductUnit

/**
 * Entity класс для хранения информации о товарах в Room
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val accountingModel: String,
    val articleNumber: String,
    val mainUnitId: String
) {
    /**
     * Преобразование в доменную модель (без единиц измерения)
     */
    fun toDomainModel(units: List<ProductUnit> = emptyList()): Product {
        return Product(
            id = id,
            name = name,
            accountingModel = AccountingModel.fromString(accountingModel),
            articleNumber = articleNumber,
            mainUnitId = mainUnitId,
            units = units
        )
    }

    companion object {
        /**
         * Создание Entity из доменной модели
         */
        fun fromDomainModel(product: Product): ProductEntity {
            return ProductEntity(
                id = product.id,
                name = product.name,
                accountingModel = product.accountingModel.name,
                articleNumber = product.articleNumber,
                mainUnitId = product.mainUnitId
            )
        }
    }
}

/**
 * Entity класс для хранения единиц измерения товаров в Room
 */
@Entity(
    tableName = "product_units",
    primaryKeys = ["id", "productId"]
)
data class ProductUnitEntity(
    val id: String,
    val productId: String,
    val name: String,
    val quantity: Float,
    val mainBarcode: String
) {
    /**
     * Преобразование в доменную модель (без штрихкодов)
     */
    fun toDomainModel(barcodes: List<String> = emptyList()): ProductUnit {
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
         * Создание Entity из доменной модели
         */
        fun fromDomainModel(productUnit: ProductUnit): ProductUnitEntity {
            return ProductUnitEntity(
                id = productUnit.id,
                productId = productUnit.productId,
                name = productUnit.name,
                quantity = productUnit.quantity,
                mainBarcode = productUnit.mainBarcode
            )
        }
    }
}

/**
 * Entity класс для хранения штрихкодов единиц измерения товаров в Room
 */
@Entity(
    tableName = "barcodes",
    primaryKeys = ["code", "productUnitId"]
)
data class BarcodeEntity(
    val code: String,
    val productUnitId: String,
    val productId: String // Добавляем для удобства поиска
) {
    companion object {
        /**
         * Создание Entity из доменной модели
         */
        fun fromProductUnit(productUnit: ProductUnit): List<BarcodeEntity> {
            return productUnit.barcodes.map { barcode ->
                BarcodeEntity(
                    code = barcode,
                    productUnitId = productUnit.id,
                    productId = productUnit.productId
                )
            }
        }
    }
}