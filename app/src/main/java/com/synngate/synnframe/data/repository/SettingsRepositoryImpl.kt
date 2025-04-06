package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.AppUpdateApi
import com.synngate.synnframe.data.remote.dto.AppVersionDto
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.util.logging.LogLevel
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val appUpdateApi: AppUpdateApi
) : SettingsRepository {

    override val showServersOnStartup: Flow<Boolean> = appSettingsDataStore.showServersOnStartup
    override val periodicUploadEnabled: Flow<Boolean> = appSettingsDataStore.periodicUploadEnabled
    override val uploadIntervalSeconds: Flow<Int> = appSettingsDataStore.uploadIntervalSeconds
    override val themeMode: Flow<ThemeMode> = appSettingsDataStore.themeMode
    override val languageCode: Flow<String> = appSettingsDataStore.languageCode
    override val navigationButtonHeight: Flow<Float> = appSettingsDataStore.navigationButtonHeight
    override val logLevel: Flow<LogLevel> = appSettingsDataStore.logLevel

    override suspend fun setShowServersOnStartup(show: Boolean) {
        appSettingsDataStore.setShowServersOnStartup(show)
    }

    override suspend fun setPeriodicUpload(enabled: Boolean, intervalSeconds: Int?) {
        appSettingsDataStore.setPeriodicUpload(enabled, intervalSeconds)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        appSettingsDataStore.setThemeMode(mode)
    }

    override suspend fun setLanguageCode(code: String) {
        appSettingsDataStore.setLanguageCode(code)
    }

    override suspend fun setNavigationButtonHeight(height: Float) {
        appSettingsDataStore.setNavigationButtonHeight(height)
    }

    override suspend fun getLatestAppVersion(): ApiResult<AppVersionDto> {
        return appUpdateApi.getLastVersion()
    }

    override suspend fun downloadAppUpdate(version: String): ApiResult<ByteArray> {
        return appUpdateApi.downloadUpdate(version)
    }

    override fun getBinCodePattern(): Flow<String> = appSettingsDataStore.binCodePattern

    override suspend fun setBinCodePattern(pattern: String) {
        appSettingsDataStore.setBinCodePattern(pattern)
    }

    override suspend fun setLogLevel(level: LogLevel) {
        appSettingsDataStore.setLogLevel(level)
    }
}