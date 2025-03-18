// Файл: com.synngate.synnframe.domain.usecase.settings.SettingsUseCases.kt

package com.synngate.synnframe.domain.usecase.settings

import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import com.synngate.synnframe.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Use Case класс для операций с настройками
 */
class SettingsUseCases(
    private val settingsRepository: SettingsRepository,
    private val logRepository: LogRepository
) : BaseUseCase {

    // Геттеры для настроек
    val showServersOnStartup: Flow<Boolean> = settingsRepository.showServersOnStartup
    val periodicUploadEnabled: Flow<Boolean> = settingsRepository.periodicUploadEnabled
    val uploadIntervalSeconds: Flow<Int> = settingsRepository.uploadIntervalSeconds
    val themeMode: Flow<ThemeMode> = settingsRepository.themeMode
    val languageCode: Flow<String> = settingsRepository.languageCode
    val navigationButtonHeight: Flow<Float> = settingsRepository.navigationButtonHeight

    // Операции с настройками
    suspend fun setShowServersOnStartup(show: Boolean): Result<Unit> {
        return try {
            settingsRepository.setShowServersOnStartup(show)
            logRepository.logInfo("Настройка 'Показывать при запуске' установлена: $show")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting ShowServersOnStartup")
            logRepository.logError("Ошибка при изменении настройки: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setPeriodicUpload(enabled: Boolean, intervalSeconds: Int? = null): Result<Unit> {
        return try {
            settingsRepository.setPeriodicUpload(enabled, intervalSeconds)
            logRepository.logInfo("Настройка 'Периодическая выгрузка' установлена: $enabled, интервал: $intervalSeconds")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting PeriodicUpload")
            logRepository.logError("Ошибка при изменении настройки: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode): Result<Unit> {
        return try {
            settingsRepository.setThemeMode(mode)
            logRepository.logInfo("Настройка 'Тема оформления' установлена: $mode")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting ThemeMode")
            logRepository.logError("Ошибка при изменении настройки: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setLanguageCode(code: String): Result<Unit> {
        return try {
            settingsRepository.setLanguageCode(code)
            logRepository.logInfo("Настройка 'Язык интерфейса' установлена: $code")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting LanguageCode")
            logRepository.logError("Ошибка при изменении настройки: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setNavigationButtonHeight(height: Float): Result<Unit> {
        return try {
            settingsRepository.setNavigationButtonHeight(height)
            logRepository.logInfo("Настройка 'Высота кнопки навигации' установлена: $height")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting NavigationButtonHeight")
            logRepository.logError("Ошибка при изменении настройки: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun checkForUpdates(): Result<Pair<String?, String?>> {
        return try {
            val result = settingsRepository.checkForUpdates()

            if (result.isSuccess) {
                val (version, releaseDate) = result.getOrNull()!!
                logRepository.logInfo("Проверка обновлений: доступна версия $version")
            } else {
                logRepository.logWarning("Ошибка проверки обновлений: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Exception during checking for updates")
            logRepository.logError("Исключение при проверке обновлений: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun downloadUpdate(version: String): Result<String> {
        return try {
            val result = settingsRepository.downloadUpdate(version)

            if (result.isSuccess) {
                val filePath = result.getOrNull()!!
                logRepository.logInfo("Обновление загружено: $version, путь: $filePath")
            } else {
                logRepository.logWarning("Ошибка загрузки обновления: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Exception during downloading update")
            logRepository.logError("Исключение при загрузке обновления: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun installUpdate(filePath: String): Result<Boolean> {
        return try {
            val result = settingsRepository.installUpdate(filePath)

            if (result.isSuccess) {
                logRepository.logInfo("Обновление подготовлено к установке: $filePath")
            } else {
                logRepository.logWarning("Ошибка подготовки обновления: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Exception during installing update")
            logRepository.logError("Исключение при установке обновления: ${e.message}")
            Result.failure(e)
        }
    }
}