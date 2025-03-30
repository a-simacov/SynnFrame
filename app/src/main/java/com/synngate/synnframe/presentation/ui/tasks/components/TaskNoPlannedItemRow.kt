package com.synngate.synnframe.presentation.ui.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.synngate.synnframe.presentation.ui.tasks.model.ProductDisplayProperty
import com.synngate.synnframe.presentation.ui.tasks.model.ProductPropertyType
import com.synngate.synnframe.presentation.ui.tasks.model.TaskLineItem
import com.synngate.synnframe.presentation.ui.tasks.model.formatProductProperty
import com.synngate.synnframe.presentation.util.formatQuantity

/**
 * Строка таблицы с данными факта задания без плана (две колонки вместо трех)
 * @param lineItem Элемент строки задания
 * @param isEditable Флаг редактируемости
 * @param onClick Обработчик нажатия
 * @param productProperties Список дополнительных свойств товара для отображения
 * @param modifier Модификатор компонента
 */
@Composable
fun TaskNoPlannedItemRow(
    lineItem: TaskLineItem,
    isEditable: Boolean,
    onClick: () -> Unit,
    productProperties: List<ProductDisplayProperty> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Определяем цвет фона строки
    val rowBackground = when {
        lineItem.factLine != null && lineItem.factLine.quantity > 0f ->
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else ->
            Color.Transparent
    }

    val factQuantity = lineItem.factLine?.quantity ?: 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .clickable(enabled = isEditable) { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        // Основная строка с именем и значениями
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Колонка товара (70%)
            Column(
                modifier = Modifier.weight(0.7f)
            ) {
                // Блок с названием товара с фиксированной высотой
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = lineItem.product?.name ?: "Неизвестный товар",
                        style = MaterialTheme.typography.titleMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 4,
                    )
                }
            }

            // Колонка факт (30%)
            Box(
                modifier = Modifier.weight(0.3f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatQuantity(factQuantity),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = if (factQuantity > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Дополнительные свойства товара, если они заданы
        if (productProperties.isNotEmpty() && lineItem.product != null) {
            val additionalProperties = productProperties.filter {
                it.type != ProductPropertyType.NAME // Исключаем NAME, так как мы его уже показали
            }

            if (additionalProperties.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 4.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(0.7f)
                    ) {
                        additionalProperties.forEach { property ->
                            val formattedProperty =
                                formatProductProperty(lineItem.product, property)
                            if (formattedProperty.isNotEmpty()) {
                                Text(
                                    text = formattedProperty,
                                    style = MaterialTheme.typography.bodyMedium,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }

                    // Пустая колонка для факта (сохраняем структуру)
                    Spacer(modifier = Modifier.weight(0.3f))
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}