package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

@Composable
fun ActionStatusConfirmationDialog(
    action: PlannedAction,
    isCompleted: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogTitle = if (isCompleted) {
        "Remove completion mark?"
    } else {
        "Mark as completed?"
    }

    val dialogText = if (isCompleted) {
        "Action \"${action.actionTemplate?.name}\" will be marked as not completed."
    } else {
        "Action \"${action.actionTemplate?.name}\" will be marked as completed."
    }

    val confirmButtonText = if (isCompleted) {
        "Remove mark"
    } else {
        "Mark"
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
                Text("Cancel")
            }
        }
    )
}