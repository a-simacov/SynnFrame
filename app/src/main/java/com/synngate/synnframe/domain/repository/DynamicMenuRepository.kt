package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask

interface DynamicMenuRepository {

    suspend fun getDynamicMenu(menuItemId: String? = null): ApiResult<List<DynamicMenuItem>>

    suspend fun getDynamicTasks(endpoint: String, params: Map<String, String> = emptyMap()): ApiResult<List<DynamicTask>>

    suspend fun searchDynamicTask(endpoint: String, searchValue: String): ApiResult<DynamicTask>

    suspend fun getDynamicProducts(endpoint: String, params: Map<String, String> = emptyMap()): ApiResult<List<DynamicProduct>>
}