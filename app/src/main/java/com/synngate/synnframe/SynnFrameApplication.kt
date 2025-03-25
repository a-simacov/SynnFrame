package com.synngate.synnframe

import android.app.Application
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.util.logging.ReleaseTree
import com.synngate.synnframe.util.network.TrustAllCertificates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

class SynnFrameApplication : Application() {

    // Ручной DI контейнер для всего приложения
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()

        // Инициализация Timber для логирования
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        // Настраиваем доверие всем SSL сертификатам (только для разработки)
        TrustAllCertificates.initialize()

        // Инициализация DI контейнера
        appContainer = AppContainer(applicationContext)

        // Устанавливаем локаль из настроек
        launchInBackground {
            val languageCode = appContainer.appSettingsDataStore.languageCode.first()
            setAppLocale(languageCode)
        }

        Timber.i("SynnFrame Application initialized")
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

    override fun onTerminate() {
        super.onTerminate()

        // Очистка ресурсов при завершении работы приложения
        Timber.i("Cleaning up Application resources")
    }
}