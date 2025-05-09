package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct

/**
 * Компонент карточки продукта
 *
 * @param product Продукт для отображения
 * @param onClick Обработчик нажатия на карточку (null, если карточка не кликабельна)
 * @param isSelected Выбран ли продукт
 * @param modifier Модификатор
 */
@Composable
fun ProductCard(
    product: Product,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
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
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Артикул: ${product.articleNumber}",
                style = MaterialTheme.typography.bodySmall
            )

            product.getMainUnit()?.let { unit ->
                Text(
                    text = "Основная ЕИ: ${unit.name}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (unit.mainBarcode.isNotEmpty()) {
                    Text(
                        text = "Штрихкод: ${unit.mainBarcode}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Компонент карточки продукта задания
 *
 * @param taskProduct Продукт задания для отображения
 * @param onClick Обработчик нажатия на карточку
 * @param isSelected Выбран ли продукт
 * @param modifier Модификатор
 */
@Composable
fun TaskProductCard(
    taskProduct: TaskProduct,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = taskProduct.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Артикул: ${taskProduct.product.articleNumber}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (taskProduct.quantity > 0) {
                    Text(
                        text = "Количество: ${taskProduct.quantity}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (taskProduct.hasExpirationDate()) {
                    Text(
                        text = "Срок годности: ${taskProduct.expirationDate}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            onClick?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Выбрать",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Компонент для отображения списка продуктов из плана
 *
 * @param planProducts Список продуктов из плана
 * @param onProductSelect Обработчик выбора продукта
 * @param title Заголовок списка
 * @param modifier Модификатор
 */
@Composable
fun PlanProductsList(
    planProducts: List<TaskProduct>,
    onProductSelect: (TaskProduct) -> Unit,
    title: String = "По плану:",
    modifier: Modifier = Modifier
) {
    if (planProducts.isEmpty()) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        planProducts.forEach { taskProduct ->
            TaskProductCard(
                taskProduct = taskProduct,
                onClick = { onProductSelect(taskProduct) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}