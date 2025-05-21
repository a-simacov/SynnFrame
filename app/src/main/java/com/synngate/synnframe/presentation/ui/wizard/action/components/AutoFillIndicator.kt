package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoFillIndicator(
    source: String? = null,
    modifier: Modifier = Modifier
) {
    val tooltipState = rememberTooltipState()
    val tooltipText = source?.let {
        "Автозаполнено из: $it"
    } ?: "Автоматически заполнено"

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                Text(
                    text = tooltipText,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        state = tooltipState,
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Автозаполнено",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = "Автозаполнено",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    )
}