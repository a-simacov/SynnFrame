package com.synngate.synnframe

import android.app.Application
import android.content.Context
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.util.logging.AppTree
import com.synngate.synnframe.util.logging.LogLevelProvider
import com.synngate.synnframe.util.network.TrustAllCertificates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import kotlin.system.exitProcess

class SynnFrameApplication : Application() {

    lateinit var appContainer: AppContainer

    // Глобальная область корутин для задач, не привязанных к UI
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // 1. Сначала инициализируем Timber с простым DebugTree или специальным InitTree
        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // Только вывод в консоль, без сохранения в БД
                super.log(priority, tag, message, t)
            }
        })

        // 2. Инициализация DI контейнера
        appContainer = AppContainer(applicationContext)

        // 3. Заменяем Timber tree на полнофункциональный
        Timber.uprootAll() // Удаляем временный tree
        val logLevelProvider = LogLevelProvider(appContainer.getCoreContainer().appSettingsDataStore)
        Timber.plant(AppTree(appContainer.getCoreContainer().loggingService, logLevelProvider))

        // Настраиваем доверие всем SSL сертификатам (только для разработки)
        TrustAllCertificates.initialize()

        // Устанавливаем локаль из настроек
        launchInBackground {
            val languageCode = appContainer.getCoreContainer().appSettingsDataStore.languageCode.first()
            setAppLocale(languageCode)
        }

        initializeAppInsets()

        // Регистрируем обработчик неперехваченных исключений
        setupUncaughtExceptionHandler()
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun launchInBackground(block: suspend () -> Unit) {
        applicationScope.launch {
            block()
        }
    }

    private fun initializeAppInsets() {
        // Здесь можно получить характеристики устройства
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val screenWidthDp = displayMetrics.widthPixels / density
        val screenHeightDp = displayMetrics.heightPixels / density

        // Определяем тип устройства
        val isTablet = screenWidthDp >= 600 // Стандартный порог для планшетов

        // Устанавливаем значение в статическое поле нашего конфигуратора
        // Для этого нужно модифицировать AppInsetsConfig, чтобы оно содержало изменяемое поле
        AppInsetsConfigHolder.initialize(isTablet)
    }

    private fun setupUncaughtExceptionHandler() {
        // Установка обработчика неперехваченных исключений
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Unhandled exception in thread: ${thread.name}")

            try {
                // Попытка корректно освободить ресурсы перед завершением
                cleanupResources()
            } catch (e: Exception) {
                Timber.e(e, "Error during cleanup after uncaught exception")
            }

            // Вызываем оригинальный обработчик
            defaultExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Очистка ресурсов приложения.
     * Вызывается как при нормальном завершении, так и при критических ошибках.
     */
    private fun cleanupResources() {
        Timber.i("Cleaning up Application resources")

        try {
            // Отменяем все корутины в глобальной области
            applicationScope.cancel()

            // Освобождаем ресурсы DI контейнера
            if (::appContainer.isInitialized) {
                appContainer.dispose()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during application cleanup")
        }
    }

    override fun onTerminate() {
        Timber.i("Application terminating - cleaning up resources")
        cleanupResources()
        super.onTerminate()
    }

    /**
     * Завершение работы приложения с корректной очисткой ресурсов
     */
    fun exitApplication() {
        cleanupResources()
        exitProcess(0)
    }

    /**
     * Этот метод вызывается системой при низком уровне памяти
     */
    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("System is running low on memory - freeing non-essential resources")

        // Очистка кэшей и неиспользуемых ресурсов
        System.gc()
    }

    /**
     * Этот метод вызывается перед уничтожением процесса приложения
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        when (level) {
            // Приложение в фоне и система нуждается в памяти
            TRIM_MEMORY_COMPLETE,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_BACKGROUND -> {
                Timber.i("Trimming memory in background - level: $level")
                // Освобождаем кэши и другие ненужные ресурсы
            }

            // Приложение видимо, но не в фокусе
            TRIM_MEMORY_UI_HIDDEN -> {
                Timber.i("UI hidden - trimming UI resources")
                // Освобождаем ресурсы UI, которые не видны
            }

            // Приложение в фокусе, но система нуждается в памяти
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_MODERATE -> {
                Timber.i("System running low on memory - level: $level")
                // Освобождаем некритичные ресурсы
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        // Необходимо для мультиязычности
        super.attachBaseContext(base)
    }
}

object AppInsetsConfigHolder {
    var topInsetDp: Int = 0
        private set

    fun initialize(isTablet: Boolean) {
        topInsetDp = if (isTablet) 0 else -16
    }
}