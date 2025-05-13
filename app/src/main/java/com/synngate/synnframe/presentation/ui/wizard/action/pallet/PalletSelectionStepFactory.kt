package com.synngate.synnframe.presentation.ui.wizard.action.pallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.BarcodeEntryField
import com.synngate.synnframe.presentation.ui.wizard.action.components.PalletCard
import com.synngate.synnframe.presentation.ui.wizard.action.components.PlanPalletsList
import com.synngate.synnframe.presentation.ui.wizard.action.components.StepContainer
import com.synngate.synnframe.presentation.ui.wizard.service.PalletLookupService
import timber.log.Timber

class PalletSelectionStepFactory(
    private val palletLookupService: PalletLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<Pallet>(), AutoCompleteCapableFactory {

    override fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ): BaseStepViewModel<Pallet> {
        return PalletSelectionViewModel(
            step = step,
            action = action,
            context = context,
            palletLookupService = palletLookupService,
            validationService = validationService
        )
    }

    @Composable
    override fun StepContent(
        state: StepViewState<Pallet>,
        viewModel: BaseStepViewModel<Pallet>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        val palletViewModel = try {
            viewModel as PalletSelectionViewModel
        } catch (e: ClassCastException) {
            Timber.e(e, "Ошибка приведения ViewModel к PalletSelectionViewModel")
            null
        }

        if (palletViewModel == null) {
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

        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                palletViewModel.processBarcode(barcode)
            }
        }

        if (palletViewModel.showCameraScannerDialog) {
            val isStorageStep = palletViewModel.isStoragePalletStep()
            val plannedPallet = if (isStorageStep) action.storagePallet else action.placementPallet

            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    palletViewModel.processBarcode(barcode)
                    palletViewModel.hideCameraScannerDialog()
                },
                onClose = {
                    palletViewModel.hideCameraScannerDialog()
                },
                instructionText = if (plannedPallet != null)
                    stringResource(R.string.scan_pallet_expected, plannedPallet.code)
                else
                    stringResource(R.string.scan_pallet),
                expectedBarcode = plannedPallet?.code
            )
        }

        StepContainer(
            state = state,
            step = step,
            action = action,
            onBack = { context.onBack() },
            onForward = {
                palletViewModel.getSelectedPallet()?.let { pallet ->
                    palletViewModel.completeStep(pallet)
                }
            },
            onCancel = { context.onCancel() },
            forwardEnabled = palletViewModel.hasSelectedPallet(),
            isProcessingGlobal = context.isProcessingStep,
            isFirstStep = context.isFirstStep,
            content = {
                SafePalletSelectionContent(
                    state = state,
                    viewModel = palletViewModel,
                    step = step
                )
            }
        )
    }

    @Composable
    private fun SafePalletSelectionContent(
        state: StepViewState<Pallet>,
        viewModel: PalletSelectionViewModel,
        step: ActionStep
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val selectedPallet = viewModel.getSelectedPallet()
            val selectedPalletCode = selectedPallet?.code

            if (viewModel.hasPlanPallets()) {
                PlanPalletsList(
                    planPallets = viewModel.getPlanPallets(),
                    onPalletSelect = { pallet ->
                        viewModel.selectPallet(pallet)
                    },
                    selectedPalletCode = selectedPalletCode, // Передаем код для подсветки
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // ИЗМЕНЕНИЕ: Скрываем поле ввода, когда выбрана запланированная паллета
            if (!viewModel.hasPlanPallets() ||
                !viewModel.isSelectedPalletMatchingPlan() ||
                viewModel.getSelectedPallet() == null) {

                BarcodeEntryField(
                    value = viewModel.palletCodeInput,
                    onValueChange = { viewModel.updatePalletCodeInput(it) },
                    onSearch = { viewModel.searchByPalletCode() },
                    onScannerClick = { viewModel.toggleCameraScannerDialog(true) },
                    isError = state.error != null,
                    errorText = state.error,
                    label = stringResource(R.string.enter_pallet_code),
                    modifier = Modifier.fillMaxWidth()
                )

                // Кнопка создания новой паллеты - показываем только если:
                // - нет строгого требования использовать паллету из плана
                // - либо план пуст
                if (!step.validationRules.rules.any { it.type == ValidationType.FROM_PLAN } || !viewModel.hasPlanPallets()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.createNewPallet() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !viewModel.isCreatingPallet && !state.isLoading
                    ) {
                        if (viewModel.isCreatingPallet) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .height(20.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        Text("Создать новую паллету")
                    }
                }
            }

            // Если выбранная паллета не из плана, показываем ее
            if (selectedPallet != null && !viewModel.isSelectedPalletMatchingPlan()) {
                Spacer(modifier = Modifier.height(8.dp))

                PalletCard(
                    pallet = selectedPallet,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is Pallet
    }

    override fun getAutoCompleteFieldName(step: ActionStep): String? {
        return "selectedPallet" // автопереход при выборе паллеты
    }

    override fun isAutoCompleteEnabled(step: ActionStep): Boolean {
        // Включаем автопереход для шагов выбора паллеты из плана
        return step.promptText.contains("план", ignoreCase = true)
    }

    override fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean {
        // Паллета не требует подтверждения
        return false
    }
}