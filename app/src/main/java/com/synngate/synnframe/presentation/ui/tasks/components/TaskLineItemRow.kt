package com.synngate.synnframe.presentation.ui.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.tasks.model.TaskLineItem
import com.synngate.synnframe.presentation.util.formatQuantity

/**
 * Строка таблицы с данными плана и факта задания
 */
@Composable
fun TaskLineItemRow(
    lineItem: TaskLineItem,
    isEditable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowBackground = if (lineItem.factLine != null && lineItem.factLine.quantity > 0f) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }

    val planQuantity = lineItem.planLine.quantity
    val factQuantity = lineItem.factLine?.quantity ?: 0f
    val completionPercent = if (planQuantity > 0) (factQuantity / planQuantity) * 100f else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .clickable(enabled = isEditable) { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Колонка товара (40%)
            Text(
                text = lineItem.product?.name ?: "Неизвестный товар",
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                modifier = Modifier.weight(0.4f)
            )

            // Колонка план (30%)
            Text(
                text = formatQuantity(planQuantity),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.3f)
            )

            // Колонка факт (30%)
            Text(
                text = formatQuantity(factQuantity),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = getCompletionColor(completionPercent),
                modifier = Modifier.weight(0.3f)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

/**
 * Возвращает цвет в зависимости от процента выполнения
 */
@Composable
private fun getCompletionColor(completionPercent: Float): Color {
    return when {
        completionPercent >= 100f -> MaterialTheme.colorScheme.primary
        completionPercent >= 80f -> MaterialTheme.colorScheme.secondary
        completionPercent > 0f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}