package com.synngate.synnframe.presentation.ui.wizard.action.quantity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
import com.synngate.synnframe.presentation.ui.wizard.action.components.WizardQuantityInput
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.ProductCard
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.TaskProductCard
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardStepUtils
import timber.log.Timber

/**
 * Обновленная фабрика для шага ввода количества продукта
 */
class ProductQuantityStepFactory(
    private val validationService: ValidationService
) : BaseActionStepFactory<TaskProduct>() {

    /**
     * Создает ViewModel для шага
     */
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
        val quantityViewModel = viewModel as? ProductQuantityViewModel

        if (quantityViewModel == null) {
            WizardStepUtils.ViewModelErrorScreen()
            return
        }

        // Отладочная информация
        LaunchedEffect(Unit) {
            Timber.d("ProductQuantityStepFactory.StepContent: hasSelectedProduct=${quantityViewModel.hasSelectedProduct()}")
            Timber.d("Context results: ${context.results.entries.joinToString { "${it.key} -> ${it.value?.javaClass?.simpleName}" }}")
        }

        // Проверяем, что товар выбран
        if (!quantityViewModel.hasSelectedProduct()) {
            WizardStepUtils.ViewModelErrorScreen(message =
            "Ошибка: Товар не обнаружен. Вернитесь назад и повторно выберите товар."
            )
            return
        }

        // Используем стандартный контейнер для шага
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
        Column(modifier = Modifier.fillMaxWidth()) {
            // Получаем данные продукта для отображения
            val product = viewModel.getSelectedProduct()
            val taskProduct = viewModel.getSelectedTaskProduct()

            if (product != null) {
                // Отображаем информацию о товаре
                if (taskProduct != null) {
                    TaskProductCard(
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

                // Отображаем показатели количеств
                QuantityIndicators(viewModel)

                FormSpacer(8)

                // Поле ввода количества
                WizardQuantityInput(
                    value = viewModel.quantityInput,
                    onValueChange = { viewModel.updateQuantityInput(it) },
                    onIncrement = { viewModel.incrementQuantity() },
                    onDecrement = { viewModel.decrementQuantity() },
                    onClear = { viewModel.clearQuantity() },
                    isError = state.error != null,
                    errorText = state.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    label = ""  // Убираем метку, так как она не нужна
                )

                // Предупреждение о превышении планового количества
                if (viewModel.willExceedPlan) {
                    FormSpacer(8)

                    WarningMessage(
                        message = "Внимание: превышение планового количества!"
                    )
                }
            }
        }
    }

    /**
     * Компонент для отображения показателей количеств
     */
    @Composable
    private fun QuantityIndicators(viewModel: ProductQuantityViewModel) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Строка с плановым количеством
            QuantityRow(
                label = "План:",
                value = formatQuantityForDisplay(viewModel.plannedQuantity)
            )

            FormSpacer(4)

            // Строка с выполненным количеством
            QuantityRow(
                label = "Выполнено:",
                value = formatQuantityForDisplay(viewModel.completedQuantity)
            )

            FormSpacer(4)

            // Строка с вводимым количеством
            QuantityRow(
                label = "Текущее:",
                value = formatQuantityForDisplay(viewModel.currentInputQuantity)
            )

            FormSpacer(4)

            // Строка с прогнозируемым итогом
            QuantityRow(
                label = "Итого будет:",
                value = formatQuantityForDisplay(viewModel.projectedTotalQuantity),
                isHighlighted = true,
                isWarning = viewModel.willExceedPlan
            )

            FormSpacer(4)

            // Строка с оставшимся количеством
            QuantityRow(
                label = "Осталось:",
                value = formatQuantityForDisplay(viewModel.remainingQuantity),
                isWarning = viewModel.willExceedPlan
            )
        }
    }

    /**
     * Компонент для отображения строки с меткой и значением количества
     */
    @Composable
    private fun QuantityRow(
        label: String,
        value: String,
        isHighlighted: Boolean = false,
        isWarning: Boolean = false
    ) {
        com.synngate.synnframe.presentation.ui.wizard.action.components.QuantityRow(
            label = label,
            value = value,
            highlight = isHighlighted,
            warning = isWarning
        )
    }

    /**
     * Компонент для отображения предупреждения
     */
    @Composable
    private fun WarningMessage(message: String) {
        com.synngate.synnframe.presentation.ui.wizard.action.components.WizardStateMessage(
            message = message,
            type = com.synngate.synnframe.presentation.ui.wizard.action.components.StateType.WARNING
        )
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        // Валидация результата шага
        val taskProduct = value as? TaskProduct
        return taskProduct != null && taskProduct.quantity > 0
    }

    /**
     * Форматирует количество для отображения
     */
    private fun formatQuantityForDisplay(value: Float): String {
        return com.synngate.synnframe.presentation.ui.wizard.action.components.formatQuantityDisplay(value)
    }
}