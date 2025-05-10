package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
        modifier = cardModifier.fillMaxWidth(),
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
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ячейка: ${bin.code}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

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

@Composable
fun PlanBinsList(
    planBins: List<BinX>,
    onBinSelect: (BinX) -> Unit,
    selectedBinCode: String? = null,
    modifier: Modifier = Modifier
) {
    if (planBins.isEmpty()) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        planBins.forEach { bin ->
            val isSelected = selectedBinCode == bin.code

            BinCard(
                bin = bin,
                onClick = { onBinSelect(bin) },
                isSelected = isSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}