package com.synngate.synnframe.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.synngate.synnframe.data.barcodescanner.DeviceType
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.util.logging.LogLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException

class AppSettingsDataStore(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val SHOW_SERVERS_ON_STARTUP = booleanPreferencesKey("show_servers_on_startup")
        private val ACTIVE_SERVER_ID = intPreferencesKey("active_server_id")
        private val ACTIVE_SERVER_NAME = stringPreferencesKey("active_server_name")
        private val PERIODIC_UPLOAD_ENABLED = booleanPreferencesKey("periodic_upload_enabled")
        private val UPLOAD_INTERVAL_SECONDS = intPreferencesKey("upload_interval_seconds")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE_CODE = stringPreferencesKey("language_code")
        private val NAVIGATION_BUTTON_HEIGHT = floatPreferencesKey("navigation_button_height")
        private val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        private val BIN_CODE_PATTERN = stringPreferencesKey("bin_code_pattern")
        const val DEFAULT_BIN_PATTERN = "^[a-zA-Z][0-9][0-9]{2}[1-9][1-9]$"
        private val LOG_LEVEL = stringPreferencesKey("log_level")
        const val DEFAULT_LOG_LEVEL = "FULL"
        private val deviceTypeKey = stringPreferencesKey("device_type")
        private val DEVICE_TYPE_MANUALLY_SET = booleanPreferencesKey("device_type_manually_set")
        const val SHARED_PREFS_NAME = "settings"
        const val SHARED_PREFS_LANGUAGE_KEY = "language_code"
    }

    val showServersOnStartup: Flow<Boolean> = dataStore.data.map { preferences ->
        false
    }

    val activeServerId: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[ACTIVE_SERVER_ID]
    }

    val periodicUploadEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PERIODIC_UPLOAD_ENABLED] ?: false
    }

    val uploadIntervalSeconds: Flow<Int> = dataStore.data.map { preferences ->
        preferences[UPLOAD_INTERVAL_SECONDS] ?: 300
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val themeModeString = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(themeModeString)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid theme mode: $themeModeString")
            ThemeMode.SYSTEM
        }
    }

    // Проверка, был ли уже установлен язык
    val isLanguageSet: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences.contains(LANGUAGE_CODE)
    }

    val languageCode: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_CODE] ?: "en"
    }

    val navigationButtonHeight: Flow<Float> = dataStore.data.map { preferences ->
        preferences[NAVIGATION_BUTTON_HEIGHT] ?: 72f
    }

    val binCodePattern: Flow<String> = dataStore.data.map { preferences ->
        preferences[BIN_CODE_PATTERN] ?: DEFAULT_BIN_PATTERN
    }

    val logLevel: Flow<LogLevel> = dataStore.data
        .map { preferences ->
            try {
                LogLevel.fromString(preferences[LOG_LEVEL] ?: DEFAULT_LOG_LEVEL)
            } catch (e: Exception) {
                LogLevel.FULL
            }
        }

    val deviceType: Flow<DeviceType> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading settings")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val deviceTypeValue = preferences[deviceTypeKey] ?: DeviceType.STANDARD.name
            try {
                DeviceType.valueOf(deviceTypeValue)
            } catch (e: IllegalArgumentException) {
                DeviceType.STANDARD
            }
        }

    val deviceTypeManuallySet: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading settings")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DEVICE_TYPE_MANUALLY_SET] ?: false
        }

    suspend fun setDeviceType(type: DeviceType, isManuallySet: Boolean = true) {
        dataStore.edit { preferences ->
            preferences[deviceTypeKey] = type.name
            preferences[DEVICE_TYPE_MANUALLY_SET] = isManuallySet
        }
    }

    suspend fun resetDeviceTypeManualFlag() {
        dataStore.edit { preferences ->
            preferences[DEVICE_TYPE_MANUALLY_SET] = false
        }
    }

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

    suspend fun setLanguageCode(code: String, context: Context) {
        // Сохраняем в DataStore
        dataStore.edit { preferences ->
            preferences[LANGUAGE_CODE] = code
        }

        // Синхронизируем с SharedPreferences для attachBaseContext
        try {
            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(SHARED_PREFS_LANGUAGE_KEY, code).apply()
            Timber.d("Language code synchronized to SharedPreferences: $code")
        } catch (e: Exception) {
            Timber.e(e, "Failed to synchronize language code to SharedPreferences")
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

    suspend fun setBinCodePattern(pattern: String) {
        dataStore.edit { preferences ->
            preferences[BIN_CODE_PATTERN] = pattern
        }
    }

    suspend fun setLogLevel(level: LogLevel) {
        dataStore.edit { preferences ->
            preferences[LOG_LEVEL] = level.name
        }
    }
}