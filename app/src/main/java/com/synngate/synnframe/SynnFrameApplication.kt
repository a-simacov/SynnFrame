package com.synngate.synnframe

import android.app.Application
import com.synngate.synnframe.data.barcodescanner.DeviceType
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.util.LocaleHelper
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
        initializeLanguage()

        launchInBackground {
            try {
                // Проверяем, был ли тип устройства установлен вручную
                val manuallySet = appContainer.settingsRepository.getDeviceTypeManuallySet().first()

                if (!manuallySet) {
                    // Получаем текущий тип устройства
                    val currentDeviceType = appContainer.settingsUseCases.deviceType.first()

                    // Если тип устройства не установлен или стандартный, выполняем автоопределение
                    if (currentDeviceType == DeviceType.STANDARD) {
                        Timber.i("Выполняется автоматическое определение типа устройства при первом запуске")
                        val detectedType = appContainer.deviceDetectionService.detectDeviceType(false)

                        if (detectedType != DeviceType.STANDARD) {
                            Timber.i("Обнаружен тип устройства: $detectedType, применяем настройку")
                            // Устанавливаем тип устройства БЕЗ флага ручной настройки
                            appContainer.settingsUseCases.setDeviceType(detectedType, false)

                            // Перезапускаем сканер с новым типом устройства
                            appContainer.scannerService.restart()
                        }
                    }
                } else {
                    Timber.i("Тип устройства установлен вручную, пропускаем автоопределение")
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при автоматическом определении типа устройства")
            }
        }

        initializeAppInsets()
    }

    private fun setAppLocale(languageCode: String) {
        try {
            // Используем LocaleHelper для обновления локали
            val updatedContext = LocaleHelper.updateLocale(applicationContext, languageCode)

            // Дополнительно обновляем глобальные настройки
            val resources = resources
            val configuration = resources.configuration
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            configuration.setLocale(locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)

            Timber.d("App locale set to: $languageCode")
        } catch (e: Exception) {
            Timber.e(e, "Error setting app locale to: $languageCode")
        }
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

    private fun initializeLanguage() {
        launchInBackground {
            try {
                // Проверяем, был ли уже установлен язык
                val isLanguageSet = appContainer.appSettingsDataStore.isLanguageSet.first()

                if (!isLanguageSet) {
                    // Если язык еще не был установлен, определяем системный язык
                    val systemLanguage = Locale.getDefault().language

                    // Проверяем, поддерживается ли системный язык приложением
                    val supportedLanguage = when (systemLanguage) {
                        "ru", "en" -> systemLanguage  // Поддерживаемые языки
                        else -> "en"  // По умолчанию английский для других языков
                    }

                    Timber.i("Setting system language as default: $supportedLanguage")
                    // Используем метод с контекстом для синхронизации с SharedPreferences
                    appContainer.settingsUseCases.setLanguageCode(supportedLanguage)

                    // Применяем локаль
                    setAppLocale(supportedLanguage)
                } else {
                    // Если язык уже был установлен, используем его
                    val languageCode = appContainer.appSettingsDataStore.languageCode.first()
                    setAppLocale(languageCode)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing language settings")
                // В случае ошибки используем язык по умолчанию
                setAppLocale("en")
            }
        }
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