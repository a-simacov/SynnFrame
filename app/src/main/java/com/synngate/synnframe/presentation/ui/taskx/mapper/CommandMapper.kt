package com.synngate.synnframe.presentation.ui.taskx.mapper

import com.synngate.synnframe.presentation.ui.taskx.dto.CommandParameterDto
import com.synngate.synnframe.presentation.ui.taskx.dto.ParameterOptionDto
import com.synngate.synnframe.presentation.ui.taskx.dto.ParameterValidationDto
import com.synngate.synnframe.presentation.ui.taskx.dto.StepCommandDto
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandButtonStyle
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandDisplayCondition
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandExecutionBehavior
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameter
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameterType
import com.synngate.synnframe.presentation.ui.taskx.entity.ParameterOption
import com.synngate.synnframe.presentation.ui.taskx.entity.ParameterValidation
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand

object CommandMapper {

    fun mapStepCommand(dto: StepCommandDto): StepCommand {
        return StepCommand(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            endpoint = dto.endpoint,
            icon = dto.icon,
            buttonStyle = CommandButtonStyle.valueOf(dto.buttonStyle),
            displayCondition = CommandDisplayCondition.valueOf(dto.displayCondition),
            executionBehavior = CommandExecutionBehavior.valueOf(dto.executionBehavior),
            parameters = dto.parameters.map { mapCommandParameter(it) },
            confirmationRequired = dto.confirmationRequired,
            confirmationMessage = dto.confirmationMessage,
            order = dto.order
        )
    }

    private fun mapCommandParameter(dto: CommandParameterDto): CommandParameter {
        return CommandParameter(
            id = dto.id,
            name = dto.name,
            displayName = dto.displayName,
            type = CommandParameterType.valueOf(dto.type),
            isRequired = dto.isRequired,
            defaultValue = dto.defaultValue,
            placeholder = dto.placeholder,
            validation = dto.validation?.let { mapParameterValidation(it) },
            options = dto.options?.map { mapParameterOption(it) },
            order = dto.order
        )
    }

    private fun mapParameterValidation(dto: ParameterValidationDto): ParameterValidation {
        return ParameterValidation(
            minLength = dto.minLength,
            maxLength = dto.maxLength,
            minValue = dto.minValue,
            maxValue = dto.maxValue,
            pattern = dto.pattern,
            errorMessage = dto.errorMessage
        )
    }

    private fun mapParameterOption(dto: ParameterOptionDto): ParameterOption {
        return ParameterOption(
            value = dto.value,
            displayName = dto.displayName
        )
    }
}