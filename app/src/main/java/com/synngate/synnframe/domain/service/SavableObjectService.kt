package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.SavableObject
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import kotlinx.coroutines.flow.StateFlow

interface SavableObjectService {

    val objects: StateFlow<List<SavableObject>>

    fun addObject(objectType: ActionObjectType, data: Any, source: String): Boolean

    fun removeObject(id: String): Boolean

    fun clear()

    fun getObjectByType(type: ActionObjectType): SavableObject?

    fun <T : Any> getDataByType(type: ActionObjectType): T?

    fun getObjects(): List<SavableObject>

    fun hasObjectOfType(type: ActionObjectType): Boolean
}