package com.synngate.synnframe.presentation.ui.wizard.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.domain.model.wizard.WizardResultModel
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import timber.log.Timber

class PalletCreationFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        var isCreating by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Нажмите кнопку для создания новой паллеты",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    isCreating = true
                    wizardViewModel.createPallet { result ->
                        isCreating = false
                        result.getOrNull()?.let { newPallet ->
                            // Определяем, куда записать паллету на основе targetFieldType группы
                            when (groupContext.targetFieldType) {
                                TaskXLineFieldType.STORAGE_PALLET ->
                                    wizardContext.completeWithStoragePallet(newPallet)
                                TaskXLineFieldType.PLACEMENT_PALLET ->
                                    wizardContext.completeWithPlacementPallet(newPallet)
                                else -> {
                                    Timber.w("Неизвестный тип целевого поля: ${groupContext.targetFieldType}")
                                    wizardContext.onComplete(newPallet)
                                }
                            }
                        }
                    }
                },
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCreating) "Создание..." else "Создать паллету")
            }
        }
    }

    override fun validateStepResult(action: FactLineXAction, results: WizardResultModel): Boolean {
        // Проверяем, что паллета создана
        return when (groupContext.targetFieldType) {
            TaskXLineFieldType.STORAGE_PALLET -> results.storagePallet != null
            TaskXLineFieldType.PLACEMENT_PALLET -> results.placementPallet != null
            else -> true
        }
    }
}