package com.synngate.synnframe.presentation.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.data.barcodescanner.DataWedgeReceiver
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.navigation.AppNavHost
import com.synngate.synnframe.presentation.navigation.routes.AuthRoutes
import com.synngate.synnframe.presentation.navigation.routes.ServerRoutes
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.presentation.util.LocaleHelper
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    private val dataWedgeReceiver = DataWedgeReceiver()
    private var receiverRegistered = false

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
            ServerRoutes.ServerList.route
        } else {
            AuthRoutes.Login.route
        }

        Timber.d("MainActivity onCreate, startDestination=$startDestination, fromSplash=$fromSplash")

        registerDataWedgeReceiver()

        setContent {
            val themeMode by app.appContainer.appSettingsDataStore.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            // Применение темы
            SynnFrameTheme(themeMode = themeMode, settingsUseCases = app.appContainer.settingsUseCases) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(
                        LocalScannerService provides app.appContainer.scannerService
                    ) {
                        AppNavHost(startDestination = startDestination)
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