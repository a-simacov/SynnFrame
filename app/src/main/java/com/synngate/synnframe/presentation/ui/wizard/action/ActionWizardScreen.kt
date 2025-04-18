package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Экран визарда действий
 */
@Composable
fun ActionWizardScreen(
    actionWizardController: ActionWizardController,
    actionWizardContextFactory: ActionWizardContextFactory,
    actionStepFactoryRegistry: ActionStepFactoryRegistry,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Получаем текущее состояние визарда
    val state by actionWizardController.wizardState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    state?.let { wizardState ->
        // Обработчик действий при нажатии на "Назад"
        val handleBack = {
            Timber.d("ActionWizardScreen: handleBack called")
            coroutineScope.launch {
                actionWizardController.processStepResult(null)
            }
        }

        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = wizardState.action?.actionTemplate?.name ?: "Выполнение действия",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = {
                        Timber.d("Close wizard button was clicked")
                        onCancel()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                }

                // Прогресс
                LinearProgressIndicator(
                    progress = { wizardState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )

                // Содержимое текущего шага
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val currentStep = wizardState.currentStep

                    if (currentStep == null || wizardState.isCompleted) {
                        // Итоговый экран
                        ActionSummaryScreen(
                            state = wizardState,
                            onComplete = {
                                Timber.d("Action complete button was clicked")
                                onComplete()
                            },
                            onBack = {
                                Timber.d("Back button was pressed in summary screen")
                                handleBack()
                            }
                        )
                    } else {
                        val action = wizardState.action
                        if (action != null) {
                            // Находим ActionStep для текущего шага визарда
                            val actionStep = findActionStepForWizardStep(action, currentStep.id)

                            if (actionStep != null) {
                                // Находим фабрику для типа объекта
                                val factory = actionStepFactoryRegistry.getFactory(actionStep.objectType)

                                if (factory != null) {
                                    // Создаем контекст для шага
                                    val context = actionWizardContextFactory.createContext(
                                        state = wizardState,
                                        onStepComplete = { result ->
                                            coroutineScope.launch {
                                                actionWizardController.processStepResult(result)
                                            }
                                        },
                                        onBack = { // Это должна быть лямбда без аргументов
                                            handleBack()
                                        },
                                        onSkip = { result ->
                                            // TODO: Реализовать пропуск шага
                                            coroutineScope.launch {
                                                // Заглушка, реализация будет добавлена позже
                                            }
                                        },
                                        onCancel = onCancel
                                    )

                                    // Создаем компонент шага
                                    factory.createComponent(
                                        step = actionStep,
                                        action = action,
                                        context = context
                                    )
                                } else {
                                    // Фабрика не найдена для типа объекта
                                    Text(
                                        text = "Нет компонента для типа объекта: ${actionStep.objectType}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                // ActionStep не найден для текущего шага
                                Text(
                                    text = "Шаг не найден: ${currentStep.id}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            // Действие не найдено в состоянии
                            Text(
                                text = "Действие не найдено",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Навигационные кнопки - только кнопка назад, если это возможно
                if (wizardState.currentStep != null && !wizardState.isCompleted && wizardState.canGoBack) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        OutlinedButton(
                            onClick = {
                                Timber.d("Back button was pressed in navigation")
                                handleBack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Назад")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Экран сводки действия
 */
@Composable
fun ActionSummaryScreen(
    state: ActionWizardState,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val action = state.action

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Проверьте информацию перед завершением",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (action != null) {
            Text(
                text = "Действие: ${action.actionTemplate.name}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Тип действия: ${wmsActionToString(action.wmsAction)}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Отображение результатов шагов
            for (result in state.results.entries) {
                when (val value = result.value) {
                    is TaskProduct -> {
                        Text(
                            text = "Товар: ${value.product.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Количество: ${value.quantity}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    is Pallet -> {
                        Text(
                            text = "Паллета: ${value.code}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Статус: ${if (value.isClosed) "Закрыта" else "Открыта"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    is BinX -> {
                        Text(
                            text = "Ячейка: ${value.code}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Зона: ${value.zone}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопки действий
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    Timber.d("Back button in summary screen clicked")
                    onBack()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Назад")
            }

            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f)
            ) {
                Text("Подтвердить")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
            }
        }
    }
}

/**
 * Находит шаг действия для шага визарда
 */
private fun findActionStepForWizardStep(
    action: PlannedAction,
    stepId: String
): ActionStep? {
    // Ищем в шагах хранения
    action.actionTemplate.storageSteps.find { it.id == stepId }?.let { return it }

    // Ищем в шагах размещения
    action.actionTemplate.placementSteps.find { it.id == stepId }?.let { return it }

    return null
}

/**
 * Преобразует WmsAction в строку
 */
@Composable
private fun wmsActionToString(action: WmsAction): String {
    return when (action) {
        WmsAction.PUT_INTO -> "Положить"
        WmsAction.TAKE_FROM -> "Взять"
        WmsAction.RECEIPT -> "Оприходовать"
        WmsAction.EXPENSE -> "Списать"
        WmsAction.RECOUNT -> "Пересчитать"
        WmsAction.USE -> "Использовать"
        WmsAction.ASSERT -> "Подтвердить"
    }
}