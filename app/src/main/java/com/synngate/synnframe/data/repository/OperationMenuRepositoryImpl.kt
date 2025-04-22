package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.OperationMenuApi
import com.synngate.synnframe.domain.entity.operation.OperationMenuItem
import com.synngate.synnframe.domain.entity.operation.OperationTask
import com.synngate.synnframe.domain.repository.OperationMenuRepository

class OperationMenuRepositoryImpl(
    private val operationMenuApi: OperationMenuApi
) : OperationMenuRepository {

    override suspend fun getOperationMenu(): ApiResult<List<OperationMenuItem>> {
        return operationMenuApi.getOperationMenu()
    }

    override suspend fun getOperationTasks(operationId: String): ApiResult<List<OperationTask>> {
        return operationMenuApi.getOperationTasks(operationId)
    }
}