package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.dto.AppVersionDto
import com.synngate.synnframe.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val showServersOnStartup: Flow<Boolean>
    val periodicUploadEnabled: Flow<Boolean>
    val uploadIntervalSeconds: Flow<Int>
    val themeMode: Flow<ThemeMode>
    val languageCode: Flow<String>
    val navigationButtonHeight: Flow<Float>

    suspend fun setShowServersOnStartup(show: Boolean)
    suspend fun setPeriodicUpload(enabled: Boolean, intervalSeconds: Int? = null)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguageCode(code: String)
    suspend fun setNavigationButtonHeight(height: Float)

    // Низкоуровневые операции без бизнес-логики
    suspend fun getLatestAppVersion(): ApiResult<AppVersionDto>
    suspend fun downloadAppUpdate(version: String): ApiResult<ByteArray>
}