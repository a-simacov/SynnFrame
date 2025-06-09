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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import com.synngate.synnframe.presentation.ui.taskx.entity.BooleanParameterOptions
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameter
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameterType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                    onValueChange = onValueChange,
                    booleanOptions = parameter.booleanOptions ?: BooleanParameterOptions()
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

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    OutlinedTextField(
        value = dateValue?.format(dateFormatter) ?: "",
        onValueChange = { /* Обрабатывается через DatePicker */ },
        label = { ParameterLabel(parameter) },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Choose date"
                )
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateValue?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onValueChange(selectedDate.toString())
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (isError && errorMessage != null) {
        ParameterErrorMessage(errorMessage)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    // Пытаемся распарсить значение в LocalDateTime
    val dateTimeValue = if (value.isNotEmpty()) {
        try {
            LocalDateTime.parse(value)
        } catch (e: Exception) {
            null
        }
    } else null

    // Определяем состояния для диалогов выбора даты и времени
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Временное хранение выбранной даты до выбора времени
    var selectedDate by remember { mutableStateOf<LocalDate?>(dateTimeValue?.toLocalDate()) }

    // Форматтер для отображения даты и времени в поле
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    OutlinedTextField(
        value = dateTimeValue?.format(dateTimeFormatter) ?: "",
        onValueChange = { /* Обрабатывается через диалоги выбора */ },
        label = { ParameterLabel(parameter) },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        readOnly = true,
        trailingIcon = {
            Row {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null
                    )
                }
                IconButton(onClick = {
                    // Если дата уже выбрана, открываем выбор времени, иначе сначала выбираем дату
                    if (selectedDate != null) {
                        showTimePicker = true
                    } else {
                        showDatePicker = true
                    }
                }) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null
                    )
                }
            }
        }
    )

    // Диалог выбора даты
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: dateTimeValue?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                        // После выбора даты сразу переходим к выбору времени
                        showDatePicker = false
                        showTimePicker = true
                    } ?: run {
                        showDatePicker = false
                    }
                }) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Диалог выбора времени
    if (showTimePicker) {
        val initialHour = dateTimeValue?.hour ?: LocalTime.now().hour
        val initialMinute = dateTimeValue?.minute ?: LocalTime.now().minute

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                if (selectedDate != null) {
                    val newDateTime = LocalDateTime.of(
                        selectedDate,
                        LocalTime.of(timePickerState.hour, timePickerState.minute)
                    )
                    onValueChange(newDateTime.toString())
                }
                showTimePicker = false
            },
            onCancel = { showTimePicker = false }
        ) {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    if (isError && errorMessage != null) {
        ParameterErrorMessage(errorMessage)
    }
}

@Composable
private fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
        text = { content() }
    )
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
            contentDescription = null,
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