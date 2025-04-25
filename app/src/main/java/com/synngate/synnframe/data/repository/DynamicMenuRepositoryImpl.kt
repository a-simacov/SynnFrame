package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.DynamicMenuApi
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.repository.DynamicMenuRepository

class DynamicMenuRepositoryImpl(
    private val dynamicMenuApi: DynamicMenuApi
) : DynamicMenuRepository {

    override suspend fun getDynamicMenu(menuItemId: String?): ApiResult<List<DynamicMenuItem>> {
        return dynamicMenuApi.getDynamicMenu(menuItemId)
    }

    override suspend fun getDynamicTasks(endpoint: String, params: Map<String, String>): ApiResult<List<DynamicTask>> {
        return dynamicMenuApi.getDynamicTasks(endpoint, params)
    }

    override suspend fun searchDynamicTask(endpoint: String, searchValue: String): ApiResult<DynamicTask> {
        return dynamicMenuApi.searchDynamicTask(endpoint, searchValue)
    }

    override suspend fun getDynamicProducts(endpoint: String, params: Map<String, String>): ApiResult<List<DynamicProduct>> {
        return dynamicMenuApi.getDynamicProducts(endpoint, params)
    }
}