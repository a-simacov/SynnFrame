package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.AppVersionDto
import kotlinx.coroutines.Job
import java.io.File

interface AppUpdateApi {

    suspend fun getLastVersion(): ApiResult<AppVersionDto>

    suspend fun downloadUpdateToFile(
        version: String,
        destinationFile: File,
        progressListener: DownloadProgressListener? = null,
        downloadJob: Job? = null
    ): ApiResult<Unit>
}