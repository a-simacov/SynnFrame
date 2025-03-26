package com.synngate.synnframe.presentation.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Диалог подтверждения действия
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(id = R.string.confirm),
    dismissText: String = stringResource(id = R.string.cancel)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text(text = dismissText)
            }
        }
    )
}

/**
 * Диалог прогресса операции
 */
@Composable
fun ProgressDialog(
    message: String,
    onDismiss: (() -> Unit)? = null // null для некоторых операций, которые нельзя отменить
) {
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = onDismiss != null
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.material3.MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.CircularProgressIndicator()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    textAlign = TextAlign.Center
                )

                if (onDismiss != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            }
        }
    }
}

/**
 * Диалог строки факта для задания
 */
@Composable
fun FactLineDialog(
    productName: String,
    currentQuantity: Float,
    onQuantityChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var additionalQuantity = ""
    var isError = false

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.material3.MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = productName,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.task_scan_current_quantity,
                            currentQuantity
                        ),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(id = R.string.task_scan_add_quantity),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    com.synngate.synnframe.presentation.common.inputs.QuantityTextField(
                        value = additionalQuantity,
                        onValueChange = { additionalQuantity = it },
                        label = "",
                        isError = isError,
                        errorText = if (isError) "Неверное значение" else null,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val totalQuantity = try {
                        currentQuantity + (additionalQuantity.toFloatOrNull() ?: 0f)
                    } catch (e: Exception) {
                        currentQuantity
                    }

                    Text(
                        text = stringResource(
                            id = R.string.task_scan_total_quantity,
                            totalQuantity
                        ),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss
                    ) {
                        Text(text = stringResource(id = R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            try {
                                val addValue = additionalQuantity.toFloatOrNull() ?: 0f
                                if (addValue != 0f) {
                                    onQuantityChange(currentQuantity + addValue)
                                    onDismiss()
                                } else {
                                    isError = true
                                }
                            } catch (e: Exception) {
                                isError = true
                            }
                        },
                        enabled = additionalQuantity.isNotEmpty()
                    ) {
                        Text(text = stringResource(id = R.string.task_scan_modify))
                    }
                }
            }
        }
    }
}

/**
 * Предустановленные периоды фильтрации
 */
enum class DateFilterPreset {
    LAST_5_MINUTES,
    LAST_30_MINUTES,
    LAST_HOUR;

    fun getDates(): Pair<LocalDateTime, LocalDateTime> {
        val endDate = LocalDateTime.now()
        val startDate = when(this) {
            LAST_5_MINUTES -> endDate.minus(5, ChronoUnit.MINUTES)
            LAST_30_MINUTES -> endDate.minus(30, ChronoUnit.MINUTES)
            LAST_HOUR -> endDate.minus(1, ChronoUnit.HOURS)
        }
        return Pair(startDate, endDate)
    }

    companion object {
        fun getResourceId(preset: DateFilterPreset): Int {
            return when(preset) {
                LAST_5_MINUTES -> R.string.last_5_minutes
                LAST_30_MINUTES -> R.string.last_30_minutes
                LAST_HOUR -> R.string.last_hour
            }
        }
    }
}


