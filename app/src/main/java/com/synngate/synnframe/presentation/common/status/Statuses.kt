package com.synngate.synnframe.presentation.common.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus

enum class StatusType {
    INFO,
    WARNING,
    ERROR
}

@Composable
fun NotificationBar(
    visible: Boolean,
    message: String,
    type: StatusType,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Определяем цвет фона и иконку в зависимости от типа уведомления
    val (backgroundColor, icon) = when (type) {
        StatusType.INFO -> MaterialTheme.colorScheme.primaryContainer to Icons.Default.Info
        StatusType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer to Icons.Default.Warning
        StatusType.ERROR -> MaterialTheme.colorScheme.errorContainer to Icons.Default.Error
    }

    // Определяем цвет текста в зависимости от типа уведомления
    val textColor = when (type) {
        StatusType.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
        StatusType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        StatusType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }

    // Анимированное появление и исчезновение панели уведомлений
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = backgroundColor,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = type.name,
                    tint = textColor
                )

                Text(
                    text = message,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )

                // Добавляем кнопку закрытия, если предоставлен обработчик onDismiss
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncStatusIndicator(
    isSyncing: Boolean,
    lastSyncTime: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "Синхронизация...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = if (lastSyncTime != null) {
                    "Синхр.: $lastSyncTime"
                } else {
                    "Нет синхронизации"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TaskStatusIndicator(
    status: TaskStatus
) {
    val icon = when (status) {
        TaskStatus.TO_DO -> Icons.Default.Schedule
        TaskStatus.IN_PROGRESS -> Icons.Default.SyncAlt
        TaskStatus.COMPLETED -> Icons.Default.CheckCircle
    }

    val iconTint = when (status) {
        TaskStatus.TO_DO -> Color.DarkGray
        TaskStatus.IN_PROGRESS -> Color.Magenta
        TaskStatus.COMPLETED -> Color.DarkGray
    }

    Icon(
        imageVector = icon,
        tint = iconTint,
        contentDescription = status.name
    )
}

@Composable
fun TaskXStatusIndicator(
    status: TaskXStatus
) {
    val icon = when (status) {
        TaskXStatus.TO_DO -> Icons.Default.Schedule
        TaskXStatus.IN_PROGRESS -> Icons.Default.SyncAlt
        TaskXStatus.COMPLETED -> Icons.Default.CheckCircle
        TaskXStatus.PAUSED -> Icons.Default.Pause
        TaskXStatus.CANCELLED -> Icons.Default.Cancel
    }

    val iconTint = when (status) {
        TaskXStatus.TO_DO -> Color.DarkGray
        TaskXStatus.IN_PROGRESS -> Color.Magenta
        TaskXStatus.COMPLETED -> Color.DarkGray
        TaskXStatus.PAUSED -> Color(0xFFFFF9C4)
        TaskXStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
    }

    Icon(
        imageVector = icon,
        tint = iconTint,
        contentDescription = status.name
    )
}

@Composable
fun LogTypeIndicator(
    type: LogType,
    modifier: Modifier = Modifier
) {
    // Определяем иконку и цвет в зависимости от типа лога
    val (icon, color) = when (type) {
        LogType.INFO -> Icons.Default.Info to MaterialTheme.colorScheme.primary
        LogType.WARNING -> Icons.Default.Warning to MaterialTheme.colorScheme.tertiary
        LogType.ERROR -> Icons.Default.Error to MaterialTheme.colorScheme.error
    }

    // Отображаем иконку в круглом контейнере
    Box(
        modifier = modifier
            .size(36.dp)
            .background(
                color = color.copy(alpha = 0.12f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.name,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}