package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.action.SearchableActionObject

interface ActionSearchService {

    suspend fun searchActions(
        searchValue: String,
        searchableObjects: List<SearchableActionObject>,
        plannedActions: List<PlannedAction>,
        taskId: String,
        currentActionId: String? = null
    ): Result<List<String>>
}