package com.synngate.synnframe.presentation.ui.wizard.action.pallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.BarcodeEntryField
import com.synngate.synnframe.presentation.ui.wizard.action.components.PalletCard
import com.synngate.synnframe.presentation.ui.wizard.action.components.PlanPalletsList
import com.synngate.synnframe.presentation.ui.wizard.action.components.StepContainer
import com.synngate.synnframe.presentation.ui.wizard.service.PalletLookupService
import timber.log.Timber

/**
 * Фабрика для шага выбора паллеты
 */
class PalletSelectionStepFactory(
    private val palletLookupService: PalletLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<Pallet>() {

    /**
     * Создание ViewModel для шага
     */
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

    /**
     * Отображение UI для шага
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
        val palletViewModel = try {
            viewModel as PalletSelectionViewModel
        } catch (e: ClassCastException) {
            Timber.e(e, "Ошибка приведения ViewModel к PalletSelectionViewModel")
            null
        }

        // Если приведение не удалось, показываем сообщение об ошибке
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

        // Обработка штрих-кода из контекста, если он есть
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

        // Используем StepContainer для унифицированного отображения шага
        StepContainer(
            state = state,
            step = step,
            action = action,
            onBack = { context.onBack() },
            onForward = {
                // Безопасно используем выбранную паллету
                palletViewModel.getSelectedPallet()?.let { pallet ->
                    palletViewModel.completeStep(pallet)
                }
            },
            onCancel = { context.onCancel() },
            forwardEnabled = palletViewModel.hasSelectedPallet(),
            isProcessingGlobal = context.isProcessingStep,
            isFirstStep = context.isFirstStep,  // Передаем флаг первого шага
            content = {
                SafePalletSelectionContent(
                    state = state,
                    viewModel = palletViewModel,
                    step = step
                )
            }
        )
    }

    /**
     * Безопасное содержимое шага выбора паллеты
     */
    @Composable
    private fun SafePalletSelectionContent(
        state: StepViewState<Pallet>,
        viewModel: PalletSelectionViewModel,
        step: ActionStep
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Поле ввода кода паллеты
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

            Spacer(modifier = Modifier.height(16.dp))

            // Отображение выбранной паллеты
            val selectedPallet = viewModel.getSelectedPallet()
            if (selectedPallet != null) {
                Text(
                    text = "Выбранная паллета:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                PalletCard(
                    pallet = selectedPallet,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Отображение паллет из плана
            if (viewModel.hasPlanPallets()) {
                PlanPalletsList(
                    planPallets = viewModel.getPlanPallets(),
                    onPalletSelect = { selectedPallet ->
                        viewModel.selectPallet(selectedPallet)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Кнопка создания новой паллеты
            // Показываем, если нет жесткого требования использовать паллету из плана
            if (!step.validationRules.rules.any { it.type == ValidationType.FROM_PLAN }) {
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

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Кнопка выбора из списка паллет
            // Показываем только если нет ограничения по плану или нет запланированной паллеты
            val plannedPallet = viewModel.getPlanPallets().firstOrNull()
            if (plannedPallet == null || !step.validationRules.rules.any { it.type == ValidationType.FROM_PLAN }) {
                Button(
                    onClick = { viewModel.togglePalletsList(!viewModel.showPalletsList) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(if (viewModel.showPalletsList) "Скрыть список" else "Выбрать из списка")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Список паллет для выбора
            if (viewModel.showPalletsList) {
                // Поле поиска
                BarcodeEntryField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { viewModel.filterPallets() },
                    onScannerClick = { viewModel.toggleCameraScannerDialog(true) },
                    label = stringResource(R.string.search_pallets),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Список отфильтрованных паллет
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(viewModel.filteredPallets) { pallet ->
                        PalletCard(
                            pallet = pallet,
                            onClick = { viewModel.selectPallet(pallet) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    /**
     * Валидация результата шага
     */
    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is Pallet
    }
}