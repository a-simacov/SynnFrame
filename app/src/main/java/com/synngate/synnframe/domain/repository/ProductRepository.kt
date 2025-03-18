package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.Product
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с товарами
 */
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
     * Синхронизация товаров с сервером
     */
    suspend fun syncProductsWithServer(): Result<Int>
}