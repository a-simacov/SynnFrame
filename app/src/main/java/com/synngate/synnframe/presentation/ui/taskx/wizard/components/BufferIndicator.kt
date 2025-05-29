package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Индикатор для отображения источника данных из буфера
 */
@Composable
fun BufferIndicator(
    source: String,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Info,
            contentDescription = null,
            tint = if (isLocked) Color(0xFFEC407A) else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isLocked)
                "Значение из буфера (заблокировано)"
            else
                "Значение из буфера ($source)",
            style = MaterialTheme.typography.bodySmall,
            color = if (isLocked) Color(0xFFEC407A) else MaterialTheme.colorScheme.primary
        )
    }
}