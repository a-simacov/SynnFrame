package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.model.buffer.BufferDisplayItem

@Composable
fun BufferItemChipList(
    items: List<BufferDisplayItem>,
    onRemove: (FactActionField) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = items.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEach { item ->
                FilterChip(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    selected = true,
                    onClick = { /* Чип уже выбран, ничего не делаем */ },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.value,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Добавляем иконку в зависимости от типа поля
                            when (item.field) {
                                FactActionField.STORAGE_BIN, FactActionField.ALLOCATION_BIN -> {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                FactActionField.STORAGE_PALLET, FactActionField.ALLOCATION_PALLET -> {
                                    Icon(
                                        imageVector = Icons.Default.ViewInAr,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                else -> {

                                }
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { onRemove(item.field) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    elevation = FilterChipDefaults.filterChipElevation(4.dp)
                )
            }
        }
    }
}