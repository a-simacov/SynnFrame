package com.synngate.synnframe.presentation.ui.wizard.action.pallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.FormSpacer
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.PalletCard
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardStepUtils
import com.synngate.synnframe.presentation.ui.wizard.service.PalletLookupService

/**
 * Обновленная фабрика для шага выбора паллеты
 */
class PalletSelectionStepFactory(
    private val palletLookupService: PalletLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<Pallet>(), AutoCompleteCapableFactory {

    /**
     * Создает ViewModel для шага
     */
    override fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext,
        factory: ActionStepFactory
    ): BaseStepViewModel<Pallet> {
        return PalletSelectionViewModel(
            step = step,
            action = action,
            context = context,
            palletLookupService = palletLookupService,
            validationService = validationService,
            stepFactory = factory
        )
    }

    /**
     * Отображает UI для шага
     */
    @Composable
    override fun StepContent(
        state: StepViewState<Pallet>,
        viewModel: BaseStepViewModel<Pallet>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Безопасное приведение ViewModel к конкретному типу
        val palletViewModel = viewModel as? PalletSelectionViewModel

        if (palletViewModel == null) {
            WizardStepUtils.ViewModelErrorScreen()
            return
        }

        // Обработка штрих-кода из контекста
        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                palletViewModel.processBarcode(barcode)
            }
        }

        // Диалог сканирования
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

        // Используем стандартный контейнер для шага
        WizardStepUtils.StandardStepContainer(
            state = state,
            step = step,
            action = action,
            viewModel = palletViewModel,
            context = context,
            forwardEnabled = palletViewModel.hasSelectedPallet(),
            content = {
                PalletSelectionContent(
                    state = state,
                    viewModel = palletViewModel,
                    step = step
                )
            }
        )
    }

    @Composable
    private fun PalletSelectionContent(
        state: StepViewState<Pallet>,
        viewModel: PalletSelectionViewModel,
        step: ActionStep
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val selectedPallet = viewModel.getSelectedPallet()
            val selectedPalletCode = selectedPallet?.code

            // Отображаем паллеты из плана, если они есть
            if (viewModel.hasPlanPallets()) {
                WizardStepUtils.PalletList(
                    pallets = viewModel.getPlanPallets(),
                    onPalletSelect = { pallet ->
                        viewModel.selectPallet(pallet)
                    },
                    selectedPalletCode = selectedPalletCode,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(8)
            }

            // Скрываем поле ввода, когда выбрана запланированная паллета
            if (!viewModel.hasPlanPallets() ||
                !viewModel.isSelectedPalletMatchingPlan() ||
                viewModel.getSelectedPallet() == null) {

                // Используем стандартное поле ввода штрих-кода
                WizardStepUtils.StandardBarcodeField(
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
                if (!step.validationRules.rules.any { it.type == ValidationType.FROM_PLAN } ||
                    !viewModel.hasPlanPallets()) {

                    FormSpacer(8)

                    Button(
                        onClick = { viewModel.createNewPallet() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !viewModel.isCreatingPallet && !state.isLoading
                    ) {
                        Text("Создать новую паллету")
                    }
                }
            }

            // Если выбранная паллета не из плана, показываем ее
            if (selectedPallet != null && !viewModel.isSelectedPalletMatchingPlan()) {
                FormSpacer(8)

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

    // Реализация интерфейса AutoCompleteCapableFactory

    override fun getAutoCompleteFieldName(step: ActionStep): String? {
        return "selectedPallet" // Автопереход при выборе паллеты
    }

    override fun isAutoCompleteEnabled(step: ActionStep): Boolean {
        // Включаем автопереход для шагов выбора паллеты из плана
        return true
    }

    override fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean {
        // Паллета не требует подтверждения
        return false
    }
}