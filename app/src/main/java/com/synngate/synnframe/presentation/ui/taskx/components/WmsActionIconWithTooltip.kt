package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WmsActionIconWithTooltip(
    wmsAction: WmsAction,
    modifier: Modifier = Modifier,
    iconSize: Int = 16,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val icon = getWmsActionIcon(wmsAction)
    val description = getWmsActionDescription(wmsAction)
    val tooltipState = rememberTooltipState()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        },
        state = tooltipState,
        content = {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = modifier.size(iconSize.dp),
                tint = tint
            )
        }
    )
}

/**
 * Компонент для отображения иконки со всплывающей подсказкой
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconWithTooltip(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    iconSize: Int = 16,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val tooltipState = rememberTooltipState()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        },
        state = tooltipState,
        content = {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = modifier.size(iconSize.dp),
                tint = tint
            )
        }
    )
}