package com.synngate.synnframe.presentation.common.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R

/**
 * Расширенный диалог прогресса с возможностью отмены и повтора
 *
 * @param message Основное сообщение диалога
 * @param progress Значение прогресса от 0 до 100, или null для неопределенного прогресса
 * @param error Сообщение об ошибке, если произошла ошибка
 * @param onCancel Обработчик отмены операции
 * @param onRetry Обработчик повтора операции при ошибке
 * @param onDismiss Обработчик закрытия диалога (для ошибок)
 */
@Composable
fun EnhancedProgressDialog(
    message: String,
    progress: Int? = null,
    error: String? = null,
    onCancel: () -> Unit = {},
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    // Определяем, находимся ли мы в состоянии ошибки
    val isError = error != null

    // Создаем непрерывающийся диалог (если это не ошибка)
    Dialog(
        onDismissRequest = {
            if (isError) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = isError,
            dismissOnClickOutside = isError
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок диалога
                Text(
                    text = if (isError) stringResource(R.string.error) else stringResource(R.string.loading),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Основное сообщение
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Показываем индикатор прогресса, если нет ошибки
                if (!isError) {
                    if (progress != null) {
                        // Определенный прогресс
                        Column {
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "$progress%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    } else {
                        // Неопределенный прогресс
                        CircularProgressIndicator()
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Кнопка отмены при загрузке
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                } else {
                    // Показываем сообщение об ошибке
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Кнопки при ошибке
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.close))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
}