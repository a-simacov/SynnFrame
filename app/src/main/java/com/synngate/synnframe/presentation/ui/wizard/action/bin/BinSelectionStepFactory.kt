package com.synngate.synnframe.presentation.ui.wizard.action.bin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.BarcodeEntryField
import com.synngate.synnframe.presentation.ui.wizard.action.components.BinCard
import com.synngate.synnframe.presentation.ui.wizard.action.components.PlanBinsList
import com.synngate.synnframe.presentation.ui.wizard.action.components.StepContainer
import com.synngate.synnframe.presentation.ui.wizard.service.BinLookupService
import timber.log.Timber

/**
 * Фабрика для шага выбора ячейки
 */
class BinSelectionStepFactory(
    private val binLookupService: BinLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<BinX>() {

    /**
     * Создание ViewModel для шага
     */
    override fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ): BaseStepViewModel<BinX> {
        return BinSelectionViewModel(
            step = step,
            action = action,
            context = context,
            binLookupService = binLookupService,
            validationService = validationService
        )
    }

    /**
     * Отображение UI для шага
     */
    @Composable
    override fun StepContent(
        state: StepViewState<BinX>,
        viewModel: BaseStepViewModel<BinX>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Безопасное приведение ViewModel к конкретному типу
        val binViewModel = try {
            viewModel as BinSelectionViewModel
        } catch (e: ClassCastException) {
            Timber.e(e, "Ошибка приведения ViewModel к BinSelectionViewModel")
            null
        }

        // Если приведение не удалось, показываем сообщение об ошибке
        if (binViewModel == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ошибка инициализации шага. Пожалуйста, вернитесь назад и попробуйте снова.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return
        }

        // Обработка штрих-кода из контекста, если он есть
        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                binViewModel.processBarcode(barcode)
            }
        }

        // Диалог сканирования
        if (binViewModel.showCameraScannerDialog) {
            val plannedBin = action.placementBin
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    binViewModel.processBarcode(barcode)
                    binViewModel.hideCameraScannerDialog()
                },
                onClose = {
                    binViewModel.hideCameraScannerDialog()
                },
                instructionText = if (plannedBin != null)
                    stringResource(R.string.scan_bin_expected, plannedBin.code)
                else
                    stringResource(R.string.scan_bin),
                expectedBarcode = plannedBin?.code
            )
        }

        StepContainer(
            state = state,
            step = step,
            action = action,
            onBack = { context.onBack() },
            onForward = {
                // Безопасно выполняем действие завершения
                binViewModel.getSelectedBin()?.let { bin ->
                    binViewModel.completeStep(bin)
                }
            },
            onCancel = { context.onCancel() },
            forwardEnabled = binViewModel.hasSelectedBin(),
            isProcessingGlobal = context.isProcessingStep,
            isFirstStep = context.isFirstStep,  // Передаем флаг первого шага
            content = {
                SafeBinSelectionContent(
                    state = state,
                    viewModel = binViewModel
                )
            }
        )
    }

    @Composable
    private fun SafeBinSelectionContent(
        state: StepViewState<BinX>,
        viewModel: BinSelectionViewModel
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val selectedBin = viewModel.getSelectedBin()
            val selectedBinCode = selectedBin?.code

            if (viewModel.hasPlanBins()) {
                PlanBinsList(
                    planBins = viewModel.getPlanBins(),
                    onBinSelect = { bin ->
                        viewModel.selectBin(bin)
                    },
                    selectedBinCode = selectedBinCode,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // ИЗМЕНЕНИЕ: Скрываем поле ввода, когда выбрана запланированная ячейка
            if (!viewModel.hasPlanBins() ||
                !viewModel.isSelectedBinMatchingPlan() ||
                viewModel.getSelectedBin() == null) {

                BarcodeEntryField(
                    value = viewModel.binCodeInput,
                    onValueChange = { viewModel.updateBinCodeInput(it) },
                    onSearch = { viewModel.searchByBinCode() },
                    onScannerClick = { viewModel.toggleCameraScannerDialog(true) },
                    isError = state.error != null,
                    errorText = state.error,
                    label = stringResource(R.string.enter_bin_code),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Если выбранная ячейка не из плана, показываем ее
            if (selectedBin != null && !viewModel.isSelectedBinMatchingPlan()) {
                Spacer(modifier = Modifier.height(8.dp))

                BinCard(
                    bin = selectedBin,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is BinX
    }
}