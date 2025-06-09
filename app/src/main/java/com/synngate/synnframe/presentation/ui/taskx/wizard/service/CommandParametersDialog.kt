package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameter
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameterType.DECIMAL
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameterType.INTEGER
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameterType.NUMBER
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand

@Composable
fun CommandParametersDialog(
    command: StepCommand,
    onExecute: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var parameterValues by remember {
        mutableStateOf(
            command.parameters.associate { param ->
                param.id to (param.defaultValue ?: "")
            }
        )
    }

    var validationErrors by remember { mutableStateOf(emptyMap<String, String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = command.name,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (command.description.isNotEmpty()) {
                    Text(
                        text = command.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                command.parameters.sortedBy { it.order }.forEach { parameter ->
                    val currentValue = parameterValues[parameter.id] ?: ""
                    val errorMessage = validationErrors[parameter.id]

                    ParameterInputField(
                        parameter = parameter,
                        value = currentValue,
                        onValueChange = { newValue ->
                            parameterValues = parameterValues.toMutableMap().apply {
                                put(parameter.id, newValue)
                            }
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
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val errors = validateParameters(command.parameters, parameterValues)
                    if (errors.isEmpty()) {
                        onExecute(parameterValues)
                    } else {
                        validationErrors = errors
                    }
                }
            ) {
                Text("Execute")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

/**
 * Валидация параметров команды
 */
fun validateParameters(
    parameters: List<CommandParameter>,
    values: Map<String, String>
): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    parameters.forEach { parameter ->
        val value = values[parameter.id] ?: ""

        // Проверка обязательности
        if (parameter.isRequired && value.isEmpty()) {
            errors[parameter.id] = "Field \"${parameter.displayName}\" is required"
            return@forEach
        }

        // Если значение пустое и не обязательное, пропускаем дальнейшую валидацию
        if (value.isEmpty()) return@forEach

        val validation = parameter.validation
        if (validation != null) {
            // Проверка числовых значений приоритетнее для числовых типов
            if (parameter.type in listOf(
                    NUMBER,
                    INTEGER,
                    DECIMAL
                )) {
                val numValue = value.toDoubleOrNull()
                if (numValue == null) {
                    errors[parameter.id] = "Invalid numeric value"
                    return@forEach
                }

                validation.minValue?.let { minValue ->
                    if (numValue < minValue) {
                        errors[parameter.id] = validation.errorMessage ?: "Minimum value: $minValue"
                        return@forEach
                    }
                }

                validation.maxValue?.let { maxValue ->
                    if (numValue > maxValue) {
                        errors[parameter.id] = validation.errorMessage ?: "Maximum value: $maxValue"
                        return@forEach
                    }
                }

                // Для числовых типов пропускаем проверку minLength/maxLength
                // Или можно проверять длину с учетом форматирования числа, если это нужно
            } else {
                // Проверка длины для не-числовых типов
                validation.minLength?.let { minLength ->
                    if (value.length < minLength) {
                        errors[parameter.id] = validation.errorMessage ?: "Minimum length: $minLength characters"
                        return@forEach
                    }
                }

                validation.maxLength?.let { maxLength ->
                    if (value.length > maxLength) {
                        errors[parameter.id] = validation.errorMessage ?: "Maximum length: $maxLength characters"
                        return@forEach
                    }
                }
            }

            // Проверка регулярного выражения
            validation.pattern?.let { pattern ->
                try {
                    if (pattern.isNotEmpty() && !value.matches(Regex(pattern))) {
                        errors[parameter.id] = validation.errorMessage ?: "Invalid format"
                        return@forEach
                    }
                } catch (e: Exception) {
                    // Игнорируем некорректные регулярные выражения
                }
            }
        }
    }

    return errors
}