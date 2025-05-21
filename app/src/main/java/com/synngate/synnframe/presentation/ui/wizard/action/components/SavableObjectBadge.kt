package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType

/**
 * Отображает информацию о сохраняемом объекте в виде бейджа
 */
@Composable
fun SavableObjectBadge(
    data: Any,
    objectType: ActionObjectType,
    modifier: Modifier = Modifier
) {
    val (icon, label, color) = getObjectVisualInfo(data, objectType)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun getObjectVisualInfo(data: Any, objectType: ActionObjectType): Triple<ImageVector, String, Color> {
    return when (objectType) {
        ActionObjectType.PALLET -> {
            val pallet = data as? Pallet
            val label = pallet?.code ?: "Паллета"
            Triple(
                Icons.Default.ViewInAr,
                label,
                MaterialTheme.colorScheme.tertiary
            )
        }
        ActionObjectType.BIN -> {
            val bin = data as? BinX
            val label = bin?.code ?: "Ячейка"
            Triple(
                Icons.Default.Archive,
                label,
                MaterialTheme.colorScheme.secondary
            )
        }
        ActionObjectType.TASK_PRODUCT -> {
            val product = data as? TaskProduct
            val label = product?.product?.name ?: "Товар"
            Triple(
                Icons.Default.Inventory,
                label,
                MaterialTheme.colorScheme.primary
            )
        }
        ActionObjectType.CLASSIFIER_PRODUCT -> {
            val product = data as? Product
            val label = product?.name ?: "Товар"
            Triple(
                Icons.Default.Inventory2,
                label,
                MaterialTheme.colorScheme.primary
            )
        }
        else -> {
            Triple(
                Icons.Default.Inventory,
                "Объект",
                MaterialTheme.colorScheme.error
            )
        }
    }
}