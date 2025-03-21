package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.synngate.synnframe.data.local.entity.BarcodeEntity
import com.synngate.synnframe.data.local.entity.ProductEntity
import com.synngate.synnframe.data.local.entity.ProductUnitEntity
import com.synngate.synnframe.data.local.entity.ProductWithUnits
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Transaction
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsWithDetails(): Flow<List<ProductWithUnits>>

    @Transaction
    @Query("SELECT * FROM products WHERE name LIKE '%' || :nameFilter || '%' ORDER BY name ASC")
    fun getProductsByNameFilter(nameFilter: String): Flow<List<ProductWithUnits>>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductWithDetailsById(id: String): ProductWithUnits?

    @Transaction
    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getProductsByIds(ids: Set<String>): List<ProductEntity>

    @Transaction
    @Query("SELECT p.* FROM products p INNER JOIN product_units pu ON p.id = pu.productId WHERE pu.mainBarcode = :barcode OR EXISTS (SELECT 1 FROM barcodes b WHERE b.productId = p.id AND b.code = :barcode) LIMIT 1")
    suspend fun findProductByBarcode(barcode: String): ProductWithUnits?

    @Query("SELECT COUNT(*) FROM products")
    fun getProductsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductUnit(unit: ProductUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcode(barcode: BarcodeEntity)

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

    /**
     * Удаление товара по идентификатору (каскадно удаляет единицы измерения и штрихкоды)
     */
    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("DELETE FROM product_units WHERE productId = :productId")
    suspend fun deleteProductUnitsForProduct(productId: String)

    @Query("DELETE FROM barcodes WHERE productId = :productId")
    suspend fun deleteBarcodesForProduct(productId: String)

    @Query("DELETE FROM barcodes WHERE productUnitId = :productUnitId")
    suspend fun deleteBarcodesForProductUnit(productUnitId: String)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Transaction
    @Query("SELECT * FROM products WHERE id IN (:productIds)")
    suspend fun getProductsByIds(productIds: List<String>): List<ProductWithUnits>
}