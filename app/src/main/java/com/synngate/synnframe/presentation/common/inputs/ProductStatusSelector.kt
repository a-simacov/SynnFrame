package com.synngate.synnframe.presentation.common.inputs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.synngate.synnframe.domain.entity.taskx.ProductStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductStatusSelector(
    selectedStatus: ProductStatus,
    onStatusSelected: (ProductStatus) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    val statusOptions = remember {
        mapOf(
            ProductStatus.STANDARD to "Кондиция (стандарт)",
            ProductStatus.DEFECTIVE to "Брак",
            ProductStatus.EXPIRED to "Просрочен"
        )
    }

    val selectedStatusText = statusOptions[selectedStatus] ?: "Кондиция (стандарт)"

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedStatusText,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize()
            ) {
                statusOptions.forEach { (status, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onStatusSelected(status)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}