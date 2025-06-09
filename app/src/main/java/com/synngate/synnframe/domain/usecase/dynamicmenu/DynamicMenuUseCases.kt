package com.synngate.synnframe.domain.usecase.dynamicmenu

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.dto.DynamicTasksResponseDto
import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.repository.DynamicMenuRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import com.synngate.synnframe.presentation.ui.taskx.mapper.TaskXMapper
import timber.log.Timber

class DynamicMenuUseCases(
    private val dynamicMenuRepository: DynamicMenuRepository
) : BaseUseCase {

    suspend fun getDynamicMenu(menuItemId: String? = null): ApiResult<List<DynamicMenuItem>> {
        return try {
            dynamicMenuRepository.getDynamicMenu(menuItemId)
        } catch (e: Exception) {
            Timber.e(e, "Error in getDynamicMenu use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    suspend fun getDynamicItems(
        endpoint: String,
        params: Map<String, String> = emptyMap(),
        type: DynamicMenuItemType
    ): ApiResult<Any> {
        return try {
            when (type) {
                DynamicMenuItemType.TASKS -> dynamicMenuRepository.getDynamicTasks(endpoint, params)
                DynamicMenuItemType.PRODUCTS -> dynamicMenuRepository.getDynamicProducts(endpoint, params)
                DynamicMenuItemType.SUBMENU -> ApiResult.Error(500, "Invalid operation type for getDynamicItems")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in getDynamicItems use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    suspend fun getDynamicTasks(endpoint: String, params: Map<String, String> = emptyMap()): ApiResult<DynamicTasksResponseDto> {
        return try {
            dynamicMenuRepository.getDynamicTasks(endpoint, params)
        } catch (e: Exception) {
            Timber.e(e, "Error in getDynamicTasks use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    suspend fun createTask(endpoint: String, taskTypeId: String): ApiResult<TaskX> {
        return try {
            Timber.d("Creating new task with taskTypeId: $taskTypeId")
            val result = dynamicMenuRepository.createTask(endpoint, taskTypeId)

            when (result) {
                is ApiResult.Success -> {
                    // Используем TaskXMapper для преобразования, так же как в startDynamicTask
                    val taskX = TaskXMapper.mapTaskXResponse(result.data)
                    ApiResult.Success(taskX)
                }
                is ApiResult.Error -> result
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in createTask use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    suspend fun searchDynamicTask(endpoint: String, searchValue: String): ApiResult<DynamicTask> {
        return try {
            dynamicMenuRepository.searchDynamicTask(endpoint, searchValue)
        } catch (e: Exception) {
            Timber.e(e, "Error in searchDynamicTask use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    // Новый метод для получения деталей задания
    suspend fun getTaskDetails(endpoint: String, taskId: String): ApiResult<DynamicTask> {
        return try {
            dynamicMenuRepository.getTaskDetails(endpoint, taskId)
        } catch (e: Exception) {
            Timber.e(e, "Error in getTaskDetails use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    suspend fun getDynamicProducts(endpoint: String, params: Map<String, String> = emptyMap()): ApiResult<List<DynamicProduct>> {
        return try {
            dynamicMenuRepository.getDynamicProducts(endpoint, params)
        } catch (e: Exception) {
            Timber.e(e, "Error in getDynamicProducts use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    suspend fun startDynamicTask(endpoint: String, taskId: String): ApiResult<TaskX> {
        return try {
            val result = dynamicMenuRepository.startDynamicTask(endpoint, taskId)

            when (result) {
                is ApiResult.Success -> {
                    // Используем TaskXMapper для преобразования
                    val taskX = TaskXMapper.mapTaskXResponse(result.data)
                    ApiResult.Success(taskX)
                }
                is ApiResult.Error -> result
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in startDynamicTask use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }
}