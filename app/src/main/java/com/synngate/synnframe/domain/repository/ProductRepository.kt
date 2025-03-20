package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    /**
     * Получение списка всех товаров
     */
    fun getProducts(): Flow<List<Product>>

    /**
     * Получение списка товаров с фильтрацией по имени
     */
    fun getProductsByNameFilter(nameFilter: String): Flow<List<Product>>

    /**
     * Получение товара по идентификатору
     */
    suspend fun getProductById(id: String): Product?

    /**
     * Получение товаров по списку идентификаторов
     */
    suspend fun getProductsByIds(ids: Set<String>): List<Product>

    /**
     * Поиск товара по штрихкоду
     */
    suspend fun findProductByBarcode(barcode: String): Product?

    /**
     * Получение количества товаров
     */
    fun getProductsCount(): Flow<Int>

    /**
     * Добавление нового товара
     */
    suspend fun addProduct(product: Product)

    /**
     * Добавление списка товаров
     */
    suspend fun addProducts(products: List<Product>)

    /**
     * Обновление существующего товара
     */
    suspend fun updateProduct(product: Product)

    /**
     * Удаление товара
     */
    suspend fun deleteProduct(id: String)

    /**
     * Удаление всех товаров
     */
    suspend fun deleteAllProducts()

    /**
     * Получение списка товаров с удаленного сервера
     */
    suspend fun getProductsFromRemote(): ApiResult<List<Product>>
}