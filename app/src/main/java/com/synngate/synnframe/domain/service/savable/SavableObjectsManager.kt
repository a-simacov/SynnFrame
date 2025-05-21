package com.synngate.synnframe.domain.service.savable

import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.savable.SavableObject
import com.synngate.synnframe.domain.entity.taskx.savable.createSavableObjectData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.time.LocalDateTime

class SavableObjectsManager : SavableObjectService {

    private val _objects = MutableStateFlow<List<SavableObject>>(emptyList())
    override val objects: StateFlow<List<SavableObject>> = _objects.asStateFlow()

    override fun addObject(objectType: ActionObjectType, data: Any, source: String): Boolean {
        try {
            val objectData = createSavableObjectData(objectType, data)

            if (objectData == null) {
                Timber.w("Невозможно создать SavableObjectData для типа $objectType и данных $data")
                return false
            }

            val newObject = SavableObject(
                objectType = objectType,
                objectData = objectData,
                savedAt = LocalDateTime.now(),
                source = source
            )

            val currentObjects = _objects.value
            val existingIndex = currentObjects.indexOfFirst { it.objectType == objectType }

            _objects.update { objects ->
                if (existingIndex != -1) {
                    objects.subList(0, existingIndex) + newObject
                } else {
                    objects + newObject
                }
            }

            Timber.d("Добавлен сохраняемый объект: ${newObject.getShortDescription()}")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении сохраняемого объекта")
            return false
        }
    }

    override fun removeObject(id: String): Boolean {
        val currentObjects = _objects.value
        val index = currentObjects.indexOfFirst { it.id == id }

        if (index == -1) {
            Timber.w("Объект с идентификатором $id не найден")
            return false
        }

        _objects.update { objects ->
            objects.subList(0, index)
        }

        Timber.d("Удален сохраняемый объект с id=$id и все последующие объекты")
        return true
    }

    override fun clear() {
        _objects.value = emptyList()
        Timber.d("Список сохраняемых объектов очищен")
    }

    override fun getObjectByType(type: ActionObjectType): SavableObject? {
        return _objects.value.lastOrNull { it.objectType == type }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDataByType(type: ActionObjectType): T? {
        val obj = getObjectByType(type) ?: return null
        return try {
            obj.objectData.extractData() as? T
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при получении данных объекта типа $type")
            null
        }
    }

    override fun getObjects(): List<SavableObject> {
        return _objects.value
    }

    override fun hasObjectOfType(type: ActionObjectType): Boolean {
        return _objects.value.any { it.objectType == type }
    }
}