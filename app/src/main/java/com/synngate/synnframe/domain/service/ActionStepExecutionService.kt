package com.synngate.synnframe.domain.service

/**
 * Сервис для выполнения шагов действия
 * Упрощенная версия, работающая только с локальными данными и TaskContextManager
 */
class ActionStepExecutionService(
    private val validationService: ValidationService,
    private val taskContextManager: TaskContextManager
) {

}

sealed class StepExecutionResult {

    data class Success(
        val stepId: String,
        val value: Any
    ) : StepExecutionResult()

    data class Error(val message: String) : StepExecutionResult()

    object Skipped : StepExecutionResult()
}