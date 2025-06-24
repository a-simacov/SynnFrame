package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.util.html.HtmlUtils

@Composable
fun TaskDeleteConfirmationDialog(
    task: DynamicTask,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDeleting: Boolean = false
) {
    AlertDialog(
        onDismissRequest = {
            // Запрещаем закрытие диалога во время удаления
            if (!isDeleting) onDismiss()
        },
        title = {
            Text(
                text = stringResource(R.string.delete_task_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            if (isDeleting) {
                // Показываем прогресс-индикатор по центру
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = "${stringResource(R.string.delete_task_message)}\n\n${
                        HtmlUtils.htmlToAnnotatedString(task.name)
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Вызываем onConfirm только если не идет удаление
                    if (!isDeleting) {
                        onConfirm()
                    }
                },
                enabled = !isDeleting // Кнопка недоступна во время удаления
            ) {
                Text(
                    text = if (isDeleting) {
                        stringResource(R.string.deleting) // "Deleting..."
                    } else {
                        stringResource(R.string.delete_task_confirm) // "Delete"
                    },
                    color = if (isDeleting) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting // Кнопка Cancel тоже недоступна
            ) {
                Text(
                    text = stringResource(R.string.delete_task_cancel),
                    color = if (isDeleting) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDeleting,
            dismissOnClickOutside = !isDeleting
        )
    )
}