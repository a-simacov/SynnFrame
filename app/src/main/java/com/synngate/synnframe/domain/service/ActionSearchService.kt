package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.action.SearchableActionObject

/**
 * Сервис для поиска действий в задании
 */
interface ActionSearchService {

    /**
     * Поиск действий по значению
     * @param searchValue значение для поиска (штрихкод, код, ID и т.д.)
     * @param searchableObjects настройки поиска из типа задания
     * @param plannedActions список запланированных действий в задании
     * @param taskId ID текущего задания
     * @param currentActionId ID текущего действия (опционально)
     * @return список найденных ID действий
     */
    suspend fun searchActions(
        searchValue: String,
        searchableObjects: List<SearchableActionObject>,
        plannedActions: List<PlannedAction>,
        taskId: String,
        currentActionId: String? = null
    ): Result<List<String>>
}