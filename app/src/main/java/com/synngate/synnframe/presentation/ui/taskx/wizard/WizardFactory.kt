package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.FactLineXActionType
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.presentation.ui.taskx.components.BinSelectionStep
import com.synngate.synnframe.presentation.ui.taskx.components.ClosePalletStep
import com.synngate.synnframe.presentation.ui.taskx.components.CreatePalletStep
import com.synngate.synnframe.presentation.ui.taskx.components.ExpirationDateStep
import com.synngate.synnframe.presentation.ui.taskx.components.PalletSelectionStep
import com.synngate.synnframe.presentation.ui.taskx.components.PrintLabelStep
import com.synngate.synnframe.presentation.ui.taskx.components.ProductQuantityStep
import com.synngate.synnframe.presentation.ui.taskx.components.ProductSelectionStep
import com.synngate.synnframe.presentation.ui.taskx.components.ProductStatusStep
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import timber.log.Timber

/**
 * Фабрика для создания шагов мастера на основе конфигурации типа задания
 */
class WizardFactory(private val wizardViewModel: FactLineWizardViewModel) {

    /**
     * Создает шаги мастера на основе конфигурации типа задания
     */
    fun createWizardSteps(taskType: TaskTypeX): List<WizardStep> {
        val steps = mutableListOf<WizardStep>()

        // Преобразуем каждую группу действий в шаги мастера
        taskType.factLineActionGroups.forEach { actionGroup ->
            Timber.d("Создание шагов для группы: ${actionGroup.name}")

            // Добавляем шаг для установки WMS-действия для текущей группы
            steps.add(createWmsActionStep(actionGroup))

            // Создаем шаги для каждого действия в группе
            actionGroup.actions.sortedBy { it.order }.forEach { action ->
                try {
                    Timber.d("Добавление шага: ${action.name}")
                    steps.add(createStepFromAction(action, actionGroup))
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при создании шага для действия ${action.actionType}")
                }
            }
        }

        Timber.d("Всего создано шагов: ${steps.size}")
        steps.forEachIndexed { index, step ->
            Timber.d("Шаг ${index + 1}: ${step.title}")
        }

        return steps
    }

    /**
     * Создает шаг мастера на основе конфигурации действия
     */
    private fun createStepFromAction(
        action: FactLineXAction,
        parentGroup: FactLineActionGroup
    ): WizardStep {
        // Генерируем id на основе типа целевого поля и типа действия
        val stepId = "${parentGroup.targetFieldType.name}_${action.id}"

        return WizardStep(
            id = stepId,
            title = action.name,
            content = { context ->
                // В зависимости от типа действия выбираем соответствующий компонент
                when (action.actionType) {
                    FactLineXActionType.SELECT_PRODUCT -> ProductSelectionStep(
                        promptText = action.promptText,
                        selectionCondition = action.selectionCondition,
                        intermediateResults = context.results,
                        onProductSelected = { product -> context.onComplete(product) },
                        viewModel = wizardViewModel
                    )

                    FactLineXActionType.ENTER_QUANTITY -> ProductQuantityStep(
                        promptText = action.promptText,
                        intermediateResults = context.results,
                        onQuantityEntered = { quantity -> context.onComplete(quantity) },
                        viewModel = wizardViewModel
                    )

                    FactLineXActionType.SELECT_BIN -> {
                        // Получаем зону из параметров действия, если указана
                        val zone = action.additionalParams["zone"]

                        BinSelectionStep(
                            promptText = action.promptText,
                            zoneFilter = zone,
                            onBinSelected = { bin -> context.onComplete(bin) },
                            viewModel = wizardViewModel
                        )
                    }

                    FactLineXActionType.SELECT_PALLET -> PalletSelectionStep(
                        promptText = action.promptText,
                        selectionCondition = action.selectionCondition,
                        onPalletSelected = { pallet -> context.onComplete(pallet) },
                        viewModel = wizardViewModel
                    )

                    FactLineXActionType.CREATE_PALLET -> CreatePalletStep(
                        promptText = action.promptText,
                        onPalletCreated = { pallet -> context.onComplete(pallet) },
                        viewModel = wizardViewModel
                    )

                    FactLineXActionType.CLOSE_PALLET -> ClosePalletStep(
                        promptText = action.promptText,
                        intermediateResults = context.results,
                        onPalletClosed = { success -> context.onComplete(success) },
                        viewModel = wizardViewModel
                    )

                    FactLineXActionType.PRINT_LABEL -> PrintLabelStep(
                        promptText = action.promptText,
                        intermediateResults = context.results,
                        onLabelPrinted = { success -> context.onComplete(success) },
                        viewModel = wizardViewModel
                    )

                    FactLineXActionType.SELECT_PRODUCT_STATUS -> ProductStatusStep(
                        promptText = action.promptText,
                        intermediateResults = context.results,
                        onStatusSelected = { status -> context.onComplete(status) },
                        viewModel = wizardViewModel
                    )

                    FactLineXActionType.ENTER_EXPIRATION_DATE -> ExpirationDateStep(
                        promptText = action.promptText,
                        intermediateResults = context.results,
                        onDateEntered = { date -> context.onComplete(date) },
                        viewModel = wizardViewModel
                    )
                }
            },
            validator = { results ->
                // Базовая валидация - проверяем наличие результата для этого шага
                results.containsKey(stepId)
            },
            // Навигация назад разрешена для всех шагов
            canNavigateBack = true
        )
    }

    /**
     * Создает шаг установки действия WMS для группы
     */
    private fun createWmsActionStep(actionGroup: FactLineActionGroup): WizardStep {
        return WizardStep(
            id = "WMS_ACTION_${actionGroup.id}",
            title = "Установка действия",
            content = { context ->
                // Вместо автоматического вызова onComplete, отобразим UI для подтверждения
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Начало группы действий: ${actionGroup.name}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            // При нажатии кнопки устанавливаем оба значения и переходим дальше
                            val wmsAction = actionGroup.wmsAction
                            context.onComplete(wmsAction)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Начать")
                    }
                }
            },
            canNavigateBack = false
        )
    }

    /**
     * Преобразует тип целевого поля в результат для строки факта
     */
    private fun mapTargetFieldTypeToResult(
        fieldType: TaskXLineFieldType,
        currentResults: Map<String, Any?>
    ): Any? {
        return when (fieldType) {
            TaskXLineFieldType.STORAGE_PRODUCT -> currentResults["STORAGE_PRODUCT"]
            TaskXLineFieldType.STORAGE_PALLET -> currentResults["STORAGE_PALLET"]
            TaskXLineFieldType.PLACEMENT_PALLET -> currentResults["PLACEMENT_PALLET"]
            TaskXLineFieldType.PLACEMENT_BIN -> currentResults["PLACEMENT_BIN"]
            TaskXLineFieldType.WMS_ACTION -> TODO()
        }
    }
}