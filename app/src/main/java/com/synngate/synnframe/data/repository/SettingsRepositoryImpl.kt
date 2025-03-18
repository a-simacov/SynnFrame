package com.synngate.synnframe.data.repository

import android.content.Context
import androidx.core.content.FileProvider
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.AppUpdateApi
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.presentation.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Имплементация репозитория настроек
 */
class SettingsRepositoryImpl(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val appUpdateApi: AppUpdateApi,
    private val logRepository: LogRepository,
    private val applicationContext: Context
) : SettingsRepository {

    override val showServersOnStartup: Flow<Boolean> = appSettingsDataStore.showServersOnStartup

    override val periodicUploadEnabled: Flow<Boolean> = appSettingsDataStore.periodicUploadEnabled

    override val uploadIntervalSeconds: Flow<Int> = appSettingsDataStore.uploadIntervalSeconds

    override val themeMode: Flow<ThemeMode> = appSettingsDataStore.themeMode

    override val languageCode: Flow<String> = appSettingsDataStore.languageCode

    override val navigationButtonHeight: Flow<Float> = appSettingsDataStore.navigationButtonHeight

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

    override suspend fun checkForUpdates(): Result<Pair<String?, String?>> {
        return try {
            val response = appUpdateApi.getLastVersion()
            when (response) {
                is ApiResult.Success -> {
                    val updateInfo = response.data
                    logRepository.logInfo("Проверка обновлений: доступна версия ${updateInfo?.lastVersion}")
                    Result.success(Pair(updateInfo?.lastVersion, updateInfo?.releaseDate))
                }

                is ApiResult.Error -> {
                    logRepository.logError("Ошибка проверки обновлений: ${response.message}")
                    Result.failure(IOException("Failed to check for updates: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during check for updates")
            logRepository.logError("Исключение при проверке обновлений: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun downloadUpdate(version: String): Result<String> {
        return try {
            val response = appUpdateApi.downloadUpdate(version)
            when (response) {
                is ApiResult.Success -> {
                    val responseBody = response.data

                    // Создаем директорию для загрузки обновлений
                    val downloadDir = File(applicationContext.filesDir, "updates")
                    if (!downloadDir.exists()) {
                        downloadDir.mkdirs()
                    }

                    // Создаем файл для сохранения APK
                    val apkFile = File(downloadDir, "app-update-$version.apk")

                    // Записываем данные в файл
                    withContext(Dispatchers.IO) {
                        FileOutputStream(apkFile).use { outputStream ->
                            outputStream.write(responseBody)
                            outputStream.flush()
                        }
                    }

                    logRepository.logInfo("Обновление успешно загружено: $version")
                    Result.success(apkFile.absolutePath)
                }

                is ApiResult.Error -> {
                    logRepository.logError("Ошибка загрузки обновления: ${response.message}")
                    Result.failure(IOException("Failed to download update: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during update download")
            logRepository.logError("Исключение при загрузке обновления: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun installUpdate(filePath: String): Result<Boolean> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(IOException("Update file does not exist"))
            }

            // Создаем URI для файла с использованием FileProvider
            val apkUri = FileProvider.getUriForFile(
                applicationContext,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            // Возвращаем URI для последующей установки в Activity
            logRepository.logInfo("Подготовка к установке обновления: $filePath")
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Exception during update installation preparation")
            logRepository.logError("Исключение при подготовке установки обновления: ${e.message}")
            Result.failure(e)
        }
    }
}