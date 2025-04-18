package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet

@Composable
fun ProductItem(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Артикул: ${product.articleNumber}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun BinItem(
    bin: BinX,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Код: ${bin.code}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Зона: ${bin.zone}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Расположение: ${bin.line}-${bin.rack}-${bin.tier}-${bin.position}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PalletItem(
    pallet: Pallet,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Код: ${pallet.code}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}