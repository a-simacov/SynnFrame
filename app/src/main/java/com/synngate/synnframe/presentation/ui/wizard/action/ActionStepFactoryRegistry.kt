package com.synngate.synnframe.presentation.ui.wizard.action

import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import timber.log.Timber

class ActionStepFactoryRegistry {
    private val factories = mutableMapOf<ActionObjectType, ActionStepFactory>()

    fun registerFactory(objectType: ActionObjectType, factory: ActionStepFactory) {
        factories[objectType] = factory
    }

    fun getFactory(objectType: ActionObjectType): ActionStepFactory? {
        val factory = factories[objectType]
        if (factory == null) {
            Timber.w("Factory not found for object type: $objectType")
        }
        return factory
    }

    fun hasFactory(objectType: ActionObjectType): Boolean {
        return factories.containsKey(objectType)
    }

    fun getRegisteredObjectTypes(): List<ActionObjectType> {
        return factories.keys.toList()
    }
}