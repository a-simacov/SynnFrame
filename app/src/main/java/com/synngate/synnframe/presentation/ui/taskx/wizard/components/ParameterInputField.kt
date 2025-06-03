package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameter
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameterType
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun ParameterInputField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (parameter.type) {
            CommandParameterType.TEXT -> {
                TextParameterField(
                    parameter = parameter,
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    errorMessage = errorMessage
                )
            }
            CommandParameterType.NUMBER,
            CommandParameterType.INTEGER,
            CommandParameterType.DECIMAL -> {
                NumberParameterField(
                    parameter = parameter,
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    errorMessage = errorMessage
                )
            }
            CommandParameterType.BOOLEAN -> {
                BooleanParameterField(
                    parameter = parameter,
                    value = value,
                    onValueChange = onValueChange
                )
            }
            CommandParameterType.SELECT -> {
                SelectParameterField(
                    parameter = parameter,
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    errorMessage = errorMessage
                )
            }
            CommandParameterType.DATE -> {
                DateParameterField(
                    parameter = parameter,
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    errorMessage = errorMessage
                )
            }
            CommandParameterType.DATETIME -> {
                DateTimeParameterField(
                    parameter = parameter,
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    errorMessage = errorMessage
                )
            }
            CommandParameterType.PASSWORD -> {
                PasswordParameterField(
                    parameter = parameter,
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    errorMessage = errorMessage
                )
            }
            CommandParameterType.TEXTAREA -> {
                TextAreaParameterField(
                    parameter = parameter,
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    errorMessage = errorMessage
                )
            }
            CommandParameterType.EMAIL -> {
                EmailParameterField(
                    parameter = parameter,
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    errorMessage = errorMessage
                )
            }
            CommandParameterType.PHONE -> {
                PhoneParameterField(
                    parameter = parameter,
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    errorMessage = errorMessage
                )
            }
        }
    }
}

@Composable
private fun TextParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { ParameterLabel(parameter) },
        placeholder = parameter.placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        singleLine = true
    )

    if (isError && errorMessage != null) {
        ParameterErrorMessage(errorMessage)
    }
}

@Composable
private fun NumberParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    val keyboardType = when (parameter.type) {
        CommandParameterType.INTEGER -> KeyboardType.Number
        CommandParameterType.DECIMAL -> KeyboardType.Decimal
        else -> KeyboardType.Number
    }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Фильтруем ввод в зависимости от типа
            val filteredValue = when (parameter.type) {
                CommandParameterType.INTEGER -> newValue.filter { it.isDigit() || it == '-' }
                CommandParameterType.DECIMAL -> newValue.filter { it.isDigit() || it in ".-" }
                else -> newValue.filter { it.isDigit() || it in ".-" }
            }
            onValueChange(filteredValue)
        },
        label = { ParameterLabel(parameter) },
        placeholder = parameter.placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )

    if (isError && errorMessage != null) {
        ParameterErrorMessage(errorMessage)
    }
}

@Composable
private fun BooleanParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit
) {
    val checked = value.toBooleanStrictOrNull() ?: false

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { newChecked ->
                onValueChange(newChecked.toString())
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = parameter.displayName,
            style = MaterialTheme.typography.bodyLarge
        )
        if (parameter.isRequired) {
            Text(
                text = " *",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    var expanded by remember { mutableStateOf(false) }
    val options = parameter.options ?: emptyList()
    val selectedOption = options.find { it.value == value }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption?.displayName ?: value,
            onValueChange = { },
            readOnly = true,
            label = { ParameterLabel(parameter) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            isError = isError
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onValueChange(option.value)
                        expanded = false
                    }
                )
            }
        }
    }

    if (isError && errorMessage != null) {
        ParameterErrorMessage(errorMessage)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    val dateValue = if (value.isNotEmpty()) {
        try {
            LocalDate.parse(value)
        } catch (e: Exception) {
            null
        }
    } else null

//    DatePicker(
//        selectedDate = dateValue,
//        onDateSelected = { date ->
//            onValueChange(date?.toString() ?: "")
//        },
//        label = parameter.displayName,
//        isRequired = parameter.isRequired,
//        modifier = Modifier.fillMaxWidth()
//    )

    if (isError && errorMessage != null) {
        ParameterErrorMessage(errorMessage)
    }
}

@Composable
private fun DateTimeParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    val dateTimeValue = if (value.isNotEmpty()) {
        try {
            LocalDateTime.parse(value)
        } catch (e: Exception) {
            null
        }
    } else null

//    DateTimePicker(
//        selectedDateTime = dateTimeValue,
//        onDateTimeSelected = { dateTime ->
//            onValueChange(dateTime?.toString() ?: "")
//        },
//        label = parameter.displayName,
//        isRequired = parameter.isRequired,
//        modifier = Modifier.fillMaxWidth()
//    )
//
//    if (isError && errorMessage != null) {
//        ParameterErrorMessage(errorMessage)
//    }
}

@Composable
private fun PasswordParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { ParameterLabel(parameter) },
        placeholder = parameter.placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )

    if (isError && errorMessage != null) {
        ParameterErrorMessage(errorMessage)
    }
}

@Composable
private fun TextAreaParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { ParameterLabel(parameter) },
        placeholder = parameter.placeholder?.let { { Text(it) } },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        isError = isError,
        maxLines = 5
    )

    if (isError && errorMessage != null) {
        ParameterErrorMessage(errorMessage)
    }
}

@Composable
private fun EmailParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { ParameterLabel(parameter) },
        placeholder = parameter.placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )

    if (isError && errorMessage != null) {
        ParameterErrorMessage(errorMessage)
    }
}

@Composable
private fun PhoneParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Фильтруем ввод для телефонных номеров
            val filteredValue = newValue.filter { it.isDigit() || it in "+()-" }
            onValueChange(filteredValue)
        },
        label = { ParameterLabel(parameter) },
        placeholder = parameter.placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
    )

    if (isError && errorMessage != null) {
        ParameterErrorMessage(errorMessage)
    }
}

@Composable
private fun ParameterLabel(parameter: CommandParameter) {
    Row {
        Text(parameter.displayName)
        if (parameter.isRequired) {
            Text(
                text = " *",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ParameterErrorMessage(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Ошибка",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}