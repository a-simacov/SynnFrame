package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandButtonStyle
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.CommandExecutionStatus

/**
 * Компонент для отображения кнопки команды с раскрывающейся панелью параметров
 */
@Composable
fun ExpandableParametersForm(
    command: StepCommand,
    onExecute: (Map<String, String>) -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    executionStatus: CommandExecutionStatus? = null,
    modifier: Modifier = Modifier
) {
    val icon = getCommandIcon(command.icon)

    var expanded by remember { mutableStateOf(false) }

    // Инициализируем параметры значениями по умолчанию
    val parameterValues = remember {
        mutableStateMapOf<String, String>().apply {
            command.parameters.forEach { param ->
                put(param.id, param.defaultValue ?: "")
            }
        }
    }

    // Состояние ошибок валидации
    var validationErrors by remember { mutableStateOf(emptyMap<String, String>()) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Кнопка команды, которая будет также раскрывать панель параметров
        when (command.buttonStyle) {
            CommandButtonStyle.PRIMARY -> {
                Button(
                    onClick = {
                        if (command.parameters.isEmpty()) {
                            onExecute(emptyMap())
                        } else {
                            expanded = !expanded
                        }
                    },
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    ExpandableButtonContent(
                        text = command.name,
                        icon = icon,
                        isLoading = isLoading,
                        hasParameters = command.parameters.isNotEmpty(),
                        isExpanded = expanded
                    )
                }
            }
            CommandButtonStyle.SECONDARY -> {
                Button(
                    onClick = {
                        if (command.parameters.isEmpty()) {
                            onExecute(emptyMap())
                        } else {
                            expanded = !expanded
                        }
                    },
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    ExpandableButtonContent(
                        text = command.name,
                        icon = icon,
                        isLoading = isLoading,
                        hasParameters = command.parameters.isNotEmpty(),
                        isExpanded = expanded
                    )
                }
            }
            CommandButtonStyle.SUCCESS -> {
                Button(
                    onClick = {
                        if (command.parameters.isEmpty()) {
                            onExecute(emptyMap())
                        } else {
                            expanded = !expanded
                        }
                    },
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    )
                ) {
                    ExpandableButtonContent(
                        text = command.name,
                        icon = icon,
                        isLoading = isLoading,
                        hasParameters = command.parameters.isNotEmpty(),
                        isExpanded = expanded
                    )
                }
            }
            CommandButtonStyle.WARNING -> {
                Button(
                    onClick = {
                        if (command.parameters.isEmpty()) {
                            onExecute(emptyMap())
                        } else {
                            expanded = !expanded
                        }
                    },
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFF9800)
                    )
                ) {
                    ExpandableButtonContent(
                        text = command.name,
                        icon = icon,
                        isLoading = isLoading,
                        hasParameters = command.parameters.isNotEmpty(),
                        isExpanded = expanded
                    )
                }
            }
            CommandButtonStyle.DANGER -> {
                Button(
                    onClick = {
                        if (command.parameters.isEmpty()) {
                            onExecute(emptyMap())
                        } else {
                            expanded = !expanded
                        }
                    },
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    ExpandableButtonContent(
                        text = command.name,
                        icon = icon,
                        isLoading = isLoading,
                        hasParameters = command.parameters.isNotEmpty(),
                        isExpanded = expanded
                    )
                }
            }
            CommandButtonStyle.OUTLINE -> {
                OutlinedButton(
                    onClick = {
                        if (command.parameters.isEmpty()) {
                            onExecute(emptyMap())
                        } else {
                            expanded = !expanded
                        }
                    },
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExpandableButtonContent(
                        text = command.name,
                        icon = icon,
                        isLoading = isLoading,
                        hasParameters = command.parameters.isNotEmpty(),
                        isExpanded = expanded
                    )
                }
            }
        }

        // Отображаем статус выполнения команды, если он есть
        if (executionStatus != null) {
            Spacer(modifier = Modifier.height(4.dp))
            CommandStatusIndicator(
                status = executionStatus,
                modifier = Modifier.align(Alignment.End)
            )
        }

        // Анимированная панель параметров
        val visibleState = remember { MutableTransitionState(false) }
        visibleState.targetState = expanded && command.parameters.isNotEmpty()

        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(150)) +
                    expandVertically(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)) +
                    shrinkVertically(animationSpec = tween(150))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Описание команды, если есть
                    if (command.description.isNotEmpty()) {
                        Text(
                            text = command.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Поля для ввода параметров
                    command.parameters.sortedBy { it.order }.forEach { parameter ->
                        val currentValue = parameterValues[parameter.id] ?: ""
                        val errorMessage = validationErrors[parameter.id]

                        ParameterInputField(
                            parameter = parameter,
                            value = currentValue,
                            onValueChange = { newValue ->
                                parameterValues[parameter.id] = newValue
                                // Очищаем ошибку валидации при изменении значения
                                if (errorMessage != null) {
                                    validationErrors = validationErrors.toMutableMap().apply {
                                        remove(parameter.id)
                                    }
                                }
                            },
                            isError = errorMessage != null,
                            errorMessage = errorMessage
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Кнопки действий
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { expanded = false },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Отмена")
                        }

                        Button(
                            onClick = {
                                val errors = validateParameters(command.parameters, parameterValues)
                                if (errors.isEmpty()) {
                                    expanded = false
                                    onExecute(parameterValues)
                                } else {
                                    validationErrors = errors
                                }
                            }
                        ) {
                            Text("Выполнить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableButtonContent(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean,
    hasParameters: Boolean,
    isExpanded: Boolean
) {
    if (isLoading) {
        CommandButtonContent(text, icon, isLoading = true)
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else if (hasParameters) {
                // Показываем иконку "шестеренки" по умолчанию, если нет своей иконки
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Text(
                text = text,
                modifier = Modifier.weight(1f)
            )

            if (hasParameters) {
                // Показываем иконку раскрытия/скрытия
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Скрыть параметры" else "Показать параметры"
                )
            }
        }
    }
}