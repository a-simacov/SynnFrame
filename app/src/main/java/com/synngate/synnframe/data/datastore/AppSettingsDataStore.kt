package com.synngate.synnframe.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.presentation.ui.tasks.model.ScanOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

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
        private val ALLOW_MOBILE_UPLOAD = booleanPreferencesKey("allow_mobile_upload")
        private val MOBILE_SIZE_LIMIT = intPreferencesKey("mobile_size_limit")
        private val BIN_CODE_PATTERN = stringPreferencesKey("bin_code_pattern")
        private val SCAN_ORDER = stringPreferencesKey("scan_order")
        const val DEFAULT_BIN_PATTERN = "{Aisle:@[a-zA-Z][0-9]}{Rack:@[0-9]{2}}{Shelf:@[1-9]{1}}{Position:@[1-9]}"
    }

    val showServersOnStartup: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SHOW_SERVERS_ON_STARTUP] ?: true
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

    val languageCode: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_CODE] ?: "ru"
    }

    val navigationButtonHeight: Flow<Float> = dataStore.data.map { preferences ->
        preferences[NAVIGATION_BUTTON_HEIGHT] ?: 72f
    }

    val allowMobileUpload: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ALLOW_MOBILE_UPLOAD] ?: false
    }

    val mobileSizeLimit: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MOBILE_SIZE_LIMIT] ?: 500_000
    }

    // Шаблон кода ячейки
    val binCodePattern: Flow<String> = dataStore.data.map { preferences ->
        preferences[BIN_CODE_PATTERN] ?: DEFAULT_BIN_PATTERN
    }

    // Порядок сканирования
    val scanOrder: Flow<ScanOrder> = dataStore.data.map { preferences ->
        val orderString = preferences[SCAN_ORDER] ?: ScanOrder.PRODUCT_FIRST.name
        try {
            ScanOrder.valueOf(orderString)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid scan order: $orderString")
            ScanOrder.PRODUCT_FIRST
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

    // Методы для установки настроек ячеек и порядка сканирования
    suspend fun setBinCodePattern(pattern: String) {
        dataStore.edit { preferences ->
            preferences[BIN_CODE_PATTERN] = pattern
        }
    }

    suspend fun setScanOrder(order: ScanOrder) {
        dataStore.edit { preferences ->
            preferences[SCAN_ORDER] = order.name
        }
    }
}