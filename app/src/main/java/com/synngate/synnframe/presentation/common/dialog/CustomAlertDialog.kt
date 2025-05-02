package com.synngate.synnframe.presentation.common.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.synngate.synnframe.R

/**
 * Простой компонент AlertDialog для отображения сообщений об ошибках
 * и информационных сообщений
 */
@Composable
fun CustomAlertDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    confirmButton: String = stringResource(id = R.string.ok)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(text = confirmButton)
            }
        }
    )
}