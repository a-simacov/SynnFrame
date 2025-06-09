package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.entity.ParametersDisplayMode
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState

@Composable
fun StepCommandsSection(
    state: ActionWizardState,
    onCommandExecute: (StepCommand, Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStep = state.getCurrentStep() ?: return

    val isObjectSelected = state.selectedObjects.containsKey(currentStep.id)
    val isStepCompleted = state.isStepFieldCompleted(currentStep.factActionField)

    val visibleCommands = currentStep.getVisibleCommands(
        isObjectSelected = isObjectSelected,
        isStepCompleted = isStepCompleted
    )

    if (visibleCommands.isEmpty()) {
        return
    }

    // Состояния для диалогов и выбранной команды
    var selectedCommand by remember { mutableStateOf<StepCommand?>(null) }
    var showParametersDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var commandToExecute by remember { mutableStateOf<Pair<StepCommand, Map<String, String>>?>(null) }

    // Для инлайн-параметров мы отслеживаем, какая команда сейчас отображает свои параметры
    var activeInlineCommand by remember { mutableStateOf<StepCommand?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Для всех режимов отображения, кроме INLINE, показываем кнопки команд
        visibleCommands.forEach { command ->
            val executionStatus = state.getCommandStatus(command.id)

            when (command.parametersDisplayMode) {
                ParametersDisplayMode.DIALOG -> {
                    // Стандартная кнопка с диалогом параметров
                    CommandButton(
                        command = command,
                        onClick = {
                            selectedCommand = command
                            if (command.parameters.isNotEmpty()) {
                                showParametersDialog = true
                            } else {
                                if (command.confirmationRequired) {
                                    commandToExecute = command to emptyMap()
                                    showConfirmationDialog = true
                                } else {
                                    onCommandExecute(command, emptyMap())
                                }
                            }
                        },
                        isLoading = state.isLoading,
                        enabled = !state.isLoading,
                        executionStatus = executionStatus
                    )
                }

                ParametersDisplayMode.EXPANDABLE -> {
                    // Кнопка с раскрывающейся панелью параметров
                    ExpandableParametersForm(
                        command = command,
                        onExecute = { parameters ->
                            if (command.confirmationRequired) {
                                commandToExecute = command to parameters
                                showConfirmationDialog = true
                            } else {
                                onCommandExecute(command, parameters)
                            }
                        },
                        isLoading = state.isLoading,
                        enabled = !state.isLoading,
                        executionStatus = executionStatus
                    )
                }

                ParametersDisplayMode.INLINE -> {
                    // Для INLINE режима показываем кнопку только если команда не активна
                    if (activeInlineCommand != command) {
                        CommandButton(
                            command = command,
                            onClick = {
                                if (command.parameters.isEmpty()) {
                                    // Если параметров нет, выполняем сразу
                                    if (command.confirmationRequired) {
                                        commandToExecute = command to emptyMap()
                                        showConfirmationDialog = true
                                    } else {
                                        onCommandExecute(command, emptyMap())
                                    }
                                } else {
                                    // Если есть параметры, активируем отображение формы
                                    activeInlineCommand = command
                                }
                            },
                            isLoading = state.isLoading,
                            enabled = !state.isLoading,
                            executionStatus = executionStatus
                        )
                    }
                }
            }
        }

        // Отображаем активную инлайн-форму, если такая есть
        activeInlineCommand?.let { command ->
            InlineParametersForm(
                command = command,
                onExecute = { parameters ->
                    activeInlineCommand = null
                    if (command.confirmationRequired) {
                        commandToExecute = command to parameters
                        showConfirmationDialog = true
                    } else {
                        onCommandExecute(command, parameters)
                    }
                },
                onCancel = {
                    activeInlineCommand = null
                }
            )
        }
    }

    // Диалог ввода параметров (для режима DIALOG)
    if (showParametersDialog && selectedCommand != null) {
        CommandParametersDialog(
            command = selectedCommand!!,
            onExecute = { parameters ->
                showParametersDialog = false
                if (selectedCommand!!.confirmationRequired) {
                    commandToExecute = selectedCommand!! to parameters
                    showConfirmationDialog = true
                } else {
                    onCommandExecute(selectedCommand!!, parameters)
                }
                selectedCommand = null
            },
            onDismiss = {
                showParametersDialog = false
                selectedCommand = null
            }
        )
    }

    // Диалог подтверждения
    if (showConfirmationDialog && commandToExecute != null) {
        val (command, parameters) = commandToExecute!!

        AlertDialog(
            onDismissRequest = {
                showConfirmationDialog = false
                commandToExecute = null
            },
            title = { Text("Confirmation") },
            text = {
                Text(
                    command.confirmationMessage ?: "Are you sure you want to execute the command \"${command.name}\"?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        onCommandExecute(command, parameters)
                        commandToExecute = null
                    }
                ) {
                    Text("Execute")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showConfirmationDialog = false
                        commandToExecute = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}