@Composable
@androidx.compose.ui.tooling.preview.Preview(
    name = "Date Time Filter Dialog",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
fun DateTimeFilterDialogPreview() {
    // Используем текущую дату и время для превью
    val currentDateTime = LocalDateTime.now()
    val fromDate = currentDateTime.minusHours(1)
    val toDate = currentDateTime

    MaterialTheme {
        Surface {
            DateTimeFilterDialog(
                fromDate = fromDate,
                toDate = toDate,
                onApply = { _, _ -> /* Ничего не делаем в превью */ },
                onDismiss = { /* Ничего не делаем в превью */ }
            )
        }
    }
}

/**
 * Диалог выбора периода фильтрации логов с поддержкой пикеров даты и времени
 */
@Composable
fun DateTimeFilterDialog(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    onApply: (LocalDateTime?, LocalDateTime?) -> Unit,
    onDismiss: () -> Unit,
    onApplyPreset: (preset: DateFilterPreset) -> Unit = { preset ->
        val (start, end) = preset.getDates()
        onApply(start, end)
    }
) {
    // Локальные переменные состояния для хранения выбранных дат
    var selectedFromDate by remember { mutableStateOf(fromDate ?: LocalDateTime.now().minusHours(1)) }
    var selectedToDate by remember { mutableStateOf(toDate ?: LocalDateTime.now()) }

    // Состояния для отображения диалогов выбора даты и времени
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showFromTimePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    var showToTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    // Диалоги выбора даты и времени
    if (showFromDatePicker) {
        CustomDatePickerDialog(
            selectedDate = selectedFromDate.toLocalDate(),
            onDateSelected = { newDate ->
                // Сохраняем выбранную дату, сохраняя текущее время
                selectedFromDate = LocalDateTime.of(
                    newDate,
                    selectedFromDate.toLocalTime()
                )
                showFromDatePicker = false
            },
            onDismiss = { showFromDatePicker = false }
        )
    }

    if (showFromTimePicker) {
        CustomTimePickerDialog(
            selectedTime = selectedFromDate.toLocalTime(),
            onTimeSelected = { newTime ->
                // Сохраняем выбранное время, сохраняя текущую дату
                selectedFromDate = LocalDateTime.of(
                    selectedFromDate.toLocalDate(),
                    newTime
                )
                showFromTimePicker = false
            },
            onDismiss = { showFromTimePicker = false }
        )
    }

    if (showToDatePicker) {
        CustomDatePickerDialog(
            selectedDate = selectedToDate.toLocalDate(),
            onDateSelected = { newDate ->
                // Сохраняем выбранную дату, сохраняя текущее время
                selectedToDate = LocalDateTime.of(
                    newDate,
                    selectedToDate.toLocalTime()
                )
                showToDatePicker = false
            },
            onDismiss = { showToDatePicker = false }
        )
    }

    if (showToTimePicker) {
        CustomTimePickerDialog(
            selectedTime = selectedToDate.toLocalTime(),
            onTimeSelected = { newTime ->
                // Сохраняем выбранное время, сохраняя текущую дату
                selectedToDate = LocalDateTime.of(
                    selectedToDate.toLocalDate(),
                    newTime
                )
                showToTimePicker = false
            },
            onDismiss = { showToTimePicker = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // Отключаем ограничение ширины платформы
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .widthIn(min = 360.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(id = R.string.date_filter_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Раздел предустановленных периодов
                Text(
                    text = stringResource(id = R.string.preset_periods),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            val (start, end) = DateFilterPreset.LAST_5_MINUTES.getDates()
                            selectedFromDate = start
                            selectedToDate = end
                            onApplyPreset(DateFilterPreset.LAST_5_MINUTES)
                        }
                    ) {
                        Text(stringResource(id = R.string.last_5_minutes))
                    }

                    TextButton(
                        onClick = {
                            val (start, end) = DateFilterPreset.LAST_30_MINUTES.getDates()
                            selectedFromDate = start
                            selectedToDate = end
                            onApplyPreset(DateFilterPreset.LAST_30_MINUTES)
                        }
                    ) {
                        Text(stringResource(id = R.string.last_30_minutes))
                    }

                    TextButton(
                        onClick = {
                            val (start, end) = DateFilterPreset.LAST_HOUR.getDates()
                            selectedFromDate = start
                            selectedToDate = end
                            onApplyPreset(DateFilterPreset.LAST_HOUR)
                        }
                    ) {
                        Text(stringResource(id = R.string.last_hour))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Раздел выбора начальной даты и времени
                Text(
                    text = stringResource(id = R.string.start_date_time),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Компоненты выбора даты и времени для начальной даты
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка выбора даты
                    OutlinedButton(
                        onClick = { showFromDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedFromDate.format(dateFormatter))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка выбора времени
                    OutlinedButton(
                        onClick = { showFromTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedFromDate.format(timeFormatter))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Раздел выбора конечной даты и времени
                Text(
                    text = stringResource(id = R.string.end_date_time),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Компоненты выбора даты и времени для конечной даты
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка выбора даты
                    OutlinedButton(
                        onClick = { showToDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedToDate.format(dateFormatter))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка выбора времени
                    OutlinedButton(
                        onClick = { showToTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedToDate.format(timeFormatter))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            // Очистка фильтра
                            onApply(null, null)
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(id = R.string.clear))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Применение выбранного периода
                            onApply(selectedFromDate, selectedToDate)
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(id = R.string.apply))
                    }
                }
            }
        }
    }
}

/**
 * Диалог выбора даты
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val localDate = Instant.ofEpochMilli(dateMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(localDate)
                    }
                }
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Диалог выбора времени
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimePickerDialog(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.select_time),
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                TimePicker(state = timePickerState)

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            val selectedTimeFromPicker = LocalTime.of(
                                timePickerState.hour,
                                timePickerState.minute,
                                0  // seconds
                            )
                            onTimeSelected(selectedTimeFromPicker)
                        }
                    ) {
                        Text(stringResource(id = R.string.ok))
                    }
                }
            }
        }
    }
}

/**
 * Компонент для отображения выбранного периода фильтрации
 */
@Composable
fun DateFilterSummary(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    modifier: Modifier = Modifier
) {
    if (fromDate == null || toDate == null) return

    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.date_filter_active),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${fromDate.format(dateTimeFormatter)} - ${toDate.format(dateTimeFormatter)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}