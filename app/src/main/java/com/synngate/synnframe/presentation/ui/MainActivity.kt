package com.synngate.synnframe.presentation.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.navigation.AppNavigation
import com.synngate.synnframe.presentation.navigation.Screen
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.presentation.util.LocaleHelper
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    companion object {
        const val EXTRA_SHOW_SERVERS_SCREEN = "extra_show_servers_screen"
        const val EXTRA_FROM_SPLASH = "extra_from_splash"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение приложения
        val app = application as SynnFrameApplication

        // Настройка edge-to-edge дисплея
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Определяем начальный экран
        val showServersScreen = intent.getBooleanExtra(EXTRA_SHOW_SERVERS_SCREEN, true)
        val fromSplash = intent.getBooleanExtra(EXTRA_FROM_SPLASH, false)

        val startDestination = if (showServersScreen) {
            Screen.ServerList.route
        } else {
            Screen.Login.route
        }

        Timber.d("MainActivity onCreate, startDestination=$startDestination, fromSplash=$fromSplash")

        setContent {
            val themeMode by app.appContainer.appSettingsDataStore.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            // Применение темы
            SynnFrameTheme(themeMode = themeMode, settingsUseCases = app.appContainer.settingsUseCases) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Основная навигация приложения
                    AppNavigation(
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    // В MainActivity.kt
    override fun attachBaseContext(newBase: Context) {
        // Получаем сохраненный код языка (можно использовать другой способ хранения)
        val sharedPreferences = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("language_code", "ru") ?: "ru"

        // Применяем локаль
        val context = LocaleHelper.updateLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Очистка ресурсов при необходимости
        Timber.d("MainActivity onDestroy")
    }
}