package com.synngate.synnframe

import android.app.Application
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.util.logging.ReleaseTree
import com.synngate.synnframe.util.network.TrustAllCertificates
import timber.log.Timber

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

        Timber.i("SynnFrame Application initialized")
    }

    override fun onTerminate() {
        super.onTerminate()

        // Очистка ресурсов при завершении работы приложения
        Timber.i("Cleaning up Application resources")
    }
}