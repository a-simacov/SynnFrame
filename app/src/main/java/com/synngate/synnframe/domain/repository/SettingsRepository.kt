package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.barcodescanner.DeviceType
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.DownloadProgressListener
import com.synngate.synnframe.data.remote.dto.AppVersionDto
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.util.logging.LogLevel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import java.io.File

interface SettingsRepository {

    val showServersOnStartup: Flow<Boolean>
    val periodicUploadEnabled: Flow<Boolean>
    val uploadIntervalSeconds: Flow<Int>
    val themeMode: Flow<ThemeMode>
    val languageCode: Flow<String>
    val navigationButtonHeight: Flow<Float>
    val logLevel: Flow<LogLevel>

    suspend fun setShowServersOnStartup(show: Boolean)
    suspend fun setPeriodicUpload(enabled: Boolean, intervalSeconds: Int? = null)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguageCode(code: String)
    suspend fun setNavigationButtonHeight(height: Float)

    // Низкоуровневые операции без бизнес-логики
    suspend fun getLatestAppVersion(): ApiResult<AppVersionDto>

    /**
     * Загружает обновление приложения напрямую в файл
     * Это предпочтительный метод для загрузки больших файлов обновлений
     */
    suspend fun downloadUpdateToFile(
        version: String,
        destinationFile: File,
        progressListener: DownloadProgressListener? = null,
        downloadJob: Job? = null
    ): ApiResult<Unit>

    fun getBinCodePattern(): Flow<String>

    suspend fun setBinCodePattern(pattern: String)
    suspend fun setLogLevel(level: LogLevel)

    fun getDeviceType(): Flow<DeviceType>

    suspend fun setDeviceType(type: DeviceType)
}