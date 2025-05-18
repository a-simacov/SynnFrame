package com.synngate.synnframe.presentation.ui.wizard.action

import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.presentation.di.Disposable
import timber.log.Timber

/**
 * Реестр фабрик для шагов действий.
 * Обеспечивает поиск подходящей фабрики по типу объекта и управление жизненным циклом фабрик.
 */
class ActionStepFactoryRegistry : Disposable {
    private val factories = mutableMapOf<ActionObjectType, ActionStepFactory>()

    /**
     * Регистрирует фабрику для указанного типа объекта
     */
    fun registerFactory(objectType: ActionObjectType, factory: ActionStepFactory) {
        factories[objectType] = factory
        Timber.d("Зарегистрирована фабрика для типа объекта: $objectType")
    }

    /**
     * Возвращает фабрику для указанного типа объекта или null, если фабрика не найдена
     */
    fun getFactory(objectType: ActionObjectType): ActionStepFactory? {
        val factory = factories[objectType]
        if (factory == null) {
            Timber.w("Фабрика не найдена для типа объекта: $objectType")
        }
        return factory
    }

    /**
     * Проверяет наличие фабрики для указанного типа объекта
     */
    fun hasFactory(objectType: ActionObjectType): Boolean {
        return factories.containsKey(objectType)
    }

    /**
     * Возвращает список всех зарегистрированных типов объектов
     */
    fun getRegisteredObjectTypes(): List<ActionObjectType> {
        return factories.keys.toList()
    }

    /**
     * Очищает кэши всех зарегистрированных фабрик
     */
    fun clearAllCaches() {
        factories.values.forEach { factory ->
            factory.clearCache()
        }
        Timber.d("Очищены кэши всех фабрик")
    }

    /**
     * Освобождает ресурсы всех зарегистрированных фабрик
     */
    override fun dispose() {
        Timber.d("Освобождение ресурсов всех фабрик в реестре")
        factories.values.forEach { factory ->
            factory.dispose()
        }
        factories.clear()
    }
}