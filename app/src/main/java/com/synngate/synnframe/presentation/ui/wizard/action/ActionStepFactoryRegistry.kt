package com.synngate.synnframe.presentation.ui.wizard.action

import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.presentation.di.Disposable
import timber.log.Timber

/**
 * Реестр фабрик для шагов действий.
 * Обеспечивает поиск подходящей фабрики по типу объекта и управление жизненным циклом фабрик.
 * Использует ленивую инициализацию фабрик для экономии памяти.
 */
class ActionStepFactoryRegistry(
    private val factoryProviders: Map<ActionObjectType, () -> ActionStepFactory>
) : Disposable {
    // Хранит только фактически созданные фабрики
    private val factories = mutableMapOf<ActionObjectType, ActionStepFactory>()

    /**
     * Возвращает фабрику для указанного типа объекта или null, если провайдер не найден.
     * Если фабрика еще не была создана, создает ее при первом запросе.
     */
    fun getFactory(objectType: ActionObjectType): ActionStepFactory? {
        return factories.getOrPut(objectType) {
            factoryProviders[objectType]?.invoke()?.also {
                Timber.d("Создана фабрика для типа объекта: $objectType")
            } ?: run {
                Timber.w("Провайдер фабрики не найден для типа объекта: $objectType")
                return null
            }
        }
    }

    /**
     * Проверяет наличие провайдера фабрики для указанного типа объекта
     */
    fun hasFactory(objectType: ActionObjectType): Boolean {
        return factoryProviders.containsKey(objectType)
    }

    /**
     * Возвращает список всех зарегистрированных типов объектов
     */
    fun getRegisteredObjectTypes(): List<ActionObjectType> {
        return factoryProviders.keys.toList()
    }

    /**
     * Возвращает все уже созданные фабрики
     */
    fun getAllFactories(): Collection<ActionStepFactory> {
        return factories.values
    }

    /**
     * Очищает кэши всех созданных фабрик
     */
    fun clearAllCaches() {
        factories.values.forEach { factory ->
            factory.clearCache()
        }
        Timber.d("Очищены кэши всех фабрик")
    }

    /**
     * Освобождает ресурсы всех созданных фабрик
     */
    override fun dispose() {
        Timber.d("Освобождение ресурсов всех фабрик в реестре")
        factories.values.forEach { factory ->
            factory.dispose()
        }
        factories.clear()
    }
}