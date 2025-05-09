package com.synngate.synnframe.presentation.ui.wizard.action.quantity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.ProductCard
import com.synngate.synnframe.presentation.ui.wizard.action.components.QuantityTextField
import com.synngate.synnframe.presentation.ui.wizard.action.components.StepContainer
import timber.log.Timber

/**
 * Фабрика для шага ввода количества продукта
 */
class ProductQuantityStepFactory(
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
        return ProductQuantityViewModel(
            step = step,
            action = action,
            context = context,
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
        // Безопасное приведение ViewModel к конкретному типу
        val quantityViewModel = try {
            viewModel as ProductQuantityViewModel
        } catch (e: ClassCastException) {
            Timber.e(e, "Ошибка приведения ViewModel к ProductQuantityViewModel")
            null
        }

        // Если приведение не удалось, показываем сообщение об ошибке
        if (quantityViewModel == null) {
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

        // Проверяем, выбран ли продукт
        if (!quantityViewModel.hasSelectedProduct()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Сначала необходимо выбрать товар",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        // Используем StepContainer для унифицированного отображения шага
        StepContainer(
            state = state,
            step = step,
            action = action,
            onBack = { context.onBack() },
            onForward = {
                quantityViewModel.saveResult()
            },
            onCancel = { context.onCancel() },
            forwardEnabled = state.data != null && quantityViewModel.currentInputQuantity > 0,
            isProcessingGlobal = context.isProcessingStep,
            isFirstStep = context.isFirstStep,  // Передаем флаг первого шага
            content = {
                // Используем безопасную версию содержимого
                SafeProductQuantityContent(
                    state = state,
                    viewModel = quantityViewModel
                )
            }
        )
    }

    /**
     * Безопасная версия содержимого шага ввода количества
     */
    @Composable
    private fun SafeProductQuantityContent(
        state: StepViewState<TaskProduct>,
        viewModel: ProductQuantityViewModel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Используем product из ViewModel, а не из state.data
            val product = viewModel.getSelectedProduct()

            if (product != null) {
                // Отображаем информацию о выбранном продукте
                ProductCard(
                    product = product,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Запланированное количество: значение крупным шрифтом, надпись - маленьким
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${viewModel.plannedQuantity}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "план",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Поле ввода количества с центрированным текстом и увеличенным шрифтом
                QuantityTextFieldCentered(
                    value = viewModel.quantityInput,
                    onValueChange = { viewModel.updateQuantityInput(it) },
                    onIncrement = { viewModel.incrementQuantity() },
                    onDecrement = { viewModel.decrementQuantity() },
                    isError = state.error != null,
                    errorText = state.error,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Строка с двумя значениями: выполненное и прогнозируемое
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Выполненное количество
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${viewModel.completedQuantity}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "выполнено",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Прогнозируемый итог
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${viewModel.projectedTotalQuantity}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "будет",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Оставшееся количество с цветовой индикацией
                val remainingColor = when {
                    // Зеленый, если осталось 0 (план выполнен)
                    viewModel.remainingQuantity <= 0 && !viewModel.willExceedPlan ->
                        MaterialTheme.colorScheme.primary
                    // Красный, если идет превышение плана
                    viewModel.willExceedPlan ->
                        MaterialTheme.colorScheme.error
                    // Синий по умолчанию (план еще не выполнен)
                    else ->
                        MaterialTheme.colorScheme.tertiary
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${viewModel.remainingQuantity}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = remainingColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "осталось",
                        fontSize = 12.sp,
                        color = remainingColor,
                        textAlign = TextAlign.Center
                    )

                    // Показываем предупреждение, если есть превышение плана
                    if (viewModel.willExceedPlan) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Внимание: превышение планового количества!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Показываем сообщение, если продукт не найден
                Text(
                    text = "Продукт не выбран или данные некорректны",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    /**
     * Центрированное поле ввода количества с большим шрифтом
     */
    @Composable
    private fun QuantityTextFieldCentered(
        value: String,
        onValueChange: (String) -> Unit,
        onIncrement: () -> Unit,
        onDecrement: () -> Unit,
        modifier: Modifier = Modifier,
        isError: Boolean = false,
        errorText: String? = null
    ) {
        // Используем модифицированную версию стандартного QuantityTextField
        QuantityTextField(
            value = value,
            onValueChange = onValueChange,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
            label = "",  // Убираем лейбл
            isError = isError,
            errorText = errorText,
            modifier = modifier,
            textAlign = TextAlign.Center,  // Центрируем текст
            fontSize = 24.sp  // Устанавливаем большой размер шрифта
        )
    }

    /**
     * Валидация результата шага
     */
    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        val taskProduct = value as? TaskProduct
        return taskProduct != null && taskProduct.quantity > 0
    }
}