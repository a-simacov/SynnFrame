package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.components.StepCommandsSection
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState

@Composable
fun StepScreen(
    state: ActionWizardState,
    onConfirm: () -> Unit,
    onObjectSelected: (Any, Boolean) -> Unit,
    handleBarcode: (String) -> Unit,
    onRequestServerObject: () -> Unit,
    onCancelServerRequest: () -> Unit,
    onCommandExecute: (StepCommand, Map<String, String>) -> Unit, // Новый параметр
    modifier: Modifier = Modifier
) {
    val currentStep = state.getCurrentStep() ?: return
    val scrollState = rememberScrollState()

    val isObjectSelected = state.selectedObjects.containsKey(currentStep.id)

    val isLocked = state.lockedObjectSteps.contains(currentStep.id)
    val bufferSource = state.bufferObjectSources[currentStep.id]

    val isButtonEnabled = if (currentStep.isRequired) {
        currentStep.factActionField == FactActionField.NONE || isObjectSelected
    } else {
        true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentStep.promptText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // Если для шага используется serverSelectionEndpoint, показываем индикатор
            if (currentStep.serverSelectionEndpoint.isNotEmpty()) {
                Text(
                    text = "С сервера",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        if (bufferSource != null) {
            BufferIndicator(
                source = bufferSource,
                isLocked = isLocked
            )
        }

        when (currentStep.factActionField) {
            FactActionField.NONE -> {

            }
            FactActionField.STORAGE_PRODUCT -> {
                StorageProductStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = { obj -> onObjectSelected(obj, true) },
                    handleBarcode = handleBarcode,
                    onRequestServerObject = onRequestServerObject,
                    onCancelServerRequest = onCancelServerRequest,
                    isLocked = isLocked
                )
            }
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> {
                ProductClassifierStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = { obj -> onObjectSelected(obj, true) },
                    handleBarcode = handleBarcode,
                    onRequestServerObject = onRequestServerObject,
                    onCancelServerRequest = onCancelServerRequest,
                    isLocked = isLocked
                )
            }
            FactActionField.STORAGE_BIN -> {
                BinStep(
                    step = currentStep,
                    state = state,
                    onObjectSelected = { obj -> onObjectSelected(obj, true) },
                    handleBarcode = handleBarcode,
                    onRequestServerObject = onRequestServerObject,
                    onCancelServerRequest = onCancelServerRequest,
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
                    onRequestServerObject = onRequestServerObject,
                    onCancelServerRequest = onCancelServerRequest,
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
                    onRequestServerObject = onRequestServerObject,
                    onCancelServerRequest = onCancelServerRequest,
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
                    onRequestServerObject = onRequestServerObject,
                    onCancelServerRequest = onCancelServerRequest,
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
                    onRequestServerObject = onRequestServerObject,
                    onCancelServerRequest = onCancelServerRequest,
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

        // Секция команд - добавляем между основным контентом и кнопкой "Далее"
        StepCommandsSection(
            state = state,
            onCommandExecute = onCommandExecute
        )

        // Разделитель, если есть команды
        if (currentStep.commands.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = isButtonEnabled && !state.isRequestingServerObject
        ) {
            Text("Далее")
        }
    }
}