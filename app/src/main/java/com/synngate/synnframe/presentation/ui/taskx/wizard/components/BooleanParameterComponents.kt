package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.entity.BooleanDisplayType
import com.synngate.synnframe.presentation.ui.taskx.entity.BooleanLabelPair
import com.synngate.synnframe.presentation.ui.taskx.entity.BooleanParameterOptions
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandParameter

/**
 * Входная точка для выбора компонента отображения булевого параметра
 * в зависимости от типа отображения
 */
@Composable
fun BooleanParameterField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    booleanOptions: BooleanParameterOptions = BooleanParameterOptions()
) {
    when (booleanOptions.displayType) {
        BooleanDisplayType.CHECKBOX ->
            CheckboxBooleanField(parameter, value, onValueChange)

        BooleanDisplayType.SWITCH ->
            SwitchBooleanField(parameter, value, onValueChange, booleanOptions.labelPair)

        BooleanDisplayType.RADIO_BUTTONS ->
            RadioButtonsBooleanField(parameter, value, onValueChange, booleanOptions.labelPair)

        BooleanDisplayType.SEGMENTED_BUTTONS ->
            SegmentedButtonsBooleanField(parameter, value, onValueChange, booleanOptions.labelPair)

        BooleanDisplayType.TOGGLE_BUTTONS ->
            ToggleButtonsBooleanField(parameter, value, onValueChange, booleanOptions.labelPair)

        BooleanDisplayType.DROPDOWN ->
            DropdownBooleanField(parameter, value, onValueChange, booleanOptions.labelPair)
    }
}

/**
 * Стандартный флажок (уже реализован, но обновлен для единообразия)
 */
@Composable
fun CheckboxBooleanField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit
) {
    val checked = value.toBooleanStrictOrNull() ?: false

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (parameter.isRequired) {
            Text(
                text = "${parameter.displayName} *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = parameter.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { newChecked ->
                    onValueChange(newChecked.toString())
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (checked) "Выбрано" else "Не выбрано",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Переключатель (Switch)
 */
@Composable
fun SwitchBooleanField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    labelPair: BooleanLabelPair = BooleanLabelPair.DEFAULT
) {
    val checked = value.toBooleanStrictOrNull() ?: false

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (parameter.isRequired) {
            Text(
                text = "${parameter.displayName} *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = parameter.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (checked) labelPair.trueLabel else labelPair.falseLabel,
                style = MaterialTheme.typography.bodyLarge
            )

            Switch(
                checked = checked,
                onCheckedChange = { newChecked ->
                    onValueChange(newChecked.toString())
                }
            )
        }
    }
}

/**
 * Радио-кнопки Да/Нет
 */
@Composable
fun RadioButtonsBooleanField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    labelPair: BooleanLabelPair = BooleanLabelPair.DEFAULT
) {
    val checked = value.toBooleanStrictOrNull() ?: false

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (parameter.isRequired) {
            Text(
                text = "${parameter.displayName} *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = parameter.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // True option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                RadioButton(
                    selected = checked,
                    onClick = { onValueChange("true") }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = labelPair.trueLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // False option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                RadioButton(
                    selected = !checked,
                    onClick = { onValueChange("false") }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = labelPair.falseLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Сегментированные кнопки
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButtonsBooleanField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    labelPair: BooleanLabelPair = BooleanLabelPair.DEFAULT
) {
    val checked = value.toBooleanStrictOrNull() ?: false
    val selectedIndex = if (checked) 0 else 1

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (parameter.isRequired) {
            Text(
                text = "${parameter.displayName} *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = parameter.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { onValueChange("true") },
                selected = selectedIndex == 0
            ) {
                Text(labelPair.trueLabel)
            }

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { onValueChange("false") },
                selected = selectedIndex == 1
            ) {
                Text(labelPair.falseLabel)
            }
        }
    }
}

/**
 * Кнопки выбора (Toggle Buttons)
 */
@Composable
fun ToggleButtonsBooleanField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    labelPair: BooleanLabelPair = BooleanLabelPair.DEFAULT
) {
    val checked = value.toBooleanStrictOrNull() ?: false

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (parameter.isRequired) {
            Text(
                text = "${parameter.displayName} *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = parameter.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { onValueChange("true") },
                colors = if (checked) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(labelPair.trueLabel)
            }

            FilledTonalButton(
                onClick = { onValueChange("false") },
                colors = if (!checked) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(labelPair.falseLabel)
            }
        }
    }
}

/**
 * Выпадающий список с двумя опциями
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownBooleanField(
    parameter: CommandParameter,
    value: String,
    onValueChange: (String) -> Unit,
    labelPair: BooleanLabelPair = BooleanLabelPair.DEFAULT
) {
    val checked = value.toBooleanStrictOrNull() ?: false
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (parameter.isRequired) {
            Text(
                text = "${parameter.displayName} *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = parameter.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = if (checked) labelPair.trueLabel else labelPair.falseLabel,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(labelPair.trueLabel) },
                    onClick = {
                        onValueChange("true")
                        expanded = false
                    }
                )

                DropdownMenuItem(
                    text = { Text(labelPair.falseLabel) },
                    onClick = {
                        onValueChange("false")
                        expanded = false
                    }
                )
            }
        }
    }
}