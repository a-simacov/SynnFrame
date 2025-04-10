package com.synngate.synnframe.presentation.ui.taskx

import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.FactLineXActionType
import com.synngate.synnframe.presentation.ui.taskx.components.BinSelectionStep
import com.synngate.synnframe.presentation.ui.taskx.components.ClosePalletStep
import com.synngate.synnframe.presentation.ui.taskx.components.CreatePalletStep
import com.synngate.synnframe.presentation.ui.taskx.components.PalletSelectionStep
import com.synngate.synnframe.presentation.ui.taskx.components.PrintLabelStep
import com.synngate.synnframe.presentation.ui.taskx.components.ProductQuantityStep
import com.synngate.synnframe.presentation.ui.taskx.components.ProductSelectionStep
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel

/**
 * Фабрика для создания компонентов шагов мастера
 */
object FactLineWizardStepFactory {

    @Composable
    fun CreateStep(
        action: FactLineXAction,
        intermediateResults: Map<String, Any?>,
        onStepComplete: (Any?) -> Unit,
        wizardViewModel: FactLineWizardViewModel
    ) {
        // Получаем реализацию для конкретного типа действия
        val strategy = getStepStrategy(action.actionType)

        // Отрисовываем компонент с помощью стратегии
        strategy.CreateStepContent(
            action = action,
            intermediateResults = intermediateResults,
            onStepComplete = onStepComplete,
            wizardViewModel = wizardViewModel
        )
    }

    private fun getStepStrategy(actionType: FactLineXActionType): WizardStepStrategy {
        return when (actionType) {
            FactLineXActionType.SELECT_PRODUCT -> ProductSelectionStrategy()
            FactLineXActionType.ENTER_QUANTITY -> QuantityInputStrategy()
            FactLineXActionType.SELECT_BIN -> BinSelectionStrategy()
            FactLineXActionType.SELECT_PALLET -> PalletSelectionStrategy()
            FactLineXActionType.CREATE_PALLET -> PalletCreationStrategy()
            FactLineXActionType.CLOSE_PALLET -> PalletClosingStrategy()
            FactLineXActionType.PRINT_LABEL -> LabelPrintingStrategy()
        }
    }
}

/**
 * Базовый интерфейс стратегии для шага мастера
 */
interface WizardStepStrategy {
    @Composable
    fun CreateStepContent(
        action: FactLineXAction,
        intermediateResults: Map<String, Any?>,
        onStepComplete: (Any?) -> Unit,
        wizardViewModel: FactLineWizardViewModel
    )
}

/**
 * Стратегия для шага выбора продукта
 */
class ProductSelectionStrategy : WizardStepStrategy {
    @Composable
    override fun CreateStepContent(
        action: FactLineXAction,
        intermediateResults: Map<String, Any?>,
        onStepComplete: (Any?) -> Unit,
        wizardViewModel: FactLineWizardViewModel
    ) {
        ProductSelectionStep(
            promptText = action.promptText,
            selectionCondition = action.selectionCondition,
            intermediateResults = intermediateResults,
            onProductSelected = onStepComplete,
            viewModel = wizardViewModel
        )
    }
}

/**
 * Стратегия для шага ввода количества
 */
class QuantityInputStrategy : WizardStepStrategy {
    @Composable
    override fun CreateStepContent(
        action: FactLineXAction,
        intermediateResults: Map<String, Any?>,
        onStepComplete: (Any?) -> Unit,
        wizardViewModel: FactLineWizardViewModel
    ) {
        ProductQuantityStep(
            promptText = action.promptText,
            intermediateResults = intermediateResults,
            onQuantityEntered = onStepComplete,
            viewModel = wizardViewModel
        )
    }
}

// Аналогично для остальных стратегий...
class BinSelectionStrategy : WizardStepStrategy {
    @Composable
    override fun CreateStepContent(
        action: FactLineXAction,
        intermediateResults: Map<String, Any?>,
        onStepComplete: (Any?) -> Unit,
        wizardViewModel: FactLineWizardViewModel
    ) {
        BinSelectionStep(
            promptText = action.promptText,
            zoneFilter = action.additionalParams["zone"],
            onBinSelected = onStepComplete,
            viewModel = wizardViewModel
        )
    }
}

class PalletSelectionStrategy : WizardStepStrategy {
    @Composable
    override fun CreateStepContent(
        action: FactLineXAction,
        intermediateResults: Map<String, Any?>,
        onStepComplete: (Any?) -> Unit,
        wizardViewModel: FactLineWizardViewModel
    ) {
        PalletSelectionStep(
            promptText = action.promptText,
            selectionCondition = action.selectionCondition,
            onPalletSelected = onStepComplete,
            viewModel = wizardViewModel
        )
    }
}

class PalletCreationStrategy : WizardStepStrategy {
    @Composable
    override fun CreateStepContent(
        action: FactLineXAction,
        intermediateResults: Map<String, Any?>,
        onStepComplete: (Any?) -> Unit,
        wizardViewModel: FactLineWizardViewModel
    ) {
        CreatePalletStep(
            promptText = action.promptText,
            onPalletCreated = onStepComplete,
            viewModel = wizardViewModel
        )
    }
}

class PalletClosingStrategy : WizardStepStrategy {
    @Composable
    override fun CreateStepContent(
        action: FactLineXAction,
        intermediateResults: Map<String, Any?>,
        onStepComplete: (Any?) -> Unit,
        wizardViewModel: FactLineWizardViewModel
    ) {
        ClosePalletStep(
            promptText = action.promptText,
            intermediateResults = intermediateResults,
            onPalletClosed = onStepComplete,
            viewModel = wizardViewModel
        )
    }
}

class LabelPrintingStrategy : WizardStepStrategy {
    @Composable
    override fun CreateStepContent(
        action: FactLineXAction,
        intermediateResults: Map<String, Any?>,
        onStepComplete: (Any?) -> Unit,
        wizardViewModel: FactLineWizardViewModel
    ) {
        PrintLabelStep(
            promptText = action.promptText,
            intermediateResults = intermediateResults,
            onLabelPrinted = onStepComplete,
            viewModel = wizardViewModel
        )
    }
}