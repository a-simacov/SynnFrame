package com.synngate.synnframe.presentation.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.data.barcodescanner.DataWedgeReceiver
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.navigation.AppNavHost
import com.synngate.synnframe.presentation.navigation.routes.AuthRoutes
import com.synngate.synnframe.presentation.navigation.routes.ServerRoutes
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.presentation.ui.splash.SplashScreenComposable
import com.synngate.synnframe.presentation.util.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val dataWedgeReceiver = DataWedgeReceiver()
    private var receiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение приложения
        val app = application as SynnFrameApplication

        // Настройка edge-to-edge дисплея
        WindowCompat.setDecorFitsSystemWindows(window, false)

        Timber.d("MainActivity onCreate")

        // Регистрируем DataWedge receiver
        registerDataWedgeReceiver()

        setContent {
            val themeMode by app.appContainer.appSettingsDataStore.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            // Применение темы
            SynnFrameTheme(
                themeMode = themeMode,
                settingsUseCases = app.appContainer.settingsUseCases
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(
                        LocalScannerService provides app.appContainer.scannerService
                    ) {
                        MainContent()
                    }
                }
            }
        }
    }

    private fun registerDataWedgeReceiver() {
        try {
            // Регистрируем receiver динамически
            val intentFilter = IntentFilter().apply {
                addAction("com.symbol.datawedge.api.RESULT_ACTION")
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            registerReceiver(dataWedgeReceiver, intentFilter)
            receiverRegistered = true
            Timber.d("DataWedgeReceiver registered dynamically")
        } catch (e: Exception) {
            Timber.e(e, "Error registering DataWedgeReceiver dynamically: ${e.message}")
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // Получаем сохраненный код языка из SharedPreferences
        val sharedPreferences = newBase.getSharedPreferences(
            AppSettingsDataStore.SHARED_PREFS_NAME,
            Context.MODE_PRIVATE
        )

        // Используем "en" как язык по умолчанию
        val languageCode = sharedPreferences.getString(
            AppSettingsDataStore.SHARED_PREFS_LANGUAGE_KEY,
            "en"
        ) ?: "en"

        // Применяем локаль
        val context = LocaleHelper.updateLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onDestroy() {
        // Обязательно отменяем регистрацию при уничтожении активности
        if (receiverRegistered) {
            try {
                unregisterReceiver(dataWedgeReceiver)
                receiverRegistered = false
                Timber.d("DataWedgeReceiver unregistered")
            } catch (e: Exception) {
                Timber.e(e, "Error unregistering DataWedgeReceiver: ${e.message}")
            }
        }
        super.onDestroy()
    }
}

@Composable
private fun MainContent() {
    val coroutineScope = rememberCoroutineScope()

    // Состояния для управления показом экранов
    var showSplash by remember { mutableStateOf(true) }
    var isInitialized by remember { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf<String?>(null) }

    // Анимированное переключение между Splash и основным контентом
    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        SplashScreenComposable(
            versionName = BuildConfig.VERSION_NAME,
            onInitializationComplete = { showServersOnStartup, hasActiveServer ->
                coroutineScope.launch {
                    // Определяем начальный экран
                    startDestination = if (showServersOnStartup || !hasActiveServer) {
                        ServerRoutes.ServersGraph.route
                    } else {
                        AuthRoutes.Login.route
                    }

                    isInitialized = true

                    // Небольшая задержка для плавности анимации
                    delay(300)
                    showSplash = false
                }
            }
        )
    }

    // Показываем основную навигацию только после инициализации
    if (isInitialized && !showSplash) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn()
        ) {
            startDestination?.let { destination ->
                AppNavHost(startDestination = destination)
            }
        }
    }
}