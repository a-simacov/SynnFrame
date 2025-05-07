package com.synngate.synnframe.presentation.common.inputs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
 * Компонент для выбора статуса товара.
 * Отображает список доступных статусов в виде радиокнопок.
 *
 * @param selectedStatus Текущий выбранный статус
 * @param onStatusSelected Обработчик выбора статуса
 * @param modifier Модификатор для компонента
 * @param isRequired Признак обязательного выбора статуса
 */
@Composable
fun ProductStatusSelector(
    selectedStatus: ProductStatus,
    onStatusSelected: (ProductStatus) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = true
) {
    // Словарь статусов и их описаний
    val statusOptions = mapOf(
        ProductStatus.STANDARD to "Кондиция (стандарт)",
        ProductStatus.DEFECTIVE to "Брак",
        ProductStatus.EXPIRED to "Просрочен"
    )

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
                .selectableGroup()
        ) {
            Text(
                text = if (isRequired) "Статус товара *" else "Статус товара",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            statusOptions.forEach { (status, description) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (status == selectedStatus),
                            onClick = { onStatusSelected(status) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (status == selectedStatus),
                        onClick = null // обработчик щелчка установлен на Row выше
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}