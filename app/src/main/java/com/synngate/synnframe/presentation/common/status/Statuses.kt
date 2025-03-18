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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Тип статуса уведомления
 */
enum class StatusType {
    INFO,
    WARNING,
    ERROR
}

/**
 * Компонент панели уведомлений, который отображается
 * с анимацией появления и исчезновения
 */
@Composable
fun NotificationBar(
    visible: Boolean,
    message: String,
    type: StatusType,
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
            }
        }
    }
}

/**
 * Компонент для отображения статуса синхронизации
 */
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
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Синхронизация",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Text(
            text = if (isSyncing) {
                "Синхронизация..."
            } else if (lastSyncTime != null) {
                "Синхр.: $lastSyncTime"
            } else {
                "Нет синхронизации"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Компонент для отображения статуса задания
 */
@Composable
fun TaskStatusIndicator(
    status: String,
    modifier: Modifier = Modifier
) {
    // Определяем цвет в зависимости от статуса
    val color = when (status.lowercase()) {
        "к выполнению" -> MaterialTheme.colorScheme.primaryContainer
        "выполняется" -> MaterialTheme.colorScheme.tertiaryContainer
        "выполнено" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    // Определяем цвет текста в зависимости от статуса
    val textColor = when (status.lowercase()) {
        "к выполнению" -> MaterialTheme.colorScheme.onPrimaryContainer
        "выполняется" -> MaterialTheme.colorScheme.onTertiaryContainer
        "выполнено" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .background(
                color = color,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

/**
 * Компонент для отображения типа лога
 */
@Composable
fun LogTypeIndicator(
    type: String,
    modifier: Modifier = Modifier
) {
    // Определяем цвет в зависимости от типа лога
    val (color, textColor) = when (type.uppercase()) {
        "INFO" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "WARNING" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "ERROR" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .background(
                color = color,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = type,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}