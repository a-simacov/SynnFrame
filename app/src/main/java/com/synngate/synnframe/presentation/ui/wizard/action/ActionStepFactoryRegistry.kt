package com.synngate.synnframe.presentation.ui.wizard.action

import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import timber.log.Timber

/**
 * Реестр фабрик компонентов шагов действий
 */
class ActionStepFactoryRegistry {
    private val factories = mutableMapOf<ActionObjectType, ActionStepFactory>()

    /**
     * Регистрирует фабрику для типа объекта
     * @param objectType Тип объекта
     * @param factory Фабрика компонентов
     */
    fun registerFactory(objectType: ActionObjectType, factory: ActionStepFactory) {
        factories[objectType] = factory
        Timber.d("Registered factory for object type: $objectType (${factory::class.java.simpleName})")
    }

    /**
     * Получает фабрику для типа объекта
     * @param objectType Тип объекта
     * @return Фабрика компонентов или null, если не найдена
     */
    fun getFactory(objectType: ActionObjectType): ActionStepFactory? {
        val factory = factories[objectType]
        if (factory == null) {
            Timber.w("Factory not found for object type: $objectType")
        }
        return factory
    }

    /**
     * Проверяет, зарегистрирована ли фабрика для указанного типа объекта
     * @param objectType Тип объекта
     * @return true, если фабрика зарегистрирована, иначе false
     */
    fun hasFactory(objectType: ActionObjectType): Boolean {
        return factories.containsKey(objectType)
    }

    /**
     * Возвращает список всех зарегистрированных типов объектов
     * @return Список типов объектов
     */
    fun getRegisteredObjectTypes(): List<ActionObjectType> {
        return factories.keys.toList()
    }
}