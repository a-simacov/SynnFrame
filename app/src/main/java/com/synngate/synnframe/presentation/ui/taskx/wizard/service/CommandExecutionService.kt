package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.StepCommandApi
import com.synngate.synnframe.data.remote.dto.CommandExecutionRequestDto
import com.synngate.synnframe.data.remote.dto.FactActionRequestDto
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class CommandExecutionService(
    private val stepCommandApi: StepCommandApi
) {
    /**
     * Выполняет команду с указанными параметрами
     */
    suspend fun executeCommand(
        command: StepCommand,
        stepId: String,
        factAction: FactAction,
        parameters: Map<String, String> = emptyMap(),
        additionalContext: Map<String, String> = emptyMap()
    ): NetworkResult<CommandExecutionResult> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Выполнение команды: ${command.name} (${command.id})")

            val requestDto = CommandExecutionRequestDto(
                commandId = command.id,
                stepId = stepId,
                factAction = FactActionRequestDto.fromDomain(factAction),
                parameters = parameters,
                additionalContext = additionalContext
            )

            val result = stepCommandApi.executeCommand(command.endpoint, requestDto)

            when (result) {
                is ApiResult.Success -> {
                    val response = result.data

                    val executionResult = CommandExecutionResult(
                        success = response.success,
                        message = response.message,
                        resultData = response.resultData ?: emptyMap(),
                        nextAction = response.nextAction,
                        updatedFactAction = response.updatedFactAction,
                        commandBehavior = command.executionBehavior
                    )

                    if (response.success) {
                        Timber.d("Команда ${command.id} выполнена успешно")
                        NetworkResult.success(executionResult)
                    } else {
                        Timber.w("Команда ${command.id} завершилась с ошибкой: ${response.message}")
                        NetworkResult.error(response.message ?: "Команда завершилась с ошибкой")
                    }
                }
                is ApiResult.Error -> {
                    Timber.e("Ошибка API при выполнении команды ${command.id}: ${result.message}")
                    NetworkResult.error(result.message)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Исключение при выполнении команды ${command.id}")
            NetworkResult.error("Ошибка: ${e.message}")
        }
    }
}

/**
 * Результат выполнения команды
 */
data class CommandExecutionResult(
    val success: Boolean,
    val message: String?,
    val resultData: Map<String, String>,
    val nextAction: com.synngate.synnframe.data.remote.dto.CommandNextAction?,
    val updatedFactAction: com.synngate.synnframe.data.remote.dto.FactActionRequestDto?,
    val commandBehavior: com.synngate.synnframe.presentation.ui.taskx.entity.CommandExecutionBehavior
)