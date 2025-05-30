package com.synngate.synnframe.presentation.navigation

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.presentation.ui.taskx.buffer.TaskBuffer
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.model.filter.PlannedActionsFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * Глобальный синглтон для хранения данных задания в памяти.
 * Данные не очищаются при переходе между экранами.
 */
object TaskXDataHolderSingleton {
    private val _currentTask = MutableStateFlow<TaskX?>(null)
    val currentTask: StateFlow<TaskX?> = _currentTask.asStateFlow()

    val currentTaskType: TaskTypeX?
        get() = _currentTask.value?.taskType

    private val _taskBuffer = TaskBuffer()
    val taskBuffer: TaskBuffer get() = _taskBuffer

    private var _endpoint: String? = null
    val endpoint: String? get() = _endpoint

    // Фильтр действий
    private val _actionsFilter = PlannedActionsFilter()
    val actionsFilter: PlannedActionsFilter get() = _actionsFilter

    // Отслеживание объектов, добавленных в буфер из фильтра
    private val filterObjectsAddedToBuffer = mutableSetOf<FactActionField>()

    // Последнее добавленное значение фильтра
    private var lastAddedSearchValue: Any? = null
    private var lastAddedSearchField: FactActionField? = null

    fun setTaskData(task: TaskX, taskType: TaskTypeX, endpoint: String) {
        Timber.d("TaskXDataHolderSingleton: установка данных задания ${task.id} с типом ${taskType.id}")

        val taskWithType = if (task.taskType == null) {
            task.copy(taskType = taskType)
        } else {
            task
        }

        _currentTask.value = taskWithType
        _endpoint = endpoint
        // НЕ очищаем буфер при установке данных, чтобы сохранять данные между заданиями

        Timber.d("TaskXDataHolderSingleton: данные задания установлены, endpoint = $endpoint")
    }

    fun updateTask(task: TaskX) {
        Timber.d("TaskXDataHolderSingleton: обновление задания ${task.id}")
        _currentTask.value = task
    }

    fun addFactAction(factAction: FactAction) {
        Timber.d("TaskXDataHolderSingleton: добавление фактического действия ${factAction.id} к заданию ${factAction.taskId}")
        _currentTask.update { task ->
            task?.let {
                val updatedFactActions = it.factActions + factAction
                val updatedPlannedActions = it.plannedActions.map { plannedAction ->
                    if (plannedAction.id == factAction.plannedActionId) {
                        plannedAction.copy(isCompleted = true)
                    } else {
                        plannedAction
                    }
                }

                it.copy(
                    factActions = updatedFactActions,
                    plannedActions = updatedPlannedActions
                )
            }
        }
    }

    fun hasData(): Boolean {
        val hasTask = _currentTask.value != null
        val hasEndpoint = _endpoint != null
        Timber.d("TaskXDataHolderSingleton: проверка наличия данных: hasTask=$hasTask, hasEndpoint=$hasEndpoint")
        return hasTask && hasEndpoint
    }

    fun addFilterAndSaveToBuffer(field: FactActionField, value: Any, saveToBuffer: Boolean) {
        // Добавляем фильтр
        _actionsFilter.addFilter(field, value)

        // Сохраняем последнее добавленное значение для поиска
        lastAddedSearchField = field
        lastAddedSearchValue = value

        Timber.d("Добавлен фильтр и сохранено значение поиска: поле $field, значение $value")

        // Если нужно сохранить в буфер и это ещё не было сделано
        if (saveToBuffer && !filterObjectsAddedToBuffer.contains(field)) {
            saveObjectToBuffer(field, value)
            filterObjectsAddedToBuffer.add(field)

            Timber.d("Объект из фильтра по полю $field добавлен в буфер")
        }
    }

    /**
     * Получает последнее использованное для поиска значение для указанного поля
     */
    fun getLastSearchValueForField(field: FactActionField): Any? {
        return if (lastAddedSearchField == field) lastAddedSearchValue else null
    }

    /**
     * Удаляет фильтр по указанному полю и очищает объект из буфера, если он был добавлен из фильтра
     */
    fun removeFilterAndClearFromBuffer(field: FactActionField) {
        // Удаляем фильтр
        _actionsFilter.removeFilter(field)

        // Если объект был добавлен в буфер из фильтра, удаляем его оттуда
        if (filterObjectsAddedToBuffer.contains(field)) {
            taskBuffer.clearField(field)
            filterObjectsAddedToBuffer.remove(field)

            Timber.d("Объект из буфера по полю $field удален после удаления фильтра")
        }

        // Очищаем последнее добавленное значение, если оно соответствует удаляемому полю
        if (lastAddedSearchField == field) {
            lastAddedSearchField = null
            lastAddedSearchValue = null
            Timber.d("Очищено последнее добавленное значение поиска для поля $field")
        }
    }

    /**
     * Очищает все фильтры и объекты, добавленные в буфер из фильтра
     */
    fun clearAllFiltersAndBufferObjects() {
        // Очищаем все фильтры
        _actionsFilter.clearAllFilters()

        // Удаляем все объекты, добавленные в буфер из фильтра
        filterObjectsAddedToBuffer.forEach { field ->
            taskBuffer.clearField(field)
        }

        filterObjectsAddedToBuffer.clear()

        // Очищаем последнее добавленное значение
        lastAddedSearchField = null
        lastAddedSearchValue = null

        Timber.d("Очищены все фильтры и связанные объекты в буфере")
    }

    /**
     * Очищает последний добавленный фильтр и удаляет объект из буфера, если он был добавлен из фильтра
     */
    fun clearLastAddedFilter() {
        val lastField = _actionsFilter.getLastAddedFilterField() ?: return

        // Удаляем фильтр и очищаем из буфера, если нужно
        removeFilterAndClearFromBuffer(lastField)

        Timber.d("Очищен последний добавленный фильтр по полю $lastField")
    }

    private fun saveObjectToBuffer(field: FactActionField, value: Any) {
        when (field) {
            FactActionField.STORAGE_BIN ->
                taskBuffer.addBin(value as BinX, true, "filter")

            FactActionField.ALLOCATION_BIN ->
                taskBuffer.addBin(value as BinX, false, "filter")

            FactActionField.STORAGE_PALLET ->
                taskBuffer.addPallet(value as Pallet, true, "filter")

            FactActionField.ALLOCATION_PALLET ->
                taskBuffer.addPallet(value as Pallet, false, "filter")

            FactActionField.STORAGE_PRODUCT_CLASSIFIER ->
                taskBuffer.addProduct(value as Product, "filter")

            FactActionField.STORAGE_PRODUCT ->
                taskBuffer.addTaskProduct(value as TaskProduct, "filter")

            else -> {} // Другие типы не сохраняем
        }
    }

    fun clear() {
        Timber.d("TaskXDataHolderSingleton: очистка данных - вызов отклонен, данные сохраняются")
        // Преднамеренно не очищаем данные при переходе между экранами
        // Очистка должна вызываться только при завершении работы с заданием
    }

    /**
     * Принудительная очистка данных (только для специальных случаев)
     */
    fun forceClean() {
        Timber.d("TaskXDataHolderSingleton: принудительная очистка данных")
        _currentTask.value = null
        _endpoint = null
        _taskBuffer.clear()
        clearAllFiltersAndBufferObjects()
    }
}