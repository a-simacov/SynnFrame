package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicTask

interface DynamicMenuApi {

    suspend fun getDynamicMenu(): ApiResult<List<DynamicMenuItem>>

    suspend fun getDynamicTasks(operationId: String): ApiResult<List<DynamicTask>>

    suspend fun searchTaskByValue(operationId: String, searchValue: String): ApiResult<DynamicTask>
}