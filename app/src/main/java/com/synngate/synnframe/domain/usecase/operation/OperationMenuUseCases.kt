package com.synngate.synnframe.domain.usecase.operation

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.OperationMenuItem
import com.synngate.synnframe.domain.entity.operation.OperationTask
import com.synngate.synnframe.domain.repository.OperationMenuRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import timber.log.Timber

class OperationMenuUseCases(
    private val operationMenuRepository: OperationMenuRepository
) : BaseUseCase {

    suspend fun getOperationMenu(): ApiResult<List<OperationMenuItem>> {
        Timber.d("Getting operation menu")
        return try {
            operationMenuRepository.getOperationMenu()
        } catch (e: Exception) {
            Timber.e(e, "Error in getOperationMenu use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    suspend fun getOperationTasks(operationId: String): ApiResult<List<OperationTask>> {
        Timber.d("Getting tasks for operation: $operationId")
        return try {
            operationMenuRepository.getOperationTasks(operationId)
        } catch (e: Exception) {
            Timber.e(e, "Error in getOperationTasks use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }

    suspend fun searchTaskByValue(operationId: String, searchValue: String): ApiResult<OperationTask> {
        Timber.d("Search task for operation $operationId by value: $searchValue")
        return try {
            operationMenuRepository.searchTaskByValue(operationId, searchValue)
        } catch (e: Exception) {
            Timber.e(e, "Error in searchTaskByValue use case")
            ApiResult.Error(500, e.message ?: "Unknown error")
        }
    }
}