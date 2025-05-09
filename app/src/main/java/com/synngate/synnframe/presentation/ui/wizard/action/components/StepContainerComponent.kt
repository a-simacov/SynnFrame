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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState

/**
 * Контейнер для шага визарда
 *
 * @param state Состояние шага
 * @param step Объект шага
 * @param action Запланированное действие
 * @param onBack Обработчик перехода назад
 * @param onForward Обработчик перехода вперед
 * @param onCancel Обработчик отмены
 * @param forwardEnabled Доступна ли кнопка "Вперед"
 * @param content Содержимое шага
 */
@Composable
fun <T> StepContainer(
    state: StepViewState<T>,
    step: ActionStep,
    action: PlannedAction,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onCancel: () -> Unit,
    forwardEnabled: Boolean = state.data != null,
    content: @Composable () -> Unit
) {
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

        // Если идет загрузка, отображаем индикатор
        if (state.isLoading) {
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
            // Кнопка назад
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.NavigateBefore,
                    contentDescription = "Назад",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Назад")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка отмены
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Отмена",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Отмена")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка вперед
            Button(
                onClick = onForward,
                enabled = forwardEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text("Вперед")
                Icon(
                    imageVector = Icons.Default.NavigateNext,
                    contentDescription = "Вперед",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}