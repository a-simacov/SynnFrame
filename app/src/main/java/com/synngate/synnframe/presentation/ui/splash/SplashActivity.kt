package com.synngate.synnframe.presentation.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Активность экрана заставки.
 * Выполняет инициализацию зависимостей и переходит к соответствующему экрану
 * в зависимости от настроек приложения.
 */
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

    /**
     * Переход к следующему экрану на основе настроек приложения
     */
    private fun navigateToNextScreen(showServersOnStartup: Boolean, hasActiveServer: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            // Передаем флаги для определения начального экрана
            putExtra(MainActivity.EXTRA_SHOW_SERVERS_SCREEN, showServersOnStartup || !hasActiveServer)

            // Указываем, что запуск производится из SplashActivity
            putExtra(MainActivity.EXTRA_FROM_SPLASH, true)

            // Флаги для корректного запуска активности
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

/**
 * Composable функция для отображения экрана заставки
 */
@Composable
fun SplashScreen(
    versionName: String,
    onInitializationComplete: (Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appContainer = (context.applicationContext as SynnFrameApplication).appContainer

    // Состояния для анимации
    var progress by remember { mutableFloatStateOf(0f) }
    var fadeIn by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (fadeIn) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "Alpha Animation"
    )

    // Стадии инициализации
    var initializationStage by remember { mutableStateOf(InitializationStage.STARTING) }
    var initializationMessage by remember { mutableStateOf("Инициализация...") }

    // Запускаем процесс инициализации
    LaunchedEffect(key1 = true) {
        fadeIn = true
        delay(300) // Небольшая задержка для начальной анимации

        coroutineScope.launch {
            try {
                // Стадия 1: Загрузка настроек
                initializationStage = InitializationStage.LOADING_SETTINGS
                initializationMessage = "Загрузка настроек..."
                progress = 0.2f
                delay(300) // Имитация загрузки

                // Стадия 2: Инициализация БД
                initializationStage = InitializationStage.INITIALIZING_DB
                initializationMessage = "Инициализация базы данных..."
                progress = 0.5f
                delay(500) // Имитация загрузки

                // Стадия 3: Проверка зависимостей
                initializationStage = InitializationStage.CHECKING_DEPENDENCIES
                initializationMessage = "Проверка зависимостей..."
                progress = 0.8f
                delay(300) // Имитация загрузки

                // Завершение инициализации
                initializationStage = InitializationStage.COMPLETED
                initializationMessage = "Инициализация завершена"
                progress = 1f
                delay(300) // Небольшая задержка перед переходом

                // Определяем, нужно ли показывать экран серверов при запуске
                val showServersOnStartup = appContainer.appSettingsDataStore.showServersOnStartup.first()

                // Определяем, есть ли активный сервер
                val hasActiveServer = appContainer.appSettingsDataStore.activeServerId.first() != null

                // Задержка для отображения заставки минимум 1.5 секунды
                delay(300)

                // Переходим к следующему экрану
                onInitializationComplete(showServersOnStartup, hasActiveServer)
            } catch (e: Exception) {
                // Логируем ошибку
                Timber.e(e, "Error during initialization")
                initializationMessage = "Ошибка инициализации: ${e.message}"

                // В случае ошибки все равно продолжаем через 2 секунды
                delay(2000)
                onInitializationComplete(true, false) // По умолчанию открываем экран серверов
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .alpha(alpha)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Логотип приложения
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "SynnFrame Logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Название приложения
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Версия приложения
            Text(
                text = stringResource(id = R.string.splash_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Индикатор прогресса
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(250.dp)
                    .height(6.dp),
                strokeCap = StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Стадия инициализации
            Text(
                text = initializationMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Копирайт внизу экрана
        Text(
            text = "© $2025 SynnGate",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * Стадии инициализации
 */
enum class InitializationStage {
    STARTING,
    LOADING_SETTINGS,
    INITIALIZING_DB,
    CHECKING_DEPENDENCIES,
    COMPLETED
}