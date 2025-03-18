package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с настройками приложения
 */
interface SettingsRepository {
    /**
     * Показывать ли экран серверов при запуске
     */
    val showServersOnStartup: Flow<Boolean>

    /**
     * Установка показа экрана серверов при запуске
     */
    suspend fun setShowServersOnStartup(show: Boolean)

    /**
     * Включена ли периодическая выгрузка заданий
     */
    val periodicUploadEnabled: Flow<Boolean>

    /**
     * Интервал выгрузки в секундах
     */
    val uploadIntervalSeconds: Flow<Int>

    /**
     * Установка параметров периодической выгрузки
     */
    suspend fun setPeriodicUpload(enabled: Boolean, intervalSeconds: Int? = null)

    /**
     * Режим темы
     */
    val themeMode: Flow<ThemeMode>

    /**
     * Установка режима темы
     */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * Код языка (ru или en)
     */
    val languageCode: Flow<String>

    /**
     * Установка кода языка
     */
    suspend fun setLanguageCode(code: String)

    /**
     * Высота кнопки навигации
     */
    val navigationButtonHeight: Flow<Float>

    /**
     * Установка высоты кнопки навигации
     */
    suspend fun setNavigationButtonHeight(height: Float)

    /**
     * Проверка наличия обновлений
     */
    suspend fun checkForUpdates(): Result<Pair<String?, String?>>

    /**
     * Загрузка обновления
     */
    suspend fun downloadUpdate(version: String): Result<String>

    /**
     * Установка загруженного обновления
     */
    suspend fun installUpdate(filePath: String): Result<Boolean>
}