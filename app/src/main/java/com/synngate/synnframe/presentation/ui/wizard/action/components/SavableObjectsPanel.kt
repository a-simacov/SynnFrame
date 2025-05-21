package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.SavableObject

/**
 * Панель для отображения сохраняемых объектов
 */
@Composable
fun SavableObjectsPanel(
    savableObjects: List<SavableObject>,
    onRemoveObject: (String) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    if (!visible || savableObjects.isEmpty()) return

    val scrollState = rememberScrollState()

    AnimatedVisibility(
        visible = visible && savableObjects.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Автозаполнение",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    Text(
                        text = "Используемые объекты",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = "Автоподстановка",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    Text(
                        text = "Автоподстановка",
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(bottom = 4.dp)
                ) {
                    savableObjects.forEach { savableObject ->
                        SavableObjectChip(
                            savableObject = savableObject,
                            onRemove = onRemoveObject
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * Упрощенная версия панели для ситуаций, когда нужно только отобразить
 * небольшое количество объектов, без возможности удаления
 */
@Composable
fun SimpleSavableObjectsPanel(
    savableObjects: List<SavableObject>,
    modifier: Modifier = Modifier
) {
    if (savableObjects.isEmpty()) return

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            savableObjects.forEach { savableObject ->
                SimpleObjectBadge(savableObject)
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

/**
 * Простая версия бейджа для отображения сохраняемого объекта
 * без возможности удаления
 */
@Composable
private fun SimpleObjectBadge(
    savableObject: SavableObject,
    modifier: Modifier = Modifier
) {
    val (iconVector, description, label, chipColor) = getObjectVisualData(savableObject)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = chipColor.copy(alpha = 0.15f)
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = description,
                tint = chipColor,
                modifier = Modifier.size(16.dp).padding(end = 4.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}