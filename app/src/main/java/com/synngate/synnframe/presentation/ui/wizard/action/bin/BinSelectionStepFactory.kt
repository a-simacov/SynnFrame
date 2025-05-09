package com.synngate.synnframe.presentation.ui.wizard.action.bin

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
import com.synngate.synnframe.domain.entity.taskx.BinX
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

        // Используем StepContainer для унифицированного отображения шага
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
                    viewModel = binViewModel,
                    step = step
                )
            }
        )
    }

    /**
     * Безопасное содержимое шага выбора ячейки
     */
    @Composable
    private fun SafeBinSelectionContent(
        state: StepViewState<BinX>,
        viewModel: BinSelectionViewModel,
        step: ActionStep
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Поле ввода кода ячейки
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

            Spacer(modifier = Modifier.height(16.dp))

            // Отображение выбранной ячейки
            val selectedBin = viewModel.getSelectedBin()
            if (selectedBin != null) {
                Text(
                    text = "Выбранная ячейка:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                BinCard(
                    bin = selectedBin,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Отображение ячеек из плана
            if (viewModel.hasPlanBins()) {
                PlanBinsList(
                    planBins = viewModel.getPlanBins(),
                    onBinSelect = { bin ->
                        viewModel.selectBin(bin)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Кнопка выбора из списка ячеек
            // Показываем только если нет ограничения по плану или нет запланированной ячейки
            val plannedBin = viewModel.getPlanBins().firstOrNull()
            if (plannedBin == null || !step.validationRules.rules.any { it.type == ValidationType.FROM_PLAN }) {
                Button(
                    onClick = { viewModel.toggleBinsList(!viewModel.showBinsList) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(if (viewModel.showBinsList) "Скрыть список" else "Выбрать из списка")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Список ячеек для выбора
            if (viewModel.showBinsList) {
                // Поле поиска
                BarcodeEntryField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { viewModel.filterBins() },
                    onScannerClick = { viewModel.toggleCameraScannerDialog(true) },
                    label = stringResource(R.string.search_bins),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Список отфильтрованных ячеек
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(viewModel.filteredBins) { bin ->
                        BinCard(
                            bin = bin,
                            onClick = { viewModel.selectBin(bin) },
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
        return value is BinX
    }
}