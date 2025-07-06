package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.DynamicMenuApi
import com.synngate.synnframe.data.remote.dto.CommonResponseDto
import com.synngate.synnframe.data.remote.dto.CustomListResponseDto
import com.synngate.synnframe.data.remote.dto.DynamicTasksResponseDto
import com.synngate.synnframe.data.remote.dto.SearchKeyValidationResponseDto
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.repository.DynamicMenuRepository
import com.synngate.synnframe.presentation.ui.taskx.dto.TaskXResponseDto

class DynamicMenuRepositoryImpl(
    private val dynamicMenuApi: DynamicMenuApi
) : DynamicMenuRepository {

    override suspend fun getDynamicMenu(menuItemId: String?): ApiResult<List<DynamicMenuItem>> {
        return dynamicMenuApi.getDynamicMenu(menuItemId)
    }

    override suspend fun getDynamicTasks(endpoint: String, params: Map<String, String>): ApiResult<DynamicTasksResponseDto> {
        return dynamicMenuApi.getDynamicTasks(endpoint, params)
    }

    override suspend fun createTask(endpoint: String, taskTypeId: String, searchKey: String?): ApiResult<TaskXResponseDto> {
        return dynamicMenuApi.createTask(endpoint, taskTypeId, searchKey)
    }

    override suspend fun searchDynamicTask(endpoint: String, searchValue: String): ApiResult<DynamicTasksResponseDto> {
        return dynamicMenuApi.searchDynamicTask(endpoint, searchValue)
    }

    override suspend fun getTaskDetails(endpoint: String, taskId: String): ApiResult<DynamicTask> {
        return dynamicMenuApi.getTaskDetails(endpoint, taskId)
    }

    override suspend fun getDynamicProducts(endpoint: String, params: Map<String, String>): ApiResult<List<DynamicProduct>> {
        return dynamicMenuApi.getDynamicProducts(endpoint, params)
    }

    override suspend fun startDynamicTask(endpoint: String, taskId: String): ApiResult<TaskXResponseDto> {
        return dynamicMenuApi.startDynamicTask(endpoint, taskId)
    }

    override suspend fun validateSearchKey(endpoint: String, key: String): ApiResult<SearchKeyValidationResponseDto> {
        return dynamicMenuApi.validateSearchKey(endpoint, key)
    }

    override suspend fun deleteTask(endpoint: String, taskId: String): ApiResult<CommonResponseDto> {
        return dynamicMenuApi.deleteTask(endpoint, taskId)
    }

    override suspend fun getCustomList(endpoint: String, params: Map<String, String>): ApiResult<CustomListResponseDto> {
        return dynamicMenuApi.getCustomList(endpoint, params)
    }

    override suspend fun searchCustomList(endpoint: String, searchValue: String): ApiResult<CustomListResponseDto> {
        return dynamicMenuApi.searchCustomList(endpoint, searchValue)
    }
}