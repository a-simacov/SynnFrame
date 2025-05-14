package com.synngate.synnframe.presentation.ui.wizard.action.quantity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.FormSpacer
import com.synngate.synnframe.presentation.ui.wizard.action.components.QuantityColumn
import com.synngate.synnframe.presentation.ui.wizard.action.components.StateType
import com.synngate.synnframe.presentation.ui.wizard.action.components.WizardQuantityInput
import com.synngate.synnframe.presentation.ui.wizard.action.components.WizardStateMessage
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.ProductCard
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.TaskProductCardShort
import com.synngate.synnframe.presentation.ui.wizard.action.components.formatQuantityDisplay
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardStepUtils
import kotlinx.coroutines.delay
import timber.log.Timber

class ProductQuantityStepFactory(
    private val validationService: ValidationService
) : BaseActionStepFactory<TaskProduct>() {

    override fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext,
        factory: ActionStepFactory
    ): BaseStepViewModel<TaskProduct> {
        return ProductQuantityViewModel(
            step = step,
            action = action,
            context = context,
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
        val quantityViewModel = viewModel as? ProductQuantityViewModel

        if (quantityViewModel == null) {
            WizardStepUtils.ViewModelErrorScreen()
            return
        }

        if (!quantityViewModel.hasSelectedProduct()) {
            WizardStepUtils.ViewModelErrorScreen(
                message =
                "Ошибка: Товар не обнаружен. Вернитесь назад и повторно выберите товар."
            )
            return
        }

        WizardStepUtils.StandardStepContainer(
            state = state,
            step = step,
            action = action,
            viewModel = quantityViewModel,
            context = context,
            forwardEnabled = quantityViewModel.quantityInput.let { input ->
                val floatValue = input.toFloatOrNull() ?: 0f
                floatValue > 0f
            },
            content = {
                ProductQuantityContent(
                    state = state,
                    viewModel = quantityViewModel
                )
            }
        )
    }

    @Composable
    private fun ProductQuantityContent(
        state: StepViewState<TaskProduct>,
        viewModel: ProductQuantityViewModel
    ) {
        // Создаем FocusRequester для управления фокусом
        val focusRequester = remember { FocusRequester() }
        var inputValue by remember { mutableStateOf(viewModel.quantityInput) } // default will be viewModel.quantityInput

        // Используем LaunchedEffect для установки фокуса при появлении компонента
        LaunchedEffect(Unit) {
            try {
                // Небольшая задержка для гарантии, что поле уже отрисовано
                delay(100)
                Timber.d("Запрашиваем фокус для поля ввода количества")
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при установке фокуса на поле ввода количества")
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            val product = viewModel.getSelectedProduct()
            val taskProduct = viewModel.getSelectedTaskProduct()

            if (product != null) {
                if (taskProduct != null) {
                    TaskProductCardShort(
                        taskProduct = taskProduct,
                        isSelected = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ProductCard(
                        product = product,
                        isSelected = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                FormSpacer(8)

                QuantityIndicators(viewModel)

                FormSpacer(8)

                WizardQuantityInput(
                    value = inputValue,
                    onValueChange = { newValue ->
                        inputValue = newValue
                        viewModel.updateQuantityInput(inputValue)
                    },
                    onIncrement = {
                        viewModel.incrementQuantity()
                        inputValue = viewModel.quantityInput
                    },
                    onDecrement = {
                        viewModel.decrementQuantity()
                        inputValue = viewModel.quantityInput
                    },
                    isError = state.error != null,
                    errorText = state.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    label = "",
                    focusRequester = focusRequester
                )

                if (viewModel.willExceedPlan) {
                    FormSpacer(8)

                    WarningMessage(
                        message = "Внимание: превышение планового количества!"
                    )
                }
            }
        }
    }

    @Composable
    private fun QuantityIndicators(viewModel: ProductQuantityViewModel) {
        val color = if (viewModel.willExceedPlan)
            MaterialTheme.colorScheme.error
        else if (viewModel.plannedQuantity == viewModel.projectedTotalQuantity)
            Color.Green
        else
            Color.Blue

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            QuantityColumn(
                label = "план",
                valueLarge = formatQuantityDisplay(viewModel.plannedQuantity),
                valueSmall = formatQuantityDisplay(viewModel.completedQuantity),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(32.dp))
            QuantityColumn(
                label = "будет",
                valueLarge = formatQuantityDisplay(viewModel.projectedTotalQuantity),
                valueSmall = formatQuantityDisplay(viewModel.remainingQuantity),
                color = color
            )
        }
    }

    @Composable
    private fun WarningMessage(message: String) {
        WizardStateMessage(
            message = message,
            type = StateType.WARNING
        )
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        val taskProduct = value as? TaskProduct
        return taskProduct != null && taskProduct.quantity > 0
    }
}