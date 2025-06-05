package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandButtonStyle
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.CommandExecutionStatus

@Composable
fun CommandButton(
    command: StepCommand,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    executionStatus: CommandExecutionStatus? = null,
    modifier: Modifier = Modifier
) {
    val icon = getCommandIcon(command.icon)

    Column(modifier = modifier.fillMaxWidth()) {
        when (command.buttonStyle) {
            CommandButtonStyle.PRIMARY -> {
                Button(
                    onClick = onClick,
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    CommandButtonContent(command.name, icon, isLoading)
                }
            }
            CommandButtonStyle.SECONDARY -> {
                Button(
                    onClick = onClick,
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    CommandButtonContent(command.name, icon, isLoading)
                }
            }
            CommandButtonStyle.SUCCESS -> {
                Button(
                    onClick = onClick,
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    )
                ) {
                    CommandButtonContent(command.name, icon, isLoading)
                }
            }
            CommandButtonStyle.WARNING -> {
                Button(
                    onClick = onClick,
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFF9800)
                    )
                ) {
                    CommandButtonContent(command.name, icon, isLoading)
                }
            }
            CommandButtonStyle.DANGER -> {
                Button(
                    onClick = onClick,
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    CommandButtonContent(command.name, icon, isLoading)
                }
            }
            CommandButtonStyle.OUTLINE -> {
                OutlinedButton(
                    onClick = onClick,
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CommandButtonContent(command.name, icon, isLoading)
                }
            }
        }

        // Отображаем статус выполнения команды, если он есть
        if (executionStatus != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CommandStatusIndicator(status = executionStatus)
            }
        }
    }
}

@Composable
fun CommandButtonContent(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean
) {
    if (isLoading) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выполнение...")
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text)
        }
    }
}

fun getCommandIcon(iconName: String?): ImageVector? {
    return when (iconName) {
        "edit" -> Icons.Default.Edit
        "delete" -> Icons.Default.Delete
        "check" -> Icons.Default.Check
        "warning" -> Icons.Default.Warning
        "info" -> Icons.Default.Info
        "settings" -> Icons.Default.Settings
        "build" -> Icons.Default.Build
        else -> null
    }
}