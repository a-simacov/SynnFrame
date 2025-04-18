package com.synngate.synnframe.presentation.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Расширяемая плавающая кнопка, которая при нажатии показывает дополнительные кнопки
 *
 * @param icon Иконка основной кнопки
 * @param contentDescription Описание основной кнопки для доступности
 * @param items Список дочерних кнопок, которые будут показаны при разворачивании
 * @param modifier Модификатор для контейнера
 */
@Composable
fun ExpandableFab(
    icon: ImageVector,
    contentDescription: String,
    items: List<FabItem>,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        label = "rotation"
    )

    Column(
        modifier = modifier.padding(bottom = 72.dp), // Отступ снизу, чтобы не перекрывать другие элементы
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Показываем дочерние кнопки, когда расширено
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    // Используем ExtendedFloatingActionButton вместо SmallFloatingActionButton
                    // для отображения текста и полноразмерной кнопки
                    ExtendedFloatingActionButton(
                        onClick = {
                            item.onClick()
                            expanded = false // Скрываем кнопки после выбора
                        },
                        containerColor = item.containerColor ?: containerColor,
                        contentColor = item.contentColor ?: contentColor,
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.contentDescription
                            )
                        },
                        text = {
                            item.text?.let { Text(text = it) }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Основная кнопка, которая разворачивает/сворачивает дочерние кнопки
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = containerColor,
            contentColor = contentColor
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

data class FabItem(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val text: String? = null,
    val containerColor: Color? = null,
    val contentColor: Color? = null
)