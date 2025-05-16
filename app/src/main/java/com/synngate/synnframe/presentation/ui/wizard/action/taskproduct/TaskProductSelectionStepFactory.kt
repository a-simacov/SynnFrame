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

class TaskProductSelectionStepFactory(
    private val productLookupService: ProductLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<TaskProduct>(), AutoCompleteCapableFactory {

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

    @Composable
    override fun StepContent(
        state: StepViewState<TaskProduct>,
        viewModel: BaseStepViewModel<TaskProduct>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        val taskProductViewModel = viewModel as? TaskProductSelectionViewModel

        if (taskProductViewModel == null) {
            WizardStepUtils.ViewModelErrorScreen()
            return
        }

        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                taskProductViewModel.processBarcode(barcode)
            }
        }

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

        WizardStepUtils.StandardStepContainer(
            state = state,
            step = step,
            action = action,
            viewModel = taskProductViewModel,
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
            val selectedProductId = viewModel.getSelectedProduct()?.id

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

            if (!viewModel.hasPlanProducts() ||
                !viewModel.isSelectedProductMatchingPlan() ||
                viewModel.getSelectedProduct() == null) {

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

            val selectedProduct = viewModel.getSelectedProduct()

            if (selectedProduct != null) {
                if (!viewModel.isSelectedProductMatchingPlan()) {
                    ProductCard(
                        product = selectedProduct,
                        isSelected = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormSpacer(8)
                }

                ProductStatusSelector(
                    selectedStatus = viewModel.selectedStatus,
                    onStatusSelected = { viewModel.setSelectedStatus(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(8)

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

    override fun getAutoCompleteFieldName(step: ActionStep): String? {
        return "selectedTaskProduct"
    }

    override fun isAutoCompleteEnabled(step: ActionStep): Boolean {
        return true
    }

    override fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean {
        return fieldName == "selectedStatus"
    }
}