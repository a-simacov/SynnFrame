package com.synngate.synnframe.domain.usecase.dynamicmenu

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.repository.DynamicMenuRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
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

    suspend fun getDynamicTasks(endpoint: String, params: Map<String, String> = emptyMap()): ApiResult<List<DynamicTask>> {
        return try {
            dynamicMenuRepository.getDynamicTasks(endpoint, params)
        } catch (e: Exception) {
            Timber.e(e, "Error in getDynamicTasks use case")
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

    suspend fun getDynamicProducts(endpoint: String, params: Map<String, String> = emptyMap()): ApiResult<List<DynamicProduct>> {
        return try {
            dynamicMenuRepository.getDynamicProducts(endpoint, params)
        } catch (e: Exception) {
            Timber.e(e, "Error in getDynamicProducts use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }
}