package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

interface ActionSearchService {

    suspend fun searchActions(
        searchValue: String,
        plannedActions: List<PlannedAction>,
        taskId: String,
        currentActionId: String? = null
    ): Result<List<String>>
}