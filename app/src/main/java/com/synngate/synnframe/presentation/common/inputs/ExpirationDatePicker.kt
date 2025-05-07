package com.synngate.synnframe.presentation.common.inputs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Компонент для выбора даты и времени срока годности.
 *
 * @param expirationDate Текущее значение срока годности
 * @param onDateSelected Обработчик выбора срока годности
 * @param modifier Модификатор для компонента
 * @param isRequired Признак обязательного ввода срока годности
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpirationDatePicker(
    expirationDate: LocalDateTime?,
    onDateSelected: (LocalDateTime?) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var hasNoExpiration by remember { mutableStateOf(expirationDate == null) }

    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

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
                text = if (isRequired) "Срок годности *" else "Срок годности",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Переключатель наличия/отсутствия срока годности
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Без срока годности",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                Switch(
                    checked = hasNoExpiration,
                    onCheckedChange = {
                        hasNoExpiration = it
                        if (it) {
                            onDateSelected(null)
                        } else {
                            // Устанавливаем дату через месяц по умолчанию
                            val defaultDate = LocalDateTime.now().plusMonths(1)
                            onDateSelected(defaultDate)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Поле выбора даты
            if (!hasNoExpiration) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Выбрать дату",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = expirationDate?.format(displayFormatter) ?: "Выберите дату",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = {
                            val defaultDate = LocalDateTime.now().plusMonths(1)
                            onDateSelected(defaultDate)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Сбросить",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Диалог выбора даты
    if (showDatePickerDialog) {
        val currentMillis =
            expirationDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000) // +30 дней по умолчанию

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            val localDate = Instant.ofEpochMilli(dateMillis)
                                .atZone(ZoneId.systemDefault())
                            onDateSelected(localDate.toLocalDateTime())
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}