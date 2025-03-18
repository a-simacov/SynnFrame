package com.synngate.synnframe.presentation.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.R
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.presentation.ui.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получение контейнера зависимостей
        val appContainer = (application as SynnFrameApplication).appContainer

        Timber.d("SplashActivity onCreate")

        setContent {
            // Получение темы из настроек
            val themeMode by appContainer.appSettingsDataStore.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            SynnFrameTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashScreen(
                        versionName = BuildConfig.VERSION_NAME,
                        onInitializationComplete = { showServersOnStartup, hasActiveServer ->
                            navigateToNextScreen(showServersOnStartup, hasActiveServer)
                        }
                    )
                }
            }
        }
    }

    private fun navigateToNextScreen(showServersOnStartup: Boolean, hasActiveServer: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            // Передаем флаги для определения начального экрана
            putExtra("SHOW_SERVERS_SCREEN", showServersOnStartup || !hasActiveServer)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun SplashScreen(
    versionName: String,
    onInitializationComplete: (Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appContainer = (context.applicationContext as SynnFrameApplication).appContainer

    // Эффект для имитации задержки при загрузке и проверке настроек
    LaunchedEffect(key1 = true) {
        coroutineScope.launch {
            // Минимальная задержка для отображения экрана заставки
            delay(1500)

            // Определяем, нужно ли показывать экран серверов при запуске
            val showServersOnStartup = appContainer.appSettingsDataStore.showServersOnStartup.first()

            // Определяем, есть ли активный сервер
            val hasActiveServer = appContainer.appSettingsDataStore.activeServerId.first() != null

            // Переходим к следующему экрану
            onInitializationComplete(showServersOnStartup, hasActiveServer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Логотип приложения
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Название приложения
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Версия приложения
            Text(
                text = stringResource(id = R.string.splash_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Индикатор загрузки
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Сообщение об инициализации
            Text(
                text = stringResource(id = R.string.splash_initializing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}