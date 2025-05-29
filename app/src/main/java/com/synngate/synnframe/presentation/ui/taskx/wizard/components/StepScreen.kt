package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState

@Composable
fun StepScreen(
    state: ActionWizardState,
    onConfirm: () -> Unit,
    onObjectSelected: (Any, Boolean) -> Unit,
    handleBarcode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStep = state.getCurrentStep() ?: return
    val scrollState = rememberScrollState()

    val isObjectSelected = state.selectedObjects.containsKey(currentStep.id)

    // Определяем, заблокирован ли шаг буфером
    val isLocked = state.lockedObjectSteps.contains(currentStep.id)
    val bufferSource = state.bufferObjectSources[currentStep.id]

    val isButtonEnabled = if (currentStep.isRequired) {
        isObjectSelected
    } else {
        true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = currentStep.promptText,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Показываем индикатор буфера, если значение взято из буфера
        if (bufferSource != null) {
            BufferIndicator(
                source = bufferSource,
                isLocked = isLocked
            )
        }

        // Выбираем компонент в зависимости от типа поля
        when (currentStep.factActionField) {
            FactActionField.STORAGE_PRODUCT -> {
                StorageProductStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = { obj -> onObjectSelected(obj, true) },
                    handleBarcode = handleBarcode,
                    isLocked = isLocked
                )
            }
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> {
                ProductClassifierStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = { obj -> onObjectSelected(obj, true) },
                    handleBarcode = handleBarcode,
                    isLocked = isLocked
                )
            }
            FactActionField.STORAGE_BIN -> {
                BinStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = { obj -> onObjectSelected(obj, true) },
                    handleBarcode = handleBarcode,
                    isStorage = true,
                    isLocked = isLocked
                )
            }
            FactActionField.ALLOCATION_BIN -> {
                BinStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = { obj -> onObjectSelected(obj, true) },
                    handleBarcode = handleBarcode,
                    isStorage = false,
                    isLocked = isLocked
                )
            }
            FactActionField.STORAGE_PALLET -> {
                PalletStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = { obj -> onObjectSelected(obj, true) },
                    handleBarcode = handleBarcode,
                    isStorage = true,
                    isLocked = isLocked
                )
            }
            FactActionField.ALLOCATION_PALLET -> {
                PalletStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = { obj -> onObjectSelected(obj, true) },
                    handleBarcode = handleBarcode,
                    isStorage = false,
                    isLocked = isLocked
                )
            }
            FactActionField.QUANTITY -> {
                QuantityStep(
                    step = currentStep,
                    state = state,
                    onQuantityChanged = { value, autoAdvance ->
                        onObjectSelected(value, autoAdvance)
                    },
                    isLocked = isLocked
                )
            }
            else -> {
                Text(
                    text = "Тип шага не поддерживается: ${currentStep.factActionField}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = isButtonEnabled
        ) {
            Text("Далее")
        }
    }
}