package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.AppVersionDto

/**
 * Интерфейс для работы с API обновления приложения
 */
interface AppUpdateApi {

    suspend fun getLastVersion(): ApiResult<AppVersionDto>

    suspend fun downloadUpdate(
        version: String,
        progressListener: DownloadProgressListener? = null
    ): ApiResult<ByteArray>
}