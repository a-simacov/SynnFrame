package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.wizard.action.components.QuantityRow
import com.synngate.synnframe.presentation.ui.wizard.action.components.formatQuantityDisplay
import com.synngate.synnframe.presentation.util.formatDate

@Composable
private fun PalletInfo(pallet: Pallet) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = "Паллета: ${pallet.code}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
private fun BinInfo(bin: BinX) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = "Ячейка: ${bin.code}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
private fun TaskProductInfo(
    action: PlannedAction,
    factActionsInfo: Map<out Any?, Any?>,
    productEntry: Pair<String, TaskProduct>
) {
    val taskProduct = productEntry.second

    @Suppress("UNCHECKED_CAST")
    val relatedFactActions =
        (factActionsInfo[action.id] as? List<FactAction>) ?: emptyList()

    val previousCompletedQuantity = relatedFactActions.sumOf {
        val quantity = it.storageProduct?.quantity?.toDouble() ?: 0.0
        quantity
    }.toFloat()

    val currentQuantity = taskProduct.quantity

    val totalQuantity = previousCompletedQuantity + currentQuantity

    val plannedQuantity = action.storageProduct?.let {
        if (it.product.id == taskProduct.product.id) it.quantity else 0f
    } ?: 0f

    val remainingQuantity = (plannedQuantity - totalQuantity).coerceAtLeast(0f)

    val isOverLimit = plannedQuantity > 0f && totalQuantity > plannedQuantity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                text = taskProduct.product.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            DefaultSpacer(8.dp)

            Text(
                text = "Артикул: ${taskProduct.product.articleNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = "Статус: ${taskProduct.status.format()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )

            if (taskProduct.hasExpirationDate()) {
                Text(
                    text = "Срок годности: ${formatDate(taskProduct.expirationDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            DefaultSpacer()
            HorizontalDivider()
            DefaultSpacer()

            QuantityRow(label = "Запланировано:", value = formatQuantityDisplay(plannedQuantity))

            if (previousCompletedQuantity > 0f) {
                DefaultSpacer()
                QuantityRow(
                    label = "Выполнено:",
                    value = formatQuantityDisplay(previousCompletedQuantity)
                )
            }

            DefaultSpacer()
            QuantityRow(label = "Текущее:", value = formatQuantityDisplay(currentQuantity))

            if (previousCompletedQuantity > 0f) {
                DefaultSpacer()
                HorizontalDivider()
                DefaultSpacer()

                QuantityRow(label = "Текущий итог:", value = formatQuantityDisplay(totalQuantity))

                if (plannedQuantity > 0f && totalQuantity > plannedQuantity) {
                    DefaultSpacer()
                    Text(
                        text = "Внимание: общее количество превышает плановое!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            DefaultSpacer()
            QuantityRow(
                label = "Осталось:",
                value = formatQuantityDisplay(remainingQuantity),
                warning = isOverLimit
            )
        }
    }
}

@Composable
fun DefaultSpacer(height: Dp = 4.dp) {
    Spacer(modifier = Modifier.height(height))
}