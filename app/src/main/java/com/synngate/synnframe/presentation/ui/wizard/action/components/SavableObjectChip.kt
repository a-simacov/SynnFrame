package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.SavableObject
import com.synngate.synnframe.domain.entity.taskx.SavableObjectData
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType

@Composable
fun SavableObjectChip(
    savableObject: SavableObject,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (iconVector, description, label, chipColor) = getObjectVisualData(savableObject)

    AssistChip(
        onClick = { /* Нет действия при клике */ },
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = iconVector,
                contentDescription = description,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        trailingIcon = {
            IconButton(
                onClick = { onRemove(savableObject.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить",
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = chipColor.copy(alpha = 0.15f),
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = chipColor,
            trailingIconContentColor = MaterialTheme.colorScheme.error
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = chipColor.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun getObjectVisualData(savableObject: SavableObject): Quadruple<ImageVector, String, String, Color> {
    return when (savableObject.objectType) {
        ActionObjectType.PALLET -> {
            val pallet = (savableObject.objectData as? SavableObjectData.PalletData)?.pallet
            val label = pallet?.code ?: "Паллета"
            Quadruple(
                Icons.Default.ViewInAr,
                "Паллета",
                label,
                MaterialTheme.colorScheme.tertiary
            )
        }
        ActionObjectType.BIN -> {
            val bin = (savableObject.objectData as? SavableObjectData.BinData)?.bin
            val label = bin?.code ?: "Ячейка"
            Quadruple(
                Icons.Default.Archive,
                "Ячейка",
                label,
                MaterialTheme.colorScheme.secondary
            )
        }
        ActionObjectType.TASK_PRODUCT -> {
            val product = (savableObject.objectData as? SavableObjectData.TaskProductData)?.taskProduct
            val label = product?.product?.name ?: "Товар"
            Quadruple(
                Icons.Default.Inventory,
                "Товар",
                label,
                MaterialTheme.colorScheme.primary
            )
        }
        ActionObjectType.CLASSIFIER_PRODUCT -> {
            val product = (savableObject.objectData as? SavableObjectData.ProductData)?.product
            val label = product?.name ?: "Товар"
            Quadruple(
                Icons.Default.Inventory2,
                "Товар",
                label,
                MaterialTheme.colorScheme.primary
            )
        }
        else -> {
            Quadruple(
                Icons.Default.Inventory,
                "Объект",
                "Неизвестный объект",
                MaterialTheme.colorScheme.error
            )
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)