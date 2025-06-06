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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    var selectedFromDate by remember {
        mutableStateOf(
            fromDate ?: LocalDateTime.now().minusHours(1)
        )
    }
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

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