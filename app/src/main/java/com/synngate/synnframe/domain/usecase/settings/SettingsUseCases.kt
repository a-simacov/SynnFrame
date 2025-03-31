package com.synngate.synnframe.domain.usecase.settings

import android.content.Context
import android.os.StatFs
import androidx.core.content.FileProvider
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.service.FileService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.BaseUseCase
import com.synngate.synnframe.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.io.File
import java.io.IOException

class SettingsUseCases(
    private val settingsRepository: SettingsRepository,
    private val loggingService: LoggingService,
    private val fileService: FileService,
    private val applicationContext: Context
) : BaseUseCase {

    val showServersOnStartup: Flow<Boolean> = settingsRepository.showServersOnStartup
    val periodicUploadEnabled: Flow<Boolean> = settingsRepository.periodicUploadEnabled
    val uploadIntervalSeconds: Flow<Int> = settingsRepository.uploadIntervalSeconds
    val themeMode: Flow<ThemeMode> = settingsRepository.themeMode
    val languageCode: Flow<String> = settingsRepository.languageCode
    val navigationButtonHeight: Flow<Float> = settingsRepository.navigationButtonHeight

    suspend fun setShowServersOnStartup(show: Boolean): Result<Unit> {
        return try {
            settingsRepository.setShowServersOnStartup(show)
            loggingService.logInfo("Настройка 'Показывать при запуске' установлена: $show")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting ShowServersOnStartup")
            loggingService.logError("Ошибка при изменении настройки: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setPeriodicUpload(enabled: Boolean, intervalSeconds: Int? = null): Result<Unit> {
        return try {
            settingsRepository.setPeriodicUpload(enabled, intervalSeconds)
            loggingService.logInfo("Настройка 'Периодическая выгрузка' установлена: $enabled, интервал: $intervalSeconds")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting PeriodicUpload")
            loggingService.logError("Ошибка при изменении настройки: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode): Result<Unit> {
        return try {
            settingsRepository.setThemeMode(mode)
            loggingService.logInfo("Настройка 'Тема оформления' установлена: $mode")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting ThemeMode")
            loggingService.logError("Ошибка при изменении настройки: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setLanguageCode(code: String): Result<Unit> {
        return try {
            settingsRepository.setLanguageCode(code)
            loggingService.logInfo("Настройка 'Язык интерфейса' установлена: $code")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting LanguageCode")
            loggingService.logError("Ошибка при изменении настройки: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setNavigationButtonHeight(height: Float): Result<Unit> {
        return try {
            settingsRepository.setNavigationButtonHeight(height)
            loggingService.logInfo("Настройка 'Высота кнопки навигации' установлена: $height")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting NavigationButtonHeight")
            loggingService.logError("Ошибка при изменении настройки: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun checkForUpdates(): Result<Pair<String?, String?>> {
        return try {
            val response = settingsRepository.getLatestAppVersion()

            when (response) {
                is ApiResult.Success -> {
                    val updateInfo = response.data
                    val serverVersion = updateInfo?.lastVersion

                    // Проверяем, является ли версия с сервера новее текущей
                    val currentVersion = BuildConfig.VERSION_NAME
                    val isNewVersionAvailable = serverVersion != null &&
                            isNewVersionAvailable(currentVersion, serverVersion)

                    if (isNewVersionAvailable) {
                        loggingService.logInfo("Проверка обновлений: доступна версия ${updateInfo?.lastVersion}")
                        Result.success(Pair(updateInfo?.lastVersion, updateInfo?.releaseDate))
                    } else {
                        loggingService.logInfo("Проверка обновлений: обновлений не найдено")
                        Result.success(Pair(null, null))
                    }
                }
                is ApiResult.Error -> {
                    loggingService.logWarning("Ошибка проверки обновлений: ${response.message}")
                    Result.failure(IOException("Failed to check for updates: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during checking for updates")
            loggingService.logError("Исключение при проверке обновлений: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun downloadUpdate(version: String): Result<String> {
        return try {
            // Проверяем наличие свободного места (примерно 50 МБ для APK)
            if (!fileService.hasEnoughStorage(50 * 1024 * 1024L)) {
                loggingService.logWarning("Недостаточно места для загрузки обновления")
                return Result.failure(IOException("Insufficient storage space"))
            }

            // Создаем директорию для обновлений, если она не существует
            if (!fileService.ensureDirectoryExists("updates")) {
                loggingService.logWarning("Не удалось создать директорию для обновлений")
                return Result.failure(IOException("Failed to create updates directory"))
            }

            // Загружаем обновление
            val response = settingsRepository.downloadAppUpdate(version)

            when (response) {
                is ApiResult.Success -> {
                    val responseBody = response.data

                    // Сохраняем файл
                    val fileName = "app-update-$version.apk"
                    val filePath = fileService.saveFile(fileName, responseBody)

                    if (filePath != null) {
                        loggingService.logInfo("Обновление загружено: $version, путь: $filePath")
                        Result.success(filePath)
                    } else {
                        loggingService.logWarning("Не удалось сохранить обновление")
                        Result.failure(IOException("Failed to save update file"))
                    }
                }
                is ApiResult.Error -> {
                    loggingService.logWarning("Ошибка загрузки обновления: ${response.message}")
                    Result.failure(IOException("Failed to download update: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during update download")
            loggingService.logError("Исключение при загрузке обновления: ${e.message}")
            Result.failure(e)
        }
    }
    // Вспомогательный метод для проверки доступного места
    private fun getAvailableStorage(): Long {
        val stat = StatFs(applicationContext.filesDir.path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    suspend fun installUpdate(filePath: String): Result<Boolean> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                loggingService.logError("Файл обновления не существует: $filePath")
                return Result.failure(IOException("Update file does not exist"))
            }

            // Бизнес-логика работы с FileProvider
            val apkUri = FileProvider.getUriForFile(
                applicationContext,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            loggingService.logInfo("Подготовка к установке обновления: $filePath")
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Exception during update installation preparation")
            loggingService.logError("Исключение при подготовке установки обновления: ${e.message}")
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
}