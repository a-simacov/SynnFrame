package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.AppVersionDto

/**
 * Интерфейс для работы с API обновления приложения
 */
interface AppUpdateApi {
    /**
     * Получение информации о последней версии приложения
     *
     * @return ответ с информацией о последней версии
     */
    suspend fun getLastVersion(): ApiResult<AppVersionDto>

    /**
     * Загрузка обновления приложения
     *
     * @param version версия обновления
     * @return ответ с файлом обновления
     */
    suspend fun downloadUpdate(version: String): ApiResult<ByteArray>
}