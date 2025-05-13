package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Базовый компонент карточки для использования в шагах визарда.
 * Обеспечивает единый внешний вид и поведение для всех типов объектов.
 *
 * @param title Основной заголовок карточки
 * @param onClick Обработчик нажатия на карточку (null, если карточка не кликабельна)
 * @param isSelected Выбрана ли карточка
 * @param modifier Модификатор
 * @param containerColor Цвет фона карточки (по умолчанию использует цвет из темы)
 * @param actionIcon Иконка действия (по умолчанию галочка)
 * @param actionIconDescription Описание иконки действия для доступности
 * @param content Содержимое карточки
 */
@Composable
fun WizardCard(
    title: String,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = if (isSelected)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surface,
    actionIcon: ImageVector = Icons.Default.Check,
    actionIconDescription: String = "Выбрать",
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (onClick != null) {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = actionIconDescription,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Содержимое карточки
            content()
        }
    }
}

/**
 * Вспомогательный компонент для отображения строки с меткой и значением
 */
@Composable
fun CardProperty(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Вспомогательный компонент для добавления вертикального отступа между элементами
 */
@Composable
fun CardSpacer(height: Int = 4) {
    Spacer(modifier = Modifier.height(height.dp))
}