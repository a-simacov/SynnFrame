package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.presentation.ui.taskx.model.OperationResult

/**
 * Диалог с операциями при выходе из экрана задания
 */
@Composable
fun TaskXExitDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onPause: () -> Unit,
    onComplete: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onOkClick: () -> Unit,
    canComplete: Boolean = true,
    isProcessing: Boolean = false,
    operationResult: OperationResult? = null,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = !isProcessing
        )
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Choose an action",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Показываем результат операции, если есть
                operationResult?.let { result ->
                    val (icon, color, message) = when (result) {
                        is OperationResult.Success -> Triple(
                            Icons.Default.CheckCircle,
                            Color.Green,
                            result.message ?: "Operation completed successfully"
                        )
                        is OperationResult.Error -> Triple(
                            Icons.Default.Error,
                            MaterialTheme.colorScheme.error,
                            result.message
                        )
                        is OperationResult.UserMessage -> Triple(
                            if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            if (result.isSuccess) Color.Green else MaterialTheme.colorScheme.error,
                            result.message
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Показываем основной текст только если нет успешного userMessage
                val showMainText = operationResult !is OperationResult.UserMessage || !operationResult.isSuccess
                if (showMainText) {
                    Text(
                        text = "Task is in progress. What would you like to do?",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Определяем, показывать ли только кнопку OK
                val showOnlyOkButton = operationResult is OperationResult.UserMessage && operationResult.isSuccess

                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Processing request...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else if (showOnlyOkButton) {
                    // Показываем только кнопку OK для успешных операций с userMessage
                    Button(
                        onClick = onOkClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("OK")
                    }
                } else {
                    // Показываем все кнопки действий
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Продолжить работу
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Stay")
                        }

                        // Приостановить задание
                        OutlinedButton(
                            onClick = onPause,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pause")
                        }

                        // Завершить задание
                        if (canComplete) {
                            OutlinedButton(
                                onClick = onComplete,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Complete")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Выйти без сохранения
                        OutlinedButton(
                            onClick = onExitWithoutSaving,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exit")
                        }
                    }
                }
            }
        }
    }
}