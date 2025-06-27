package com.synngate.synnframe.presentation.ui.splash

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.synngate.synnframe.R
import com.synngate.synnframe.SynnFrameApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Composable функция для отображения экрана заставки внутри единой Activity
 */
@Composable
fun SplashScreenComposable(
    versionName: String,
    onInitializationComplete: (showServersOnStartup: Boolean, hasActiveServer: Boolean) -> Unit
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
    var initializationMessage by remember { mutableStateOf("Initialization...") }

    // Запускаем процесс инициализации
    LaunchedEffect(key1 = true) {
        fadeIn = true
        delay(300) // Небольшая задержка для начальной анимации

        coroutineScope.launch {
            try {
                // Стадия 1: Загрузка настроек
                initializationStage = InitializationStage.LOADING_SETTINGS
                initializationMessage = context.getString(R.string.splash_loading_settings)
                progress = 0.2f
                delay(300)

                // Стадия 2: Инициализация БД
                initializationStage = InitializationStage.INITIALIZING_DB
                initializationMessage = context.getString(R.string.splash_initializing_db)
                progress = 0.5f
                delay(500)

                // Стадия 3: Проверка зависимостей
                initializationStage = InitializationStage.CHECKING_DEPENDENCIES
                initializationMessage = context.getString(R.string.splash_checking_dependencies)
                progress = 0.8f
                delay(300)

                // Завершение инициализации
                initializationStage = InitializationStage.COMPLETED
                initializationMessage = context.getString(R.string.splash_completed)
                progress = 1f
                delay(300)

                // Определяем параметры запуска
                val showServersOnStartup = appContainer.appSettingsDataStore.showServersOnStartup.first()
                val hasActiveServer = appContainer.appSettingsDataStore.activeServerId.first() != null

                Timber.d("Initialization complete: showServersOnStartup=$showServersOnStartup, hasActiveServer=$hasActiveServer")

                // Минимальная задержка для отображения заставки
                delay(300)

                // Вызываем колбэк завершения
                onInitializationComplete(showServersOnStartup, hasActiveServer)
            } catch (e: Exception) {
                Timber.e(e, "Error during initialization")
                initializationMessage = context.getString(R.string.splash_error, e.message ?: "Unknown error")

                // В случае ошибки все равно продолжаем через 2 секунды
                delay(2000)
                onInitializationComplete(true, false) // По умолчанию открываем экран серверов
            }
        }
    }

    // UI экрана заставки
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
            text = stringResource(id = R.string.splash_copyright),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * Стадии инициализации (перенесено из SplashActivity)
 */
enum class InitializationStage {
    STARTING,
    LOADING_SETTINGS,
    INITIALIZING_DB,
    CHECKING_DEPENDENCIES,
    COMPLETED
}