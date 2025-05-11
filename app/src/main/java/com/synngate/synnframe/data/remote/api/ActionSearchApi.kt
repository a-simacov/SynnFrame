package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.ActionSearchResponseDto

/**
 * API для поиска действий
 */
interface ActionSearchApi {

    /**
     * Поиск действия по значению
     * @param endpoint Эндпоинт для поиска
     * @param searchValue Значение для поиска
     * @return Результат поиска с ID действия или ошибкой
     */
    suspend fun searchAction(
        endpoint: String,
        searchValue: String
    ): ApiResult<ActionSearchResponseDto>
}