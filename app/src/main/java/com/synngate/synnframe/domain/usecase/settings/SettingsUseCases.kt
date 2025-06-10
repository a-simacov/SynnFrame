package com.synngate.synnframe.domain.usecase.settings

import android.content.Context
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.data.barcodescanner.DeviceType
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.DownloadProgressListener
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.service.FileService
import com.synngate.synnframe.domain.usecase.BaseUseCase
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.util.logging.LogLevel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class SettingsUseCases(
    private val settingsRepository: SettingsRepository,
    private val fileService: FileService,
    private val applicationContext: Context
) : BaseUseCase {

    val showServersOnStartup: Flow<Boolean> = settingsRepository.showServersOnStartup
    val periodicUploadEnabled: Flow<Boolean> = settingsRepository.periodicUploadEnabled
    val uploadIntervalSeconds: Flow<Int> = settingsRepository.uploadIntervalSeconds
    val themeMode: Flow<ThemeMode> = settingsRepository.themeMode
    val languageCode: Flow<String> = settingsRepository.languageCode
    val navigationButtonHeight: Flow<Float> = settingsRepository.navigationButtonHeight

    val binCodePattern = settingsRepository.getBinCodePattern()
    val logLevel: Flow<LogLevel> = settingsRepository.logLevel

    val deviceType = settingsRepository.getDeviceType()

    suspend fun setShowServersOnStartup(show: Boolean): Result<Unit> {
        return try {
            settingsRepository.setShowServersOnStartup(show)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting ShowServersOnStartup")
            Result.failure(e)
        }
    }

    suspend fun setPeriodicUpload(enabled: Boolean, intervalSeconds: Int? = null): Result<Unit> {
        return try {
            settingsRepository.setPeriodicUpload(enabled, intervalSeconds)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting PeriodicUpload")
            Result.failure(e)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode): Result<Unit> {
        return try {
            settingsRepository.setThemeMode(mode)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting ThemeMode")
            Result.failure(e)
        }
    }

    suspend fun setLanguageCode(code: String): Result<Unit> {
        return try {
            // Используем новый метод, который синхронизирует DataStore и SharedPreferences
            settingsRepository.setLanguageCode(code, applicationContext)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting LanguageCode")
            Result.failure(e)
        }
    }

    suspend fun setNavigationButtonHeight(height: Float): Result<Unit> {
        return try {
            settingsRepository.setNavigationButtonHeight(height)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting NavigationButtonHeight")
            Result.failure(e)
        }
    }

    suspend fun checkForUpdates(): Result<Pair<String?, String?>> {
        return try {
            val response = settingsRepository.getLatestAppVersion()

            when (response) {
                is ApiResult.Success -> {
                    val updateInfo = response.data
                    val serverVersion = updateInfo.lastVersion

                    // Проверяем, является ли версия с сервера новее текущей
                    val currentVersion = BuildConfig.VERSION_NAME
                    val isNewVersionAvailable = isNewVersionAvailable(currentVersion, serverVersion)

                    if (isNewVersionAvailable) {
                        Result.success(Pair(updateInfo.lastVersion, updateInfo.releaseDate))
                    } else {
                        Result.success(Pair(null, null))
                    }
                }
                is ApiResult.Error -> {
                    Result.failure(IOException("Failed to check for updates: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during checking for updates")
            Result.failure(e)
        }
    }

    suspend fun downloadUpdate(
        version: String,
        progressListener: DownloadProgressListener? = null,
        downloadJob: Job? = null
    ): Result<String> {
        return try {
            // Проверяем наличие свободного места (примерно 150 МБ для APK)
            if (!fileService.hasEnoughStorage(150 * 1024 * 1024L)) {
                return Result.failure(IOException("Insufficient storage space"))
            }

            // Создаем директорию для обновлений, если она не существует
            if (!fileService.ensureDirectoryExists("updates")) {
                return Result.failure(IOException("Failed to create updates directory"))
            }

            // Подготавливаем файл для сохранения обновления
            val fileName = "app-update-$version.apk"
            val destinationFile = File(applicationContext.filesDir, "updates/$fileName")

            // Загружаем обновление
            val result = settingsRepository.downloadUpdateToFile(
                version = version,
                destinationFile = destinationFile,
                progressListener = progressListener,
                downloadJob = downloadJob
            )

            when (result) {
                is ApiResult.Success -> {
                    Result.success(destinationFile.absolutePath)
                }
                is ApiResult.Error -> {
                    Result.failure(IOException("Failed to download update: ${result.message}"))
                }
            }
        } catch (e: CancellationException) {
            // Пробрасываем исключение отмены для корректной обработки выше
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Exception during update download")
            Result.failure(e)
        }
    }

    private fun isNewVersionAvailable(currentVersion: String, serverVersion: String): Boolean {
        try {
            val current = currentVersion.split(".").map { it.toInt() }
            val server = serverVersion.split(".").map { it.toInt() }

            for (i in 0 until minOf(current.size, server.size)) {
                if (server[i] > current[i]) return true
                if (server[i] < current[i]) return false
            }

            return server.size > current.size
        } catch (e: Exception) {
            Timber.e(e, "Error comparing versions")
            return false
        }
    }

    suspend fun setBinCodePattern(pattern: String) {
        settingsRepository.setBinCodePattern(pattern)
    }

    suspend fun setLogLevel(level: LogLevel): Result<Unit> {
        return try {
            settingsRepository.setLogLevel(level)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating log level")
            Result.failure(e)
        }
    }

    suspend fun setDeviceType(type: DeviceType, isManuallySet: Boolean = true) {
        settingsRepository.setDeviceType(type, isManuallySet)
    }

    // Добавить новый метод для сброса флага ручной настройки:
    suspend fun resetDeviceTypeManualFlag() {
        settingsRepository.resetDeviceTypeManualFlag()
    }
}