package com.synngate.synnframe.presentation.di.modules

import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.DiContainer
import timber.log.Timber

/**
 * Базовый класс для всех модульных контейнеров.
 * Наследуется от DiContainer и обеспечивает доступ к основному контейнеру.
 *
 * @param appContainer Основной контейнер приложения
 */
abstract class ModuleContainer(
    protected val appContainer: AppContainer
) : DiContainer() {

    /**
     * Название модуля для логирования
     */
    abstract val moduleName: String

    /**
     * Метод для ленивой инициализации модуля
     * Переопределяется в дочерних классах для специфичных действий при инициализации
     */
    open fun initialize() {
        Timber.d("Initializing module: $moduleName")
    }

    /**
     * Освобождение ресурсов контейнера
     */
    override fun dispose() {
        Timber.d("Disposing module: $moduleName")
        super.dispose()
    }
}