package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.action.SearchableActionObject

/**
 * Сервис для поиска действий
 */
interface ActionSearchService {

    /**
     * Поиск действий по значению
     * @param searchValue Значение для поиска (штрихкод, код товара/паллеты/ячейки)
     * @param searchableObjects Список объектов для поиска из конфигурации типа задания
     * @param plannedActions Запланированные действия задания
     * @return Список ID найденных действий
     */
    suspend fun searchActions(
        searchValue: String,
        searchableObjects: List<SearchableActionObject>,
        plannedActions: List<PlannedAction>
    ): Result<List<String>>
}