package com.synngate.synnframe.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.synngate.synnframe.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Класс для работы с настройками приложения через DataStore
 */
class AppSettingsDataStore(private val dataStore: DataStore<Preferences>) {

    companion object {
        // Ключи для настроек
        private val SHOW_SERVERS_ON_STARTUP = booleanPreferencesKey("show_servers_on_startup")
        private val ACTIVE_SERVER_ID = intPreferencesKey("active_server_id")
        private val ACTIVE_SERVER_NAME = stringPreferencesKey("active_server_name")
        private val PERIODIC_UPLOAD_ENABLED = booleanPreferencesKey("periodic_upload_enabled")
        private val UPLOAD_INTERVAL_SECONDS = intPreferencesKey("upload_interval_seconds")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE_CODE = stringPreferencesKey("language_code")
        private val NAVIGATION_BUTTON_HEIGHT = floatPreferencesKey("navigation_button_height")
        private val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        private val ALLOW_MOBILE_UPLOAD = booleanPreferencesKey("allow_mobile_upload")
        private val MOBILE_SIZE_LIMIT = intPreferencesKey("mobile_size_limit")
    }

    // Показывать ли экран серверов при запуске
    val showServersOnStartup: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SHOW_SERVERS_ON_STARTUP] ?: true
    }

    // ID активного сервера
    val activeServerId: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[ACTIVE_SERVER_ID]
    }

    // Имя активного сервера (для отображения)
    val activeServerName: Flow<String> = dataStore.data.map { preferences ->
        preferences[ACTIVE_SERVER_NAME] ?: ""
    }

    // Включена ли периодическая выгрузка заданий
    val periodicUploadEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PERIODIC_UPLOAD_ENABLED] ?: false
    }

    // Интервал выгрузки в секундах
    val uploadIntervalSeconds: Flow<Int> = dataStore.data.map { preferences ->
        preferences[UPLOAD_INTERVAL_SECONDS] ?: 300 // 5 минут по умолчанию
    }

    // Режим темы
    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val themeModeString = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(themeModeString)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid theme mode: $themeModeString")
            ThemeMode.SYSTEM
        }
    }

    // Код языка (ru или en)
    val languageCode: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_CODE] ?: "ru" // Русский по умолчанию
    }

    // Высота кнопки навигации
    val navigationButtonHeight: Flow<Float> = dataStore.data.map { preferences ->
        preferences[NAVIGATION_BUTTON_HEIGHT] ?: 72f // 72dp по умолчанию
    }

    // ID текущего пользователя
    val currentUserId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[CURRENT_USER_ID]
    }

    // Геттеры для новых настроек
    val allowMobileUpload: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ALLOW_MOBILE_UPLOAD] ?: false  // По умолчанию не разрешаем
    }

    val mobileSizeLimit: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MOBILE_SIZE_LIMIT] ?: 500_000  // По умолчанию 500 KB
    }

    // Методы для сохранения настроек

    suspend fun setShowServersOnStartup(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_SERVERS_ON_STARTUP] = show
        }
    }

    suspend fun setActiveServer(id: Int, name: String) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_SERVER_ID] = id
            preferences[ACTIVE_SERVER_NAME] = name
        }
    }

    suspend fun clearActiveServer() {
        dataStore.edit { preferences ->
            preferences.remove(ACTIVE_SERVER_ID)
            preferences.remove(ACTIVE_SERVER_NAME)
        }
    }

    suspend fun setPeriodicUpload(enabled: Boolean, intervalSeconds: Int? = null) {
        dataStore.edit { preferences ->
            preferences[PERIODIC_UPLOAD_ENABLED] = enabled
            intervalSeconds?.let {
                preferences[UPLOAD_INTERVAL_SECONDS] = it
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    suspend fun setLanguageCode(code: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_CODE] = code
        }
    }

    suspend fun setNavigationButtonHeight(height: Float) {
        dataStore.edit { preferences ->
            preferences[NAVIGATION_BUTTON_HEIGHT] = height
        }
    }

    suspend fun setCurrentUser(userId: String?) {
        dataStore.edit { preferences ->
            if (userId != null) {
                preferences[CURRENT_USER_ID] = userId
            } else {
                preferences.remove(CURRENT_USER_ID)
            }
        }
    }

    suspend fun setAllowMobileUpload(allow: Boolean) {
        dataStore.edit { preferences ->
            preferences[ALLOW_MOBILE_UPLOAD] = allow
        }
    }

    suspend fun setMobileSizeLimit(sizeInBytes: Int) {
        dataStore.edit { preferences ->
            preferences[MOBILE_SIZE_LIMIT] = sizeInBytes
        }
    }
}