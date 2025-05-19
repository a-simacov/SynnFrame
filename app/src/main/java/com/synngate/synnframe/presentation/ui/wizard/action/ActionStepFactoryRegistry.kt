package com.synngate.synnframe.presentation.ui.wizard.action

import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.presentation.di.Disposable
import timber.log.Timber

class ActionStepFactoryRegistry(
    private val factoryProviders: Map<ActionObjectType, () -> ActionStepFactory>
) : Disposable {
    // Хранит только фактически созданные фабрики
    private val factories = mutableMapOf<ActionObjectType, ActionStepFactory>()

    fun getFactory(objectType: ActionObjectType): ActionStepFactory? {
        return factories.getOrPut(objectType) {
            factoryProviders[objectType]?.invoke()?.also {
            } ?: run {
                return null
            }
        }
    }

    fun hasFactory(objectType: ActionObjectType): Boolean {
        return factoryProviders.containsKey(objectType)
    }

    fun getRegisteredObjectTypes(): List<ActionObjectType> {
        return factoryProviders.keys.toList()
    }

    fun getAllFactories(): Collection<ActionStepFactory> {
        return factories.values
    }

    fun clearAllCaches() {
        factories.values.forEach { factory ->
            factory.clearCache()
        }
    }

    override fun dispose() {
        factories.values.forEach { factory ->
            factory.dispose()
        }
        factories.clear()
    }
}