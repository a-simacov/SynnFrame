package com.synngate.synnframe.domain.usecase.settings

import android.content.Context
import androidx.core.content.FileProvider
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.BaseUseCase
import com.synngate.synnframe.presentation.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Use Case класс для операций с настройками
 */
class SettingsUseCases(
    private val settingsRepository: SettingsRepository,
    private val loggingService: LoggingService,
    private val applicationContext: Context
) : BaseUseCase {

    // Прокси для настроек приложения
    val showServersOnStartup: Flow<Boolean> = settingsRepository.showServersOnStartup
    val periodicUploadEnabled: Flow<Boolean> = settingsRepository.periodicUploadEnabled
    val uploadIntervalSeconds: Flow<Int> = settingsRepository.uploadIntervalSeconds
    val themeMode: Flow<ThemeMode> = settingsRepository.themeMode
    val languageCode: Flow<String> = settingsRepository.languageCode
    val navigationButtonHeight: Flow<Float> = settingsRepository.navigationButtonHeight

    // Методы установки настроек с бизнес-логикой
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

    // Методы работы с обновлениями с перенесенной бизнес-логикой
    suspend fun checkForUpdates(): Result<Pair<String?, String?>> {
        return try {
            val response = settingsRepository.getLatestAppVersion()

            when (response) {
                is ApiResult.Success -> {
                    val updateInfo = response.data
                    loggingService.logInfo("Проверка обновлений: доступна версия ${updateInfo?.lastVersion}")
                    Result.success(Pair(updateInfo?.lastVersion, updateInfo?.releaseDate))
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
            val response = settingsRepository.downloadAppUpdate(version)

            when (response) {
                is ApiResult.Success -> {
                    val responseBody = response.data

                    // Бизнес-логика работы с файловой системой
                    val downloadDir = File(applicationContext.filesDir, "updates")
                    if (!downloadDir.exists()) {
                        downloadDir.mkdirs()
                    }

                    val apkFile = File(downloadDir, "app-update-$version.apk")

                    withContext(Dispatchers.IO) {
                        FileOutputStream(apkFile).use { outputStream ->
                            outputStream.write(responseBody)
                            outputStream.flush()
                        }
                    }

                    loggingService.logInfo("Обновление загружено: $version, путь: ${apkFile.absolutePath}")
                    Result.success(apkFile.absolutePath)
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
}