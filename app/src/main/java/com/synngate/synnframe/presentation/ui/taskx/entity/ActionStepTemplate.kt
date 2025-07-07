package com.synngate.synnframe.presentation.ui.taskx.entity

import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import com.synngate.synnframe.presentation.ui.taskx.enums.BufferUsage
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import kotlinx.serialization.Serializable

@Serializable
data class ActionStepTemplate(
    val id: String,
    val order: Int,
    val name: String,
    val promptText: String,
    val factActionField: FactActionField,
    val isRequired: Boolean = true,
    val serverSelectionEndpoint: String = "",
    val updateActionFieldEndpoint: String = "",
    val inputAdditionalProps: Boolean = false,
    val bufferUsage: BufferUsage = BufferUsage.NEVER,
    val saveToTaskBuffer: Boolean = false,
    val validationRules: ValidationRule? = null,
    val autoAdvance: Boolean = true,
    val commands: List<StepCommand> = emptyList(),
    val visibilityCondition: String? = null
) {
    /**
     * Возвращает команды, которые должны отображаться в текущих условиях
     */
    fun getVisibleCommands(
        isObjectSelected: Boolean,
        isStepCompleted: Boolean
    ): List<StepCommand> {
        return commands.filter { command ->
            when (command.displayCondition) {
                CommandDisplayCondition.ALWAYS -> true
                CommandDisplayCondition.WHEN_OBJECT_SELECTED -> isObjectSelected
                CommandDisplayCondition.WHEN_OBJECT_NOT_SELECTED -> !isObjectSelected
                CommandDisplayCondition.WHEN_STEP_COMPLETED -> isStepCompleted
                CommandDisplayCondition.WHEN_STEP_NOT_COMPLETED -> !isStepCompleted
            }
        }.sortedBy { it.order }
    }
}