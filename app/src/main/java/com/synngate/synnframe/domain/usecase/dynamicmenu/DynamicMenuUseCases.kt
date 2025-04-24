package com.synngate.synnframe.domain.usecase.dynamicmenu

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.repository.DynamicMenuRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import timber.log.Timber

class DynamicMenuUseCases(
    private val dynamicMenuRepository: DynamicMenuRepository
) : BaseUseCase {

    suspend fun getDynamicMenu(): ApiResult<List<DynamicMenuItem>> {
        return try {
            dynamicMenuRepository.getDynamicMenu()
        } catch (e: Exception) {
            Timber.e(e, "Error in getOperationMenu use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    suspend fun getOperationTasks(operationId: String): ApiResult<List<DynamicTask>> {
        return try {
            dynamicMenuRepository.getDynamicTasks(operationId)
        } catch (e: Exception) {
            Timber.e(e, "Error in getOperationTasks use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    suspend fun searchTaskByValue(operationId: String, searchValue: String): ApiResult<DynamicTask> {
        return try {
            dynamicMenuRepository.searchTaskByValue(operationId, searchValue)
        } catch (e: Exception) {
            Timber.e(e, "Error in searchTaskByValue use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }
}