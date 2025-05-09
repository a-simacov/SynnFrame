package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.ProductStatus

/**
 * Компонент для выбора статуса продукта
 *
 * @param selectedStatus Выбранный статус
 * @param onStatusSelected Обработчик выбора статуса
 * @param modifier Модификатор
 * @param isEnabled Доступен ли компонент
 */
@Composable
fun ProductStatusSelector(
    selectedStatus: ProductStatus,
    onStatusSelected: (ProductStatus) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Статус продукта",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Перебираем все возможные значения статуса
            ProductStatus.values().forEach { status ->
                ProductStatusOption(
                    status = status,
                    selectedStatus = selectedStatus,
                    onStatusSelected = onStatusSelected,
                    isEnabled = isEnabled
                )
            }
        }
    }
}

/**
 * Опция для выбора статуса продукта
 */
@Composable
private fun ProductStatusOption(
    status: ProductStatus,
    selectedStatus: ProductStatus,
    onStatusSelected: (ProductStatus) -> Unit,
    isEnabled: Boolean
) {
    val isSelected = status == selectedStatus

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onStatusSelected(status) },
                enabled = isEnabled,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null, // использует selectable
            enabled = isEnabled
        )

        val statusDescription = when (status) {
            ProductStatus.STANDARD -> "Кондиция (стандарт)"
            ProductStatus.DEFECTIVE -> "Брак"
            ProductStatus.EXPIRED -> "Просрочен"
        }

        Text(
            text = statusDescription,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isEnabled)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}