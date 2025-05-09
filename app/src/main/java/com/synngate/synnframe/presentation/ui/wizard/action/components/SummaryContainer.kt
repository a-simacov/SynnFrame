package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Контейнер для отображения итогового экрана визарда с результатами
 *
 * @param title Заголовок экрана
 * @param onBack Обработчик кнопки "Назад"
 * @param onComplete Обработчик кнопки "Завершить"
 * @param onRetry Обработчик кнопки "Повторить" (при ошибке)
 * @param isSending Флаг отправки данных
 * @param hasError Есть ли ошибка при отправке
 * @param content Содержимое экрана
 */
@Composable
fun SummaryContainer(
    title: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onRetry: () -> Unit,
    isSending: Boolean,
    hasError: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Основное содержимое
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Нижний блок с кнопками
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка "Назад"
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                enabled = !isSending
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Назад")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка "Завершить" или "Повторить"
            if (hasError) {
                // Кнопка "Повторить" при ошибке
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Повторить",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Повторить")
                }
            } else {
                // Кнопка "Завершить"
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Отправка...")
                    } else {
                        Text("Завершить")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Завершить"
                        )
                    }
                }
            }
        }
    }
}