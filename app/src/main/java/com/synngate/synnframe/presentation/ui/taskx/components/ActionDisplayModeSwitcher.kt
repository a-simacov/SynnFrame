package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.model.ActionDisplayMode

@Composable
fun ActionDisplayModeSwitcher(
    currentMode: ActionDisplayMode,
    onModeChange: (ActionDisplayMode) -> Unit,
    hasFinalActions: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionModeButton(
            text = "Текущие",
            isSelected = currentMode == ActionDisplayMode.CURRENT,
            onClick = { onModeChange(ActionDisplayMode.CURRENT) },
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        ActionModeButton(
            text = "Выполненные",
            isSelected = currentMode == ActionDisplayMode.COMPLETED,
            onClick = { onModeChange(ActionDisplayMode.COMPLETED) },
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        ActionModeButton(
            text = "Все",
            isSelected = currentMode == ActionDisplayMode.ALL,
            onClick = { onModeChange(ActionDisplayMode.ALL) },
            modifier = Modifier.weight(1f)
        )

        if (hasFinalActions) {
            Spacer(modifier = Modifier.width(4.dp))

            ActionModeButton(
                text = "Финальные",
                isSelected = currentMode == ActionDisplayMode.FINALS,
                onClick = { onModeChange(ActionDisplayMode.FINALS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ActionModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text)
    }
}