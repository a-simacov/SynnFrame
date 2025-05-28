package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Сервис для сетевого взаимодействия визарда с сервером
 */
class WizardNetworkService(
    private val taskXRepository: TaskXRepository
) {
    /**
     * Отправляет действие на сервер
     * @return Пара (успех операции, сообщение об ошибке или null при успехе)
     */
    suspend fun completeAction(factAction: FactAction, syncWithServer: Boolean): Pair<Boolean, String?> {
        try {
            val updatedFactAction = factAction.copy(completedAt = LocalDateTime.now())

            if (syncWithServer) {
                val endpoint = TaskXDataHolderSingleton.endpoint ?: ""
                val result = taskXRepository.addFactAction(updatedFactAction, endpoint)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    if (updatedTask != null) {
                        TaskXDataHolderSingleton.updateTask(updatedTask)
                    }
                    return Pair(true, null)
                } else {
                    return Pair(false, "Ошибка отправки: ${result.exceptionOrNull()?.message}")
                }
            } else {
                TaskXDataHolderSingleton.addFactAction(updatedFactAction)
                return Pair(true, null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка завершения действия: ${e.message}")
            return Pair(false, "Ошибка: ${e.message}")
        }
    }
}