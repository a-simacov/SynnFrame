package com.synngate.synnframe.presentation.common.inputs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpirationDatePicker(
    expirationDate: LocalDateTime?,
    onDateSelected: (LocalDateTime?) -> Unit,
    isRequired: Boolean,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    val today = LocalDate.now()

    LaunchedEffect(expirationDate) {
        textFieldValue = if (expirationDate != null && expirationDate.year > 1970) {
            TextFieldValue(dateFormatter.format(expirationDate.toLocalDate()))
        } else {
            TextFieldValue("")
        }
    }

    val isCloseToExpiration = remember(expirationDate) {
        if (expirationDate == null) false
        else {
            val daysUntilExpiration = java.time.Duration.between(
                LocalDateTime.now(),
                expirationDate
            ).toDays()
            daysUntilExpiration in 0..3
        }
    }

    val isExpired = remember(expirationDate) {
        expirationDate?.isBefore(LocalDateTime.now()) ?: false
    }

    val formatDateInput = { input: String ->
        val digitsOnly = input.filter { it.isDigit() }
        val formatted = StringBuilder()

        digitsOnly.forEachIndexed { index, char ->
            if (formatted.length == 2 || formatted.length == 5) formatted.append('.')
            formatted.append(char)
            if (formatted.length >= 10) return@forEachIndexed
        }

        formatted.toString()
    }

    val parseTextDate = { text: String ->
        try {
            if (text.matches(Regex("""^\d{2}\.\d{2}\.\d{4}$"""))) {
                val date = LocalDate.parse(text, dateFormatter)
                val dateTime = date.atTime(0, 0)
                onDateSelected(dateTime)

                when {
                    date.isBefore(today) -> {
                        isError = true
                        errorMessage = "Дата истекла"
                    }
                    date.isEqual(today) || date.isBefore(today.plusDays(4)) -> {
                        isError = true
                        errorMessage = "Срок годности истекает менее чем через 3 дня"
                    }
                    else -> {
                        isError = false
                        errorMessage = ""
                    }
                }

                true
            } else {
                false
            }
        } catch (e: DateTimeParseException) {
            isError = true
            errorMessage = "Неверный формат даты (ДД.ММ.ГГГГ)"
            false
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val processed = if (newValue.text.length <= 10) {
                    formatDateInput(newValue.text)
                } else {
                    textFieldValue.text
                }

                val selection = when {
                    processed.length < textFieldValue.text.length -> newValue.selection
                    processed.length > textFieldValue.text.length &&
                            (processed.length == 3 || processed.length == 6) ->
                        TextRange(processed.length)
                    else -> TextRange(processed.length.coerceAtMost(10))
                }

                textFieldValue = TextFieldValue(processed, selection)

                if (processed.length == 10) {
                    parseTextDate(processed)
                } else if (processed.isEmpty()) {
                    onDateSelected(null)
                    isError = false
                    errorMessage = ""
                } else {
                    isError = false
                    errorMessage = ""
                }
            },
            label = { Text("Срок годности") },
            placeholder = { Text("ДД.ММ.ГГГГ") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && textFieldValue.text.isNotEmpty() && textFieldValue.text.length < 10) {
                        isError = true
                        errorMessage = "Неверный формат даты (ДД.ММ.ГГГГ)"
                    }
                },
            trailingIcon = {
                Row {
                    if (textFieldValue.text.isNotEmpty()) {
                        IconButton(onClick = {
                            textFieldValue = TextFieldValue("")
                            onDateSelected(null)
                            isError = false
                            errorMessage = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистить"
                            )
                        }
                    }

                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Выбрать дату"
                        )
                    }
                }
            },
            isError = isError || isCloseToExpiration || isExpired,
            supportingText = {
                when {
                    errorMessage.isNotEmpty() -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Предупреждение",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    isCloseToExpiration -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Предупреждение",
                                tint = Color(0xFFFF9800), // Оранжевый
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Срок годности истекает в ближайшие 3 дня", color = Color(0xFFFF9800))
                        }
                    }
                    isExpired -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Предупреждение",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Срок годности истек", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    isRequired -> Text("Обязательное поле")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )
    }

    if (showDatePicker) {
        val initialSelectedDateMillis = expirationDate?.let {
            it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } ?: LocalDate.now().plusDays(30).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedDateMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            val selectedDateTime = selectedDate.atTime(0, 0)
                            onDateSelected(selectedDateTime)
                            textFieldValue = TextFieldValue(dateFormatter.format(selectedDate))

                            when {
                                selectedDate.isBefore(today) -> {
                                    isError = true
                                    errorMessage = "Дата истекла"
                                }
                                selectedDate.isEqual(today) || selectedDate.isBefore(today.plusDays(4)) -> {
                                    isError = true
                                    errorMessage = "Срок годности истекает менее чем через 3 дня"
                                }
                                else -> {
                                    isError = false
                                    errorMessage = ""
                                }
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}