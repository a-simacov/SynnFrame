package com.synngate.synnframe.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.synngate.synnframe.domain.entity.Product

data class ProductWithRelations(
    @Embedded
    val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val units: List<ProductUnitEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val barcodes: List<BarcodeEntity>
) {
    fun toDomainModel(): Product {
        // Группируем штрихкоды по unitId
        val barcodesByUnit = barcodes.groupBy { it.productUnitId }
        // Преобразуем единицы измерения с их штрихкодами
        val productUnits = units.map { unit ->
            val unitBarcodes = barcodesByUnit[unit.id]?.map { it.code } ?: emptyList()
            unit.toDomainModel(unitBarcodes)
        }

        return product.toDomainModel(productUnits)
    }
}
