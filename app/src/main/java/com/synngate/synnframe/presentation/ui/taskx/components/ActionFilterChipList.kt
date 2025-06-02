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
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.FilterAlt
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.model.filter.FilterItem

@Composable
fun ActionFilterChipList(
    filters: List<FilterItem>,
    onRemove: (FactActionField) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = filters.isNotEmpty(),
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
            filters.forEach { filterItem ->
                FilterChip(
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    selected = true,
                    onClick = { /* Чип уже выбран, ничего не делаем */ },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = filterItem.value,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Добавляем иконку в зависимости от типа поля
                            when (filterItem.field) {
                                FactActionField.STORAGE_BIN, FactActionField.ALLOCATION_BIN -> {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Ячейка",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                FactActionField.STORAGE_PALLET, FactActionField.ALLOCATION_PALLET -> {
                                    Icon(
                                        imageVector = Icons.Default.ViewInAr,
                                        contentDescription = "Паллета",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                else -> {
                                    // Для других типов полей оставляем текстовое обозначение
                                    Text(
                                        text = "(${filterItem.displayName.first().uppercase()})",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.FilterAlt,
                            contentDescription = "Фильтр",
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { onRemove(filterItem.field) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Удалить фильтр"
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = FilterChipDefaults.filterChipElevation(4.dp)
                )
            }
        }
    }
}