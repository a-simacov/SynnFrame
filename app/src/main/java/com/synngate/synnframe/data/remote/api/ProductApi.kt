package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.domain.entity.Product

/**
 * Интерфейс для работы с API товаров
 */
interface ProductApi {
    /**
     * Получение списка товаров
     *
     * @return ответ со списком товаров
     */
    suspend fun getProducts(): ApiResult<List<Product>>
}