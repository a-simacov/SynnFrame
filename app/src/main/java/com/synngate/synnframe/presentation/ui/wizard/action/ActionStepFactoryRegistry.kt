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
        Timber.d("Registered factory for object type: $objectType")
    }

    /**
     * Получает фабрику для типа объекта
     * @param objectType Тип объекта
     * @return Фабрика компонентов или null, если не найдена
     */
    fun getFactory(objectType: ActionObjectType): ActionStepFactory? {
        return factories[objectType]
    }
}