package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.domain.entity.operation.OperationMenuItem
import com.synngate.synnframe.domain.entity.operation.OperationTask

interface OperationMenuApi {

    suspend fun getOperationMenu(): ApiResult<List<OperationMenuItem>>

    suspend fun getOperationTasks(operationId: String): ApiResult<List<OperationTask>>

    suspend fun searchTaskByValue(operationId: String, searchValue: String): ApiResult<OperationTask>
}