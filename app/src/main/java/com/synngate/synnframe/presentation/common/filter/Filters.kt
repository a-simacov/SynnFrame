package com.synngate.synnframe.presentation.common.filter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Компонент выбора фильтров для списка
 */
@Composable
fun FilterPanel(
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Заголовок панели фильтров
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(id = R.string.filter),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.filter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = { onVisibilityChange(!isVisible) }
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Close else Icons.Default.FilterList,
                    contentDescription = if (isVisible) stringResource(id = R.string.close)
                    else stringResource(id = R.string.filter)
                )
            }
        }

        HorizontalDivider()

        // Содержимое панели фильтров
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Компонент выбора диапазона дат для фильтрации
 */
@Composable
fun DateRangeFilter(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    onFromDateChange: (LocalDateTime?) -> Unit,
    onToDateChange: (LocalDateTime?) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.task_date_filter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Поля выбора диапазона дат
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DatePickerField(
                label = stringResource(id = R.string.task_date_from),
                value = fromDate,
                onValueChange = onFromDateChange,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            DatePickerField(
                label = stringResource(id = R.string.task_date_to),
                value = toDate,
                onValueChange = onToDateChange,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопки для применения и сброса фильтров
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onClear
            ) {
                Text(text = stringResource(id = R.string.clear))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onApply
            ) {
                Text(text = stringResource(id = R.string.apply))
            }
        }
    }
}

/**
 * Поле выбора даты
 */
@Composable
fun DatePickerField(
    label: String,
    value: LocalDateTime?,
    onValueChange: (LocalDateTime?) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    OutlinedTextField(
        value = value?.format(formatter) ?: "",
        onValueChange = { newValue ->
            // Обработка ввода даты вручную
            try {
                if (newValue.isBlank()) {
                    onValueChange(null)
                } else {
                    // Попытка парсинга введенной даты
                    val parsedDateTime = LocalDateTime.parse(
                        newValue,
                        formatter
                    )
                    onValueChange(parsedDateTime)
                }
            } catch (e: Exception) {
                // Ошибка парсинга - игнорируем ввод
            }
        },
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = {
                    // Здесь будет вызов диалога выбора даты
                    // Временная заглушка
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Выбрать дату"
                )
            }
        },
        singleLine = true,
        modifier = modifier
    )
}

/**
 * Группа фильтров-чипов для статусов
 */
@Composable
fun <T> StatusFilterChips(
    items: List<T>,
    selectedItems: Set<T>,
    onSelectionChanged: (Set<T>) -> Unit,
    itemToString: (T) -> String,
    modifier: Modifier = Modifier,
    allItem: T? = null,
    allItemText: String = stringResource(id = R.string.task_status_all)
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Если есть специальный элемент "Все", отображаем его отдельно
        if (allItem != null) {
            StatusFilterChip(
                text = allItemText,
                selected = selectedItems.isEmpty(),
                onClick = {
                    // При выборе "Все" очищаем все выбранные фильтры
                    onSelectionChanged(emptySet())
                }
            )
        }

        // Отображаем остальные элементы
        items.forEach { item ->
            if (item != allItem) {
                StatusFilterChip(
                    text = itemToString(item),
                    selected = selectedItems.contains(item),
                    onClick = {
                        // Обновляем выбранные фильтры
                        val newSelection = selectedItems.toMutableSet()
                        if (selectedItems.contains(item)) {
                            newSelection.remove(item)
                        } else {
                            newSelection.add(item)
                        }
                        onSelectionChanged(newSelection)
                    }
                )
            }
        }
    }
}

@Composable
fun StatusFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else null
    )
}

/**
 * Выпадающий список для выбора типа
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> TypeDropdownFilter(
    label: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T?) -> Unit,
    itemToString: (T) -> String,
    modifier: Modifier = Modifier,
    includeEmpty: Boolean = true,
    emptyItemText: String = stringResource(id = R.string.task_status_all)
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedItem?.let { itemToString(it) } ?: emptyItemText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (includeEmpty) {
                DropdownMenuItem(
                    text = { Text(emptyItemText) },
                    onClick = {
                        onItemSelected(null)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }

            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemToString(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}