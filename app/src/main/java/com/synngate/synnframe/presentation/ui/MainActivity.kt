package com.synngate.synnframe.presentation.ui

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
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
import com.synngate.synnframe.presentation.theme.ThemeMode
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение контейнера зависимостей
        appContainer = (application as SynnFrameApplication).appContainer

        // Настройка edge-to-edge дисплея
        WindowCompat.setDecorFitsSystemWindows(window, false)

        Timber.d("MainActivity onCreate")

        setContent {
            // Получение темы из настроек
            val themeMode by appContainer.appSettingsDataStore.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            // Применение темы
            SynnFrameTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Основная навигация приложения
                    AppNavigation(appContainer = appContainer)
                }
            }
        }
    }
}