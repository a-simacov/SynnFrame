package com.synngate.synnframe.presentation.ui.wizard.action.taskproduct

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.common.dialog.OptimizedProductSelectionDialog
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.BarcodeEntryField
import com.synngate.synnframe.presentation.ui.wizard.action.components.ExpirationDatePicker
import com.synngate.synnframe.presentation.ui.wizard.action.components.PlanProductsList
import com.synngate.synnframe.presentation.ui.wizard.action.components.ProductCard
import com.synngate.synnframe.presentation.ui.wizard.action.components.ProductStatusSelector
import com.synngate.synnframe.presentation.ui.wizard.action.components.StepContainer
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService

/**
 * Фабрика для шага выбора TaskProduct (товара с учетными характеристиками)
 */
class TaskProductSelectionStepFactory(
    private val productLookupService: ProductLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<TaskProduct>() {

    /**
     * Создание ViewModel для шага
     */
    override fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ): BaseStepViewModel<TaskProduct> {
        return TaskProductSelectionViewModel(
            step = step,
            action = action,
            context = context,
            productLookupService = productLookupService,
            validationService = validationService
        )
    }

    /**
     * Отображение UI для шага
     */
    @Composable
    override fun StepContent(
        state: StepViewState<TaskProduct>,
        viewModel: BaseStepViewModel<TaskProduct>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Приводим базовый ViewModel к конкретному типу
        val taskProductViewModel = viewModel as TaskProductSelectionViewModel

        // Обработка штрих-кода из контекста, если он есть
        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                taskProductViewModel.processBarcode(barcode)
            }
        }

        // Диалог сканирования
        if (taskProductViewModel.showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    taskProductViewModel.processBarcode(barcode)
                    taskProductViewModel.hideCameraScannerDialog()
                },
                onClose = {
                    taskProductViewModel.hideCameraScannerDialog()
                },
                instructionText = stringResource(R.string.scan_product)
            )
        }

        // Диалог выбора продукта
        if (taskProductViewModel.showProductSelectionDialog) {
            OptimizedProductSelectionDialog(
                onProductSelected = { product ->
                    taskProductViewModel.setSelectedProduct(product)
                    taskProductViewModel.hideProductSelectionDialog()
                },
                onDismiss = {
                    taskProductViewModel.hideProductSelectionDialog()
                },
                initialFilter = "",
                title = "Выберите товар",
                planProductIds = if (taskProductViewModel.hasPlanProducts()) {
                    taskProductViewModel.getPlanProducts().map { it.product.id }.toSet()
                } else null
            )
        }

        // Используем StepContainer для унифицированного отображения шага
        StepContainer(
            state = state,
            step = step,
            action = action,
            onBack = { context.onBack() },
            onForward = {
                taskProductViewModel.saveResult()
            },
            onCancel = { context.onCancel() },
            forwardEnabled = taskProductViewModel.isFormValid(),
            content = {
                TaskProductSelectionContent(
                    state = state,
                    viewModel = taskProductViewModel
                )
            }
        )
    }

    /**
     * Содержимое шага выбора продукта задания
     */
    @Composable
    private fun TaskProductSelectionContent(
        state: StepViewState<TaskProduct>,
        viewModel: TaskProductSelectionViewModel
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Поле ввода штрих-кода
            BarcodeEntryField(
                value = viewModel.productCodeInput,
                onValueChange = { viewModel.updateProductCodeInput(it) },
                onSearch = { viewModel.searchByProductCode() },
                onScannerClick = { viewModel.toggleCameraScannerDialog(true) },
                isError = state.error != null,
                errorText = state.error,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Отображение выбранного продукта
            if (state.data != null) {
                Text(
                    text = "Выбранный товар:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ProductCard(
                    product = state.data.product,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Селектор статуса продукта
                ProductStatusSelector(
                    selectedStatus = viewModel.selectedStatus,
                    onStatusSelected = { viewModel.setSelectedStatus(it) },
                    modifier = Modifier.fillMaxWidth(),
                    isEnabled = !viewModel.isExpirationDateExpired()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Датапикер для срока годности (только для продуктов с учетом по партиям)
                if (state.data.product.accountingModel == AccountingModel.BATCH) {
                    ExpirationDatePicker(
                        expirationDate = viewModel.expirationDate,
                        onDateSelected = { viewModel.setExpirationDate(it) },
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Отображение продуктов из плана
            if (viewModel.hasPlanProducts()) {
                PlanProductsList(
                    planProducts = viewModel.getPlanProducts(),
                    onProductSelect = { taskProduct ->
                        viewModel.selectTaskProductFromPlan(taskProduct)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Кнопка выбора из списка
            Button(
                onClick = { viewModel.toggleProductSelectionDialog(true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Выбрать из списка товаров")
            }
        }
    }

    /**
     * Валидация результата шага
     */
    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        // Проверка типа
        if (value !is TaskProduct) return false

        // Проверяем наличие продукта
        if (value.product == null) return false

        // Проверяем наличие срока годности для товаров с учетом по партиям
        if (value.product.accountingModel == AccountingModel.BATCH && !value.hasExpirationDate()) {
            return false
        }

        return true
    }
}