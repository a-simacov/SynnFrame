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
    onObjectSelected: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStep = state.steps.getOrNull(state.currentStepIndex) ?: return
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Описание шага
        Text(
            text = currentStep.promptText,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Содержимое шага в зависимости от типа поля
        when (currentStep.factActionField) {
            FactActionField.STORAGE_PRODUCT -> {
                StorageProductStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = onObjectSelected
                )
            }
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> {
                StorageProductClassifierStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = onObjectSelected
                )
            }
            FactActionField.STORAGE_BIN, FactActionField.ALLOCATION_BIN -> {
                BinStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = onObjectSelected,
                    isStorage = currentStep.factActionField == FactActionField.STORAGE_BIN
                )
            }
            FactActionField.STORAGE_PALLET, FactActionField.ALLOCATION_PALLET -> {
                PalletStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = onObjectSelected,
                    isStorage = currentStep.factActionField == FactActionField.STORAGE_PALLET
                )
            }
            FactActionField.QUANTITY -> {
                QuantityStep(
                    step = currentStep,
                    state = state,
                    onQuantityChanged = { onObjectSelected(it) }
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

        // Кнопка подтверждения
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Далее")
        }
    }
}