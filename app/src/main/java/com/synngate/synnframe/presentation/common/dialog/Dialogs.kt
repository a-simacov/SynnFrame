package com.synngate.synnframe.presentation.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import java.time.LocalDateTime

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
 * Диалог для выбора даты и времени
 */
@Composable
fun DateTimePickerDialog(
    title: String,
    onDateSelected: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
    initialDateTime: LocalDateTime = LocalDateTime.now()
) {
    // Используем DatePicker из MaterialTheme
    // Реализация будет добавлена по мере разработки
    // Временная заглушка
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = "DateTimePicker будет реализован позднее") },
        confirmButton = {
            Button(
                onClick = {
                    onDateSelected(initialDateTime)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}