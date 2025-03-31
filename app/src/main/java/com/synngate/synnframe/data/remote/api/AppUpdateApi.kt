package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.AppVersionDto

interface AppUpdateApi {

    suspend fun getLastVersion(): ApiResult<AppVersionDto>

    suspend fun downloadUpdate(
        version: String,
        progressListener: DownloadProgressListener? = null
    ): ApiResult<ByteArray>
}