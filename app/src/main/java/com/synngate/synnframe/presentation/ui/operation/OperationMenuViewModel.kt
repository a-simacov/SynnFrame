package com.synngate.synnframe.presentation.ui.operation

import com.synngate.synnframe.domain.entity.OperationMenuType
import com.synngate.synnframe.domain.usecase.operation.OperationMenuUseCases
import com.synngate.synnframe.presentation.ui.operation.model.OperationMenuEvent
import com.synngate.synnframe.presentation.ui.operation.model.OperationMenuState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

class OperationMenuViewModel(
    private val operationMenuUseCases: OperationMenuUseCases,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<OperationMenuState, OperationMenuEvent>(OperationMenuState()) {

    init {
        loadOperationMenu()
    }

    fun loadOperationMenu() {
        launchIO {
            Timber.d("Loading operation menu")
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = operationMenuUseCases.getOperationMenu()
                if (result.isSuccess()) {
                    val operations = result.getOrNull() ?: emptyList()
                    Timber.d("Operation menu loaded: ${operations.size} items")
                    updateState {
                        it.copy(
                            operations = operations,
                            isLoading = false,
                            error = if (operations.isEmpty()) "Нет доступных операций" else null
                        )
                    }
                } else {
                    val errorMessage = "Ошибка загрузки операций: ${result as? com.synngate.synnframe.data.remote.api.ApiResult.Error}"
                    Timber.e(errorMessage)
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while loading operation menu")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки операций: ${e.message}"
                    )
                }
            }
        }
    }

    fun onOperationClick(operationId: String, operationName: String, operationMenuType: OperationMenuType) {
        Timber.d("Operation clicked: $operationId")
        sendEvent(OperationMenuEvent.NavigateToOperationTasks(operationId, operationName, operationType = operationMenuType))
    }

    fun onBackClick() {
        Timber.d("Back clicked in operation menu")
        sendEvent(OperationMenuEvent.NavigateBack)
    }

    fun onRefresh() {
        Timber.d("Refreshing operation menu")
        loadOperationMenu()
    }
}