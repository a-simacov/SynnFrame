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
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import timber.log.Timber

class PalletClosingFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        var isClosing by remember { mutableStateOf(false) }

        // Определяем, с какой паллетой работаем, на основе targetFieldType группы
        val pallet = when (groupContext.targetFieldType) {
            TaskXLineFieldType.STORAGE_PALLET -> wizardContext.results.storagePallet
            TaskXLineFieldType.PLACEMENT_PALLET -> wizardContext.results.placementPallet
            else -> {
                // Если тип не определен, берем первую непустую паллету
                wizardContext.results.storagePallet ?: wizardContext.results.placementPallet
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            pallet?.let {
                Text(
                    text = "Паллета: ${it.code}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Статус: ${if (it.isClosed) "Закрыта" else "Открыта"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    isClosing = true
                    pallet?.let { currentPallet ->
                        wizardViewModel.closePallet(currentPallet.code) { result ->
                            isClosing = false
                            if (result.isSuccess) {
                                val updatedPallet = currentPallet.copy(isClosed = true)

                                // Определяем, куда записать обновленную паллету
                                when (groupContext.targetFieldType) {
                                    TaskXLineFieldType.STORAGE_PALLET ->
                                        wizardContext.completeWithStoragePallet(updatedPallet)
                                    TaskXLineFieldType.PLACEMENT_PALLET ->
                                        wizardContext.completeWithPlacementPallet(updatedPallet)
                                    else -> {
                                        // Если тип не определен, обновляем по текущему состоянию
                                        if (wizardContext.results.storagePallet != null)
                                            wizardContext.completeWithStoragePallet(updatedPallet)
                                        else
                                            wizardContext.completeWithPlacementPallet(updatedPallet)
                                    }
                                }
                            } else {
                                // В случае ошибки просто идем дальше с текущей паллетой
                                Timber.e("Не удалось закрыть паллету: ${result.exceptionOrNull()?.message}")
                                wizardContext.onComplete(currentPallet)
                            }
                        }
                    } ?: run {
                        // Если паллета не найдена, просто идем дальше
                        isClosing = false
                        wizardContext.onComplete(null)
                    }
                },
                enabled = !isClosing && pallet != null && !pallet.isClosed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isClosing) "Закрытие..." else "Закрыть паллету")
            }
        }
    }
}