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

/**
 * DAO для работы с товарами в базе данных
 */
@Dao
interface ProductDao {
    /**
     * Получение всех товаров с их единицами измерения и штрихкодами
     */
    @Transaction
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsWithDetails(): Flow<List<ProductWithUnits>>

    /**
     * Получение товаров с фильтрацией по имени
     */
    @Transaction
    @Query("SELECT * FROM products WHERE name LIKE '%' || :nameFilter || '%' ORDER BY name ASC")
    fun getProductsByNameFilter(nameFilter: String): Flow<List<ProductWithUnits>>

    /**
     * Получение товара по идентификатору
     */
    @Transaction
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductWithDetailsById(id: String): ProductWithUnits?

    /**
     * Поиск товара по штрихкоду
     */
    @Transaction
    @Query("SELECT p.* FROM products p INNER JOIN product_units pu ON p.id = pu.productId WHERE pu.mainBarcode = :barcode OR EXISTS (SELECT 1 FROM barcodes b WHERE b.productId = p.id AND b.code = :barcode) LIMIT 1")
    suspend fun findProductByBarcode(barcode: String): ProductWithUnits?

    /**
     * Получение количества товаров
     */
    @Query("SELECT COUNT(*) FROM products")
    fun getProductsCount(): Flow<Int>

    /**
     * Вставка товара
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    /**
     * Вставка единицы измерения товара
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductUnit(unit: ProductUnitEntity)

    /**
     * Вставка штрихкода
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcode(barcode: BarcodeEntity)

    /**
     * Обновление товара
     */
    @Update
    suspend fun updateProduct(product: ProductEntity)

    /**
     * Обновление единицы измерения товара
     */
    @Update
    suspend fun updateProductUnit(unit: ProductUnitEntity)

    /**
     * Удаление товара
     */
    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    /**
     * Удаление единицы измерения товара
     */
    @Delete
    suspend fun deleteProductUnit(unit: ProductUnitEntity)

    /**
     * Удаление штрихкода
     */
    @Delete
    suspend fun deleteBarcode(barcode: BarcodeEntity)

    /**
     * Удаление товара по идентификатору (каскадно удаляет единицы измерения и штрихкоды)
     */
    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    /**
     * Удаление всех единиц измерения товара
     */
    @Query("DELETE FROM product_units WHERE productId = :productId")
    suspend fun deleteProductUnitsForProduct(productId: String)

    /**
     * Удаление всех штрихкодов товара
     */
    @Query("DELETE FROM barcodes WHERE productId = :productId")
    suspend fun deleteBarcodesForProduct(productId: String)

    /**
     * Удаление всех штрихкодов единицы измерения
     */
    @Query("DELETE FROM barcodes WHERE productUnitId = :productUnitId")
    suspend fun deleteBarcodesForProductUnit(productUnitId: String)

    /**
     * Удаление всех товаров
     */
    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    /**
     * Поиск товаров по списку идентификаторов
     */
    @Transaction
    @Query("SELECT * FROM products WHERE id IN (:productIds)")
    suspend fun getProductsByIds(productIds: List<String>): List<ProductWithUnits>
}