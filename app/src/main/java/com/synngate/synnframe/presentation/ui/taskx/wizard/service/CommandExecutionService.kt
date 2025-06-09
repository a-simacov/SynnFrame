package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.StepCommandApi
import com.synngate.synnframe.data.remote.dto.CommandExecutionRequestDto
import com.synngate.synnframe.data.remote.dto.CommandNextAction
import com.synngate.synnframe.data.remote.dto.FactActionRequestDto
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandExecutionBehavior
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
            Timber.d("Executing command: ${command.name} (${command.id})")

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
                        Timber.d("Command ${command.id} executed successfully")
                        NetworkResult.success(executionResult)
                    } else {
                        Timber.w("Command ${command.id} completed with error: ${response.message}")
                        NetworkResult.error(response.message ?: "Command completed with error")
                    }
                }
                is ApiResult.Error -> {
                    Timber.e("API error when executing command ${command.id}: ${result.message}")
                    NetworkResult.error(result.message)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception when executing command ${command.id}")
            NetworkResult.error("Error: ${e.message}")
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
    val nextAction: CommandNextAction?,
    val updatedFactAction: FactActionRequestDto?,
    val commandBehavior: CommandExecutionBehavior
)