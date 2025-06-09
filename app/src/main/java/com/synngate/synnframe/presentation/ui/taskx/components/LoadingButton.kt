package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Кнопка с индикатором загрузки и возможностью отмены
 *
 * @param onClick Действие при нажатии на кнопку
 * @param isLoading Флаг загрузки
 * @param onCancel Действие при отмене
 * @param text Текст кнопки
 * @param modifier Модификатор
 * @param icon Иконка для кнопки
 * @param enabled Активность кнопки
 */
@Composable
fun LoadingButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    onCancel: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    },
    enabled: Boolean = true
) {
    Box(modifier = modifier) {
        if (isLoading) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text)
            }
        }
    }
}