package com.synngate.synnframe

import android.app.Application
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.util.logging.AppTree
import com.synngate.synnframe.util.logging.LogLevelProvider
import com.synngate.synnframe.util.network.TrustAllCertificates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

class SynnFrameApplication : Application() {

    lateinit var appContainer: AppContainer

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
        val logLevelProvider = LogLevelProvider(appContainer.appSettingsDataStore)
        Timber.plant(AppTree(appContainer.loggingService, logLevelProvider))

        // Настраиваем доверие всем SSL сертификатам (только для разработки)
        TrustAllCertificates.initialize()

        // Устанавливаем локаль из настроек
        launchInBackground {
            val languageCode = appContainer.appSettingsDataStore.languageCode.first()
            setAppLocale(languageCode)
        }

        initializeAppInsets()
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
        CoroutineScope(Dispatchers.IO).launch {
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

    override fun onTerminate() {
        super.onTerminate()

        // Очистка ресурсов при завершении работы приложения
        Timber.i("Cleaning up Application resources")
        appContainer.dispose()
    }
}

object AppInsetsConfigHolder {
    var topInsetDp: Int = 0
        private set

    fun initialize(isTablet: Boolean) {
        topInsetDp = if (isTablet) 0 else -16
    }
}