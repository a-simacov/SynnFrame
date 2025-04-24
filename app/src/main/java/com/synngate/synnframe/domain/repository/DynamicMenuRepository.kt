package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicTask

interface DynamicMenuRepository {

    suspend fun getDynamicMenu(): ApiResult<List<DynamicMenuItem>>

    suspend fun getDynamicTasks(operationId: String): ApiResult<List<DynamicTask>>

    suspend fun searchTaskByValue(operationId: String, searchValue: String): ApiResult<DynamicTask>
}