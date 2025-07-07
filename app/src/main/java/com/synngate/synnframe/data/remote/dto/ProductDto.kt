package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("accountingModel")
    val accountingModel: String,

    @SerialName("articleNumber")
    val articleNumber: String,

    @SerialName("mainUnitId")
    val mainUnitId: String,

    @SerialName("weight")
    val weight: Float = 0.0f,

    @SerialName("maxQtyPerPallet")
    val maxQtyPerPallet: Float = 0.0f,

    @SerialName("units")
    val units: List<ProductUnitDto>
) {

    fun toDomainModel(): Product {
        return Product(
            id = id,
            name = name,
            accountingModel = AccountingModel.fromString(accountingModel),
            articleNumber = articleNumber,
            mainUnitId = mainUnitId,
            weight = weight,
            maxQtyPerPallet = maxQtyPerPallet,
            units = units.map { it.toDomainModel() }
        )
    }

    companion object {
        fun fromDomainModel(product: Product): ProductDto {
            return ProductDto(
                id = product.id,
                name = product.name,
                accountingModel = product.accountingModel.name,
                articleNumber = product.articleNumber,
                mainUnitId = product.mainUnitId,
                weight = product.weight,
                maxQtyPerPallet = product.maxQtyPerPallet,
                units = product.units.map { ProductUnitDto.fromDomainModel(it) }
            )
        }
    }
}

