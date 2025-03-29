package com.synngate.synnframe.presentation.ui.tasks.components

import android.content.res.Configuration
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskPlanLine
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
import com.synngate.synnframe.presentation.ui.tasks.model.ProductDisplayProperty
import com.synngate.synnframe.presentation.ui.tasks.model.ProductPropertyType
import com.synngate.synnframe.presentation.ui.tasks.model.TaskLineItem
import com.synngate.synnframe.presentation.ui.tasks.model.formatProductProperty
import com.synngate.synnframe.presentation.util.formatQuantity

/**
 * Строка таблицы с данными плана и факта задания
 * @param lineItem Элемент строки задания
 * @param isEditable Флаг редактируемости
 * @param onClick Обработчик нажатия
 * @param productProperties Список дополнительных свойств товара для отображения
 * @param modifier Модификатор компонента
 */
@Composable
fun TaskLineItemRow(
    lineItem: TaskLineItem,
    isEditable: Boolean,
    onClick: () -> Unit,
    productProperties: List<ProductDisplayProperty> = emptyList(),
    modifier: Modifier = Modifier
) {
    val rowBackground = if (lineItem.factLine != null && lineItem.factLine.quantity > 0f) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }

    val planQuantity = lineItem.planLine.quantity
    val factQuantity = lineItem.factLine?.quantity ?: 0f

    // Определяем, совпадают ли значения плана и факта
    val isPlanEqualFact = planQuantity == factQuantity && factQuantity > 0f
    // Определяем, превышает ли факт план
    val isFactExceedsPlan = factQuantity > planQuantity

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
            // Колонка товара (60%)
            Column(
                modifier = Modifier.weight(0.6f)
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

            // Колонка план (20%)
            Text(
                text = formatQuantity(planQuantity),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.2f)
            )

            // Колонка факт (20%)
            Box(
                modifier = Modifier.weight(0.2f),
                contentAlignment = Alignment.Center
            ) {
                if (isPlanEqualFact) {
                    // Если факт равен плану, показываем "ОК" зеленым цветом
                    Text(
                        text = "ОК",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        color = Color.Green
                    )
                } else {
                    // Иначе показываем количество, с красным цветом если факт > план
                    Text(
                        text = formatQuantity(factQuantity),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = when {
                            isFactExceedsPlan -> Color.Red
                            factQuantity > 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
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
                        modifier = Modifier.weight(0.6f)
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

                    // Пустые колонки для план и факт (сохраняем структуру)
                    Spacer(modifier = Modifier.weight(0.4f))
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TaskItemRowPreview() {
    SynnFrameTheme {
        TaskLineItemRow(
            lineItem = TaskLineItem(
                planLine = TaskPlanLine(
                    id = "1",
                    taskId = "4646143131",
                    productId = "0000222",
                    quantity = 123f
                ),
                factLine = TaskFactLine(
                    id = "1",
                    taskId = "4646143131",
                    productId = "0000222",
                    quantity = 123f
                ),
                product = Product(
                    id = "0000222",
                    name = "This is a long name of prduct maybe will take some lines",
                    accountingModel = AccountingModel.QTY,
                    articleNumber = "ART-0002",
                    mainUnitId = "987987987987"
                ),
            ),
            isEditable = true,
            onClick = { /*TODO*/ },
            productProperties = listOf(
                ProductDisplayProperty(
                    type = ProductPropertyType.ID,
                    label = "ID"
                ),
                ProductDisplayProperty(
                    type = ProductPropertyType.ARTICLE,
                    label = "Артикул"
                )
            )
        )
    }
}