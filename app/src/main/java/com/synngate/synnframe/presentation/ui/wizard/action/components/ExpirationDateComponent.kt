package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Компонент для выбора срока годности продукта
 *
 * @param expirationDate Текущая дата срока годности
 * @param onDateSelected Обработчик выбора даты
 * @param isRequired Обязательное ли поле
 * @param modifier Модификатор
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpirationDatePicker(
    expirationDate: LocalDateTime?,
    onDateSelected: (LocalDateTime?) -> Unit,
    isRequired: Boolean,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    // Форматтер для отображения даты
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    // Преобразуем LocalDateTime в миллисекунды для DatePickerState
    val initialMillis = remember(expirationDate) {
        expirationDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    // Определяем, просрочен ли продукт
    val isExpired by remember(expirationDate) {
        derivedStateOf {
            expirationDate != null && expirationDate.isBefore(LocalDateTime.now())
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Срок годности",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (expirationDate != null) {
                Text(
                    text = "Выбрано: ${expirationDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isExpired)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (isExpired) {
                    Text(
                        text = "Внимание: срок годности истек!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Кнопки для изменения или очистки даты
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Изменить дату")
                }

                OutlinedButton(
                    onClick = { onDateSelected(null) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Очистить дату")
                }
            } else {
                // Кнопка для установки даты, если она не выбрана
                if (isRequired) {
                    Text(
                        text = "Требуется указать срок годности",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Выбрать дату")
                }
            }
        }
    }

    // Диалог выбора даты
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                            onDateSelected(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}