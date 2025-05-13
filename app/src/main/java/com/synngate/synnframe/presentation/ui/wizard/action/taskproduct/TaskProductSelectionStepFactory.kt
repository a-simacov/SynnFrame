package com.synngate.synnframe.presentation.ui.wizard.action.taskproduct

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.common.dialog.OptimizedProductSelectionDialog
import com.synngate.synnframe.presentation.common.inputs.ExpirationDatePicker
import com.synngate.synnframe.presentation.common.inputs.ProductStatusSelector
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.FormSpacer
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.ProductCard
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardStepUtils
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService

/**
 * Обновленная фабрика для шага выбора TaskProduct (товара с учетными характеристиками)
 */
class TaskProductSelectionStepFactory(
    private val productLookupService: ProductLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<TaskProduct>(), AutoCompleteCapableFactory {

    /**
     * Создает ViewModel для шага
     */
    override fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext,
        factory: ActionStepFactory
    ): BaseStepViewModel<TaskProduct> {
        return TaskProductSelectionViewModel(
            step = step,
            action = action,
            context = context,
            productLookupService = productLookupService,
            validationService = validationService,
        )
    }

    /**
     * Отображает UI для шага
     */
    @Composable
    override fun StepContent(
        state: StepViewState<TaskProduct>,
        viewModel: BaseStepViewModel<TaskProduct>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Безопасное приведение ViewModel к конкретному типу
        val taskProductViewModel = viewModel as? TaskProductSelectionViewModel

        if (taskProductViewModel == null) {
            WizardStepUtils.ViewModelErrorScreen()
            return
        }

        // Обработка штрих-кода из контекста
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
                    taskProductViewModel.planProductIds
                } else null
            )
        }

        // Используем стандартный контейнер для шага
        WizardStepUtils.StandardStepContainer(
            state = state,
            step = step,
            action = action,
            context = context,
            forwardEnabled = taskProductViewModel.isFormValid(),
            content = {
                TaskProductSelectionContent(
                    state = state,
                    viewModel = taskProductViewModel
                )
            }
        )
    }

    @Composable
    private fun TaskProductSelectionContent(
        state: StepViewState<TaskProduct>,
        viewModel: TaskProductSelectionViewModel
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Получаем ID выбранного продукта для подсветки в списке
            val selectedProductId = viewModel.getSelectedProduct()?.id

            // Отображаем товары из плана, подсвечивая выбранный
            if (viewModel.hasPlanProducts()) {
                WizardStepUtils.ProductSelectionList(
                    products = viewModel.planProducts,
                    onProductSelect = { selectedTaskProduct ->
                        viewModel.selectTaskProductFromPlan(selectedTaskProduct)
                    },
                    selectedProductId = selectedProductId,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(8)
            }

            // Показываем поле поиска только если нет плана
            // или выбранный товар не соответствует плану, или товар еще не выбран
            if (!viewModel.hasPlanProducts() ||
                !viewModel.isSelectedProductMatchingPlan() ||
                viewModel.getSelectedProduct() == null) {

                // Используем стандартное поле ввода штрих-кода
                WizardStepUtils.StandardBarcodeField(
                    value = viewModel.productCodeInput,
                    onValueChange = { viewModel.updateProductCodeInput(it) },
                    onSearch = { viewModel.searchByProductCode() },
                    onScannerClick = { viewModel.toggleCameraScannerDialog(true) },
                    onSelectFromList = { viewModel.toggleProductSelectionDialog(true) },
                    isError = state.error != null,
                    errorText = state.error,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(8)
            }

            // Отображаем выбранный товар и дополнительные поля
            val selectedProduct = viewModel.getSelectedProduct()

            if (selectedProduct != null) {
                // Показываем карточку товара, только если он не из плана
                if (!viewModel.isSelectedProductMatchingPlan()) {
                    ProductCard(
                        product = selectedProduct,
                        isSelected = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormSpacer(8)
                }

                // Селектор статуса продукта
                ProductStatusSelector(
                    selectedStatus = viewModel.selectedStatus,
                    onStatusSelected = { viewModel.setSelectedStatus(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(8)

                // Поле для выбора срока годности (только для товаров с партионным учетом)
                if (selectedProduct.accountingModel == AccountingModel.BATCH) {
                    ExpirationDatePicker(
                        expirationDate = viewModel.expirationDate,
                        onDateSelected = { viewModel.setExpirationDate(it) },
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        if (value !is TaskProduct) return false

        if (value.product.accountingModel == AccountingModel.BATCH && !value.hasExpirationDate()) {
            return false
        }

        return true
    }

    // Реализация интерфейса AutoCompleteCapableFactory

    override fun getAutoCompleteFieldName(step: ActionStep): String? {
        return "selectedTaskProduct" // Автопереход при выборе товара
    }

    override fun isAutoCompleteEnabled(step: ActionStep): Boolean {
        // Включаем автопереход только для шагов выбора товара из плана
        // без необходимости указания статуса и срока годности
        return step.promptText.contains("выберите товар", ignoreCase = true) &&
                !step.promptText.contains("укажите статус", ignoreCase = true)
    }

    override fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean {
        // Подтверждение для автозаполненных статусов товара
        return fieldName == "selectedStatus"
    }
}