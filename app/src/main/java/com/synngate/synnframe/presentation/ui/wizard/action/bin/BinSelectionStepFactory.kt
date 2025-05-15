package com.synngate.synnframe.presentation.ui.wizard.action.bin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.FormSpacer
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.BinCard
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardStepUtils
import com.synngate.synnframe.presentation.ui.wizard.service.BinLookupService

class BinSelectionStepFactory(
    private val binLookupService: BinLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<BinX>(), AutoCompleteCapableFactory {

    override fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext,
        factory: ActionStepFactory
    ): BaseStepViewModel<BinX> {
        return BinSelectionViewModel(
            step = step,
            action = action,
            context = context,
            binLookupService = binLookupService,
            validationService = validationService,
            stepFactory = factory
        )
    }

    @Composable
    override fun StepContent(
        state: StepViewState<BinX>,
        viewModel: BaseStepViewModel<BinX>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        val binViewModel = viewModel as? BinSelectionViewModel

        if (binViewModel == null) {
            WizardStepUtils.ViewModelErrorScreen()
            return
        }

        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                binViewModel.processBarcode(barcode)
            }
        }

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

        WizardStepUtils.StandardStepContainer(
            state = state,
            step = step,
            action = action,
            viewModel = binViewModel,
            context = context,
            forwardEnabled = binViewModel.hasSelectedBin(),
            content = {
                BinSelectionContent(
                    state = state,
                    viewModel = binViewModel
                )
            }
        )
    }

    @Composable
    private fun BinSelectionContent(
        state: StepViewState<BinX>,
        viewModel: BinSelectionViewModel
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val selectedBin = viewModel.getSelectedBin()
            val selectedBinCode = selectedBin?.code

            if (viewModel.hasPlanBins()) {
                WizardStepUtils.BinList(
                    bins = viewModel.getPlanBins(),
                    onBinSelect = { bin ->
                        viewModel.selectBin(bin)
                    },
                    selectedBinCode = selectedBinCode,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(8)
            }

            if (!viewModel.hasPlanBins() ||
                !viewModel.isSelectedBinMatchingPlan() ||
                viewModel.getSelectedBin() == null) {

                WizardStepUtils.StandardBarcodeField(
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

            if (selectedBin != null && !viewModel.isSelectedBinMatchingPlan()) {
                FormSpacer(8)

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

    override fun getAutoCompleteFieldName(step: ActionStep): String? {
        return "selectedBin"
    }

    override fun isAutoCompleteEnabled(step: ActionStep): Boolean {
        return true
    }

    override fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean {
        return false
    }
}