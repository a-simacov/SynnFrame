package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand

/**
 * Компонент для отображения параметров команды непосредственно в шаге визарда
 */
@Composable
fun InlineParametersForm(
    command: StepCommand,
    onExecute: (Map<String, String>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок формы
            Text(
                text = "Command parameters \"${command.name}\"",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Описание команды, если есть
            if (command.description.isNotEmpty()) {
                Text(
                    text = command.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }

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
            }
        }
    }
}