package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

/**
 * Диалог подтверждения изменения статуса действия
 *
 * @param action Действие, статус которого меняется
 * @param isCompleted Текущий статус выполнения
 * @param onConfirm Обработчик подтверждения изменения
 * @param onDismiss Обработчик отмены
 */
@Composable
fun ActionStatusConfirmationDialog(
    action: PlannedAction,
    isCompleted: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogTitle = if (isCompleted) {
        "Снять отметку о выполнении?"
    } else {
        "Отметить как выполненное?"
    }

    val dialogText = if (isCompleted) {
        "Действие \"${action.actionTemplate.name}\" будет отмечено как невыполненное."
    } else {
        "Действие \"${action.actionTemplate.name}\" будет отмечено как выполненное."
    }

    val confirmButtonText = if (isCompleted) {
        "Снять отметку"
    } else {
        "Отметить"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = { Text(dialogText) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}