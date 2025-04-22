package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.OperationMenuItem
import com.synngate.synnframe.domain.entity.operation.OperationTask

interface OperationMenuRepository {

    suspend fun getOperationMenu(): ApiResult<List<OperationMenuItem>>

    suspend fun getOperationTasks(operationId: String): ApiResult<List<OperationTask>>

    suspend fun searchTaskByValue(operationId: String, searchValue: String): ApiResult<OperationTask>
}