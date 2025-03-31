package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.ProductUnit

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val accountingModel: String,
    val articleNumber: String,
    val mainUnitId: String
) {

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

@Entity(
    tableName = "barcodes",
    primaryKeys = ["code", "productUnitId"],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["productUnitId"])
    ]
)
data class BarcodeEntity(
    val code: String,
    val productUnitId: String,
    val productId: String // Добавляем для удобства поиска
) {
    companion object {
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