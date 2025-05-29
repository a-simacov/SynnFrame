package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.NetworkResult
import timber.log.Timber
import java.time.LocalDateTime

class WizardNetworkService(
    private val taskXRepository: TaskXRepository
) {
    suspend fun completeAction(
        factAction: FactAction,
        syncWithServer: Boolean
    ): NetworkResult<Unit> {
        try {
            val updatedFactAction = factAction.copy(completedAt = LocalDateTime.now())

            if (syncWithServer) {
                val endpoint = TaskXDataHolderSingleton.endpoint
                    ?: return NetworkResult.error("Endpoint не определен")
                val result = taskXRepository.addFactAction(updatedFactAction, endpoint)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    if (updatedTask != null) {
                        TaskXDataHolderSingleton.updateTask(updatedTask)
                    }
                    return NetworkResult.success()
                } else {
                    return NetworkResult.error("Ошибка отправки: ${result.exceptionOrNull()?.message}")
                }
            } else {
                TaskXDataHolderSingleton.addFactAction(updatedFactAction)
                return NetworkResult.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка завершения действия: ${e.message}")
            return NetworkResult.error("Ошибка: ${e.message}")
        }
    }
}