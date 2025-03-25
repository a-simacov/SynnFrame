package com.synngate.synnframe.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
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
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    companion object {
        const val EXTRA_SHOW_SERVERS_SCREEN = "extra_show_servers_screen"
        const val EXTRA_FROM_SPLASH = "extra_from_splash"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение контейнера зависимостей
        appContainer = (application as SynnFrameApplication).appContainer

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

            // Получаем настройки темы из DataStore
            val settingsUseCases = appContainer.settingsUseCases

            val themeMode by appContainer.appSettingsDataStore.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            // Применение темы
            SynnFrameTheme(themeMode = themeMode, settingsUseCases = settingsUseCases) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Основная навигация приложения
                    AppNavigation(
                        appContainer = appContainer,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Очистка ресурсов при необходимости
        Timber.d("MainActivity onDestroy")
    }
}