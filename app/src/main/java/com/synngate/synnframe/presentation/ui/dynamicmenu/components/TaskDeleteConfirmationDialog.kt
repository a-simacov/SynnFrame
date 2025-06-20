package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
                CircularProgressIndicator()
            } else {
                Text(
                    text = "${stringResource(R.string.delete_task_message)}\n\n${
                        HtmlUtils.htmlToAnnotatedString(
                            task.name
                        )
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting
            ) {
                Text(
                    text = stringResource(R.string.delete_task_confirm),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text(stringResource(R.string.delete_task_cancel))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDeleting,
            dismissOnClickOutside = !isDeleting
        )
    )
}