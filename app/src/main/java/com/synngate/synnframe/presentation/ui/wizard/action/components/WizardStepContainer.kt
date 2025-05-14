package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.pallet.PalletSelectionViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Улучшенный контейнер для шага визарда
 *
 * @param state Состояние шага
 * @param step Объект шага
 * @param action Запланированное действие
 * @param onBack Обработчик перехода назад
 * @param onForward Обработчик перехода вперед
 * @param onCancel Обработчик отмены (опционально)
 * @param forwardEnabled Доступна ли кнопка "Вперед"
 * @param isProcessingGlobal Флаг глобальной обработки шага
 * @param isFirstStep Флаг, указывающий на первый шаг
 * @param forwardText Текст кнопки перехода вперед
 * @param backText Текст кнопки возврата назад
 * @param content Содержимое шага
 */
@Composable
fun <T> WizardStepContainer(
    state: StepViewState<T>,
    step: ActionStep,
    action: PlannedAction,
    viewModel: BaseStepViewModel<T>,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onCancel: () -> Unit = {},
    forwardEnabled: Boolean = state.data != null,
    isProcessingGlobal: Boolean = false,
    isFirstStep: Boolean = false,
    forwardText: String = "",
    backText: String = "",
    content: @Composable () -> Unit
) {
    // Используем отложенное отображение индикатора загрузки
    var showLoading by remember { mutableStateOf(false) }
    var previousLoadingState by remember { mutableStateOf(false) }

    // Определяем, находится ли экран в состоянии загрузки
    val isReallyLoading = state.isLoading || isProcessingGlobal

    // Эффект для отложенного отображения индикатора загрузки
    LaunchedEffect(isReallyLoading) {
        if (isReallyLoading && !previousLoadingState) {
            // Если началась загрузка, ждем 300мс перед показом индикатора
            delay(300)
            showLoading = isReallyLoading
        } else if (!isReallyLoading && previousLoadingState) {
            // Если загрузка завершилась, сразу убираем индикатор
            showLoading = false
        }
        previousLoadingState = isReallyLoading
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Заголовок шага с информацией о действии
            WizardStepHeader(step)

            Spacer(modifier = Modifier.height(8.dp))

            // Сообщение об ошибке, если есть
            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                state.error?.let {
                    WizardStateMessage(
                        message = it,
                        type = StateType.ERROR,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Основное содержимое шага или индикатор загрузки
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (showLoading) {
                    WizardLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        content()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Панель с кнопками навигации
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Кнопка "Назад" (скрывается на первом шаге)
                if (!isFirstStep) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        enabled = !isReallyLoading // Блокируем, если идет загрузка
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(backText)
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Кнопка "Вперед"
                Button(
                    onClick = {
                        // ИСПРАВЛЕНО: Добавлена специальная обработка для разных типов ViewModel
                        // В зависимости от типа ViewModel вызываем соответствующий метод
                        when (viewModel) {
                            // Для PalletSelectionViewModel вызываем специальный метод
                            is PalletSelectionViewModel -> {
                                Timber.d("Вызываем manuallyCompleteStep для PalletSelectionViewModel")
                                viewModel.manuallyCompleteStep()
                            }
                            // Для остальных типов используем общий подход
                            else -> {
                                Timber.d("Используем стандартный onForward для ${viewModel.javaClass.simpleName}")

                                // Если есть данные, пытаемся вызвать validateAndCompleteIfValid
                                if (state.data != null) {
                                    // Проверяем, есть ли у ViewModel метод validateAndCompleteIfValid
                                    try {
                                        Timber.d("Вызываем validateAndCompleteIfValid с данными: ${state.data}")
                                        viewModel.validateAndCompleteIfValid(state.data)
                                    } catch (e: Exception) {
                                        Timber.e("Ошибка при вызове validateAndCompleteIfValid: ${e.message}")
                                        // Если возникла ошибка, используем стандартный onForward
                                        onForward()
                                    }
                                } else {
                                    // Если данных нет, просто вызываем onForward
                                    onForward()
                                }
                            }
                        }
                    },
                    enabled = forwardEnabled && !isReallyLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(forwardText)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Вперед",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Компонент для отображения заголовка шага
 */
@Composable
private fun WizardStepHeader(step: ActionStep) {
    Text(
        text = step.promptText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Контейнер для отображения итогового экрана визарда
 */
@Composable
fun WizardSummaryContainer(
    title: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onRetry: () -> Unit,
    isSending: Boolean,
    hasError: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showLoading by remember { mutableStateOf(false) }
    var previousLoadingState by remember { mutableStateOf(false) }

    // Эффект для отложенного отображения индикатора загрузки
    LaunchedEffect(isSending) {
        if (isSending && !previousLoadingState) {
            delay(300)
            showLoading = isSending
        } else if (!isSending && previousLoadingState) {
            showLoading = false
        }
        previousLoadingState = isSending
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Заголовок
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Основное содержимое
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (showLoading) {
                    WizardLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        message = "Отправка данных..."
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        content()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Нижний блок с кнопками
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Кнопка "Назад"
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !isSending && !showLoading
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = "Назад",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Назад")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Кнопка "Завершить" или "Повторить"
                if (hasError) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Повторить")
                    }
                } else {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                        enabled = !isSending && !showLoading
                    ) {
                        Text("Завершить")
                    }
                }
            }
        }
    }
}