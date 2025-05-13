package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Контроллер для управления визардом действий на основе конечного автомата состояний
 */
class ActionWizardController(
    private val fsmAdapter: FsmWizardControllerAdapter
) {
    val wizardState: StateFlow<ActionWizardState?> = fsmAdapter.adaptedState

    /**
     * Инициализирует визард для указанного задания и действия
     */
    suspend fun initialize(taskId: String, actionId: String): Result<Boolean> {
        return try {
            fsmAdapter.initialize(taskId, actionId)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing wizard: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Обрабатывает результат шага
     * @param result Результат шага или null для навигации назад
     */
    suspend fun processStepResult(result: Any?) {
        fsmAdapter.processStepResult(result)
    }

    /**
     * Обрабатывает переход вперед (если результат уже есть)
     */
    fun processForwardStep() {
        fsmAdapter.processForwardStep()
    }

    /**
     * Обрабатывает штрих-код от сканера
     */
    suspend fun processBarcodeFromScanner(barcode: String) {
        fsmAdapter.processBarcodeFromScanner(barcode)
    }

    /**
     * Отменяет визард
     */
    fun cancel() {
        fsmAdapter.cancel()
    }

    /**
     * Завершает визард и выполняет действие
     */
    suspend fun complete(): Result<TaskX> {
        return fsmAdapter.complete()
    }
}