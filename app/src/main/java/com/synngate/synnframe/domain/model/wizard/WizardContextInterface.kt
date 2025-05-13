package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

/**
 * Интерфейс контекста визарда.
 * Определяет API для доступа к данным визарда и создания состояний.
 */
interface WizardContextInterface {
    // Данные контекста
    val taskId: String
    val actionId: String
    val task: TaskX?
    val action: PlannedAction?
    val steps: List<WizardStep>
    val results: Map<String, Any>
    val errors: Map<String, String>
    val lastScannedBarcode: String?

    // Фабричные методы для создания состояний
    fun createInitializingState(): InitializingState
    fun createStepState(stepIndex: Int): StepState
    fun createCompletingState(): CompletingState
    fun createCompletedState(): CompletedState
    fun createCancelledState(): CancelledState
    fun createErrorState(error: String): ErrorState

    // Метод для создания нового контекста на основе текущего
    fun copy(
        taskId: String = this.taskId,
        actionId: String = this.actionId,
        task: TaskX? = this.task,
        action: PlannedAction? = this.action,
        steps: List<WizardStep> = this.steps,
        results: Map<String, Any> = this.results,
        errors: Map<String, String> = this.errors,
        lastScannedBarcode: String? = this.lastScannedBarcode
    ): WizardContextInterface
}