package com.synngate.synnframe.presentation.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import java.time.LocalDateTime
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

/**
 * Диалог выбора периода фильтрации логов
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

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        onClick = {
                            // Здесь будет открываться DatePicker для начальной даты
                            // В этой реализации используется заглушка, в реальном приложении
                            // нужно реализовать открытие датапикера
                        },
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(selectedFromDate.format(dateFormatter))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка выбора времени
                    OutlinedButton(
                        onClick = {
                            // Здесь будет открываться TimePicker для начального времени
                        },
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
                        onClick = {
                            // Здесь будет открываться DatePicker для конечной даты
                        },
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(selectedToDate.format(dateFormatter))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка выбора времени
                    OutlinedButton(
                        onClick = {
                            // Здесь будет открываться TimePicker для конечного времени
                        },
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