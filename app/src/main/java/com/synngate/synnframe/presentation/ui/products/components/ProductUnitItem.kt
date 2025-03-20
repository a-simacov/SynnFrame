package com.synngate.synnframe.presentation.ui.products.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.ProductUnit

/**
 * Компонент для отображения единицы измерения товара
 */
@Composable
fun ProductUnitItem(
    unit: ProductUnit,
    isSelected: Boolean,
    isMainUnit: Boolean,
    onClick: () -> Unit,
    showExtendedInfo: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = if (isSelected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Название единицы измерения
                Text(
                    text = unit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isMainUnit) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                // Индикатор основной единицы измерения
                if (isMainUnit) {
                    Text(
                        text = stringResource(id = R.string.main_unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Индикатор выбранной единицы измерения
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(id = R.string.selected),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Показываем расширенную информацию, если требуется
            if (showExtendedInfo) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Коэффициент пересчета
                    Text(
                        text = stringResource(id = R.string.unit_coefficient, unit.quantity),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Основной штрихкод
                    Text(
                        text = stringResource(id = R.string.main_barcode, unit.mainBarcode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Количество дополнительных штрихкодов
                if (unit.barcodes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            id = R.string.additional_barcodes_count,
                            unit.barcodes.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}