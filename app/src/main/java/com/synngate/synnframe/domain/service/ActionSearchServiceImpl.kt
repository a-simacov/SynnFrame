package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.api.ActionSearchApi
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.action.SearchableActionObject
import com.synngate.synnframe.domain.repository.ProductRepository
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class ActionSearchServiceImpl(
    private val actionSearchApi: ActionSearchApi,
    private val productRepository: ProductRepository
) : ActionSearchService {

    override suspend fun searchActions(
        searchValue: String,
        plannedActions: List<PlannedAction>,
        taskId: String,
        currentActionId: String?
    ): Result<List<String>> = coroutineScope {
        try {
            val foundActionIds = mutableSetOf<String>()

            Result.success(foundActionIds.toList())
        } catch (e: Exception) {
            Timber.e(e, "Error searching actions: $searchValue")
            Result.failure(e)
        }
    }

    private suspend fun remoteSearch(
        searchObject: SearchableActionObject,
        searchValue: String,
        taskId: String,
        currentActionId: String?
    ): List<String> {
        val endpoint = searchObject.endpoint ?: return emptyList()

        val processedEndpoint = endpoint
            .replace("{taskId}", taskId)
            .replace("{actionId}", currentActionId ?: "")

        return when (val result = actionSearchApi.searchAction(processedEndpoint, searchValue)) {
            is ApiResult.Success -> {
                if (!result.data.results.isNullOrEmpty()) {
                    result.data.results
                } else {
                    result.data.result?.let { listOf(it) } ?: emptyList()
                }
            }
            is ApiResult.Error -> {
                Timber.e("Remote search error: ${result.message}")
                emptyList()
            }
        }
    }
}