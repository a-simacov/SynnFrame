package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.ProductUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductUnitDto(
    @SerialName("id")
    val id: String,

    @SerialName("productId")
    val productId: String,

    @SerialName("name")
    val name: String,

    @SerialName("quantity")
    val quantity: Float,

    @SerialName("mainBarcode")
    val mainBarcode: String,

    @SerialName("barcodes")
    val barcodes: List<String> = emptyList()
) {

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