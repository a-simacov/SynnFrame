package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.BinX

/**
 * Компонент карточки ячейки
 *
 * @param bin Ячейка для отображения
 * @param onClick Обработчик нажатия на карточку (null, если карточка не кликабельна)
 * @param isSelected Выбрана ли ячейка
 * @param modifier Модификатор
 */
@Composable
fun BinCard(
    bin: BinX,
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
                    text = "Код: ${bin.code}",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Зона: ${bin.zone}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (bin.line.isNotEmpty() || bin.rack.isNotEmpty() ||
                    bin.tier.isNotEmpty() || bin.position.isNotEmpty()) {

                    val locationParts = listOfNotNull(
                        bin.line.takeIf { it.isNotEmpty() },
                        bin.rack.takeIf { it.isNotEmpty() },
                        bin.tier.takeIf { it.isNotEmpty() },
                        bin.position.takeIf { it.isNotEmpty() }
                    )

                    val location = locationParts.joinToString("-")

                    if (location.isNotEmpty()) {
                        Text(
                            text = "Расположение: $location",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
 * Компонент для отображения списка ячеек из плана
 *
 * @param planBins Список ячеек из плана
 * @param onBinSelect Обработчик выбора ячейки
 * @param title Заголовок списка
 * @param modifier Модификатор
 */
@Composable
fun PlanBinsList(
    planBins: List<BinX>,
    onBinSelect: (BinX) -> Unit,
    title: String = "По плану:",
    modifier: Modifier = Modifier,
    maxHeight: Int = 200
) {
    if (planBins.isEmpty()) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight.dp)
        ) {
            items(planBins) { bin ->
                BinCard(
                    bin = bin,
                    onClick = { onBinSelect(bin) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}