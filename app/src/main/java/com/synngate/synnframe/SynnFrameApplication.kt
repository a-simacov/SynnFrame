package com.synngate.synnframe

import android.app.Application
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.AppContainerImpl
import com.synngate.synnframe.util.logging.ReleaseTree
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

        // Инициализация DI контейнера
        appContainer = AppContainerImpl(applicationContext)

        Timber.i("SynnFrame Application initialized")
    }
}