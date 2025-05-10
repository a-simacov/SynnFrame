package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Контейнер для шага визарда
 *
 * @param state Состояние шага
 * @param step Объект шага
 * @param action Запланированное действие
 * @param onBack Обработчик перехода назад
 * @param onForward Обработчик перехода вперед
 * @param onCancel Обработчик отмены (не используется)
 * @param forwardEnabled Доступна ли кнопка "Вперед"
 * @param isProcessingGlobal Флаг глобальной обработки шага
 * @param isFirstStep Флаг, указывающий на первый шаг
 * @param content Содержимое шага
 */
@Composable
fun <T> StepContainer(
    state: StepViewState<T>,
    step: ActionStep,
    action: PlannedAction,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onCancel: () -> Unit, // Оставлен для совместимости, но не используется
    forwardEnabled: Boolean = state.data != null,
    isProcessingGlobal: Boolean = false,
    isFirstStep: Boolean = false,
    content: @Composable () -> Unit
) {
    // Используем отложенное отображение индикатора загрузки, чтобы избежать моргания
    var showLoading by remember { mutableStateOf(false) }
    var previousLoadingState by remember { mutableStateOf(false) }

    // Определяем, находится ли экран в состоянии загрузки
    // Учитываем как локальный флаг из StepViewState, так и глобальный из ActionWizardState
    val isReallyLoading = state.isLoading || isProcessingGlobal

    // Используем эффект для отложенного отображения индикатора загрузки
    LaunchedEffect(isReallyLoading) {
        if (isReallyLoading && !previousLoadingState) {
            // Если началась загрузка, ждем 300мс перед показом индикатора
            // Это предотвращает моргание при быстрых операциях
            delay(300)
            showLoading = isReallyLoading
        } else if (!isReallyLoading && previousLoadingState) {
            // Если загрузка завершилась, сразу убираем индикатор
            showLoading = false
        }
        previousLoadingState = isReallyLoading
    }

    // Логируем состояние загрузки для отладки
    if (isReallyLoading && showLoading) {
        Timber.d("StepContainer: Loading state - local: ${state.isLoading}, global: $isProcessingGlobal")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок
        StepTitle(step, action)

        // Если есть ошибка, отображаем её
        if (state.error != null) {
            ErrorMessage(state.error)
        }

        // Если идет загрузка и прошло достаточно времени для показа индикатора,
        // отображаем индикатор загрузки
        if (showLoading) {
            LoadingIndicator(
                modifier = Modifier.padding(16.dp)
            )
        } else {
            // Основное содержимое
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Нижний блок с кнопками
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Кнопка назад - отображаем только если не первый шаг
            if (!isFirstStep) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !isReallyLoading // Блокируем, если идет загрузка
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = "Назад",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Назад")
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            // Кнопка вперед - занимает все доступное пространство
            Button(
                onClick = onForward,
                enabled = forwardEnabled && !isReallyLoading, // Блокируем, если идет загрузка
                modifier = Modifier.weight(1f)
            ) {
                Text("Вперед")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "Вперед",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}