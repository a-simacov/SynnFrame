package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val isStepCompleted = isObjectSelected && currentStep.isRequired

    val visibleCommands = currentStep.getVisibleCommands(
        isObjectSelected = isObjectSelected,
        isStepCompleted = isStepCompleted
    )

    if (visibleCommands.isEmpty()) return

    var selectedCommand by remember { mutableStateOf<StepCommand?>(null) }
    var showParametersDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var commandToExecute by remember { mutableStateOf<Pair<StepCommand, Map<String, String>>?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Доступные команды:",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        visibleCommands.forEach { command ->
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
                isLoading = state.isLoading, // Можно добавить отдельное состояние для каждой команды
                enabled = !state.isLoading
            )
        }
    }

    // Диалог ввода параметров
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
            title = { Text("Подтверждение") },
            text = {
                Text(
                    command.confirmationMessage ?: "Вы уверены, что хотите выполнить команду \"${command.name}\"?"
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
                    Text("Выполнить")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showConfirmationDialog = false
                        commandToExecute = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}
