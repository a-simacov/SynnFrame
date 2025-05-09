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
import com.synngate.synnframe.domain.entity.taskx.Pallet

/**
 * Компонент карточки паллеты
 *
 * @param pallet Паллета для отображения
 * @param onClick Обработчик нажатия на карточку (null, если карточка не кликабельна)
 * @param isSelected Выбрана ли паллета
 * @param modifier Модификатор
 */
@Composable
fun PalletCard(
    pallet: Pallet,
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
                    text = "Код: ${pallet.code}",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                val statusText = if (pallet.isClosed) "Закрыта" else "Открыта"
                Text(
                    text = "Статус: $statusText",
                    style = MaterialTheme.typography.bodySmall
                )
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
 * Компонент для отображения списка паллет из плана
 *
 * @param planPallets Список паллет из плана
 * @param onPalletSelect Обработчик выбора паллеты
 * @param title Заголовок списка
 * @param modifier Модификатор
 */
@Composable
fun PlanPalletsList(
    planPallets: List<Pallet>,
    onPalletSelect: (Pallet) -> Unit,
    title: String = "По плану:",
    modifier: Modifier = Modifier,
    maxHeight: Int = 200
) {
    if (planPallets.isEmpty()) {
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
            items(planPallets) { pallet ->
                PalletCard(
                    pallet = pallet,
                    onClick = { onPalletSelect(pallet) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}