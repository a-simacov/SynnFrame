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
        title = { Text("Exit without saving?") },
        text = { Text("All entered data will be lost. Are you sure you want to exit?") },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Exit")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}