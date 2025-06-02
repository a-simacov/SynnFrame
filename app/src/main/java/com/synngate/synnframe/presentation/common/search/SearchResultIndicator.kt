package com.synngate.synnframe.presentation.common.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Тип результата поиска для визуального отображения
 */
enum class SearchResultType {
    LOCAL,     // Найдено локально
    REMOTE,    // Найдено удаленно
    MIXED      // Часть результатов найдена локально, часть удаленно
}

/**
 * Компонент для отображения индикатора результатов поиска
 */
@Composable
fun SearchResultIndicator(
    resultType: SearchResultType,
    count: Int,
    query: String,
    modifier: Modifier = Modifier
) {
    // Определяем параметры отображения в зависимости от типа результата
    val (icon, label, bgColor) = when (resultType) {
        SearchResultType.LOCAL -> Triple(
            Icons.Default.Search,
            "Найдено локально",
            MaterialTheme.colorScheme.surfaceVariant
        )
        SearchResultType.REMOTE -> Triple(
            Icons.Default.Wifi,
            "Найдено на сервере",
            MaterialTheme.colorScheme.secondaryContainer
        )
        SearchResultType.MIXED -> Triple(
            Icons.Default.Info,
            "Комбинированный поиск",
            MaterialTheme.colorScheme.tertiaryContainer
        )
    }

    // Формируем результирующий текст
    val resultText = when (count) {
        0 -> "Ничего не найдено по запросу \"$query\""
        1 -> "Найдено 1 задание"
        else -> "Найдено $count заданий"
    }

    // Отображаем индикатор
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = resultText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}