package com.synngate.synnframe.presentation.di

import android.content.Context
import com.synngate.synnframe.data.barcodescanner.BarcodeScannerFactory
import com.synngate.synnframe.data.barcodescanner.ScannerService
import com.synngate.synnframe.presentation.di.modules.CoreContainer
import com.synngate.synnframe.presentation.di.modules.DataContainer
import com.synngate.synnframe.presentation.di.modules.DomainContainer
import com.synngate.synnframe.presentation.di.modules.FeatureContainer
import com.synngate.synnframe.presentation.di.modules.NetworkContainer
import timber.log.Timber

/**
 * Основной контейнер для внедрения зависимостей в приложении.
 * Создает и предоставляет доступ к модульным контейнерам:
 * - CoreContainer: базовые сервисы и настройки
 * - NetworkContainer: сетевые компоненты
 * - DataContainer: компоненты доступа к данным
 * - DomainContainer: компоненты бизнес-логики
 * - Функциональные контейнеры для каждой функциональной области приложения
 *
 * @param applicationContext Контекст приложения
 */
class AppContainer(val applicationContext: Context) : DiContainer() {
    // applicationContext уже публичное с модификатором val


    // Хранилище для модульных контейнеров - ленивая инициализация
    private val coreContainer by lazy {
        Timber.d("Creating CoreContainer")
        CoreContainer(this)
    }

    private val networkContainer by lazy {
        Timber.d("Creating NetworkContainer")
        NetworkContainer(this, coreContainer)
    }

    private val dataContainer by lazy {
        Timber.d("Creating DataContainer")
        DataContainer(this, coreContainer, networkContainer)
    }

    private val domainContainer by lazy {
        Timber.d("Creating DomainContainer")
        DomainContainer(this, coreContainer, dataContainer, networkContainer)
    }

    // Хранилище для функциональных контейнеров (создаются по требованию)
    private val featureContainers = mutableMapOf<String, FeatureContainer>()

    // Фабрика для создания сканеров (общий компонент)
    val barcodeScannerFactory by lazy {
        BarcodeScannerFactory(applicationContext, dataContainer.settingsRepository)
    }

    // Сервис управления сканером (общий компонент)
    val scannerService by lazy {
        ScannerService(barcodeScannerFactory).also {
            // Автоматически инициализируем сканер при создании сервиса
            it.initialize()
        }
    }

    /**
     * Получение основных контейнеров
     */
    fun getCoreContainer() = coreContainer

    fun getNetworkContainer() = networkContainer

    fun getDataContainer() = dataContainer

    fun getDomainContainer() = domainContainer

    /**
     * Получение функционального контейнера по ключу
     * Если контейнер ещё не создан, то он создаётся
     *
     * @param key Ключ контейнера
     * @param factory Фабрика для создания контейнера
     * @return Созданный или существующий контейнер
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : FeatureContainer> getFeatureContainer(key: String, factory: () -> T): T {
        return featureContainers.getOrPut(key) {
            Timber.d("Creating feature container: $key")
            factory()
        } as T
    }

    /**
     * Создание контейнера для уровня навигации
     */
    fun createNavigationContainer(): NavigationContainer {
        return createChildContainer { NavigationContainer(this) }
    }

    init {
        // Инициализация основных компонентов при создании AppContainer
        Timber.d("Initializing AppContainer")

        // Инициализация каналов уведомлений
        coreContainer.notificationChannelManager.createNotificationChannels()

        // Инициализация модулей
        coreContainer.initialize()
        networkContainer.initialize()
        dataContainer.initialize()
        domainContainer.initialize()
    }

    /**
     * Освобождение ресурсов контейнера
     */
    override fun dispose() {
        Timber.d("Disposing AppContainer")

        // Освобождение ресурсов функциональных контейнеров
        featureContainers.values.forEach { it.dispose() }
        featureContainers.clear()

        // Освобождение ресурсов сервиса сканирования
        scannerService.dispose()

        // Последовательно освобождаем контейнеры, если они были инициализированы
        try { domainContainer.dispose() } catch (e: UninitializedPropertyAccessException) { /* Игнорируем */ }
        try { dataContainer.dispose() } catch (e: UninitializedPropertyAccessException) { /* Игнорируем */ }
        try { networkContainer.dispose() } catch (e: UninitializedPropertyAccessException) { /* Игнорируем */ }
        try { coreContainer.dispose() } catch (e: UninitializedPropertyAccessException) { /* Игнорируем */ }

        super.dispose()
    }
}