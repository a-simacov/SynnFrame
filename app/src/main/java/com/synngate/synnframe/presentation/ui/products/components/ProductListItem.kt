package com.synngate.synnframe.presentation.ui.products.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product

/**
 * Компонент для отображения товара в списке
 */
@Composable
fun ProductListItem(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isSelected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Название товара
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Артикул товара и единица измерения
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.product_article, product.articleNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // Основная единица измерения
                product.getMainUnit()?.let { unit ->
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(id = R.string.product_main_unit, unit.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Модель учёта товара
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val accountingModelText = when (product.accountingModel) {
                    AccountingModel.BATCH -> stringResource(id = R.string.accounting_model_batch)
                    AccountingModel.QTY -> stringResource(id = R.string.accounting_model_qty)
                }

                val color = if (product.accountingModel == AccountingModel.BATCH)
                    MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.secondaryContainer

                val textColor = if (product.accountingModel == AccountingModel.BATCH)
                    MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSecondaryContainer

                // Индикатор модели учёта
                Text(
                    text = accountingModelText,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier
                        .background(
                            color = color,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}