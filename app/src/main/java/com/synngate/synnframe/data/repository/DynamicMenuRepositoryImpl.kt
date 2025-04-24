package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.DynamicMenuApi
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.repository.DynamicMenuRepository

class DynamicMenuRepositoryImpl(
    private val dynamicMenuApi: DynamicMenuApi
) : DynamicMenuRepository {

    override suspend fun getDynamicMenu(): ApiResult<List<DynamicMenuItem>> {
        return dynamicMenuApi.getDynamicMenu()
    }

    override suspend fun getDynamicTasks(operationId: String): ApiResult<List<DynamicTask>> {
        return dynamicMenuApi.getDynamicTasks(operationId)
    }

    override suspend fun searchTaskByValue(operationId: String, searchValue: String): ApiResult<DynamicTask> {
        return dynamicMenuApi.searchTaskByValue(operationId, searchValue)
    }
}