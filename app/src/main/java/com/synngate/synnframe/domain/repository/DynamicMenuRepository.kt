package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.dto.CommonResponseDto
import com.synngate.synnframe.data.remote.dto.DynamicTasksResponseDto
import com.synngate.synnframe.data.remote.dto.SearchKeyValidationResponseDto
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.presentation.ui.taskx.dto.TaskXResponseDto

interface DynamicMenuRepository {

    suspend fun getDynamicMenu(menuItemId: String? = null): ApiResult<List<DynamicMenuItem>>

    suspend fun getDynamicTasks(endpoint: String, params: Map<String, String> = emptyMap()): ApiResult<DynamicTasksResponseDto>

    suspend fun createTask(endpoint: String, taskTypeId: String, searchKey: String? = null): ApiResult<TaskXResponseDto>

    suspend fun searchDynamicTask(endpoint: String, searchValue: String): ApiResult<DynamicTasksResponseDto>

    suspend fun getTaskDetails(endpoint: String, taskId: String): ApiResult<DynamicTask>

    suspend fun getDynamicProducts(endpoint: String, params: Map<String, String> = emptyMap()): ApiResult<List<DynamicProduct>>

    suspend fun startDynamicTask(endpoint: String, taskId: String): ApiResult<TaskXResponseDto>

    suspend fun validateSearchKey(endpoint: String, key: String): ApiResult<SearchKeyValidationResponseDto>

    suspend fun deleteTask(endpoint: String, taskId: String): ApiResult<CommonResponseDto>
}