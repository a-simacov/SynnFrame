package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.synngate.synnframe.data.local.entity.BarcodeEntity
import com.synngate.synnframe.data.local.entity.ProductEntity
import com.synngate.synnframe.data.local.entity.ProductUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :nameFilter || '%' ORDER BY name ASC")
    fun getProductsByNameFilter(nameFilter: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getProductEntitiesByIds(ids: Set<String>): List<ProductEntity>

    @Query("SELECT * FROM product_units WHERE productId = :productId")
    suspend fun getProductUnitsForProduct(productId: String): List<ProductUnitEntity>

    @Query("SELECT * FROM product_units WHERE productId IN (:productIds)")
    suspend fun getProductUnitsForProducts(productIds: List<String>): List<ProductUnitEntity>

    @Query("SELECT * FROM barcodes WHERE productUnitId = :unitId AND productId = :productId")
    suspend fun getBarcodesForUnit(productId: String, unitId: String): List<BarcodeEntity>

    @Query("SELECT * FROM barcodes WHERE productUnitId IN (:unitIds) AND productId IN (:productIds)")
    suspend fun getBarcodesForUnits(
        productIds: List<String>,
        unitIds: List<String>
    ): List<BarcodeEntity>

    @Query("SELECT * FROM barcodes WHERE code = :barcode")
    suspend fun findBarcodeEntity(barcode: String): BarcodeEntity?

    @Query("SELECT * FROM product_units WHERE mainBarcode = :barcode")
    suspend fun findProductUnitByMainBarcode(barcode: String): ProductUnitEntity?

    @Query(
        "SELECT p.* FROM products p " +
                "INNER JOIN product_units pu ON p.id = pu.productId " +
                "LEFT JOIN barcodes b ON pu.id = b.productUnitId " +
                "WHERE pu.mainBarcode = :barcode OR b.code = :barcode LIMIT 1"
    )
    suspend fun findProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    fun getProductsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductUnit(unit: ProductUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcode(barcode: BarcodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductUnits(units: List<ProductUnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcodes(barcodes: List<BarcodeEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Update
    suspend fun updateProductUnit(unit: ProductUnitEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProductUnit(unit: ProductUnitEntity)

    @Delete
    suspend fun deleteBarcode(barcode: BarcodeEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("DELETE FROM product_units WHERE productId = :productId")
    suspend fun deleteProductUnitsForProduct(productId: String)

    @Query("DELETE FROM barcodes WHERE productId = :productId")
    suspend fun deleteBarcodesForProduct(productId: String)

    @Query("DELETE FROM barcodes WHERE productUnitId = :productUnitId AND productId = :productId")
    suspend fun deleteBarcodesForProductUnit(productId: String, productUnitId: String)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("DELETE FROM product_units")
    suspend fun deleteAllProductUnits()

    @Query("DELETE FROM barcodes")
    suspend fun deleteAllBarcodes()

    @Query("SELECT * FROM products WHERE id IN (:productIds)")
    suspend fun getProductsByIds(productIds: List<String>): List<ProductEntity>
}