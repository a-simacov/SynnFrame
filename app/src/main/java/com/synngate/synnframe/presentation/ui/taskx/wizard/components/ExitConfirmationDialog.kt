package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ExitConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выйти без сохранения?") },
        text = { Text("Все введенные данные будут потеряны. Вы уверены, что хотите выйти?") },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Выйти")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Отмена")
            }
        }
    )
}